import { Injectable, computed, signal } from '@angular/core';

export interface Sesion {
  token: string;
  email: string;
  rol: string;
  expiraEn: string;
}

const CLAVE_SESION = 'elhornero.sesion';

/**
 * Estado de la sesión del usuario (JWT). No depende de HttpClient para que
 * pueda usarse desde el sidebar y los guards sin requerir ese provider.
 */
@Injectable({ providedIn: 'root' })
export class SesionService {
  readonly token = signal<string | null>(null);
  readonly email = signal<string | null>(null);
  readonly rol = signal<string | null>(null);

  readonly logueado = computed(() => this.token() !== null);

  constructor() {
    this.restaurar();
  }

  guardar(sesion: Sesion, recordar: boolean): void {
    this.limpiarAlmacen();
    const storage = recordar ? localStorage : sessionStorage;
    storage.setItem(CLAVE_SESION, JSON.stringify(sesion));

    this.token.set(sesion.token);
    this.email.set(sesion.email);
    this.rol.set(sesion.rol);
  }

  cerrarSesion(): void {
    this.limpiarAlmacen();
    this.token.set(null);
    this.email.set(null);
    this.rol.set(null);
  }

  private restaurar(): void {
    const crudo =
      localStorage.getItem(CLAVE_SESION) ?? sessionStorage.getItem(CLAVE_SESION);
    if (!crudo) {
      return;
    }

    try {
      const sesion = JSON.parse(crudo) as Sesion;
      if (!sesion.token || !sesion.email || !sesion.rol) {
        this.limpiarAlmacen();
        return;
      }

      const expiracion = Date.parse(sesion.expiraEn ?? '');
      if (Number.isFinite(expiracion) && expiracion <= Date.now()) {
        // Token vencido: se limpia la sesión.
        this.limpiarAlmacen();
        return;
      }

      this.token.set(sesion.token);
      this.email.set(sesion.email);
      this.rol.set(sesion.rol);
    } catch {
      this.limpiarAlmacen();
    }
  }

  private limpiarAlmacen(): void {
    localStorage.removeItem(CLAVE_SESION);
    sessionStorage.removeItem(CLAVE_SESION);
  }
}
