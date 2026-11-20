import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { SesionService } from './sesion.service';

export interface AuthResponse {
  token: string;
  tipo: string;
  email: string;
  rol: string;
  expiraEn: string;
  id: number;
  empleadoId: number | null;
}

const API_AUTH = `${environment.apiUrl}/auth`;

/** Cliente HTTP de autenticación: únicamente login (no hay registro público). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sesion = inject(SesionService);

  login(email: string, password: string, recordar: boolean): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_AUTH}/login`, { email, password }).pipe(
      tap((respuesta) =>
        this.sesion.guardar(
          {
            token: respuesta.token,
            email: respuesta.email,
            rol: respuesta.rol,
            expiraEn: respuesta.expiraEn,
            id: respuesta.id,
            empleadoId: respuesta.empleadoId,
          },
          recordar
        )
      ),
      catchError((error) => throwError(() => this.mensajeError(error)))
    );
  }

  /** Traduce los errores del backend (ApiError.mensaje) a texto para la UI. */
  private mensajeError(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Error inesperado. Inténtalo de nuevo.';
    }

    if (error.status === 0) {
      return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
    }

    const cuerpo = error.error as { mensaje?: unknown } | null;
    if (cuerpo && typeof cuerpo.mensaje === 'string' && cuerpo.mensaje.trim()) {
      return cuerpo.mensaje;
    }

    if (error.status === 401) {
      return 'Correo o contraseña incorrectos.';
    }

    return `No se pudo completar la operación (error ${error.status}).`;
  }
}
