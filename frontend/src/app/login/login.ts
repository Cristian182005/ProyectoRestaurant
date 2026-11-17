import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private readonly router = inject(Router);

  readonly username = signal('');
  readonly password = signal('');
  readonly rememberMe = signal(false);
  readonly showPassword = signal(false);
  readonly errorMessage = signal('');

  onSubmit(): void {
    if (!this.username().trim() || !this.password().trim()) {
      this.errorMessage.set('Ingresa tus credenciales para continuar.');
      return;
    }

    this.errorMessage.set('');
    void this.router.navigate(['/dashboard']);
  }

  onRecover(): void {
    console.log('Recuperación de acceso solicitada');
  }
}
