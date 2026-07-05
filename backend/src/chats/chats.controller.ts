import { Body, Controller, Delete, Get, Param, Post, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { ChatsService } from './chats.service';

@Controller('chats')
@UseGuards(JwtAuthGuard)
export class ChatsController {
  constructor(private readonly chatsService: ChatsService) {}

  @Get()
  list(@Request() req: any) {
    return this.chatsService.listChats(req.user.id);
  }

  @Post(':shelterId')
  start(@Request() req: any, @Param('shelterId') shelterId: string) {
    return this.chatsService.getOrCreateChat(req.user.id, shelterId);
  }

  @Get(':chatId/messages')
  messages(@Request() req: any, @Param('chatId') chatId: string) {
    return this.chatsService.listMessages(req.user.id, chatId);
  }

  @Post(':chatId/messages')
  send(@Request() req: any, @Param('chatId') chatId: string, @Body() dto: any) {
    return this.chatsService.sendMessage(req.user.id, chatId, dto);
  }

  @Delete(':chatId')
  delete(@Request() req: any, @Param('chatId') chatId: string) {
    return this.chatsService.deleteChat(req.user.id, chatId);
  }

  @Delete(':chatId/empty')
  deleteIfEmpty(@Request() req: any, @Param('chatId') chatId: string) {
    return this.chatsService.deleteChatIfEmpty(req.user.id, chatId);
  }
}
