import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { routes } from '../app.routes';
import { authGuard, loginGuard, roleGuard } from './auth.guard';
import { SesionService } from './sesion.service';

const SESION = {
  token: 'token-prueba',
  email: 'cajero@elhornero.test',
  rol: 'CAJERO',
  expiraEn: '2999-01-01T00:00:00.000Z',
  id: 1,
  empleadoId: 4,
};

describe('Guards de autenticación y roles', () => {
  let sesion: SesionService;

  const ejecutar = (guard: CanActivateFn, data: Record<string, unknown> = {}) =>
    TestBed.runInInjectionContext(() =>
      guard(
        { data } as unknown as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot
      )
    );

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    sesion = TestBed.inject(SesionService);
    sesion.cerrarSesion();
  });

  afterEach(() => sesion.cerrarSesion());

  it('authGuard redirige al login sin sesión y permite el acceso con sesión', () => {
    expect(ejecutar(authGuard) instanceof UrlTree).toBeTrue();

    sesion.guardar(SESION, false);
    expect(ejecutar(authGuard)).toBeTrue();
  });

  it('roleGuard permite el rol declarado y redirige al dashboard si no coincide', () => {
    sesion.guardar(SESION, false);

    expect(ejecutar(roleGuard, { roles: ['ADMINISTRADOR', 'CAJERO'] })).toBeTrue();

    const resultado = ejecutar(roleGuard, { roles: ['ADMINISTRADOR'] });
    expect(resultado instanceof UrlTree).toBeTrue();
    expect((resultado as UrlTree).toString()).toBe('/dashboard');
  });

  it('roleGuard usa el módulo declarado en data', () => {
    sesion.guardar(SESION, false);

    expect(ejecutar(roleGuard, { modulo: 'caja' })).toBeTrue();

    const resultado = ejecutar(roleGuard, { modulo: 'inventario' });
    expect(resultado instanceof UrlTree).toBeTrue();
    expect((resultado as UrlTree).toString()).toBe('/dashboard');
  });

  it('roleGuard redirige al login sin sesión', () => {
    const resultado = ejecutar(roleGuard, { roles: ['CAJERO'] });
    expect(resultado instanceof UrlTree).toBeTrue();
    expect((resultado as UrlTree).toString()).toBe('/');
  });

  it('loginGuard lleva al dashboard cuando ya hay sesión', () => {
    expect(ejecutar(loginGuard)).toBeTrue();

    sesion.guardar(SESION, false);
    const resultado = ejecutar(loginGuard);
    expect(resultado instanceof UrlTree).toBeTrue();
    expect((resultado as UrlTree).toString()).toBe('/dashboard');
  });

  it('las rutas declaran los roles esperados', () => {
    const router = TestBed.inject(Router);
    const configuracion = router.config;
    const ruta = (path: string) => configuracion.find((item) => item.path === path);

    expect(ruta('dashboard')?.data?.['modulo']).toBe('dashboard');
    expect(ruta('salon')?.data?.['modulo']).toBe('salon');
    expect(ruta('caja-ventas')?.data?.['modulo']).toBe('caja');
    expect(ruta('pedidos')?.data?.['modulo']).toBe('pedidos');
    expect(ruta('inventario')?.data?.['modulo']).toBe('inventario');
    expect(ruta('productos-recetas')?.data?.['modulo']).toBe('productos');
    expect(ruta('reportes')?.data?.['modulo']).toBe('reportes');
    expect(ruta('usuarios-roles')?.data?.['modulo']).toBe('usuarios');
    expect(ruta('usuarios-roles')?.data?.['roles']).toEqual(['ADMINISTRADOR']);
    expect(ruta('caja-ventas')?.data?.['roles']).toEqual(['ADMINISTRADOR', 'CAJERO']);
    expect(ruta('pedidos')?.data?.['roles']).toEqual(['ADMINISTRADOR', 'MOZO']);
    expect(ruta('inventario')?.data?.['roles']).toEqual(['ADMINISTRADOR']);
  });
});
