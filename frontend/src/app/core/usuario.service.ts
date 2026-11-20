import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { mensajeDeApiError } from './api-error';

/** Únicos roles que admite el sistema. */
export type RolUsuario = 'ADMINISTRADOR' | 'CAJERO' | 'MOZO';

export const ROLES_PERMITIDOS: RolUsuario[] = ['ADMINISTRADOR', 'CAJERO', 'MOZO'];

/** Usuario tal como lo devuelve el backend (nunca incluye la contraseña). */
export interface Usuario {
  id: number;
  nombre: string | null;
  email: string;
  rol: RolUsuario;
  telefono: string | null;
  direccion: string | null;
}

export interface RegistroUsuario {
  email: string;
  password: string;
  nombre?: string | null;
  telefono?: string | null;
  direccion?: string | null;
  rol: RolUsuario;
}

export interface ActualizarUsuario {
  nombre?: string | null;
  email?: string;
  telefono?: string | null;
  direccion?: string | null;
}

const API_USUARIOS = `${environment.apiUrl}/usuarios`;

/**
 * Cliente HTTP de la gestión de usuarios. Todos los endpoints exigen el rol
 * ADMINISTRADOR en el backend; el token lo añade el auth.interceptor.
 */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);

  listar(buscar?: string): Observable<Usuario[]> {
    return this.http
      .get<Usuario[]>(API_USUARIOS, { params: buscar ? { buscar } : undefined })
      .pipe(catchError((error) => throwError(() => this.mensajeError(error))));
  }

  obtener(id: number): Observable<Usuario> {
    return this.http
      .get<Usuario>(`${API_USUARIOS}/${id}`)
      .pipe(catchError((error) => throwError(() => this.mensajeError(error))));
  }

  crear(datos: RegistroUsuario): Observable<Usuario> {
    return this.http
      .post<Usuario>(API_USUARIOS, datos)
      .pipe(catchError((error) => throwError(() => this.mensajeError(error))));
  }

  actualizar(id: number, datos: ActualizarUsuario): Observable<Usuario> {
    return this.http
      .put<Usuario>(`${API_USUARIOS}/${id}`, datos)
      .pipe(catchError((error) => throwError(() => this.mensajeError(error))));
  }

  cambiarRol(id: number, rol: RolUsuario): Observable<Usuario> {
    return this.http
      .put<Usuario>(`${API_USUARIOS}/${id}/rol`, { rol })
      .pipe(catchError((error) => throwError(() => this.mensajeError(error))));
  }

  cambiarPassword(id: number, password: string): Observable<Usuario> {
    return this.http
      .put<Usuario>(`${API_USUARIOS}/${id}/password`, { password })
      .pipe(catchError((error) => throwError(() => this.mensajeError(error))));
  }

  private mensajeError(error: unknown): string {
    return error instanceof HttpErrorResponse ? mensajeDeApiError(error) : 'Error inesperado.';
  }
}
