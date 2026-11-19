import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Inventario } from './inventario';
import { SesionService } from '../core/sesion.service';

const INSUMOS = [
  {
    id: 1,
    nombre: 'Bife ancho crudo',
    descripcion: 'Carne vacuna',
    stockActual: 18.6,
    stockMinimo: 20,
    unidadMedida: 'kg',
    imagen: null,
  },
  {
    id: 2,
    nombre: 'Lomo fino',
    descripcion: 'Carne vacuna',
    stockActual: 12,
    stockMinimo: 5,
    unidadMedida: 'kg',
    imagen: null,
  },
];

const PROVEEDOR = {
  id: 9,
  nombre: 'Distribuidora Sur',
  ruc: '20123456789',
  email: 'ventas@proveedor.test',
  telefono: '999888777',
  direccion: 'Lima',
};

describe('Inventario', () => {
  let fixture: ComponentFixture<Inventario>;
  let componente: Inventario;
  let http: HttpTestingController;
  let sesion: SesionService;

  const responderCargaInicial = (insumos = INSUMOS) => {
    http.expectOne((req) => req.url.endsWith('/insumos') && req.method === 'GET').flush(insumos);
    http
      .expectOne((req) => req.url.endsWith('/insumos/stock-bajo') && req.method === 'GET')
      .flush([insumos[0]]);
    http.expectOne((req) => req.url.endsWith('/mermas') && req.method === 'GET').flush([
      {
        id: 4,
        insumoId: 1,
        insumo: { id: 1, nombre: 'Bife ancho crudo' },
        cantidad: 0.6,
        motivo: 'Merma',
        observacion: 'Corte',
        fechaRegistro: '2026-11-19T09:15:00',
      },
    ]);
    http.expectOne((req) => req.url.endsWith('/ingresos') && req.method === 'GET').flush([
      {
        id: 3,
        fechaIngreso: '2026-11-19T08:30:00',
        proveedorId: 9,
        empleadoId: 3,
        detalles: [{ id: 7, insumoId: 2, cantidad: 5 }],
      },
    ]);
    http
      .expectOne((req) => req.url.endsWith('/proveedores') && req.method === 'GET')
      .flush([PROVEEDOR]);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Inventario],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionService);
    sesion.guardar(
      {
        token: 'token',
        email: 'admin@elhornero.test',
        rol: 'ADMINISTRADOR',
        expiraEn: '2999-01-01T00:00:00.000Z',
        id: 1,
        empleadoId: 3,
      },
      false
    );
    fixture = TestBed.createComponent(Inventario);
    componente = fixture.componentInstance;
  });

  afterEach(() => {
    sesion.cerrarSesion();
    http.verify();
  });

  it('carga insumos, mermas, ingresos y proveedores del backend', () => {
    fixture.detectChanges();
    responderCargaInicial();
    fixture.detectChanges();

    expect(componente.insumos().length).toBe(2);
    expect(componente.insumos()[0].codigo).toBe('INS-1');
    expect(componente.insumos()[0].estado).toBe('CRÍTICO');
    expect(componente.movimientos().length).toBe(2);
    expect(componente.movimientos()[0].variacion).toBe('-0.6');
    expect(componente.proveedores()[0].nombre).toBe('Distribuidora Sur');
    expect(fixture.nativeElement.textContent).toContain('Bife ancho crudo');
    expect(fixture.nativeElement.textContent).toContain('Valor de inventario');
  });

  it('filtra los insumos por término de búsqueda', () => {
    fixture.detectChanges();
    responderCargaInicial();

    componente.searchTerm.set('lomo');
    fixture.detectChanges();

    const filas = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(filas.length).toBe(1);
    expect(filas[0].textContent).toContain('Lomo fino');
  });

  it('registra una salida como merma y recarga el inventario', () => {
    fixture.detectChanges();
    responderCargaInicial();

    componente.formCodigo.set('INS-1');
    componente.formTipo.set('salida');
    componente.formCantidad.set(0.6);
    componente.formMotivo.set('Corte');
    componente.registrarMovimiento();

    const peticion = http.expectOne(
      (req) => req.url.endsWith('/mermas') && req.method === 'POST'
    );
    expect(peticion.request.body).toEqual({
      insumoId: 1,
      cantidad: 0.6,
      motivo: 'Corte',
      observacion: 'Corte',
      empleadoId: 3,
    });
    peticion.flush({ id: 5, insumoId: 1, cantidad: 0.6, motivo: 'Corte' });
    responderCargaInicial([{ ...INSUMOS[0], stockActual: 18 }, INSUMOS[1]]);

    expect(componente.modalAbierto()).toBeFalse();
    expect(componente.notificaciones.mensaje()).toBe('Salida registrada: Bife ancho crudo.');
    expect(componente.insumos()[0].existencia).toBe(18);
  });

  it('registra una entrada con proveedor y recarga el inventario', () => {
    fixture.detectChanges();
    responderCargaInicial();

    componente.formCodigo.set('INS-2');
    componente.formTipo.set('entrada');
    componente.formProveedorId.set(9);
    componente.formCantidad.set(4);
    componente.registrarMovimiento();

    const peticion = http.expectOne(
      (req) => req.url.endsWith('/ingresos') && req.method === 'POST'
    );
    expect(peticion.request.body).toEqual({
      proveedorId: 9,
      empleadoId: 3,
      detalles: [{ insumoId: 2, cantidad: 4 }],
    });
    peticion.flush({ id: 4, fechaIngreso: '2026-11-19T10:00:00', proveedorId: 9, empleadoId: 3, detalles: [] });
    responderCargaInicial([INSUMOS[0], { ...INSUMOS[1], stockActual: 16 }]);

    expect(componente.notificaciones.mensaje()).toBe('Entrada registrada: Lomo fino.');
    expect(componente.insumos()[1].existencia).toBe(16);
  });

  it('bloquea una salida mayor al stock disponible', () => {
    fixture.detectChanges();
    responderCargaInicial();

    componente.formCodigo.set('INS-1');
    componente.formTipo.set('salida');
    componente.formCantidad.set(99);
    componente.registrarMovimiento();

    expect(componente.formError()).toContain('Solo hay');
    expect(componente.insumos()[0].existencia).toBe(18.6);
  });
});
