import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';
import {
  CajaService,
  Comprobante,
  ResumenVentas,
  TIPOS_PAGO,
  TipoPago,
  fechaISO,
} from '../core/caja.service';
import { ClienteResponse, ClienteService } from '../core/cliente.service';
import { Pedido, PedidoService } from '../core/pedido.service';
import { SesionService } from '../core/sesion.service';
import { NotificationService } from '../core/notification.service';

type KpiTone = 'green' | 'blue' | 'amber' | 'red';
type ComprobanteTipo = 'Factura' | 'Boleta';
type ComprobanteEstado = 'Emitido' | 'Enviado SUNAT' | 'Anulado';
type CategoriaFiltro = 'todas' | ComprobanteTipo;
type TipoCliente = 'natural' | 'juridico';

interface KpiCard {
  label: string;
  value: string;
  detail: string;
  tone: KpiTone;
  icon: string[];
}

interface ComprobanteFila {
  id: string;
  comprobante: string;
  tipo: ComprobanteTipo;
  cliente: string;
  documento: string;
  email: string;
  pago: string;
  metodo: string;
  total: number;
  estado: ComprobanteEstado;
  fecha: string;
  hora: string;
  cdr: string;
  gravado: number;
  igv: number;
  vuelto: number;
  montoRecibido: number;
  pedidoId: number | null;
}

const COMPROBANTE_VACIO: ComprobanteFila = {
  id: '',
  comprobante: '—',
  tipo: 'Boleta',
  cliente: '—',
  documento: '—',
  email: '—',
  pago: '—',
  metodo: '—',
  total: 0,
  estado: 'Emitido',
  fecha: '—',
  hora: '—',
  cdr: '—',
  gravado: 0,
  igv: 0,
  vuelto: 0,
  montoRecibido: 0,
  pedidoId: null,
};

const ETIQUETAS_PAGO: Record<TipoPago, string> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  YAPE: 'Yape',
  PLIN: 'Plin',
  TRANSFERENCIA: 'Transferencia',
};

@Component({
  selector: 'app-caja-ventas',
  imports: [NgClass, SidebarComponent],
  templateUrl: './caja-ventas.html',
})
export class CajaVentas {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly cajaService = inject(CajaService);
  private readonly clienteService = inject(ClienteService);
  private readonly pedidoService = inject(PedidoService);
  private readonly sesion = inject(SesionService);
  readonly notificaciones = inject(NotificationService);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Caja y ventas');
  readonly dateLabel = new Intl.DateTimeFormat('es-PE', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date());

  readonly searchTerm = signal('');
  readonly categoria = signal<CategoriaFiltro>('todas');
  readonly desde = signal(fechaISO(new Date()));
  readonly hasta = signal(fechaISO(new Date()));
  readonly page = signal(1);
  readonly selectedId = signal('');

  readonly comprobantes = signal<ComprobanteFila[]>([]);
  readonly resumen = signal<ResumenVentas | null>(null);
  readonly pedidosEntregados = signal<Pedido[]>([]);
  readonly clientesNaturales = signal<ClienteResponse[]>([]);
  readonly clientesJuridicos = signal<ClienteResponse[]>([]);
  readonly ultimoComprobante = signal<Comprobante | null>(null);

  readonly modalVentaAbierto = signal(false);
  readonly formPedidoId = signal<number | null>(null);
  readonly formTipoCliente = signal<TipoCliente>('natural');
  readonly formClienteId = signal<number | null>(null);
  readonly formTipoPago = signal<TipoPago>('EFECTIVO');
  readonly formMontoRecibido = signal<number | null>(null);
  readonly formError = signal('');
  readonly tiposPago = TIPOS_PAGO;

  readonly pageSize = 6;

  readonly kpis = computed<KpiCard[]>(() => {
    const data = this.resumen();
    const ventas = data?.totalVentas ?? 0;
    const igv = data?.totalIgv ?? 0;
    return [
      {
        label: 'Venta neta',
        value: `S/ ${this.money(ventas - igv)}`,
        detail: `Total S/ ${this.money(ventas)}`,
        tone: 'green' as KpiTone,
        icon: ['M3 17l6.5-6.5 4 4L21 7', 'M15 7h6v6'],
      },
      {
        label: 'Comprobantes',
        value: String(data?.cantidadComprobantes ?? 0),
        detail: `Ticket S/ ${this.money(data?.promedioTicket ?? 0)}`,
        tone: 'blue' as KpiTone,
        icon: [
          'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z',
          'M14 3v5h5',
          'M9 13h6',
          'M9 17h4',
        ],
      },
      {
        label: 'IGV',
        value: `S/ ${this.money(igv)}`,
        detail: 'Por declarar',
        tone: 'amber' as KpiTone,
        icon: ['M3 10 12 3l9 7', 'M5 10v10', 'M9.5 10v10', 'M14.5 10v10', 'M19 10v10', 'M3 20h18'],
      },
      {
        label: 'Anulaciones',
        value: '0',
        detail: 'Sin anulaciones registradas',
        tone: 'red' as KpiTone,
        icon: [
          'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z',
          'M14 3v5h5',
          'm9.5 12.5 5 5',
          'm14.5 12.5-5 5',
        ],
      },
    ];
  });

  readonly toneIcon: Record<KpiTone, string> = {
    green: 'bg-ok-100 text-ok-600',
    blue: 'bg-info-100 text-info-700',
    amber: 'bg-warn-100 text-warn-700',
    red: 'bg-danger-100 text-danger-600',
  };

  readonly toneDetail: Record<KpiTone, string> = {
    green: 'text-ok-600',
    blue: 'text-info-700',
    amber: 'text-warn-700',
    red: 'text-danger-700',
  };

  readonly estadoBadge: Record<ComprobanteEstado, string> = {
    Emitido: 'bg-ok-100 text-ok-700',
    'Enviado SUNAT': 'bg-info-100 text-info-700',
    Anulado: 'bg-danger-100 text-danger-700',
  };

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoria = this.categoria();
    return this.comprobantes().filter((row) => {
      const matchesTerm =
        term === '' ||
        row.comprobante.toLowerCase().includes(term) ||
        row.cliente.toLowerCase().includes(term) ||
        row.pago.toLowerCase().includes(term);
      return matchesTerm && (categoria === 'todas' || row.tipo === categoria);
    });
  });

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize)));

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

  readonly filterSummary = computed(
    () => `${this.desde()} al ${this.hasta()} · ${this.comprobantes().length} comprobantes`
  );

  readonly selected = computed(
    () => this.comprobantes().find((row) => row.id === this.selectedId()) ?? COMPROBANTE_VACIO
  );

  readonly clientesDelFormulario = computed(() =>
    this.formTipoCliente() === 'natural' ? this.clientesNaturales() : this.clientesJuridicos()
  );

  constructor() {
    this.cargarDatos();
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const pedido = params.get('pedido');
      if (pedido) {
        this.formPedidoId.set(Number(pedido));
        this.abrirVenta();
      }
    });
  }

  cargarDatos(): void {
    this.cargarComprobantes();
    this.cargarResumen();
    this.clienteService.listarNaturales().subscribe({
      next: (clientes) => this.clientesNaturales.set(clientes),
      error: () => this.clientesNaturales.set([]),
    });
    this.clienteService.listarJuridicos().subscribe({
      next: (clientes) => this.clientesJuridicos.set(clientes),
      error: () => this.clientesJuridicos.set([]),
    });
    this.pedidoService.listar('ENTREGADO').subscribe({
      next: (pedidos) => this.pedidosEntregados.set(pedidos),
      error: () => this.pedidosEntregados.set([]),
    });
  }

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
    this.page.set(1);
  }

  onCategoria(event: Event): void {
    this.categoria.set((event.target as HTMLSelectElement).value as CategoriaFiltro);
    this.page.set(1);
  }

  onDesde(event: Event): void {
    this.desde.set(fechaISO((event.target as HTMLInputElement).value));
    this.page.set(1);
    this.cargarComprobantes();
    this.cargarResumen();
  }

  onHasta(event: Event): void {
    this.hasta.set(fechaISO((event.target as HTMLInputElement).value));
    this.page.set(1);
    this.cargarComprobantes();
    this.cargarResumen();
  }

  resetFilters(): void {
    const hoy = fechaISO(new Date());
    this.searchTerm.set('');
    this.categoria.set('todas');
    this.desde.set(hoy);
    this.hasta.set(hoy);
    this.page.set(1);
    this.cargarComprobantes();
    this.cargarResumen();
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }
    this.page.set(page);
  }

  select(row: ComprobanteFila): void {
    this.selectedId.set(row.id);
  }

  subtotal(row: ComprobanteFila): string {
    return this.money(row.gravado);
  }

  igv(row: ComprobanteFila): string {
    return this.money(row.igv);
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

  abrirVenta(): void {
    this.formError.set('');
    this.formClienteId.set(this.clientesDelFormulario()[0]?.id ?? null);
    this.formTipoPago.set('EFECTIVO');
    this.formMontoRecibido.set(null);
    this.modalVentaAbierto.set(true);
  }

  cerrarVenta(): void {
    this.modalVentaAbierto.set(false);
    this.formError.set('');
  }

  onFormPedido(event: Event): void {
    this.formPedidoId.set(Number((event.target as HTMLSelectElement).value));
  }

  onTipoCliente(event: Event): void {
    this.formTipoCliente.set((event.target as HTMLSelectElement).value as TipoCliente);
    this.formClienteId.set(this.clientesDelFormulario()[0]?.id ?? null);
  }

  onFormCliente(event: Event): void {
    this.formClienteId.set(Number((event.target as HTMLSelectElement).value));
  }

  onFormTipoPago(event: Event): void {
    this.formTipoPago.set((event.target as HTMLSelectElement).value as TipoPago);
  }

  onFormMonto(event: Event): void {
    const valor = Number((event.target as HTMLInputElement).value);
    this.formMontoRecibido.set(Number.isFinite(valor) ? valor : null);
  }

  emitirComprobante(): void {
    const pedidoId = this.formPedidoId();
    const clienteId = this.formClienteId();
    const montoRecibido = this.formMontoRecibido();
    const cajeroId = this.sesion.empleadoId();

    if (!pedidoId) {
      this.formError.set('Selecciona un pedido entregado.');
      return;
    }
    if (!clienteId) {
      this.formError.set('Selecciona un cliente.');
      return;
    }
    if (cajeroId === null) {
      this.formError.set('La sesión no tiene un cajero asociado.');
      return;
    }
    if (montoRecibido === null || montoRecibido <= 0) {
      this.formError.set('Ingresa un monto recibido mayor a cero.');
      return;
    }

    const esNatural = this.formTipoCliente() === 'natural';
    this.formError.set('');
    this.cajaService
      .emitir({
        pedidoId,
        cajeroId,
        tipoPago: this.formTipoPago(),
        montoRecibido,
        clienteNaturalId: esNatural ? clienteId : null,
        clienteJuridicoId: esNatural ? null : clienteId,
      })
      .subscribe({
        next: (comprobante) => {
          this.ultimoComprobante.set(comprobante);
          this.cerrarVenta();
          this.cargarComprobantes();
          this.cargarResumen();
          this.notificaciones.exito(`Comprobante ${this.numeroComprobante(comprobante)} emitido.`);
        },
        error: () => this.notificaciones.error('No se pudo emitir el comprobante.'),
      });
  }

  proximamente(funcion: string): void {
    this.notificaciones.info(`${funcion} estará disponible próximamente.`);
  }

  numeroComprobante(comprobante: Comprobante): string {
    return (
      comprobante.numeroComprobante ??
      `${comprobante.clienteJuridicoId ? 'F' : 'B'}${String(comprobante.id).padStart(5, '0')}`
    );
  }

  private cargarComprobantes(): void {
    this.cajaService.listar(this.desde(), this.hasta()).subscribe({
      next: (respuesta) => {
        this.comprobantes.set(respuesta.map((item) => this.mapearComprobante(item)));
        if (!this.selectedId() && this.comprobantes().length) {
          this.selectedId.set(this.comprobantes()[0].id);
        }
      },
      error: () => this.comprobantes.set([]),
    });
  }

  private cargarResumen(): void {
    this.cajaService.resumen(this.desde(), this.hasta()).subscribe({
      next: (resumen) => this.resumen.set(resumen),
      error: () => this.resumen.set(null),
    });
  }

  private mapearComprobante(comprobante: Comprobante): ComprobanteFila {
    const id = String(comprobante.id);
    const clienteNatural = this.clientesNaturales().find(
      (cliente) => cliente.id === comprobante.clienteNaturalId
    );
    const clienteJuridico = this.clientesJuridicos().find(
      (cliente) => cliente.id === comprobante.clienteJuridicoId
    );
    const cliente = clienteNatural ?? clienteJuridico;
    return {
      id,
      comprobante: this.numeroComprobante(comprobante),
      tipo: clienteJuridico || comprobante.clienteJuridicoId ? 'Factura' : 'Boleta',
      cliente: comprobante.cliente ?? cliente?.nombre ?? 'Cliente registrado',
      documento: comprobante.documento ?? (cliente ? `${cliente.tipoDocumento} ${cliente.numeroDocumento}` : '—'),
      email: comprobante.email ?? cliente?.email ?? '—',
      pago: ETIQUETAS_PAGO[comprobante.tipoPago] ?? comprobante.tipoPago,
      metodo: ETIQUETAS_PAGO[comprobante.tipoPago] ?? comprobante.tipoPago,
      total: comprobante.precioTotal ?? 0,
      estado: comprobante.estado ?? 'Emitido',
      fecha: this.fechaLegible(comprobante.fechaEmision),
      hora: String(comprobante.horaEmision ?? '').slice(0, 5) || '—',
      cdr: String(comprobante.horaEmision ?? '').slice(0, 5) || '—',
      gravado: comprobante.gravado ?? 0,
      igv: comprobante.igv ?? 0,
      vuelto: comprobante.vueltoPago ?? 0,
      montoRecibido: comprobante.montoRecibido ?? 0,
      pedidoId: comprobante.pedidoId ?? null,
    };
  }

  private fechaLegible(valor: string | null | undefined): string {
    if (!valor) {
      return '—';
    }
    const partes = valor.slice(0, 10).split('-');
    return partes.length === 3 ? `${partes[2]}/${partes[1]}/${partes[0]}` : valor;
  }
}
