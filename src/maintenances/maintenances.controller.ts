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
import { MaintenancesService } from './maintenances.service';
import { CreateMaintenanceDto } from './dto/create-maintenance.dto';
import { UpdateMaintenanceDto } from './dto/update-maintenance.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { CurrentUser } from '../common/decorators/current-user.decorator';

@ApiTags('Maintenances')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, RolesGuard)
@Controller('maintenances')
export class MaintenancesController {
  constructor(private readonly maintenancesService: MaintenancesService) {}

  @Post()
  @ApiOperation({ summary: 'Créer un entretien' })
  @ApiResponse({ status: 201, description: 'Entretien créé' })
  create(@Body() createMaintenanceDto: CreateMaintenanceDto, @CurrentUser() user: any) {
    return this.maintenancesService.create(createMaintenanceDto, user.userId, user.role);
  }

  @Get()
  @ApiOperation({ summary: 'Lister les entretiens' })
  @ApiResponse({ status: 200, description: 'Liste des entretiens' })
  findAll(@CurrentUser() user: any) {
    return this.maintenancesService.findAll(user.userId, user.role);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Récupérer un entretien' })
  @ApiResponse({ status: 200, description: 'Détails de l\'entretien' })
  findOne(@Param('id') id: string, @CurrentUser() user: any) {
    return this.maintenancesService.findOne(id, user.userId, user.role);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Modifier un entretien' })
  @ApiResponse({ status: 200, description: 'Entretien modifié' })
  update(
    @Param('id') id: string,
    @Body() updateMaintenanceDto: UpdateMaintenanceDto,
    @CurrentUser() user: any,
  ) {
    return this.maintenancesService.update(id, updateMaintenanceDto, user.userId, user.role);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Supprimer un entretien' })
  @ApiResponse({ status: 200, description: 'Entretien supprimé' })
  remove(@Param('id') id: string, @CurrentUser() user: any) {
    return this.maintenancesService.remove(id, user.userId, user.role);
  }
}
