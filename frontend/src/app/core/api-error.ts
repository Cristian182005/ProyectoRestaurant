import { HttpErrorResponse } from '@angular/common/http';

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  mensaje: string;
  ruta: string;
}

export function mensajeDeApiError(error: HttpErrorResponse): string {
  const cuerpo = error.error as Partial<ApiError> | null;
  if (cuerpo && typeof cuerpo.mensaje === 'string' && cuerpo.mensaje.trim()) {
    return cuerpo.mensaje;
  }
  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.';
  }
  if (error.status === 401) {
    return 'Tu sesión ha caducado. Inicia sesión nuevamente.';
  }
  if (error.status === 403) {
    return 'No tienes permisos para realizar esta operación.';
  }
  if (error.status === 409) {
    return 'Conflicto con el estado actual del recurso.';
  }
  if (error.status === 400) {
    return 'Los datos enviados no son válidos. Revisa el formulario.';
  }
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
}
