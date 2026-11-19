import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { Pedidos } from './pedidos';
import { Pedido } from '../core/pedido.service';
import { SesionService } from '../core/sesion.service';

const PEDIDO: Pedido = {
  id: 5,
  estado: 'PENDIENTE',
  horaTomadaDelPedido: '2026-11-19T12:30:00',
  mesaId: 2,
  mesaNumero: 3,
  empleadoId: 4,
  total: 118,
  detalles: [
    {
      id: 10,
      pedidoId: 5,
      platoId: 7,
      platoNombre: 'Lomo saltado',
      cantidad: 1,
      precioUnitario: 118,
      subtotal: 118,
    },
  ],
};

describe('Pedidos', () => {
  let fixture: ComponentFixture<Pedidos>;
  let componente: Pedidos;
  let http: HttpTestingController;
  let router: Router;
  let sesion: SesionService;

  const responderCargaInicial = () => {
    http
      .expectOne((req) => req.url.endsWith('/pedidos') && req.method === 'GET')
      .flush([PEDIDO]);
    http
      .expectOne((req) => req.url.endsWith('/platos') && req.method === 'GET')
      .flush([
        {
          id: 7,
          nombre: 'Lomo saltado',
          descripcion: '',
          imagen: '',
          activo: true,
          categoria: 'Platos principales',
          precio: 118,
        },
      ]);
    http
      .expectOne((req) => req.url.endsWith('/mesas') && req.method === 'GET')
      .flush([{ id: 2, numero: 3, estado: 'OCUPADA', capacidad: 4 }]);
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Pedidos],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(Pedidos);
    componente = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    sesion = TestBed.inject(SesionService);
    sesion.cerrarSesion();
  });

  afterEach(() => {
    sesion.cerrarSesion();
    http.verify();
  });

  it('carga pedidos y catálogos del backend y renderiza el pedido', () => {
    fixture.detectChanges();
    responderCargaInicial();
    fixture.detectChanges();

    expect(componente.pedidos().length).toBe(1);
    expect(componente.selected().id).toBe('#5');
    expect(componente.selected().estado).toBe('Por cobrar');
    expect(componente.nombrePlato(7)).toBe('Lomo saltado');
    expect(fixture.nativeElement.textContent).toContain('#5');
  });

  it('cambia el estado del pedido seleccionado con PUT y notifica', () => {
    fixture.detectChanges();
    responderCargaInicial();

    componente.cambiarEstado('EN_PREPARACION');
    const peticion = http.expectOne(
      (req) => req.url.endsWith('/pedidos/5/estado') && req.method === 'PUT'
    );
    expect(peticion.request.body).toEqual({ estado: 'EN_PREPARACION' });
    peticion.flush({ ...PEDIDO, estado: 'EN_PREPARACION' });

    expect(componente.selected().estadoBackend).toBe('EN_PREPARACION');
    expect(componente.selected().estado).toBe('En preparación');
    expect(componente.notificaciones.mensaje()).toContain('En preparación');
  });

  it('navega a caja con el pedido al cobrar', () => {
    fixture.detectChanges();
    responderCargaInicial();

    const navegar = spyOn(router, 'navigate').and.resolveTo(true);
    componente.cobrar();

    expect(navegar).toHaveBeenCalledWith(['/caja-ventas'], {
      queryParams: { pedido: 5 },
    });
  });

  it('valida el formulario de nuevo pedido antes de crear', () => {
    sesion.guardar(
      {
        token: 'token',
        email: 'mozo@elhornero.test',
        rol: 'MOZO',
        expiraEn: '2999-01-01T00:00:00.000Z',
        id: 2,
        empleadoId: 4,
      },
      false
    );
    fixture.detectChanges();
    responderCargaInicial();

    componente.abrirNuevoPedido();
    componente.nuevoMesaId.set(null);
    componente.crearPedido();
    expect(componente.formError()).toBe('Selecciona una mesa.');

    componente.nuevoMesaId.set(2);
    componente.crearPedido();
    expect(componente.formError()).toBe('Agrega al menos un plato al pedido.');
  });
});
