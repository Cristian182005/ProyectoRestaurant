import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CajaVentas } from './caja-ventas';
import { SesionService } from '../core/sesion.service';

const CLIENTE = {
  id: 8,
  nombre: 'Ana Pérez',
  tipoDocumento: 'DNI',
  numeroDocumento: '12345678',
  email: 'ana@correo.test',
  telefono: '999999999',
  direccion: 'Lima',
};

const COMPROBANTE = {
  id: 1,
  fechaEmision: '2026-11-19',
  horaEmision: '13:05:00',
  gravado: 100,
  igv: 18,
  precioTotal: 118,
  tipoPago: 'EFECTIVO' as const,
  vueltoPago: 0,
  montoRecibido: 118,
  cajeroId: 2,
  pedidoId: 5,
  clienteNaturalId: 8,
  clienteJuridicoId: null,
};

const RESUMEN = {
  desde: '2026-11-19',
  hasta: '2026-11-19',
  cantidadComprobantes: 10,
  totalVentas: 1180,
  totalIgv: 180,
  promedioTicket: 118,
};

describe('CajaVentas', () => {
  let fixture: ComponentFixture<CajaVentas>;
  let componente: CajaVentas;
  let http: HttpTestingController;
  let sesion: SesionService;

  const responderCargaInicial = () => {
    http
      .expectOne((req) => req.url.endsWith('/clientes/naturales') && req.method === 'GET')
      .flush([CLIENTE]);
    http
      .expectOne((req) => req.url.endsWith('/clientes/juridicos') && req.method === 'GET')
      .flush([]);
    http
      .expectOne((req) => req.url.endsWith('/pedidos') && req.method === 'GET')
      .flush([
        {
          id: 5,
          estado: 'ENTREGADO',
          horaTomadaDelPedido: '2026-11-19T12:30:00',
          mesaId: 2,
          mesaNumero: 3,
          empleadoId: 4,
          total: 118,
          detalles: [],
        },
      ]);
    http
      .expectOne((req) => req.url.endsWith('/comprobantes/resumen') && req.method === 'GET')
      .flush(RESUMEN);
    http
      .expectOne((req) => req.url.endsWith('/comprobantes') && req.method === 'GET')
      .flush([COMPROBANTE]);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CajaVentas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    sesion = TestBed.inject(SesionService);
    sesion.cerrarSesion();
    fixture = TestBed.createComponent(CajaVentas);
    componente = fixture.componentInstance;
  });

  afterEach(() => {
    sesion.cerrarSesion();
    http.verify();
  });

  it('carga comprobantes, clientes y resumen del backend', () => {
    fixture.detectChanges();
    responderCargaInicial();
    fixture.detectChanges();

    expect(componente.comprobantes().length).toBe(1);
    expect(componente.comprobantes()[0].cliente).toBe('Ana Pérez');
    expect(componente.comprobantes()[0].comprobante).toBe('B00001');
    expect(componente.kpis()[0].value).toBe('S/ 1,000.00');
    expect(fixture.nativeElement.textContent).toContain('Ana Pérez');
  });

  it('emite un comprobante con un solo cliente y recarga el listado', () => {
    sesion.guardar(
      {
        token: 'token',
        email: 'cajero@elhornero.test',
        rol: 'CAJERO',
        expiraEn: '2999-01-01T00:00:00.000Z',
        id: 3,
        empleadoId: 2,
      },
      false
    );
    fixture.detectChanges();
    responderCargaInicial();

    componente.abrirVenta();
    expect(componente.formClienteId()).toBe(8);

    componente.formPedidoId.set(5);
    componente.formTipoPago.set('YAPE');
    componente.formMontoRecibido.set(200);
    componente.emitirComprobante();

    const peticion = http.expectOne(
      (req) => req.url.endsWith('/comprobantes') && req.method === 'POST'
    );
    expect(peticion.request.body).toEqual({
      pedidoId: 5,
      cajeroId: 2,
      tipoPago: 'YAPE',
      montoRecibido: 200,
      clienteNaturalId: 8,
      clienteJuridicoId: null,
    });
    peticion.flush({ ...COMPROBANTE, id: 2, numeroComprobante: 'B00002' });

    http
      .expectOne((req) => req.url.endsWith('/comprobantes/resumen') && req.method === 'GET')
      .flush(RESUMEN);
    http
      .expectOne((req) => req.url.endsWith('/comprobantes') && req.method === 'GET')
      .flush([COMPROBANTE]);

    expect(componente.ultimoComprobante()?.numeroComprobante).toBe('B00002');
    expect(componente.modalVentaAbierto()).toBeFalse();
    expect(componente.notificaciones.mensaje()).toBe('Comprobante B00002 emitido.');
  });

  it('valida los datos antes de emitir', () => {
    fixture.detectChanges();
    responderCargaInicial();

    componente.abrirVenta();
    componente.emitirComprobante();
    expect(componente.formError()).toBe('Selecciona un pedido entregado.');

    componente.formPedidoId.set(5);
    componente.formClienteId.set(null);
    componente.emitirComprobante();
    expect(componente.formError()).toBe('Selecciona un cliente.');
  });
});
