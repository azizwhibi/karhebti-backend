import { Controller, Get, Post, Body, Patch, Param, Delete, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiResponse, ApiNotFoundResponse, ApiUnauthorizedResponse } from '@nestjs/swagger';
import { GaragesService } from './garages.service';
import { CreateGarageDto } from './dto/create-garage.dto';
import { UpdateGarageDto } from './dto/update-garage.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles, UserRole } from '../common/decorators/roles.decorator';

@ApiTags('Garages')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('garages')
export class GaragesController {
  constructor(private readonly service: GaragesService) {}

  @Post()
  @Roles(UserRole.propGarage)
  @ApiOperation({ summary: 'Créer un nouveau garage' })
  @ApiResponse({ status: 201, description: 'Garage créé avec succès' })
  @ApiUnauthorizedResponse({ description: 'Non autorisé' })
  create(@Body() createDto: CreateGarageDto) {
    return this.service.create(createDto);
  }

  @Get()
  @ApiOperation({ summary: 'Lister tous les garages' })
  @ApiResponse({ status: 200, description: 'Liste des garages' })
  findAll() {
    return this.service.findAll();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Afficher un garage par ID' })
  @ApiResponse({ status: 200, description: 'Garage trouvé' })
  @ApiNotFoundResponse({ description: 'Garage non trouvé' })
  findOne(@Param('id') id: string) {
    return this.service.findOne(id);
  }

  @Patch(':id')
  @Roles(UserRole.propGarage)
  @ApiOperation({ summary: 'Modifier un garage' })
  @ApiResponse({ status: 200, description: 'Garage mis à jour' })
  @ApiNotFoundResponse({ description: 'Garage non trouvé' })
  @ApiUnauthorizedResponse({ description: 'Non autorisé' })
  update(@Param('id') id: string, @Body() updateDto: UpdateGarageDto) {
    return this.service.update(id, updateDto);
  }

  @Delete(':id')
  @Roles(UserRole.propGarage)
  @ApiOperation({ summary: 'Supprimer un garage' })
  @ApiResponse({ status: 200, description: 'Garage supprimé' })
  @ApiNotFoundResponse({ description: 'Garage non trouvé' })
  @ApiUnauthorizedResponse({ description: 'Non autorisé' })
  remove(@Param('id') id: string) {
    return this.service.remove(id);
  }
}
