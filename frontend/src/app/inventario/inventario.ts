import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

type EstadoInsumo = 'DISPONIBLE' | 'CRÍTICO' | 'BAJO';
type EstadoFiltro = 'todos' | EstadoInsumo;
type CategoriaFiltro = 'todas' | string;
type TipoMovimiento = 'entrada' | 'salida';
type KpiTone = 'terra' | 'danger';

interface Insumo {
  codigo: string;
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

  private avisoTimer: ReturnType<typeof setTimeout> | null = null;

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Inventario');
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly searchTerm = signal('');
  readonly categoria = signal<CategoriaFiltro>('todas');
  readonly estadoFiltro = signal<EstadoFiltro>('todos');

  readonly modalAbierto = signal(false);
  readonly formCodigo = signal('INS-101');
  readonly formTipo = signal<TipoMovimiento>('salida');
  readonly formCantidad = signal<number | null>(null);
  readonly formMotivo = signal('');
  readonly formError = signal('');

  readonly aviso = signal('');

  readonly valorInventario = signal(38420);
  readonly movimientosHoy = signal(124);

  readonly insumos = signal<Insumo[]>([
    {
      codigo: 'INS-101',
      nombre: 'Bife ancho crudo',
      categoria: 'Carnes',
      existencia: 18.6,
      unidad: 'kg',
      stockMinimo: 12,
      estado: 'DISPONIBLE',
      actualizado: '20:43',
      costo: 32,
      pedidoSugerido: 0,
    },
    {
      codigo: 'INS-102',
      nombre: 'Lomo fino',
      categoria: 'Carnes',
      existencia: 3.4,
      unidad: 'kg',
      stockMinimo: 5,
      estado: 'CRÍTICO',
      actualizado: '19:58',
      costo: 48,
      pedidoSugerido: 8,
    },
    {
      codigo: 'INS-103',
      nombre: 'Corazón de res',
      categoria: 'Carnes',
      existencia: 9.8,
      unidad: 'kg',
      stockMinimo: 6,
      estado: 'DISPONIBLE',
      actualizado: '20:43',
      costo: 18,
      pedidoSugerido: 0,
    },
    {
      codigo: 'INS-104',
      nombre: 'Chorizo artesanal',
      categoria: 'Embutidos',
      existencia: 10,
      unidad: 'und',
      stockMinimo: 18,
      estado: 'CRÍTICO',
      actualizado: '20:43',
      costo: 9,
      pedidoSugerido: 30,
    },
    {
      codigo: 'INS-105',
      nombre: 'Carbón vegetal',
      categoria: 'Operación',
      existencia: 8,
      unidad: 'kg',
      stockMinimo: 12,
      estado: 'CRÍTICO',
      actualizado: '18:10',
      costo: 6.5,
      pedidoSugerido: 20,
    },
    {
      codigo: 'INS-106',
      nombre: 'Papa amarilla',
      categoria: 'Verduras',
      existencia: 22.5,
      unidad: 'kg',
      stockMinimo: 10,
      estado: 'DISPONIBLE',
      actualizado: '17:45',
      costo: 3.2,
      pedidoSugerido: 0,
    },
    {
      codigo: 'INS-107',
      nombre: 'Queso provolone',
      categoria: 'Lácteos',
      existencia: 7.2,
      unidad: 'kg',
      stockMinimo: 4,
      estado: 'DISPONIBLE',
      actualizado: '16:30',
      costo: 34,
      pedidoSugerido: 0,
    },
    {
      codigo: 'INS-108',
      nombre: 'Ají panca',
      categoria: 'Abarrotes',
      existencia: 4.6,
      unidad: 'kg',
      stockMinimo: 3,
      estado: 'BAJO',
      actualizado: '15:20',
      costo: 12,
      pedidoSugerido: 0,
    },
  ]);

  readonly movimientos = signal<Movimiento[]>([
    {
      hora: '20:43',
      insumo: 'Bife ancho crudo',
      detalle: 'Venta C-1048',
      variacion: '-0.70 kg',
      signo: 'negativo',
    },
    {
      hora: '20:43',
      insumo: 'Corazón de res',
      detalle: 'Venta C-1048',
      variacion: '-0.45 kg',
      signo: 'negativo',
    },
    {
      hora: '20:43',
      insumo: 'Chorizo artesanal',
      detalle: 'Venta C-1048',
      variacion: '-2 und',
      signo: 'negativo',
    },
    {
      hora: '19:58',
      insumo: 'Lomo fino',
      detalle: 'Venta C-1042',
      variacion: '-0.30 kg',
      signo: 'negativo',
    },
    {
      hora: '18:10',
      insumo: 'Carbón vegetal',
      detalle: 'Salida a parrilla',
      variacion: '-4 kg',
      signo: 'negativo',
    },
  ]);

  readonly categoriaOptions: { value: CategoriaFiltro; label: string }[] = [
    { value: 'todas', label: 'Todas' },
    { value: 'Carnes', label: 'Carnes' },
    { value: 'Embutidos', label: 'Embutidos' },
    { value: 'Operación', label: 'Operación' },
    { value: 'Verduras', label: 'Verduras' },
    { value: 'Lácteos', label: 'Lácteos' },
    { value: 'Abarrotes', label: 'Abarrotes' },
  ];

  readonly estadoOptions: { value: EstadoFiltro; label: string }[] = [
    { value: 'todos', label: 'Todos' },
    { value: 'DISPONIBLE', label: 'Disponible' },
    { value: 'BAJO', label: 'Bajo' },
    { value: 'CRÍTICO', label: 'Crítico' },
  ];

  readonly tipoOptions: { value: TipoMovimiento; label: string }[] = [
    { value: 'salida', label: 'Salida (venta / consumo)' },
    { value: 'entrada', label: 'Entrada (compra / devolución)' },
  ];

  readonly kpis = computed<KpiItem[]>(() => [
    {
      label: 'Valor de inventario',
      value: this.money(this.valorInventario()),
      tone: 'terra',
      icon: [
        'M12 6a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
        'M6 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
        'M18 15a3 3 0 1 0 0 6 3 3 0 0 0 0-6z',
      ],
    },
    {
      label: 'Insumos activos',
      value: '86',
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
      value: String(this.movimientosHoy()),
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

  readonly criticos = computed(() =>
    this.insumos().filter((insumo) => insumo.estado === 'CRÍTICO')
  );

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
    this.formCodigo.set(codigo ?? this.insumos()[0].codigo);
    this.formTipo.set('salida');
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

    if (!insumo) {
      this.formError.set('Selecciona un insumo válido.');
      return;
    }
    if (cantidad === null || !Number.isFinite(cantidad) || cantidad <= 0) {
      this.formError.set('Ingresa una cantidad mayor a cero.');
      return;
    }
    if (this.formTipo() === 'salida' && cantidad > insumo.existencia) {
      this.formError.set(`Solo hay ${this.cantidadTexto(insumo.existencia)} ${insumo.unidad} disponibles.`);
      return;
    }

    const delta = this.formTipo() === 'entrada' ? cantidad : -cantidad;
    const existencia = Math.max(0, Math.round((insumo.existencia + delta) * 100) / 100);
    const hora = this.horaActual();

    this.insumos.update((list) =>
      list.map((item) =>
        item.codigo === insumo.codigo
          ? {
              ...item,
              existencia,
              estado: this.calcularEstado(existencia, item.stockMinimo),
              actualizado: hora,
            }
          : item
      )
    );

    this.valorInventario.update((valor) => Math.max(0, Math.round(valor + delta * insumo.costo)));
    this.movimientosHoy.update((total) => total + 1);

    const motivo = this.formMotivo().trim();
    const variacion = `${delta > 0 ? '+' : '-'}${this.cantidadTexto(Math.abs(delta))} ${insumo.unidad}`;

    this.movimientos.update((list) =>
      [
        {
          hora,
          insumo: insumo.nombre,
          detalle: motivo || (delta > 0 ? 'Entrada de compra' : 'Salida registrada'),
          variacion,
          signo: (delta > 0 ? 'positivo' : 'negativo') as Movimiento['signo'],
        },
        ...list,
      ].slice(0, 8)
    );

    this.cerrarModal();
    this.mostrarAviso(
      delta > 0
        ? `Entrada registrada: ${insumo.nombre} +${variacion.slice(1)}`
        : `Salida registrada: ${insumo.nombre} ${variacion}`
    );
  }

  crearOrdenCompra(): void {
    const total = this.criticos().length;
    if (total === 0) {
      this.mostrarAviso('No hay insumos críticos para reponer.');
      return;
    }
    this.mostrarAviso(`Orden de compra creada para ${total} insumos críticos.`);
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

    this.mostrarAviso(`Exportados ${filas.length} insumos a CSV.`);
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
    void this.router.navigate(['/']);
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

  private horaActual(): string {
    const now = new Date();
    return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
  }

  private mostrarAviso(mensaje: string): void {
    this.aviso.set(mensaje);
    if (this.avisoTimer) {
      clearTimeout(this.avisoTimer);
    }
    this.avisoTimer = setTimeout(() => this.aviso.set(''), 3200);
  }
}
