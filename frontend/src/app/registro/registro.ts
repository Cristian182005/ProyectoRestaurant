import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, RegistroRequest } from '../core/auth.service';

@Component({
  selector: 'app-registro',
  imports: [FormsModule],
  templateUrl: './registro.html',
  styleUrls: ['../login/login.css', './registro.css'],
})
export class Registro {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  /** El backend solo admite estos roles en el registro público (no ADMINISTRADOR). */
  readonly roles = ['MOZO', 'CAJERO'];

  readonly email = signal('');
  readonly password = signal('');
  readonly confirmar = signal('');
  readonly telefono = signal('');
  readonly direccion = signal('');
  readonly rol = signal('MOZO');
  readonly showPassword = signal(false);
  readonly cargando = signal(false);
  readonly errorMessage = signal('');

  onSubmit(): void {
    const email = this.email().trim();
    const password = this.password();
    const confirmar = this.confirmar();
    const telefono = this.telefono().trim();
    const direccion = this.direccion().trim();
    const rol = this.rol();

    if (!email || !password || !confirmar || !rol) {
      this.errorMessage.set('Completa los campos obligatorios para continuar.');
      return;
    }
    if (password.length < 6) {
      this.errorMessage.set('La contraseña debe tener al menos 6 caracteres.');
      return;
    }
    if (password !== confirmar) {
      this.errorMessage.set('Las contraseñas no coinciden.');
      return;
    }

    const datos: RegistroRequest = { email, password, rol };
    if (telefono) {
      datos.telefono = telefono;
    }
    if (direccion) {
      datos.direccion = direccion;
    }

    this.errorMessage.set('');
    this.cargando.set(true);

    this.auth.registrarse(datos).subscribe({
      next: () => {
        this.cargando.set(false);
        void this.router.navigate(['/'], { queryParams: { registro: 'ok' } });
      },
      error: (mensaje: string) => {
        this.cargando.set(false);
        this.errorMessage.set(mensaje);
      },
    });
  }

  irALogin(): void {
    void this.router.navigate(['/']);
  }

  onRolChange(rol: string): void {
    this.rol.set(rol);
  }
}
