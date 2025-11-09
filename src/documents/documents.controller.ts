import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { DocumentsService } from './documents.service';
import { CreateDocumentDto } from './dto/create-document.dto';
import { UpdateDocumentDto } from './dto/update-document.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('Documents')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('documents')
export class DocumentsController {
  constructor(private readonly service: DocumentsService) {}

  @Post()
  create(@Body() createDto: CreateDocumentDto, @CurrentUser() user: any) {
    return this.service.create(createDto, user.userId, user.role);
  }

  @Get()
  findAll(@CurrentUser() user: any) {
    return this.service.findAll(user.userId, user.role);
  }

  @Get(':id')
  findOne(@Param('id') id: string, @CurrentUser() user: any) {
    return this.service.findOne(id, user.userId, user.role);
  }

  @Patch(':id')
  update(@Param('id') id: string, @Body() updateDto: UpdateDocumentDto, @CurrentUser() user: any) {
    return this.service.update(id, updateDto, user.userId, user.role);
  }

  @Delete(':id')
  remove(@Param('id') id: string, @CurrentUser() user: any) {
    return this.service.remove(id, user.userId, user.role);
  }
}
