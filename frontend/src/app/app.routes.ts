import { Routes } from '@angular/router';
import { Login } from './login/login';
import { AdminDashboard } from './admin/admin-dashboard';
import { SalonComponent } from './salon/salon.component';
import { CajaVentas } from './caja-ventas/caja-ventas';

export const routes: Routes = [
  { path: '', component: Login, title: 'Inicio de sesión | El Hornero' },
  {
    path: 'dashboard',
    component: AdminDashboard,
    title: 'Panel administrativo | El Hornero',
  },
  {
    path: 'salon',
    component: SalonComponent,
    title: 'Gestión de salón | El Hornero',
  },
  {
    path: 'caja-ventas',
    component: CajaVentas,
    title: 'Gestión de caja y ventas | El Hornero',
  },
  { path: 'admin', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
