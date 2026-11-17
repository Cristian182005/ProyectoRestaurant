import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
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
    console.log('Login submit', {
      username: this.username(),
      password: this.password(),
      rememberMe: this.rememberMe()
    });
  }

  onRecover(): void {
    console.log('Recuperación de acceso solicitada');
  }
}
