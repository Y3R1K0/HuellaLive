import { ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class AdoptionsService {
  constructor(private prisma: PrismaService) {}

  async requestAdoption(humanId: string, animalId: string) {
    const human = await this.prisma.user.findUnique({ where: { id: humanId } });
    if (!human || human.role !== 'HUMAN') {
      throw new ForbiddenException('Solo una cuenta humana puede solicitar una adopcion');
    }
    const animal = await this.prisma.animal.findUnique({
      where: { id: animalId },
      include: { shelter: true, adoptedBy: { select: { name: true } } },
    });
    if (!animal) throw new NotFoundException('Animal no encontrado');
    if (!animal.shelterId) throw new ConflictException('Este animal no esta disponible para adopcion');
    if (animal.status === 'ADOPTED') throw new ConflictException('Este animal ya fue adoptado');

    const activeRequest = await this.prisma.adoptionRequest.findFirst({
      where: {
        animalId,
        humanId,
        status: { in: ['PENDING', 'ACCEPTED'] },
      },
    });
    if (activeRequest) {
      throw new ConflictException('Ya tienes una solicitud activa para este animal');
    }

    const { start, end } = this.currentLimaDay();
    const requestsToday = await this.prisma.adoptionRequest.count({
      where: { animalId, createdAt: { gte: start, lt: end } },
    });
    if (requestsToday >= 10) {
      throw new ConflictException('Este animal alcanzo el limite de 10 solicitudes de hoy. Intenta nuevamente manana');
    }

    const request = await this.prisma.adoptionRequest.create({
      data: { animalId, humanId, shelterId: animal.shelterId },
      include: this.requestInclude(),
    });
    if (animal.shelter) {
      await this.prisma.notification.create({
        data: {
          userId: animal.shelter.userId,
          type: 'ADOPTION_REQUEST',
          title: 'Nueva solicitud de adopcion',
          body: `${request.human?.name ?? 'Una persona'} quiere adoptar a ${animal.name}.`,
          data: { requestId: request.id, animalId: animal.id },
        },
      });
    }
    return request;
  }

  async getRequestState(humanId: string, animalId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: humanId } });
    if (!user || user.role !== 'HUMAN') {
      return {
        hasActiveRequest: false,
        activeStatus: null,
        requestsToday: 0,
        dailyLimit: 10,
        dailyLimitReached: false,
      };
    }

    const { start, end } = this.currentLimaDay();
    const [activeRequest, requestsToday] = await Promise.all([
      this.prisma.adoptionRequest.findFirst({
        where: {
          animalId,
          humanId,
          status: { in: ['PENDING', 'ACCEPTED'] },
        },
        orderBy: { createdAt: 'desc' },
      }),
      this.prisma.adoptionRequest.count({
        where: { animalId, createdAt: { gte: start, lt: end } },
      }),
    ]);

    return {
      hasActiveRequest: Boolean(activeRequest),
      activeStatus: activeRequest?.status ?? null,
      requestsToday,
      dailyLimit: 10,
      dailyLimitReached: requestsToday >= 10,
    };
  }

  async listForShelter(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    return this.prisma.adoptionRequest.findMany({
      where: { shelterId: shelter.id, status: { notIn: ['REJECTED', 'COMPLETED'] } },
      orderBy: { createdAt: 'desc' },
      include: this.requestInclude(),
    });
  }

  async updateRequest(userId: string, requestId: string, status: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    const request = await this.prisma.adoptionRequest.findUnique({
      where: { id: requestId },
      include: { animal: true },
    });
    if (!request) throw new NotFoundException('Solicitud no encontrada');
    if (request.shelterId !== shelter.id) throw new ForbiddenException('Sin permisos');

    if (status === 'ACCEPTED') {
      const chat = await this.prisma.chat.upsert({
        where: { humanId_shelterId: { humanId: request.humanId, shelterId: shelter.id } },
        update: {},
        create: { humanId: request.humanId, shelterId: shelter.id },
      });
      const updated = await this.prisma.adoptionRequest.update({
        where: { id: requestId },
        data: { status, chatId: chat.id },
        include: this.requestInclude(),
      });
      await this.prisma.notification.create({
        data: {
          userId: request.humanId,
          type: 'ADOPTION_ACCEPTED',
          title: 'Solicitud aceptada',
          body: `El albergue acepto tu solicitud para adoptar a ${request.animal.name}. Ya pueden coordinar por chat.`,
          data: { requestId, animalId: request.animalId, chatId: chat.id },
        },
      });
      return updated;
    }

    if (status !== 'REJECTED' && status !== 'PENDING') {
      throw new ForbiddenException('Estado de solicitud no valido');
    }

    if (status === 'REJECTED') {
      const chat = await this.createAdoptionMessage(
        shelter.id,
        shelter.userId,
        request.humanId,
        `Tu solicitud para adoptar a ${request.animal.name} fue rechazada porque el animal ya fue adoptado.`,
      );
      const updated = await this.prisma.adoptionRequest.update({
        where: { id: requestId },
        data: { status, chatId: chat.id },
        include: this.requestInclude(),
      });
      await this.prisma.notification.create({
        data: {
          userId: request.humanId,
          type: 'ADOPTION_REJECTED',
          title: 'Solicitud actualizada',
          body: `La solicitud para adoptar a ${request.animal.name} fue cerrada.`,
          data: { requestId, animalId: request.animalId, chatId: chat.id },
        },
      });
      return updated;
    }

    return this.prisma.adoptionRequest.update({
      where: { id: requestId },
      data: { status },
      include: this.requestInclude(),
    });
  }

  async completeTransfer(userId: string, requestId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    const request = await this.prisma.adoptionRequest.findUnique({
      where: { id: requestId },
      include: { animal: true },
    });
    if (!request) throw new NotFoundException('Solicitud no encontrada');
    if (request.shelterId !== shelter.id || request.status !== 'ACCEPTED') {
      throw new ForbiddenException('La adopcion aun no esta aceptada');
    }

    const rejectedRequests = await this.prisma.adoptionRequest.findMany({
      where: { animalId: request.animalId, id: { not: requestId }, status: { not: 'REJECTED' } },
      include: { animal: true },
    });
    for (const rejected of rejectedRequests) {
      const chat = await this.createAdoptionMessage(
        shelter.id,
        shelter.userId,
        rejected.humanId,
        `Tu solicitud para adoptar a ${rejected.animal.name} fue rechazada porque el animal ya fue adoptado.`,
      );
      await this.prisma.adoptionRequest.update({
        where: { id: rejected.id },
        data: { status: 'REJECTED', chatId: chat.id },
      });
      await this.prisma.notification.create({
        data: {
          userId: rejected.humanId,
          type: 'ADOPTION_REJECTED',
          title: 'Solicitud actualizada',
          body: `La solicitud para adoptar a ${rejected.animal.name} fue cerrada porque el animal ya encontro un hogar.`,
          data: { requestId: rejected.id, animalId: rejected.animalId, chatId: chat.id },
        },
      });
    }

    const animal = await this.prisma.animal.update({
      where: { id: request.animalId },
      data: {
        adoptedById: request.humanId,
        shelterId: null,
        status: 'ADOPTED',
      },
      include: { card: true, adoptedBy: { select: { id: true, name: true, avatarUrl: true } } },
    });

    const chat = await this.createAdoptionMessage(
      shelter.id,
      shelter.userId,
      request.humanId,
      `${request.animal.name} ahora forma parte de tu familia. El perfil, la cartilla y sus recuerdos ya estan disponibles en tu cuenta.`,
    );
    await this.prisma.adoptionRequest.update({
      where: { id: request.id },
      data: { chatId: chat.id, status: 'COMPLETED' },
    });
    await this.prisma.notification.create({
      data: {
        userId: request.humanId,
        type: 'ADOPTION_TRANSFERRED',
        title: 'Adopcion completada',
        body: `${request.animal.name} ya aparece en tu familia.`,
        data: { animalId: request.animalId, chatId: chat.id },
      },
    });
    return animal;
  }

  async listTransferableForChat(userId: string, chatId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('Solo el albergue puede transferir animales');
    const chat = await this.prisma.chat.findUnique({ where: { id: chatId } });
    if (!chat || chat.shelterId !== shelter.id) throw new ForbiddenException('Sin permisos');

    return this.prisma.adoptionRequest.findMany({
      where: {
        chatId,
        shelterId: shelter.id,
        humanId: chat.humanId,
        status: 'ACCEPTED',
        animal: { shelterId: shelter.id, status: { not: 'ADOPTED' } },
      },
      orderBy: { createdAt: 'desc' },
      include: this.requestInclude(),
    });
  }

  async transferFromChat(userId: string, chatId: string, animalId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('Solo el albergue puede transferir animales');
    const chat = await this.prisma.chat.findUnique({ where: { id: chatId } });
    if (!chat || chat.shelterId !== shelter.id) throw new ForbiddenException('Sin permisos');

    const request = await this.prisma.adoptionRequest.findFirst({
      where: {
        chatId,
        animalId,
        humanId: chat.humanId,
        shelterId: shelter.id,
        status: 'ACCEPTED',
      },
    });
    if (!request) throw new NotFoundException('No hay una solicitud aceptada para este animal');
    return this.completeTransfer(userId, request.id);
  }

  private requestInclude() {
    return {
      animal: { select: { id: true, name: true, species: true, photoUrl: true, status: true } },
      human: { select: { id: true, name: true, avatarUrl: true } },
      chat: true,
    };
  }

  private currentLimaDay() {
    const now = new Date();
    const limaNow = new Date(now.getTime() - 5 * 60 * 60 * 1000);
    const start = new Date(Date.UTC(
      limaNow.getUTCFullYear(),
      limaNow.getUTCMonth(),
      limaNow.getUTCDate(),
      5,
    ));
    const end = new Date(start.getTime() + 24 * 60 * 60 * 1000);
    return { start, end };
  }

  private async createAdoptionMessage(shelterId: string, shelterUserId: string, humanId: string, content: string) {
    const chat = await this.prisma.chat.upsert({
      where: { humanId_shelterId: { humanId, shelterId } },
      update: {},
      create: { humanId, shelterId },
    });
    await this.prisma.message.create({
      data: {
        chatId: chat.id,
        senderId: shelterUserId,
        content,
      },
    });
    return chat;
  }
}
