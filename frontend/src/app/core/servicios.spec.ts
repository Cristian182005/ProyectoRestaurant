import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CajaService, fechaISO } from './caja.service';
import { CatalogoService } from './catalogo.service';
import { ClienteService } from './cliente.service';
import { InventarioService } from './inventario.service';
import { PedidoService } from './pedido.service';

const API = 'http://localhost:8080/api';

describe('Servicios HTTP del núcleo', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('PedidoService usa las rutas y cuerpos del contrato', () => {
    const servicio = TestBed.inject(PedidoService);

    servicio.listar('PENDIENTE').subscribe();
    const listado = http.expectOne((req) => req.url === `${API}/pedidos` && req.method === 'GET');
    expect(listado.request.params.get('estado')).toBe('PENDIENTE');
    listado.flush([]);

    servicio.obtener(7).subscribe();
    http.expectOne(`${API}/pedidos/7`).flush({});

    servicio.detalles(7).subscribe();
    http.expectOne(`${API}/pedidos/7/detalles`).flush([]);

    servicio.crear({ mesaId: 1, empleadoId: 2, items: [{ platoId: 3, cantidad: 2 }] }).subscribe();
    const creacion = http.expectOne(`${API}/pedidos`);
    expect(creacion.request.method).toBe('POST');
    expect(creacion.request.body).toEqual({
      mesaId: 1,
      empleadoId: 2,
      items: [{ platoId: 3, cantidad: 2 }],
    });
    creacion.flush({});

    servicio.agregarDetalle({ pedidoId: 7, platoId: 3, cantidad: 1 }).subscribe();
    const agregar = http.expectOne(`${API}/detalles-pedido`);
    expect(agregar.request.body).toEqual({ pedidoId: 7, platoId: 3, cantidad: 1 });
    agregar.flush({});

    servicio.actualizarDetalle(9, 4).subscribe();
    const actualizar = http.expectOne(`${API}/detalles-pedido/9`);
    expect(actualizar.request.body).toEqual({ cantidad: 4 });
    actualizar.flush({});

    servicio.eliminarDetalle(9).subscribe();
    const eliminar = http.expectOne(`${API}/detalles-pedido/9`);
    expect(eliminar.request.method).toBe('DELETE');
    eliminar.flush(null);

    servicio.cambiarEstado(7, 'ENTREGADO').subscribe();
    const estado = http.expectOne(`${API}/pedidos/7/estado`);
    expect(estado.request.body).toEqual({ estado: 'ENTREGADO' });
    estado.flush({});

    expect(servicio.etiquetaEstado('PENDIENTE')).toBe('Por cobrar');
    expect(servicio.estadoDesdeEtiqueta('Anulado')).toBe('CANCELADO');
  });

  it('CajaService convierte las fechas de la UI a ISO', () => {
    const servicio = TestBed.inject(CajaService);

    expect(fechaISO('01/10/2026')).toBe('2026-10-01');
    expect(fechaISO('2026-10-01')).toBe('2026-10-01');

    servicio.listar('01/10/2026', '2026-10-02').subscribe();
    const listado = http.expectOne((req) => req.url === `${API}/comprobantes`);
    expect(listado.request.params.get('desde')).toBe('2026-10-01');
    expect(listado.request.params.get('hasta')).toBe('2026-10-02');
    listado.flush([]);

    servicio.resumen('2026-10-01', '2026-10-02').subscribe();
    const resumen = http.expectOne(
      (req) => req.url === `${API}/comprobantes/resumen` && req.method === 'GET'
    );
    expect(resumen.request.params.get('desde')).toBe('2026-10-01');
    resumen.flush({});

    servicio.emitir({
      pedidoId: 4,
      cajeroId: 2,
      tipoPago: 'YAPE',
      montoRecibido: 100,
      clienteNaturalId: 8,
      clienteJuridicoId: null,
    }).subscribe();
    const emision = http.expectOne(`${API}/comprobantes`);
    expect(emision.request.body).toEqual({
      pedidoId: 4,
      cajeroId: 2,
      tipoPago: 'YAPE',
      montoRecibido: 100,
      clienteNaturalId: 8,
      clienteJuridicoId: null,
    });
    emision.flush({});

    servicio.porPedido(4).subscribe();
    http.expectOne(`${API}/comprobantes/pedido/4`).flush({});
  });

  it('InventarioService registra mermas e ingresos', () => {
    const servicio = TestBed.inject(InventarioService);

    servicio.listarInsumos().subscribe();
    http.expectOne(`${API}/insumos`).flush([]);

    servicio.listarStockBajo().subscribe();
    http.expectOne(`${API}/insumos/stock-bajo`).flush([]);

    servicio.listarMermas().subscribe();
    http.expectOne(`${API}/mermas`).flush([]);

    servicio.listarIngresos().subscribe();
    http.expectOne(`${API}/ingresos`).flush([]);

    servicio
      .registrarMerma({
        insumoId: 1,
        cantidad: 2,
        motivo: 'Merma',
        observacion: 'prueba',
        empleadoId: 3,
      })
      .subscribe();
    const merma = http.expectOne(`${API}/mermas`);
    expect(merma.request.body).toEqual({
      insumoId: 1,
      cantidad: 2,
      motivo: 'Merma',
      observacion: 'prueba',
      empleadoId: 3,
    });
    merma.flush({});

    servicio
      .registrarIngreso({ proveedorId: 2, empleadoId: 3, detalles: [{ insumoId: 1, cantidad: 5 }] })
      .subscribe();
    const ingreso = http.expectOne(`${API}/ingresos`);
    expect(ingreso.request.body).toEqual({
      proveedorId: 2,
      empleadoId: 3,
      detalles: [{ insumoId: 1, cantidad: 5 }],
    });
    ingreso.flush({});
  });

  it('CatalogoService y ClienteService consultan los catálogos', () => {
    const catalogo = TestBed.inject(CatalogoService);
    const clientes = TestBed.inject(ClienteService);

    catalogo.listarPlatosActivos().subscribe();
    const platos = http.expectOne((req) => req.url === `${API}/platos`);
    expect(platos.request.params.get('activo')).toBe('true');
    platos.flush([]);

    catalogo.listarMesas().subscribe();
    http.expectOne(`${API}/mesas`).flush([]);

    clientes.listarNaturales().subscribe();
    http.expectOne(`${API}/clientes/naturales`).flush([]);

    clientes.listarJuridicos().subscribe();
    http.expectOne(`${API}/clientes/juridicos`).flush([]);

    clientes.listarProveedores().subscribe();
    http.expectOne(`${API}/proveedores`).flush([]);
  });
});
