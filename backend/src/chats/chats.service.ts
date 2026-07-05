import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class ChatsService {
  constructor(private prisma: PrismaService) {}

  async listChats(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    return this.prisma.chat.findMany({
      where: shelter ? { shelterId: shelter.id } : { humanId: userId },
      orderBy: { createdAt: 'desc' },
      include: this.chatInclude(),
    });
  }

  async getOrCreateChat(humanId: string, shelterId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { id: shelterId } });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
    const acceptedAdoption = await this.prisma.adoptionRequest.findFirst({
      where: { humanId, shelterId, status: 'ACCEPTED' },
    });
    if (!acceptedAdoption) {
      throw new ForbiddenException('El chat se habilita cuando el albergue acepta una solicitud de adopcion');
    }
    return this.prisma.chat.upsert({
      where: { humanId_shelterId: { humanId, shelterId } },
      update: {},
      create: { humanId, shelterId },
      include: this.chatInclude(),
    });
  }

  async listMessages(userId: string, chatId: string) {
    await this.ensureParticipant(userId, chatId);
    return this.prisma.message.findMany({
      where: { chatId },
      orderBy: { createdAt: 'asc' },
      include: { sender: { select: { id: true, name: true, avatarUrl: true, role: true } } },
    });
  }

  async sendMessage(userId: string, chatId: string, dto: any) {
    await this.ensureParticipant(userId, chatId);
    if (!dto.content && !dto.mediaUrl) throw new ForbiddenException('El mensaje necesita texto o imagen');
    return this.prisma.message.create({
      data: {
        chatId,
        senderId: userId,
        content: dto.content,
        mediaUrl: dto.mediaUrl,
      },
      include: { sender: { select: { id: true, name: true, avatarUrl: true, role: true } } },
    });
  }

  async deleteChat(userId: string, chatId: string) {
    await this.ensureParticipant(userId, chatId);
    await this.deleteChatRecord(chatId);
    return { deleted: true };
  }

  async deleteChatIfEmpty(userId: string, chatId: string) {
    await this.ensureParticipant(userId, chatId);
    const messages = await this.prisma.message.count({ where: { chatId } });
    if (messages > 0) return { deleted: false };
    const linkedAdoptions = await this.prisma.adoptionRequest.count({ where: { chatId } });
    if (linkedAdoptions > 0) return { deleted: false };
    await this.deleteChatRecord(chatId);
    return { deleted: true };
  }

  async ensureParticipant(userId: string, chatId: string) {
    const chat = await this.prisma.chat.findUnique({ where: { id: chatId }, include: { shelter: true } });
    if (!chat) throw new NotFoundException('Chat no encontrado');
    if (chat.humanId !== userId && chat.shelter.userId !== userId) {
      throw new ForbiddenException('No perteneces a este chat');
    }
    return chat;
  }

  private chatInclude() {
    return {
      human: { select: { id: true, name: true, avatarUrl: true } },
      shelter: { include: { user: { select: { id: true, name: true, avatarUrl: true } } } },
      messages: { orderBy: { createdAt: 'desc' as const }, take: 1 },
    };
  }

  private async deleteChatRecord(chatId: string) {
    await this.prisma.$transaction([
      this.prisma.adoptionRequest.updateMany({
        where: { chatId },
        data: { chatId: null },
      }),
      this.prisma.message.deleteMany({ where: { chatId } }),
      this.prisma.chat.delete({ where: { id: chatId } }),
    ]);
  }
}
