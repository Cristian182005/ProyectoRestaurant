import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SesionService } from './sesion.service';

/** Exige sesión activa para acceder al panel; si no, vuelve al login. */
export const authGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.logueado() ? true : router.createUrlTree(['/']);
};

/** Evita ver el login con sesión activa: entra directo al panel. */
export const loginGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.logueado() ? router.createUrlTree(['/dashboard']) : true;
};
