import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'exito' | 'error' | 'advertencia' | 'info';

export interface Notificacion {
  mensaje: string;
  tipo: TipoNotificacion;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly mensaje = signal('');
  readonly tipo = signal<TipoNotificacion>('info');
  readonly visible = signal(false);

  private temporizador: ReturnType<typeof setTimeout> | null = null;

  notificar(mensaje: string, tipo: TipoNotificacion = 'info'): void {
    if (!mensaje) {
      return;
    }
    this.mensaje.set(mensaje);
    this.tipo.set(tipo);
    this.visible.set(true);
    if (this.temporizador) {
      clearTimeout(this.temporizador);
    }
    this.temporizador = setTimeout(() => this.ocultar(), 3500);
  }

  exito(mensaje: string): void {
    this.notificar(mensaje, 'exito');
  }

  error(mensaje: string): void {
    this.notificar(mensaje, 'error');
  }

  advertencia(mensaje: string): void {
    this.notificar(mensaje, 'advertencia');
  }

  info(mensaje: string): void {
    this.notificar(mensaje, 'info');
  }

  ocultar(): void {
    if (this.temporizador) {
      clearTimeout(this.temporizador);
      this.temporizador = null;
    }
    this.visible.set(false);
    this.mensaje.set('');
  }
}
