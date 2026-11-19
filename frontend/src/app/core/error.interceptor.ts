import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { mensajeDeApiError } from './api-error';
import { NotificationService } from './notification.service';
import { SesionService } from './sesion.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificaciones = inject(NotificationService);
  const sesion = inject(SesionService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      const mensaje = mensajeDeApiError(error);
      if (error.status === 401) {
        sesion.cerrarSesion();
        void router.navigate(['/']);
      }
      notificaciones.notificar(mensaje, error.status === 401 ? 'advertencia' : 'error');
      return throwError(() => error);
    })
  );
};
