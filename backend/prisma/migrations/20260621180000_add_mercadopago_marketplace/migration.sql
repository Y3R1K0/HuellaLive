ALTER TABLE "shelter_profiles"
ADD COLUMN "mercadoPagoAccessToken" TEXT,
ADD COLUMN "mercadoPagoPublicKey" TEXT,
ADD COLUMN "mercadoPagoUserId" TEXT,
ADD COLUMN "mercadoPagoRefreshToken" TEXT,
ADD COLUMN "mercadoPagoConnectedAt" TIMESTAMP(3),
ADD COLUMN "mercadoPagoTokenExpiresAt" TIMESTAMP(3);

ALTER TABLE "donations"
ADD COLUMN "marketplaceFee" DOUBLE PRECISION,
ADD COLUMN "mercadopagoPreferenceId" TEXT,
ADD COLUMN "mercadopagoPaymentId" TEXT;
