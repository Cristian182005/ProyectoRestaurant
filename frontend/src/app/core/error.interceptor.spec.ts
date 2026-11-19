import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { errorInterceptor } from './error.interceptor';
import { NotificationService } from './notification.service';
import { SesionService } from './sesion.service';

describe('errorInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;
  let notificaciones: NotificationService;
  let sesion: SesionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'dashboard', children: [] },
          { path: '', children: [] },
        ]),
      ],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    notificaciones = TestBed.inject(NotificationService);
    sesion = TestBed.inject(SesionService);
    sesion.cerrarSesion();
  });

  afterEach(() => {
    notificaciones.ocultar();
    sesion.cerrarSesion();
    http.verify();
  });

  it('muestra el mensaje del ApiError y re-lanza el error', () => {
    let capturado: unknown = null;
    cliente.get('/api/conflicto').subscribe({
      error: (error: unknown) => (capturado = error),
    });

    http.expectOne('/api/conflicto').flush(
      { mensaje: 'El pedido ya fue entregado' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(capturado).not.toBeNull();
    expect(notificaciones.visible()).toBeTrue();
    expect(notificaciones.mensaje()).toBe('El pedido ya fue entregado');
    expect(notificaciones.tipo()).toBe('error');
  });

  it('cierra la sesión y vuelve al login ante un 401', async () => {
    sesion.guardar(
      {
        token: 'token',
        email: 'cajero@elhornero.test',
        rol: 'CAJERO',
        expiraEn: '2999-01-01T00:00:00.000Z',
        id: 1,
        empleadoId: 3,
      },
      false
    );
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/dashboard');

    cliente.get('/api/pedidos').subscribe({ error: () => undefined });
    http.expectOne('/api/pedidos').flush(
      { mensaje: 'Token vencido' },
      { status: 401, statusText: 'Unauthorized' }
    );
    await Promise.resolve();

    expect(sesion.token()).toBeNull();
    expect(notificaciones.tipo()).toBe('advertencia');
    expect(notificaciones.mensaje()).toBe('Token vencido');
    expect(router.url).toBe('/');
  });
});
