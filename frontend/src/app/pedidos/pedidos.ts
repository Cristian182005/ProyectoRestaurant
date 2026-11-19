import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';
import {
  CatalogoService,
  Mesa,
  Plato,
} from '../core/catalogo.service';
import {
  EstadoPedido,
  EstadoPedidoVisual,
  Pedido as PedidoBackend,
  PedidoService,
} from '../core/pedido.service';
import { SesionService } from '../core/sesion.service';
import { NotificationService } from '../core/notification.service';

type CategoriaFiltro = 'todas' | string;
type EstadoFiltro = 'todos' | EstadoPedidoVisual;
type KpiTone = 'danger' | 'ok' | 'warn';
type AlertTone = 'warn' | 'danger' | 'ok' | 'info' | 'muted';

interface PedidoItem {
  id: number;
  cantidad: number;
  producto: string;
  categoria: string;
  subtotal: number;
  precioUnitario: number;
}

interface Pedido {
  id: string;
  idNumero: number;
  hora: string;
  mesa: number;
  personas: number;
  mozo: string;
  apertura: string;
  tiempo: number;
  estado: EstadoPedidoVisual;
  estadoBackend: EstadoPedido;
  subtotal: number;
  igv: number;
  total: number;
  items: PedidoItem[];
  nota: string | null;
  ultimoCambio: string;
}

interface KpiCard {
  label: string;
  value: string;
  detail: string;
  tone: KpiTone;
  icon: string[];
}

interface AlertaEstado {
  mensaje: string;
  accion: string;
  tone: AlertTone;
}

interface ItemNuevoPedido {
  platoId: number;
  cantidad: number;
}

const PEDIDO_VACIO: Pedido = {
  id: '#—',
  idNumero: 0,
  hora: '—',
  mesa: 0,
  personas: 0,
  mozo: '—',
  apertura: '—',
  tiempo: 0,
  estado: 'Por cobrar',
  estadoBackend: 'PENDIENTE',
  subtotal: 0,
  igv: 0,
  total: 0,
  items: [],
  nota: null,
  ultimoCambio: '—',
};

@Component({
  selector: 'app-pedidos',
  imports: [NgClass, SidebarComponent],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css',
})
export class Pedidos {
  private readonly router = inject(Router);
  private readonly pedidoService = inject(PedidoService);
  private readonly catalogo = inject(CatalogoService);
  private readonly sesion = inject(SesionService);
  readonly notificaciones = inject(NotificationService);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Pedidos');
  readonly dateLabel = new Intl.DateTimeFormat('es-PE', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date());

  readonly searchTerm = signal('');
  readonly categoria = signal<CategoriaFiltro>('todas');
  readonly estadoFiltro = signal<EstadoFiltro>('todos');
  readonly page = signal(1);
  readonly selectedId = signal<string | null>(null);
  readonly drawerOpen = signal(false);
  readonly cargando = signal(false);

  readonly pedidos = signal<Pedido[]>([]);
  readonly platos = signal<Plato[]>([]);
  readonly mesas = signal<Mesa[]>([]);

  readonly modalNuevoAbierto = signal(false);
  readonly nuevoMesaId = signal<number | null>(null);
  readonly nuevoItems = signal<ItemNuevoPedido[]>([]);
  readonly detallePlatoId = signal<number | null>(null);
  readonly detalleCantidad = signal(1);
  readonly formError = signal('');

  readonly pageSize = 6;

  readonly estadoOptions: { value: EstadoFiltro; label: string }[] = [
    { value: 'todos', label: 'Todos los estados' },
    { value: 'Por cobrar', label: 'Por cobrar' },
    { value: 'En preparación', label: 'En preparación' },
    { value: 'Entregado', label: 'Entregado' },
    { value: 'Anulado', label: 'Anulado' },
  ];

  readonly kpis = computed<KpiCard[]>(() => {
    const lista = this.pedidos();
    const activos = lista.filter(
      (p) => p.estadoBackend !== 'ENTREGADO' && p.estadoBackend !== 'CANCELADO'
    );
    const porCobrar = lista.filter((p) => p.estadoBackend === 'PENDIENTE');
    const tiempos = lista.map((p) => p.tiempo).filter((t) => t > 0);
    const promedio = tiempos.length
      ? Math.round(tiempos.reduce((suma, t) => suma + t, 0) / tiempos.length)
      : 0;

    return [
      {
        label: 'Activos',
        value: String(activos.length),
        detail: `${lista.length} registrados`,
        tone: 'danger' as KpiTone,
        icon: [
          'M3 10.5 12 3l9 7.5',
          'M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5',
        ],
      },
      {
        label: 'Tiempo promedio',
        value: `${promedio} min`,
        detail: 'Meta <= 18',
        tone: 'ok' as KpiTone,
        icon: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 7v5l3.5 2'],
      },
      {
        label: 'Por cobrar',
        value: `S/ ${this.money(porCobrar.reduce((suma, p) => suma + p.total, 0))}`,
        detail: `${porCobrar.length} mesas`,
        tone: 'warn' as KpiTone,
        icon: [
          'M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z',
          'm3.5 6.5 8.5 6 8.5-6',
        ],
      },
    ];
  });

  readonly categoriaOptions = computed<{ value: CategoriaFiltro; label: string }[]>(() => {
    const categorias = [...new Set(this.platos().map((plato) => plato.categoria || 'General'))];
    return [
      { value: 'todas', label: 'Todas las categorías' },
      ...categorias.map((categoria) => ({ value: categoria, label: categoria })),
    ];
  });

  readonly toneIcon: Record<KpiTone, string> = {
    danger: 'bg-danger-100 text-danger-600',
    ok: 'bg-ok-100 text-ok-600',
    warn: 'bg-warn-100 text-warn-700',
  };

  readonly toneDetail: Record<KpiTone, string> = {
    danger: 'text-danger-700',
    ok: 'text-ok-600',
    warn: 'text-warn-700',
  };

  readonly estadoBadge: Record<EstadoPedidoVisual, string> = {
    'Por cobrar': 'bg-warn-100 text-warn-700',
    'En parrilla': 'bg-danger-100 text-danger-700',
    Listo: 'bg-ok-100 text-ok-700',
    'En preparación': 'bg-info-100 text-info-700',
    Entregado: 'bg-cream-300 text-ink-500',
    Anulado: 'bg-danger-100 text-danger-700',
  };

  readonly estadoPunto: Record<EstadoPedidoVisual, string> = {
    'Por cobrar': 'bg-[#e0813c]',
    'En parrilla': 'bg-danger-600',
    Listo: 'bg-ok-600',
    'En preparación': 'bg-info-700',
    Entregado: 'bg-ink-400',
    Anulado: 'bg-danger-600',
  };

  readonly alertaEstado: Record<EstadoPedidoVisual, AlertaEstado> = {
    'Por cobrar': { mensaje: 'Listo para iniciar la preparación.', accion: 'Preparar', tone: 'warn' },
    'En parrilla': { mensaje: 'La parrilla sigue trabajando la comanda.', accion: 'Entregar', tone: 'danger' },
    Listo: { mensaje: 'Listo para entregar a la mesa.', accion: 'Entregar', tone: 'ok' },
    'En preparación': { mensaje: 'Comanda en preparación en cocina.', accion: 'Entregar', tone: 'info' },
    Entregado: { mensaje: 'Entregado al comensal. Puedes cobrarlo.', accion: 'Cobrar', tone: 'muted' },
    Anulado: { mensaje: 'Comanda anulada desde caja.', accion: 'Ver comanda', tone: 'danger' },
  };

  readonly alertaTone: Record<AlertTone, string> = {
    warn: 'border-warn-700/70 bg-warn-100',
    danger: 'border-danger-600/70 bg-danger-100',
    ok: 'border-ok-600/70 bg-ok-50',
    info: 'border-info-700/70 bg-info-100',
    muted: 'border-cream-400 bg-cream-200',
  };

  readonly alertaTexto: Record<AlertTone, string> = {
    warn: 'text-warn-700',
    danger: 'text-danger-700',
    ok: 'text-ok-700',
    info: 'text-info-700',
    muted: 'text-ink-600',
  };

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoria = this.categoria();
    const estado = this.estadoFiltro();

    return this.pedidos().filter((pedido) => {
      const matchesTerm =
        term === '' ||
        pedido.id.toLowerCase().includes(term) ||
        pedido.mozo.toLowerCase().includes(term) ||
        this.mesaLabel(pedido.mesa).toLowerCase().includes(term) ||
        pedido.items.some((item) => item.producto.toLowerCase().includes(term));
      const matchesCategoria =
        categoria === 'todas' || pedido.items.some((item) => item.categoria === categoria);
      const matchesEstado = estado === 'todos' || pedido.estado === estado;
      return matchesTerm && matchesCategoria && matchesEstado;
    });
  });

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filtered().length / this.pageSize))
  );

  readonly pagedRows = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  readonly rangeLabel = computed(() => {
    const total = this.filtered().length;
    if (total === 0) {
      return 'Mostrando 0-0 de 0 registros';
    }
    const from = (this.page() - 1) * this.pageSize + 1;
    const to = Math.min(this.page() * this.pageSize, total);
    return `Mostrando ${from}-${to} de ${total} registros`;
  });

  readonly pageNumbers = computed(() => {
    const last = this.totalPages();
    const start = Math.max(1, Math.min(this.page() - 1, last - 2));
    const pages: number[] = [];
    for (let i = start; i <= Math.min(start + 2, last); i++) {
      pages.push(i);
    }
    return pages;
  });

  readonly shiftLabel = computed(() => `Turno actual - ${this.filtered().length} pedidos`);

  readonly selected = computed(
    () => this.pedidos().find((pedido) => pedido.id === this.selectedId()) ?? PEDIDO_VACIO
  );

  readonly selectedItemsCount = computed(() =>
    this.selected().items.reduce((total, item) => total + item.cantidad, 0)
  );

  readonly totalNuevoPedido = computed(() =>
    this.nuevoItems().reduce((total, item) => {
      const plato = this.platos().find((p) => p.id === item.platoId);
      return total + (plato?.precio ?? 0) * item.cantidad;
    }, 0)
  );

  constructor() {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.pedidoService.listar().subscribe({
      next: (respuesta) => {
        this.pedidos.set(respuesta.map((pedido) => this.mapearPedido(pedido)));
        if (!this.selectedId() && this.pedidos().length > 0) {
          this.selectedId.set(this.pedidos()[0].id);
        }
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.cargarCatalogo();
  }

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
    this.page.set(1);
  }

  onCategoria(event: Event): void {
    this.categoria.set((event.target as HTMLSelectElement).value as CategoriaFiltro);
    this.page.set(1);
  }

  onEstado(event: Event): void {
    this.estadoFiltro.set((event.target as HTMLSelectElement).value as EstadoFiltro);
    this.page.set(1);
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.categoria.set('todas');
    this.estadoFiltro.set('todos');
    this.page.set(1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }
    this.page.set(page);
  }

  select(pedido: Pedido): void {
    this.selectedId.set(pedido.id);
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  esEditable(pedido: Pedido = this.selected()): boolean {
    return pedido.estadoBackend === 'PENDIENTE';
  }

  mesaLabel(mesa: number): string {
    return `Mesa ${String(mesa).padStart(2, '0')}`;
  }

  nombrePlato(platoId: number): string {
    return this.platos().find((plato) => plato.id === platoId)?.nombre ?? 'Plato';
  }

  money(value: number): string {
    const [integer, decimals] = value.toFixed(2).split('.');
    return `${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${decimals}`;
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  onLogout(): void {
    this.sesion.cerrarSesion();
    void this.router.navigate(['/']);
  }

  abrirNuevoPedido(): void {
    this.formError.set('');
    this.nuevoItems.set([]);
    this.nuevoMesaId.set(this.mesas()[0]?.id ?? null);
    this.modalNuevoAbierto.set(true);
    this.cargarCatalogo();
  }

  cerrarNuevoPedido(): void {
    this.modalNuevoAbierto.set(false);
    this.formError.set('');
  }

  onNuevaMesa(event: Event): void {
    this.nuevoMesaId.set(Number((event.target as HTMLSelectElement).value));
  }

  agregarNuevoItem(event: Event): void {
    const platoId = Number((event.target as HTMLSelectElement).value);
    if (!platoId) {
      return;
    }
    this.nuevoItems.update((items) => {
      const existente = items.find((item) => item.platoId === platoId);
      return existente
        ? items.map((item) =>
            item.platoId === platoId ? { ...item, cantidad: item.cantidad + 1 } : item
          )
        : [...items, { platoId, cantidad: 1 }];
    });
    (event.target as HTMLSelectElement).value = '';
  }

  cambiarNuevaCantidad(platoId: number, delta: number): void {
    this.nuevoItems.update((items) =>
      items
        .map((item) =>
          item.platoId === platoId ? { ...item, cantidad: item.cantidad + delta } : item
        )
        .filter((item) => item.cantidad > 0)
    );
  }

  crearPedido(): void {
    const mesaId = this.nuevoMesaId();
    const empleadoId = this.sesion.empleadoId();
    const items = this.nuevoItems();

    if (!mesaId) {
      this.formError.set('Selecciona una mesa.');
      return;
    }
    if (empleadoId === null) {
      this.formError.set('La sesión no tiene un empleado asociado.');
      return;
    }
    if (items.length === 0) {
      this.formError.set('Agrega al menos un plato al pedido.');
      return;
    }

    this.formError.set('');
    this.pedidoService.crear({ mesaId, empleadoId, items }).subscribe({
      next: (respuesta) => {
        const pedido = this.mapearPedido(respuesta);
        this.pedidos.update((lista) => [pedido, ...lista]);
        this.selectedId.set(pedido.id);
        this.drawerOpen.set(true);
        this.cerrarNuevoPedido();
        this.notificaciones.exito(`Pedido ${pedido.id} creado correctamente.`);
      },
      error: () => this.formError.set('No se pudo crear el pedido. Revisa los datos.'),
    });
  }

  onDetallePlato(event: Event): void {
    const valor = Number((event.target as HTMLSelectElement).value);
    this.detallePlatoId.set(valor || null);
  }

  onDetalleCantidad(event: Event): void {
    const valor = Number((event.target as HTMLInputElement).value);
    this.detalleCantidad.set(Number.isFinite(valor) && valor > 0 ? Math.floor(valor) : 1);
  }

  agregarDetalle(): void {
    const pedido = this.selected();
    const platoId = this.detallePlatoId();
    const cantidad = this.detalleCantidad();

    if (!this.esEditable(pedido)) {
      this.notificaciones.advertencia('Solo se pueden modificar pedidos pendientes.');
      return;
    }
    if (!platoId || pedido.idNumero === 0) {
      this.formError.set('Selecciona un plato para agregar.');
      return;
    }
    this.formError.set('');
    this.pedidoService
      .agregarDetalle({ pedidoId: pedido.idNumero, platoId, cantidad })
      .subscribe({
        next: () => {
          this.actualizarPedido(pedido.idNumero);
          this.detallePlatoId.set(null);
          this.detalleCantidad.set(1);
          this.notificaciones.exito('Plato agregado al pedido.');
        },
        error: () => this.notificaciones.error('No se pudo agregar el plato al pedido.'),
      });
  }

  cambiarCantidad(item: PedidoItem, delta: number): void {
    const pedido = this.selected();
    if (!this.esEditable(pedido)) {
      return;
    }
    const cantidad = item.cantidad + delta;
    if (cantidad <= 0) {
      this.eliminarDetalle(item);
      return;
    }
    this.pedidoService.actualizarDetalle(item.id, cantidad).subscribe({
      next: () => {
        this.actualizarPedido(pedido.idNumero);
        this.notificaciones.exito('Cantidad actualizada.');
      },
      error: () => this.notificaciones.error('No se pudo actualizar la cantidad.'),
    });
  }

  eliminarDetalle(item: PedidoItem): void {
    const pedido = this.selected();
    if (!this.esEditable(pedido)) {
      return;
    }
    this.pedidoService.eliminarDetalle(item.id).subscribe({
      next: () => {
        this.actualizarPedido(pedido.idNumero);
        this.notificaciones.exito('Plato eliminado del pedido.');
      },
      error: () => this.notificaciones.error('No se pudo eliminar el plato.'),
    });
  }

  cambiarEstado(estado: EstadoPedido): void {
    const pedido = this.selected();
    if (pedido.idNumero === 0) {
      return;
    }
    this.pedidoService.cambiarEstado(pedido.idNumero, estado).subscribe({
      next: (respuesta) => {
        this.reemplazarPedido(this.mapearPedido(respuesta));
        this.notificaciones.exito(`Pedido ${pedido.id} actualizado a ${this.pedidoService.etiquetaEstado(estado)}.`);
      },
      error: () => this.notificaciones.error('No se pudo cambiar el estado del pedido.'),
    });
  }

  ejecutarAccion(): void {
    const pedido = this.selected();
    switch (pedido.estadoBackend) {
      case 'PENDIENTE':
        this.cambiarEstado('EN_PREPARACION');
        break;
      case 'EN_PREPARACION':
        this.cambiarEstado('ENTREGADO');
        break;
      case 'ENTREGADO':
        this.cobrar();
        break;
      default:
        this.notificaciones.info('El pedido ya no admite cambios de estado.');
    }
  }

  cobrar(): void {
    const pedido = this.selected();
    if (pedido.idNumero === 0) {
      return;
    }
    void this.router.navigate(['/caja-ventas'], {
      queryParams: { pedido: pedido.idNumero },
    });
  }

  private cargarCatalogo(): void {
    if (this.platos().length === 0) {
      this.platoServiceListar();
    }
    if (this.mesas().length === 0) {
      this.catalogo.listarMesas().subscribe({
        next: (mesas) => this.mesas.set(mesas),
        error: () => this.mesas.set([]),
      });
    }
  }

  private platoServiceListar(): void {
    this.catalogo.listarPlatosActivos().subscribe({
      next: (platos) => this.platos.set(platos),
      error: () => this.platos.set([]),
    });
  }

  private actualizarPedido(id: number): void {
    this.pedidoService.obtener(id).subscribe({
      next: (respuesta) => this.reemplazarPedido(this.mapearPedido(respuesta)),
      error: () => undefined,
    });
  }

  private reemplazarPedido(pedido: Pedido): void {
    this.pedidos.update((lista) =>
      lista.map((item) => (item.idNumero === pedido.idNumero ? pedido : item))
    );
  }

  private mapearPedido(pedido: PedidoBackend): Pedido {
    const items = (pedido.detalles ?? []).map((detalle) => ({
      id: detalle.id,
      cantidad: detalle.cantidad,
      producto: detalle.platoNombre,
      categoria: this.categoriaDePlato(detalle.platoId),
      subtotal: detalle.subtotal ?? 0,
      precioUnitario: detalle.precioUnitario ?? 0,
    }));
    const total = pedido.total ?? items.reduce((suma, item) => suma + item.subtotal, 0);
    const igv = Math.round((total - total / 1.18) * 100) / 100;
    return {
      id: `#${pedido.id}`,
      idNumero: pedido.id,
      hora: this.horaDe(pedido.horaTomadaDelPedido),
      mesa: pedido.mesaNumero ?? 0,
      personas: 0,
      mozo: pedido.empleadoId ? `Empleado #${pedido.empleadoId}` : 'Sin asignar',
      apertura: this.horaDe(pedido.horaTomadaDelPedido),
      tiempo: this.minutosDesde(pedido.horaTomadaDelPedido),
      estado: this.pedidoService.etiquetaEstado(pedido.estado),
      estadoBackend: pedido.estado,
      subtotal: Math.round((total - igv) * 100) / 100,
      igv,
      total,
      items,
      nota: null,
      ultimoCambio: '—',
    };
  }

  private categoriaDePlato(platoId: number): string {
    return this.platos().find((plato) => plato.id === platoId)?.categoria || 'General';
  }

  private horaDe(valor: string | null | undefined): string {
    if (!valor) {
      return '—';
    }
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime())
      ? String(valor).slice(11, 16)
      : `${String(fecha.getHours()).padStart(2, '0')}:${String(fecha.getMinutes()).padStart(2, '0')}`;
  }

  private minutosDesde(valor: string | null | undefined): number {
    if (!valor) {
      return 0;
    }
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) {
      return 0;
    }
    return Math.max(0, Math.round((Date.now() - fecha.getTime()) / 60000));
  }
}
