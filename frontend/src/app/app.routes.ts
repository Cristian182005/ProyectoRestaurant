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
import { authGuard, loginGuard } from './core/auth.guard';

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
    canActivate: [authGuard],
    title: 'Panel administrativo | El Hornero',
  },
  {
    path: 'salon',
    component: SalonComponent,
    canActivate: [authGuard],
    title: 'Gestión de salón | El Hornero',
  },
  {
    path: 'caja-ventas',
    component: CajaVentas,
    canActivate: [authGuard],
    title: 'Gestión de caja y ventas | El Hornero',
  },
  {
    path: 'pedidos',
    component: Pedidos,
    canActivate: [authGuard],
    title: 'Gestión de pedidos | El Hornero',
  },
  {
    path: 'inventario',
    component: Inventario,
    canActivate: [authGuard],
    title: 'Inventario | El Hornero',
  },
  {
    path: 'productos-recetas',
    component: ProductosRecetas,
    canActivate: [authGuard],
    title: 'Productos y recetas | El Hornero',
  },
  {
    path: 'reportes',
    component: Reportes,
    canActivate: [authGuard],
    title: 'Reportes | El Hornero',
  },
  {
    path: 'usuarios-roles',
    component: UsuariosRoles,
    canActivate: [authGuard],
    title: 'Usuarios y roles | El Hornero',
  },
  { path: 'admin', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
