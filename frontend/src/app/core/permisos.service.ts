import { inject, Injectable } from '@angular/core';
import { SesionService } from './sesion.service';

export type Modulo =
  | 'dashboard'
  | 'salon'
  | 'pedidos'
  | 'caja'
  | 'inventario'
  | 'productos'
  | 'reportes'
  | 'usuarios';

export type RolPermiso = 'ADMINISTRADOR' | 'MOZO' | 'CAJERO';

export const ROLES_POR_MODULO: Record<Modulo, RolPermiso[]> = {
  dashboard: ['ADMINISTRADOR', 'MOZO', 'CAJERO'],
  salon: ['ADMINISTRADOR', 'MOZO'],
  pedidos: ['ADMINISTRADOR', 'MOZO'],
  caja: ['ADMINISTRADOR', 'CAJERO'],
  inventario: ['ADMINISTRADOR'],
  productos: ['ADMINISTRADOR', 'MOZO'],
  reportes: ['ADMINISTRADOR', 'CAJERO'],
  usuarios: ['ADMINISTRADOR'],
};

export const MODULOS: Modulo[] = [
  'dashboard',
  'salon',
  'pedidos',
  'caja',
  'inventario',
  'productos',
  'reportes',
  'usuarios',
];

@Injectable({ providedIn: 'root' })
export class PermisosService {
  private readonly sesion = inject(SesionService);

  puede(modulo?: Modulo | null): boolean {
    const rol = this.sesion.rol();
    return !!modulo && !!rol && ROLES_POR_MODULO[modulo].includes(rol as RolPermiso);
  }

  rolesDe(modulo: Modulo): RolPermiso[] {
    return [...ROLES_POR_MODULO[modulo]];
  }
}
