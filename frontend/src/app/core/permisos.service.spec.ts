import { TestBed } from '@angular/core/testing';
import { MODULOS, PermisosService } from './permisos.service';
import { SesionService } from './sesion.service';

const sesionDe = (rol: string) => ({
  token: 'token-prueba',
  email: `${rol.toLowerCase()}@elhornero.test`,
  rol,
  expiraEn: '2999-01-01T00:00:00.000Z',
  id: 1,
  empleadoId: 1,
});

describe('PermisosService', () => {
  let sesion: SesionService;
  let permisos: PermisosService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    sesion = TestBed.inject(SesionService);
    permisos = TestBed.inject(PermisosService);
    sesion.cerrarSesion();
  });

  afterEach(() => sesion.cerrarSesion());

  it('niega el acceso a todos los módulos sin sesión', () => {
    expect(MODULOS.some((modulo) => permisos.puede(modulo))).toBeFalse();
  });

  it('permite todos los módulos al administrador', () => {
    sesion.guardar(sesionDe('ADMINISTRADOR'), false);
    expect(MODULOS.every((modulo) => permisos.puede(modulo))).toBeTrue();
  });

  it('limita al mozo a resumen, salón, pedidos y productos', () => {
    sesion.guardar(sesionDe('MOZO'), false);

    expect(permisos.puede('dashboard')).toBeTrue();
    expect(permisos.puede('salon')).toBeTrue();
    expect(permisos.puede('pedidos')).toBeTrue();
    expect(permisos.puede('productos')).toBeTrue();
    expect(permisos.puede('caja')).toBeFalse();
    expect(permisos.puede('inventario')).toBeFalse();
    expect(permisos.puede('reportes')).toBeFalse();
    expect(permisos.puede('usuarios')).toBeFalse();
  });

  it('limita al cajero a resumen, caja y reportes', () => {
    sesion.guardar(sesionDe('CAJERO'), false);

    expect(permisos.puede('dashboard')).toBeTrue();
    expect(permisos.puede('caja')).toBeTrue();
    expect(permisos.puede('reportes')).toBeTrue();
    expect(permisos.puede('salon')).toBeFalse();
    expect(permisos.puede('pedidos')).toBeFalse();
    expect(permisos.puede('inventario')).toBeFalse();
    expect(permisos.puede('productos')).toBeFalse();
    expect(permisos.puede('usuarios')).toBeFalse();
  });

  it('expone los roles autorizados por módulo', () => {
    expect(permisos.rolesDe('usuarios')).toEqual(['ADMINISTRADOR']);
    expect(permisos.rolesDe('caja')).toEqual(['ADMINISTRADOR', 'CAJERO']);
  });
});
