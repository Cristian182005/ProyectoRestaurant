import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly email = signal('');
  readonly password = signal('');
  readonly rememberMe = signal(true);
  readonly showPassword = signal(false);
  readonly cargando = signal(false);
  readonly errorMessage = signal('');
  readonly aviso = signal('');

  onSubmit(): void {
    const email = this.email().trim();
    const password = this.password();

    if (!email || !password) {
      this.errorMessage.set('Ingresa tu correo y contraseña para continuar.');
      return;
    }

    this.errorMessage.set('');
    this.aviso.set('');
    this.cargando.set(true);

    this.auth.login(email, password, this.rememberMe()).subscribe({
      next: () => {
        this.cargando.set(false);
        void this.router.navigate(['/dashboard']);
      },
      error: (mensaje: string) => {
        this.cargando.set(false);
        this.errorMessage.set(mensaje);
      },
    });
  }

  onRecover(): void {
    this.aviso.set('La recuperación de acceso estará disponible próximamente.');
    this.errorMessage.set('');
  }
}
