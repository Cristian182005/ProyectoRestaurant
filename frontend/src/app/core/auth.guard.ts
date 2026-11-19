import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Modulo, PermisosService } from './permisos.service';
import { SesionService } from './sesion.service';

/** Exige sesión activa para acceder al panel; si no, vuelve al login. */
export const authGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.logueado() ? true : router.createUrlTree(['/']);
};

/** Exige sesión activa y permiso para el módulo de `data['modulo']`. */
export const roleGuard: CanActivateFn = (route) => {
  const sesion = inject(SesionService);
  const permisos = inject(PermisosService);
  const router = inject(Router);

  if (!sesion.logueado()) {
    return router.createUrlTree(['/']);
  }

  const modulo = route.data['modulo'] as Modulo | undefined;
  if (modulo) {
    return permisos.puede(modulo) ? true : router.createUrlTree(['/dashboard']);
  }

  const roles = route.data['roles'];
  if (!Array.isArray(roles) || roles.length === 0) {
    return true;
  }

  const rol = sesion.rol();
  return rol && roles.includes(rol) ? true : router.createUrlTree(['/dashboard']);
};

/** Evita ver el login con sesión activa: entra directo al panel. */
export const loginGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  const router = inject(Router);
  return sesion.logueado() ? router.createUrlTree(['/dashboard']) : true;
};
