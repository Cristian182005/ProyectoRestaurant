import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';
import {
  InventarioService,
  InsumoBackend,
  Merma,
  Ingreso,
} from '../core/inventario.service';
import { ClienteService, ProveedorResponse } from '../core/cliente.service';
import { SesionService } from '../core/sesion.service';
import { NotificationService } from '../core/notification.service';

type EstadoInsumo = 'DISPONIBLE' | 'CRÍTICO' | 'BAJO';
type EstadoFiltro = 'todos' | EstadoInsumo;
type CategoriaFiltro = 'todas' | string;
type TipoMovimiento = 'entrada' | 'salida';
type KpiTone = 'terra' | 'danger';

interface Insumo {
  codigo: string;
  id: number;
  nombre: string;
  categoria: string;
  existencia: number;
  unidad: string;
  stockMinimo: number;
  estado: EstadoInsumo;
  actualizado: string;
  costo: number;
  pedidoSugerido: number;
}

interface Movimiento {
  hora: string;
  insumo: string;
  detalle: string;
  variacion: string;
  signo: 'negativo' | 'positivo';
}

interface KpiItem {
  label: string;
  value: string;
  tone: KpiTone;
  icon: string[];
}

@Component({
  selector: 'app-inventario',
  imports: [NgClass, SidebarComponent],
  templateUrl: './inventario.html',
  styleUrl: './inventario.css',
})
export class Inventario {
  private readonly router = inject(Router);
  private readonly inventarioService = inject(InventarioService);
  private readonly clienteService = inject(ClienteService);
  private readonly sesion = inject(SesionService);
  readonly notificaciones = inject(NotificationService);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Inventario');
  readonly dateLabel = new Intl.DateTimeFormat('es-PE', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date());

  readonly searchTerm = signal('');
  readonly categoria = signal<CategoriaFiltro>('todas');
  readonly estadoFiltro = signal<EstadoFiltro>('todos');

  readonly modalAbierto = signal(false);
  readonly formCodigo = signal('');
  readonly formTipo = signal<TipoMovimiento>('salida');
  readonly formProveedorId = signal<number | null>(null);
  readonly formCantidad = signal<number | null>(null);
  readonly formMotivo = signal('');
  readonly formError = signal('');
  readonly cargando = signal(false);

  readonly insumos = signal<Insumo[]>([]);
  readonly movimientos = signal<Movimiento[]>([]);
  readonly proveedores = signal<ProveedorResponse[]>([]);

  readonly estadoOptions: { value: EstadoFiltro; label: string }[] = [
    { value: 'todos', label: 'Todos' },
    { value: 'DISPONIBLE', label: 'Disponible' },
    { value: 'BAJO', label: 'Bajo' },
    { value: 'CRÍTICO', label: 'Crítico' },
  ];

  readonly tipoOptions: { value: TipoMovimiento; label: string }[] = [
    { value: 'salida', label: 'Salida (merma / consumo)' },
    { value: 'entrada', label: 'Entrada (compra / devolución)' },
  ];

  readonly categoriaOptions = computed<{ value: CategoriaFiltro; label: string }[]>(() => {
    const categorias = [...new Set(this.insumos().map((insumo) => insumo.categoria))];
    return [
      { value: 'todas', label: 'Todas' },
      ...categorias.map((categoria) => ({ value: categoria, label: categoria })),
    ];
  });

  readonly kpis = computed<KpiItem[]>(() => [
    {
      label: 'Valor de inventario',
      value: this.money(
        this.insumos().reduce((total, item) => total + item.existencia * item.costo, 0)
      ),
      tone: 'terra',
      icon: [
        'M12 6a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
        'M6 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
        'M18 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
      ],
    },
    {
      label: 'Insumos activos',
      value: String(this.insumos().length),
      tone: 'terra',
      icon: ['M21 8l-9-5-9 5 9 5 9-5z', 'M3 8v8l9 5 9-5V8', 'M12 13v8'],
    },
    {
      label: 'Stock crítico',
      value: String(this.insumos().filter((item) => item.estado === 'CRÍTICO').length),
      tone: 'danger',
      icon: [
        'M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z',
        'M12 9v4',
        'M12 17h.01',
      ],
    },
    {
      label: 'Movimientos hoy',
      value: String(this.movimientos().length),
      tone: 'terra',
      icon: ['M4 8h13', 'm14 5 3 3-3 3', 'M20 16H7', 'm10 13-3 3 3 3'],
    },
  ]);

  readonly kpiTone: Record<KpiTone, { box: string; value: string }> = {
    terra: { box: 'bg-terra-100 text-terra-600', value: 'text-[#221a14]' },
    danger: { box: 'bg-danger-100 text-danger-600', value: 'text-danger-600' },
  };

  readonly estadoBadge: Record<EstadoInsumo, string> = {
    DISPONIBLE: 'bg-ok-100 text-ok-700',
    'CRÍTICO': 'bg-danger-100 text-danger-700',
    BAJO: 'bg-warn-100 text-warn-700',
  };

  readonly estadoPunto: Record<EstadoInsumo, string> = {
    DISPONIBLE: 'bg-ok-600',
    'CRÍTICO': 'bg-danger-600',
    BAJO: 'bg-[#e0a13c]',
  };

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoria = this.categoria();
    const estado = this.estadoFiltro();

    return this.insumos().filter((insumo) => {
      const matchesTerm =
        term === '' ||
        insumo.nombre.toLowerCase().includes(term) ||
        insumo.codigo.toLowerCase().includes(term) ||
        insumo.categoria.toLowerCase().includes(term);
      const matchesCategoria = categoria === 'todas' || insumo.categoria === categoria;
      const matchesEstado = estado === 'todos' || insumo.estado === estado;
      return matchesTerm && matchesCategoria && matchesEstado;
    });
  });

  readonly criticos = computed(() => this.insumos().filter((insumo) => insumo.estado === 'CRÍTICO'));

  constructor() {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    forkJoin({
      insumos: this.inventarioService.listarInsumos(),
      stockBajo: this.inventarioService.listarStockBajo(),
      mermas: this.inventarioService.listarMermas(),
      ingresos: this.inventarioService.listarIngresos(),
    }).subscribe({
      next: ({ insumos, stockBajo, mermas, ingresos }) => {
        const mapeados = this.mapearInsumos(insumos, stockBajo);
        this.insumos.set(mapeados);
        this.movimientos.set(this.mapearMovimientos(mermas, ingresos, mapeados));
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });

    this.clienteService.listarProveedores().subscribe({
      next: (proveedores) => this.proveedores.set(proveedores),
      error: () => this.proveedores.set([]),
    });
  }

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  onCategoria(event: Event): void {
    this.categoria.set((event.target as HTMLSelectElement).value as CategoriaFiltro);
  }

  onEstado(event: Event): void {
    this.estadoFiltro.set((event.target as HTMLSelectElement).value as EstadoFiltro);
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.categoria.set('todas');
    this.estadoFiltro.set('todos');
  }

  abrirModal(codigo?: string): void {
    this.formCodigo.set(codigo ?? this.insumos()[0]?.codigo ?? '');
    this.formTipo.set('salida');
    this.formProveedorId.set(this.proveedores()[0]?.id ?? null);
    this.formCantidad.set(null);
    this.formMotivo.set('');
    this.formError.set('');
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.formError.set('');
  }

  onFormCodigo(event: Event): void {
    this.formCodigo.set((event.target as HTMLSelectElement).value);
  }

  onFormTipo(event: Event): void {
    this.formTipo.set((event.target as HTMLSelectElement).value as TipoMovimiento);
  }

  onFormProveedor(event: Event): void {
    this.formProveedorId.set(Number((event.target as HTMLSelectElement).value));
  }

  onFormCantidad(event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    const value = Number(raw);
    this.formCantidad.set(raw === '' || Number.isNaN(value) ? null : value);
  }

  onFormMotivo(event: Event): void {
    this.formMotivo.set((event.target as HTMLInputElement).value);
  }

  registrarMovimiento(): void {
    const insumo = this.insumos().find((item) => item.codigo === this.formCodigo());
    const cantidad = this.formCantidad();
    const empleadoId = this.sesion.empleadoId();

    if (!insumo) {
      this.formError.set('Selecciona un insumo válido.');
      return;
    }
    if (cantidad === null || !Number.isFinite(cantidad) || cantidad <= 0) {
      this.formError.set('Ingresa una cantidad mayor a cero.');
      return;
    }
    if (empleadoId === null) {
      this.formError.set('La sesión no tiene un empleado asociado.');
      return;
    }
    if (this.formTipo() === 'salida' && cantidad > insumo.existencia) {
      this.formError.set(`Solo hay ${this.cantidadTexto(insumo.existencia)} ${insumo.unidad} disponibles.`);
      return;
    }

    const motivo = this.formMotivo().trim() || (this.formTipo() === 'salida' ? 'Merma' : 'Compra');
    this.formError.set('');

    if (this.formTipo() === 'salida') {
      this.inventarioService
        .registrarMerma({
          insumoId: insumo.id,
          cantidad,
          motivo,
          observacion: this.formMotivo().trim(),
          empleadoId,
        })
        .subscribe({
          next: () => {
            this.cerrarModal();
            this.notificaciones.exito(`Salida registrada: ${insumo.nombre}.`);
            this.cargarDatos();
          },
          error: () => this.notificaciones.error('No se pudo registrar la salida.'),
        });
      return;
    }

    const proveedorId = this.formProveedorId();
    if (!proveedorId) {
      this.formError.set('Selecciona un proveedor.');
      return;
    }
    this.inventarioService
      .registrarIngreso({
        proveedorId,
        empleadoId,
        detalles: [{ insumoId: insumo.id, cantidad }],
      })
      .subscribe({
        next: () => {
          this.cerrarModal();
          this.notificaciones.exito(`Entrada registrada: ${insumo.nombre}.`);
          this.cargarDatos();
        },
        error: () => this.notificaciones.error('No se pudo registrar la entrada.'),
      });
  }

  crearOrdenCompra(): void {
    const total = this.criticos().length;
    if (total === 0) {
      this.notificaciones.info('No hay insumos críticos para reponer.');
      return;
    }
    this.notificaciones.exito(`Orden de compra creada para ${total} insumos críticos.`);
  }

  exportar(): void {
    const header = [
      'INSUMO',
      'CODIGO',
      'CATEGORIA',
      'EXISTENCIA',
      'UNIDAD',
      'STOCK MINIMO',
      'ESTADO',
      'ACTUALIZADO',
    ];
    const filas = this.filtered().map((item) =>
      [
        item.nombre,
        item.codigo,
        item.categoria,
        this.cantidadTexto(item.existencia),
        item.unidad,
        this.cantidadTexto(item.stockMinimo),
        item.estado,
        item.actualizado,
      ].join(',')
    );

    const csv = [header.join(','), ...filas].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'inventario-el-hornero.csv';
    link.click();
    URL.revokeObjectURL(url);

    this.notificaciones.exito(`Exportados ${filas.length} insumos a CSV.`);
  }

  pedidoLabel(insumo: Insumo): string {
    const sugerido =
      insumo.pedidoSugerido > 0
        ? insumo.pedidoSugerido
        : Math.max(1, Math.ceil(insumo.stockMinimo * 1.5 - insumo.existencia));
    return `${sugerido} ${insumo.unidad}`;
  }

  cantidadTexto(value: number): string {
    const [entero, decimal] = value.toFixed(2).split('.');
    const decimales = decimal.replace(/0+$/, '');
    return decimales ? `${entero}.${decimales}` : entero;
  }

  money(value: number): string {
    const entero = Math.round(value).toString();
    return `S/ ${entero.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`;
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

  private mapearInsumos(insumos: InsumoBackend[], stockBajo: InsumoBackend[]): Insumo[] {
    const idsBajos = new Set(stockBajo.map((item) => item.id));
    return insumos.map((insumo) => {
      const existencia = insumo.stockActual ?? 0;
      const stockMinimo = insumo.stockMinimo ?? 0;
      return {
        id: insumo.id,
        codigo: `INS-${insumo.id}`,
        nombre: insumo.nombre,
        categoria: 'General',
        existencia,
        unidad: insumo.unidadMedida || 'und',
        stockMinimo,
        estado: idsBajos.has(insumo.id)
          ? 'CRÍTICO'
          : this.calcularEstado(existencia, stockMinimo),
        actualizado: '—',
        costo: 0,
        pedidoSugerido: existencia < stockMinimo ? Math.ceil(stockMinimo - existencia) : 0,
      };
    });
  }

  private mapearMovimientos(
    mermas: Merma[],
    ingresos: Ingreso[],
    insumos: Insumo[]
  ): Movimiento[] {
    const nombreDe = (id: number | null | undefined): string =>
      insumos.find((item) => item.id === id)?.nombre ?? 'Insumo';

    const salidas = (mermas ?? []).map((merma) => ({
      hora: this.horaDe(merma.fechaRegistro),
      insumo: merma.insumo?.nombre ?? nombreDe(merma.insumoId ?? merma.insumo?.id),
      detalle: [merma.motivo, merma.observacion].filter(Boolean).join(' · ') || 'Merma registrada',
      variacion: `-${this.cantidadTexto(merma.cantidad)}`,
      signo: 'negativo' as const,
    }));

    const entradas = (ingresos ?? []).flatMap((ingreso) =>
      (ingreso.detalles ?? []).map((detalle) => ({
        hora: this.horaDe(ingreso.fechaIngreso),
        insumo: nombreDe(detalle.insumoId),
        detalle: `Ingreso #${ingreso.id}`,
        variacion: `+${this.cantidadTexto(detalle.cantidad)}`,
        signo: 'positivo' as const,
      }))
    );

    return [...salidas, ...entradas].slice(0, 12);
  }

  private calcularEstado(existencia: number, stockMinimo: number): EstadoInsumo {
    if (existencia <= stockMinimo) {
      return 'CRÍTICO';
    }
    if (existencia - stockMinimo < 2) {
      return 'BAJO';
    }
    return 'DISPONIBLE';
  }

  private horaDe(valor: string | null | undefined): string {
    if (!valor) {
      return '—';
    }
    return valor.length >= 16 ? valor.slice(11, 16) : '—';
  }
}
