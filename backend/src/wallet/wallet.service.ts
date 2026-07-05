import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHmac, timingSafeEqual } from 'crypto';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class WalletService {
  constructor(
    private prisma: PrismaService,
    private config: ConfigService,
  ) {}

  async donate(humanId: string, shelterId: string, dto: any) {
    const amount = Number(Number(dto.amount ?? 0).toFixed(2));
    if (amount <= 0) throw new ForbiddenException('Monto invalido');
    const shelter = await this.prisma.shelterProfile.findUnique({
      where: { id: shelterId },
      include: { user: true },
    });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
    if (!shelter.mercadoPagoAccessToken) {
      throw new ForbiddenException('Este albergue aun no conecto Mercado Pago');
    }

    const developerCutRate = Number(this.config.get<string>('DONATION_DEVELOPER_CUT_RATE') ?? '0.25');
    const developerCut = Number((amount * developerCutRate).toFixed(2));
    const donation = await this.prisma.donation.create({
      data: {
        humanId,
        shelterId,
        amount,
        currency: 'PEN',
        developerCut,
        shelterCut: Number((amount * (1 - developerCutRate)).toFixed(2)),
        marketplaceFee: developerCut,
        status: 'PENDING',
      },
    });

    const preference = await this.createMercadoPagoPreference(shelter.mercadoPagoAccessToken, {
      donationId: donation.id,
      shelterName: shelter.user.name,
      amount,
      marketplaceFee: developerCut,
    });
    const updatedDonation = await this.prisma.donation.update({
      where: { id: donation.id },
      data: { mercadopagoPreferenceId: preference.id },
    });

    return {
      donation: updatedDonation,
      checkoutUrl: preference.init_point,
      sandboxCheckoutUrl: preference.sandbox_init_point,
      preferenceId: preference.id,
    };
  }

  async handleMercadoPagoNotification(query: any, body: any) {
    const topic = query.topic ?? query.type ?? body?.type;
    const paymentId = query.id ?? query['data.id'] ?? body?.data?.id ?? body?.id;
    if (topic !== 'payment' || !paymentId) return { received: true };

    const donationFromQuery = query.donationId ? await this.prisma.donation.findUnique({
      where: { id: String(query.donationId) },
      include: { shelter: true },
    }) : null;

    const accessToken = donationFromQuery?.shelter.mercadoPagoAccessToken
      ?? this.config.get<string>('MERCADOPAGO_ACCESS_TOKEN');
    if (!accessToken) return { received: true };

    const payment = await this.fetchMercadoPagoPayment(accessToken, String(paymentId));
    const donationId = donationFromQuery?.id ?? payment.external_reference ?? payment.metadata?.donation_id;
    if (!donationId) return { received: true };

    const status = payment.status === 'approved'
      ? 'COMPLETED'
      : payment.status === 'rejected'
        ? 'REJECTED'
        : payment.status === 'cancelled'
          ? 'CANCELLED'
          : 'PENDING';

    await this.prisma.donation.updateMany({
      where: { id: donationId },
      data: { status, mercadopagoPaymentId: String(paymentId) },
    });

    return { received: true, status };
  }

  async createMercadoPagoConnectUrl(userId: string) {
    const shelter = await this.requireShelter(userId);
    const clientId = this.config.get<string>('MERCADOPAGO_CLIENT_ID');
    const redirectUri = this.mercadoPagoRedirectUri();
    if (!clientId) throw new ForbiddenException('Mercado Pago Client ID no esta configurado');

    const url = new URL('https://auth.mercadopago.com.pe/authorization');
    url.searchParams.set('client_id', clientId);
    url.searchParams.set('response_type', 'code');
    url.searchParams.set('platform_id', 'mp');
    url.searchParams.set('redirect_uri', redirectUri);
    url.searchParams.set('state', this.signMercadoPagoState(shelter.id));

    return {
      url: url.toString(),
      connected: Boolean(shelter.mercadoPagoAccessToken),
    };
  }

  async handleMercadoPagoOAuthCallback(query: any) {
    if (query.error) {
      throw new ForbiddenException(String(query.error_description ?? query.error));
    }
    const code = String(query.code ?? '');
    const state = String(query.state ?? '');
    if (!code || !state) throw new ForbiddenException('Callback de Mercado Pago incompleto');

    const shelterId = this.verifyMercadoPagoState(state);
    const clientId = this.config.get<string>('MERCADOPAGO_CLIENT_ID');
    const clientSecret = this.config.get<string>('MERCADOPAGO_CLIENT_SECRET');
    const redirectUri = this.mercadoPagoRedirectUri();
    if (!clientId || !clientSecret) throw new ForbiddenException('OAuth Mercado Pago no esta configurado');

    const response = await fetch('https://api.mercadopago.com/oauth/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        accept: 'application/json',
      },
      body: JSON.stringify({
        grant_type: 'authorization_code',
        client_id: clientId,
        client_secret: clientSecret,
        code,
        redirect_uri: redirectUri,
      }),
    });
    const data = await response.json();
    if (!response.ok) {
      throw new ForbiddenException(data?.message ?? 'No se pudo conectar Mercado Pago');
    }

    const expiresIn = Number(data.expires_in ?? 0);
    const expiresAt = expiresIn > 0 ? new Date(Date.now() + expiresIn * 1000) : null;
    await this.prisma.shelterProfile.update({
      where: { id: shelterId },
      data: {
        mercadoPagoAccessToken: data.access_token,
        mercadoPagoPublicKey: data.public_key,
        mercadoPagoUserId: data.user_id ? String(data.user_id) : null,
        mercadoPagoRefreshToken: data.refresh_token,
        mercadoPagoConnectedAt: new Date(),
        mercadoPagoTokenExpiresAt: expiresAt,
      },
    });

    return { connected: true };
  }

  async getShelterDashboard(userId: string) {
    const shelter = await this.requireShelter(userId);
    const [direct, withdrawals] = await Promise.all([
      this.prisma.donation.aggregate({
        where: { shelterId: shelter.id, status: 'COMPLETED' },
        _sum: { amount: true, shelterCut: true },
      }),
      this.prisma.shelterWithdrawal.findMany({
        where: { shelterId: shelter.id },
        orderBy: { createdAt: 'desc' },
      }),
    ]);
    const totalNet = direct._sum.shelterCut ?? 0;
    const reserved = withdrawals
      .filter((item) => item.status === 'PENDING' || item.status === 'APPROVED' || item.status === 'PAID')
      .reduce((sum, item) => sum + item.amount, 0);

    return {
      currency: 'PEN',
      mercadoPagoConnected: Boolean(shelter.mercadoPagoAccessToken),
      mercadoPagoConnectedAt: shelter.mercadoPagoConnectedAt,
      conversionNote: 'Los montos directos se registran en soles cuando Mercado Pago aprueba el pago.',
      totals: {
        received: direct._sum.amount ?? 0,
        direct: direct._sum.amount ?? 0,
        throughAnimals: 0,
        netForShelter: totalNet,
        availableToWithdraw: Math.max(0, totalNet - reserved),
        withdrawn: withdrawals.filter((item) => item.status === 'PAID').reduce((sum, item) => sum + item.amount, 0),
      },
      byAnimal: [],
      withdrawals,
    };
  }

  async requestWithdrawal(userId: string, dto: any) {
    const shelter = await this.requireShelter(userId);
    const amount = Math.floor(Number(dto.amount ?? 0));
    if (amount <= 0) throw new ForbiddenException('Monto de retiro invalido');
    const required = ['accountHolder', 'documentNumber', 'bankName', 'accountType', 'accountNumber', 'cci'];
    if (required.some((field) => !String(dto[field] ?? '').trim())) {
      throw new ForbiddenException('Completa todos los datos bancarios');
    }
    const active = await this.prisma.shelterWithdrawal.findFirst({
      where: { shelterId: shelter.id, status: { in: ['PENDING', 'APPROVED'] } },
    });
    if (active) throw new ConflictException('Ya existe una solicitud de retiro activa');
    const dashboard = await this.getShelterDashboard(userId);
    if (amount > dashboard.totals.availableToWithdraw) {
      throw new ForbiddenException('El monto supera tu saldo disponible');
    }

    return this.prisma.shelterWithdrawal.create({
      data: {
        shelterId: shelter.id,
        amount,
        accountHolder: String(dto.accountHolder).trim(),
        documentNumber: String(dto.documentNumber).trim(),
        bankName: String(dto.bankName).trim(),
        accountType: String(dto.accountType).trim(),
        accountNumber: String(dto.accountNumber).trim(),
        cci: String(dto.cci).trim(),
      },
    });
  }

  private async requireShelter(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('Solo los albergues pueden acceder a esta informacion');
    return shelter;
  }

  private async createMercadoPagoPreference(
    accessToken: string,
    payload: { donationId: string; shelterName: string; amount: number; marketplaceFee: number },
  ) {
    const publicBaseUrl = this.config.get<string>('PUBLIC_BASE_URL')?.replace(/\/$/, '');
    const appScheme = this.config.get<string>('APP_DEEP_LINK_SCHEME') ?? 'huellalive';
    const notificationUrl = publicBaseUrl
      ? `${publicBaseUrl}/payments/mercadopago/webhook?donationId=${encodeURIComponent(payload.donationId)}`
      : undefined;

    const response = await fetch('https://api.mercadopago.com/checkout/preferences', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        items: [
          {
            title: `Donacion a ${payload.shelterName}`,
            description: 'Aporte voluntario a un albergue verificado en HuellaLive',
            quantity: 1,
            currency_id: 'PEN',
            unit_price: payload.amount,
          },
        ],
        external_reference: payload.donationId,
        metadata: {
          donation_id: payload.donationId,
        },
        marketplace_fee: payload.marketplaceFee,
        back_urls: {
          success: `${appScheme}://payments/mercadopago/success`,
          pending: `${appScheme}://payments/mercadopago/pending`,
          failure: `${appScheme}://payments/mercadopago/failure`,
        },
        auto_return: 'approved',
        notification_url: notificationUrl,
        statement_descriptor: 'HUELLALIVE',
      }),
    });

    const data = await response.json();
    if (!response.ok) {
      throw new ForbiddenException(data?.message ?? 'No se pudo crear el pago en Mercado Pago');
    }
    return data;
  }

  private async fetchMercadoPagoPayment(accessToken: string, paymentId: string) {
    const response = await fetch(`https://api.mercadopago.com/v1/payments/${paymentId}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    const data = await response.json();
    if (!response.ok) {
      throw new ForbiddenException(data?.message ?? 'No se pudo consultar el pago en Mercado Pago');
    }
    return data;
  }

  private mercadoPagoRedirectUri() {
    const configured = this.config.get<string>('MERCADOPAGO_REDIRECT_URI');
    if (configured) return configured;
    const publicBaseUrl = this.config.get<string>('PUBLIC_BASE_URL')?.replace(/\/$/, '');
    if (!publicBaseUrl) throw new ForbiddenException('PUBLIC_BASE_URL no esta configurado');
    return `${publicBaseUrl}/payments/mercadopago/oauth/callback`;
  }

  private signMercadoPagoState(shelterId: string) {
    const secret = this.config.get<string>('JWT_SECRET') ?? 'huellalive';
    const signature = createHmac('sha256', secret).update(shelterId).digest('hex');
    return Buffer.from(JSON.stringify({ shelterId, signature })).toString('base64url');
  }

  private verifyMercadoPagoState(state: string) {
    const secret = this.config.get<string>('JWT_SECRET') ?? 'huellalive';
    const parsed = JSON.parse(Buffer.from(state, 'base64url').toString('utf8')) as {
      shelterId?: string;
      signature?: string;
    };
    if (!parsed.shelterId || !parsed.signature) throw new ForbiddenException('State invalido');
    const expected = createHmac('sha256', secret).update(parsed.shelterId).digest('hex');
    const received = Buffer.from(parsed.signature);
    const expectedBuffer = Buffer.from(expected);
    if (received.length !== expectedBuffer.length || !timingSafeEqual(received, expectedBuffer)) {
      throw new ForbiddenException('State invalido');
    }
    return parsed.shelterId;
  }
}
