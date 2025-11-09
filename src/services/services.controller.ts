import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiResponse, ApiNotFoundResponse, ApiUnauthorizedResponse } from '@nestjs/swagger';
import { ServicesService } from './services.service';
import { CreateServiceDto } from './dto/create-service.dto';
import { UpdateServiceDto } from './dto/update-service.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles, UserRole } from '../common/decorators/roles.decorator';

@ApiTags('Services')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('services')
export class ServicesController {
  constructor(private readonly service: ServicesService) {}

  @Post()
  @Roles(UserRole.propGarage)
  @ApiOperation({ summary: 'Créer un nouveau service' })
  @ApiResponse({ status: 201, description: 'Service créé avec succès' })
  @ApiNotFoundResponse({ description: 'Garage non trouvé' })
  @ApiUnauthorizedResponse({ description: 'Non autorisé' })
  create(@Body() createDto: CreateServiceDto) {
    return this.service.create(createDto);
  }

  @Get()
  @ApiOperation({ summary: 'Lister tous les services' })
  @ApiResponse({ status: 200, description: 'Liste des services' })
  findAll() {
    return this.service.findAll();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtenir un service par ID' })
  @ApiResponse({ status: 200, description: 'Service trouvé' })
  @ApiNotFoundResponse({ description: 'Service non trouvé' })
  findOne(@Param('id') id: string) {
    return this.service.findOne(id);
  }

  @Patch(':id')
  @Roles(UserRole.propGarage)
  @ApiOperation({ summary: 'Modifier un service' })
  @ApiResponse({ status: 200, description: 'Service modifié' })
  @ApiNotFoundResponse({ description: 'Service non trouvé' })
  @ApiUnauthorizedResponse({ description: 'Non autorisé' })
  update(@Param('id') id: string, @Body() updateDto: UpdateServiceDto) {
    return this.service.update(id, updateDto);
  }

  @Delete(':id')
  @Roles(UserRole.propGarage)
  @ApiOperation({ summary: 'Supprimer un service' })
  @ApiResponse({ status: 200, description: 'Service supprimé' })
  @ApiNotFoundResponse({ description: 'Service non trouvé' })
  @ApiUnauthorizedResponse({ description: 'Non autorisé' })
  remove(@Param('id') id: string) {
    return this.service.remove(id);
  }

  @Get('garage/:garageId')
  @ApiOperation({ summary: 'Lister les services d\'un garage' })
  @ApiResponse({ status: 200, description: 'Services pour le garage' })
  findByGarage(@Param('garageId') garageId: string) {
    return this.service.findByGarage(garageId);
  }
}
