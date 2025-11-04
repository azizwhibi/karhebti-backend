import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  UseGuards,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { CarsService } from './cars.service';
import { CreateCarDto } from './dto/create-car.dto';
import { UpdateCarDto } from './dto/update-car.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('Cars')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('cars')
export class CarsController {
  constructor(private readonly carsService: CarsService) {}

  @Post()
  @ApiOperation({ summary: 'Créer une voiture' })
  @ApiResponse({ status: 201, description: 'Voiture créée' })
  create(@Body() createCarDto: CreateCarDto, @CurrentUser() user: any) {
    return this.carsService.create(createCarDto, user.userId);
  }

  @Get()
  @ApiOperation({ summary: 'Lister les voitures' })
  @ApiResponse({ status: 200, description: 'Liste des voitures (utilisateur: ses voitures uniquement)' })
  findAll(@CurrentUser() user: any) {
    return this.carsService.findAll(user.userId, user.role);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Récupérer une voiture' })
  @ApiResponse({ status: 200, description: 'Détails de la voiture' })
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  @ApiResponse({ status: 404, description: 'Voiture non trouvée' })
  findOne(@Param('id') id: string, @CurrentUser() user: any) {
    return this.carsService.findOne(id, user.userId, user.role);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Modifier une voiture' })
  @ApiResponse({ status: 200, description: 'Voiture modifiée' })
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  @ApiResponse({ status: 404, description: 'Voiture non trouvée' })
  update(
    @Param('id') id: string,
    @Body() updateCarDto: UpdateCarDto,
    @CurrentUser() user: any,
  ) {
    return this.carsService.update(id, updateCarDto, user.userId, user.role);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Supprimer une voiture' })
  @ApiResponse({ status: 200, description: 'Voiture supprimée' })
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  @ApiResponse({ status: 404, description: 'Voiture non trouvée' })
  remove(@Param('id') id: string, @CurrentUser() user: any) {
    return this.carsService.remove(id, user.userId, user.role);
  }
}
