import { Routes } from '@angular/router';
import { Login } from './login/login';
import { AdminDashboard } from './admin/admin-dashboard';

export const routes: Routes = [
  { path: '', component: Login, title: 'Inicio de sesión | El Hornero' },
  {
    path: 'dashboard',
    component: AdminDashboard,
    title: 'Panel administrativo | El Hornero',
  },
  { path: 'admin', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
