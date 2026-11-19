import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Registro } from './registro/registro';
import { AdminDashboard } from './admin/admin-dashboard';
import { SalonComponent } from './salon/salon.component';
import { CajaVentas } from './caja-ventas/caja-ventas';
import { Pedidos } from './pedidos/pedidos';
import { Inventario } from './inventario/inventario';
import { ProductosRecetas } from './productos-recetas/productos-recetas';
import { Reportes } from './reportes/reportes';
import { UsuariosRoles } from './usuarios-roles/usuarios-roles';
import { authGuard, loginGuard, roleGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: Login,
    canActivate: [loginGuard],
    title: 'Inicio de sesión | El Hornero',
  },
  {
    path: 'registro',
    component: Registro,
    canActivate: [loginGuard],
    title: 'Registro de usuario | El Hornero',
  },
  {
    path: 'dashboard',
    component: AdminDashboard,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'dashboard', roles: ['ADMINISTRADOR', 'MOZO', 'CAJERO'] },
    title: 'Panel administrativo | El Hornero',
  },
  {
    path: 'salon',
    component: SalonComponent,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'salon', roles: ['ADMINISTRADOR', 'MOZO'] },
    title: 'Gestión de salón | El Hornero',
  },
  {
    path: 'caja-ventas',
    component: CajaVentas,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'caja', roles: ['ADMINISTRADOR', 'CAJERO'] },
    title: 'Gestión de caja y ventas | El Hornero',
  },
  {
    path: 'pedidos',
    component: Pedidos,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'pedidos', roles: ['ADMINISTRADOR', 'MOZO'] },
    title: 'Gestión de pedidos | El Hornero',
  },
  {
    path: 'inventario',
    component: Inventario,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'inventario', roles: ['ADMINISTRADOR'] },
    title: 'Inventario | El Hornero',
  },
  {
    path: 'productos-recetas',
    component: ProductosRecetas,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'productos', roles: ['ADMINISTRADOR', 'MOZO'] },
    title: 'Productos y recetas | El Hornero',
  },
  {
    path: 'reportes',
    component: Reportes,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'reportes', roles: ['ADMINISTRADOR', 'CAJERO'] },
    title: 'Reportes | El Hornero',
  },
  {
    path: 'usuarios-roles',
    component: UsuariosRoles,
    canActivate: [authGuard, roleGuard],
    data: { modulo: 'usuarios', roles: ['ADMINISTRADOR'] },
    title: 'Usuarios y roles | El Hornero',
  },
  { path: 'admin', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
