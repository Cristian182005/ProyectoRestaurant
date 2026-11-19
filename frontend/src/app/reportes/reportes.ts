import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SesionService } from '../core/sesion.service';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

interface HoraVenta {
  label: string;
  hora: string;
  valor: number;
}

interface MetodoPago {
  nombre: string;
  monto: number;
  pct: number;
  color: string;
}

interface PlatoVendido {
  id: string;
  nombre: string;
  unidades: number;
  venta: number;
  costo: number;
  margen: number;
}

interface FiltrosAplicados {
  desde: string;
  hasta: string;
  sucursal: string;
  turno: string;
}

interface KpiItem {
  label: string;
  value: string;
  detail: string;
}

@Component({
  selector: 'app-reportes',
  imports: [SidebarComponent],
  templateUrl: './reportes.html',
})
export class Reportes {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Reportes');
  readonly dateLabel = 'Jue, 01 oct 2026';

  // Filtros pendientes (se aplican con el botón "Aplicar")
  readonly desde = signal('2026-10-01');
  readonly hasta = signal('2026-10-01');
  readonly sucursal = signal('Chorrillos - Principal');
  readonly turno = signal('Todos los turnos');

  // Filtros que gobiernan los datos mostrados
  readonly aplicado = signal<FiltrosAplicados>({
    desde: '2026-10-01',
    hasta: '2026-10-01',
    sucursal: 'Chorrillos - Principal',
    turno: 'Todos los turnos',
  });

  readonly aviso = signal('');
  private avisoTimer: ReturnType<typeof setTimeout> | null = null;

  readonly sucursales = [
    'Chorrillos - Principal',
    'Surco - Miraflores',
    'San Isidro - Centro',
  ];

  readonly turnos = ['Todos los turnos', 'Mañana', 'Tarde', 'Noche'];

  private readonly factorSucursal: Record<string, number> = {
    'Chorrillos - Principal': 1,
    'Surco - Miraflores': 0.72,
    'San Isidro - Centro': 1.18,
  };

  private readonly ticketRef: Record<string, number> = {
    'Chorrillos - Principal': 128.2,
    'Surco - Miraflores': 112.5,
    'San Isidro - Centro': 154,
  };

  /** Rango de índices [inicio, fin] del turno sobre la serie de 12h a 23h. */
  private readonly rangoTurno: Record<string, [number, number]> = {
    'Todos los turnos': [0, 11],
    Mañana: [0, 3],
    Tarde: [4, 7],
    Noche: [8, 11],
  };

  private readonly mezclaTurno: Record<string, [number, number, number]> = {
    'Todos los turnos': [0.473456, 0.281148, 0.245396],
    Mañana: [0.41, 0.35, 0.24],
    Tarde: [0.465, 0.29, 0.245],
    Noche: [0.51, 0.24, 0.25],
  };

  /** Serie base de ventas por hora para un día completo en la sucursal principal. */
  private readonly horasBase: HoraVenta[] = [
    { label: '12h', hora: '12:00', valor: 620 },
    { label: '13h', hora: '13:00', valor: 980 },
    { label: '14h', hora: '14:00', valor: 1150 },
    { label: '15h', hora: '15:00', valor: 890 },
    { label: '16h', hora: '16:00', valor: 1320 },
    { label: '17h', hora: '17:00', valor: 1540 },
    { label: '18h', hora: '18:00', valor: 1980 },
    { label: '19h', hora: '19:00', valor: 2450 },
    { label: '20h', hora: '20:00', valor: 3620 },
    { label: '21h', hora: '21:00', valor: 2180 },
    { label: '22h', hora: '22:00', valor: 1290 },
    { label: '23h', hora: '23:00', valor: 440 },
  ];

  /** Ventas del día completo por plato (unidades) con su costo unitario de receta. */
  private readonly basePlatos: {
    id: string;
    nombre: string;
    unidades: number;
    precio: number;
    costoUnitario: number;
  }[] = [
    { id: 'PAR-001', nombre: 'Bife Ancho', unidades: 32, precio: 99, costoUnitario: 34.8 },
    {
      id: 'PAR-002',
      nombre: 'Parrillada El Hornero',
      unidades: 21,
      precio: 149,
      costoUnitario: 58.4,
    },
    { id: 'ENT-001', nombre: 'Anticuchos', unidades: 28, precio: 59, costoUnitario: 18.6 },
    { id: 'PAR-003', nombre: 'Lomo fino', unidades: 14, precio: 119, costoUnitario: 47.1 },
    {
      id: 'ENT-002',
      nombre: 'Chorizo artesanal',
      unidades: 26,
      precio: 32,
      costoUnitario: 11.2,
    },
    { id: 'PAR-005', nombre: 'Costillar de cerdo', unidades: 9, precio: 89, costoUnitario: 31.72 },
    { id: 'PAR-006', nombre: 'Pollo a las brasas', unidades: 10, precio: 79, costoUnitario: 22.67 },
    { id: 'ENT-003', nombre: 'Provoleta a la brasa', unidades: 18, precio: 42, costoUnitario: 14.7 },
    {
      id: 'GRU-001',
      nombre: 'Papa amarilla frita',
      unidades: 50,
      precio: 12,
      costoUnitario: 4.59,
    },
  ];

  private readonly factorCosto = 0.380281; // 7,020 / 18,460
  private readonly factorDescuento = 0.026116; // 482 / 18,460
  private readonly meses = [
    'ene',
    'feb',
    'mar',
    'abr',
    'may',
    'jun',
    'jul',
    'ago',
    'sep',
    'oct',
    'nov',
    'dic',
  ];

  readonly factorTotal = computed(() => {
    const filtros = this.aplicado();
    const dias = this.diasRango(filtros.desde, filtros.hasta);
    return (this.factorSucursal[filtros.sucursal] ?? 1) * dias;
  });

  readonly horas = computed<HoraVenta[]>(() => {
    const filtros = this.aplicado();
    const [inicio, fin] = this.rangoTurno[filtros.turno] ?? [0, 11];
    const factor = this.factorTotal();

    return this.horasBase
      .slice(inicio, fin + 1)
      .map((hora) => ({ ...hora, valor: Math.round(hora.valor * factor) }));
  });

  readonly ventasNetas = computed(() =>
    this.horas().reduce((total, hora) => total + hora.valor, 0)
  );

  readonly pico = computed(() => {
    const horas = this.horas();
    return horas.reduce<HoraVenta>(
      (max, hora) => (hora.valor > max.valor ? hora : max),
      horas[0]
    );
  });

  readonly comprobantes = computed(() => {
    const ref = this.ticketRef[this.aplicado().sucursal] ?? 128.2;
    return Math.max(1, Math.round(this.ventasNetas() / ref));
  });

  readonly costoInsumos = computed(() => Math.round(this.ventasNetas() * this.factorCosto));

  readonly utilidadBruta = computed(() => this.ventasNetas() - this.costoInsumos());

  readonly margenPct = computed(() =>
    this.ventasNetas() > 0
      ? Math.round((this.utilidadBruta() / this.ventasNetas()) * 1000) / 10
      : 0
  );

  readonly costoPct = computed(() =>
    this.ventasNetas() > 0
      ? Math.round((this.costoInsumos() / this.ventasNetas()) * 1000) / 10
      : 0
  );

  readonly descuentos = computed(() => Math.round(this.ventasNetas() * this.factorDescuento));

  readonly ticketPromedio = computed(
    () => Math.round((this.ventasNetas() / this.comprobantes()) * 10) / 10
  );

  readonly maxHora = computed(() =>
    this.horas().reduce((max, hora) => Math.max(max, hora.valor), 0)
  );

  readonly metodos = computed<MetodoPago[]>(() => {
    const ventas = this.ventasNetas();
    const [tarjeta, efectivo, yape] =
      this.mezclaTurno[this.aplicado().turno] ?? this.mezclaTurno['Todos los turnos'];

    const colores = ['bg-[#2b211a]', 'bg-ink-500', 'bg-[#e0a13c]'];
    const montos = [tarjeta, efectivo, yape].map((fraccion) => Math.round(ventas * fraccion));
    const nombres = ['Tarjeta', 'Efectivo', 'Yape / Plin'];

    return nombres.map((nombre, index) => ({
      nombre,
      monto: montos[index],
      pct: ventas > 0 ? Math.round((montos[index] / ventas) * 1000) / 10 : 0,
      color: colores[index],
    }));
  });

  readonly facturas = computed(() => Math.max(1, Math.round(this.comprobantes() * 0.083333)));

  readonly boletas = computed(() => this.comprobantes() - this.facturas());

  readonly topPlatos = computed<PlatoVendido[]>(() => {
    const factor = this.factorTotal();
    const [turnoInicio, turnoFin] = this.rangoTurno[this.aplicado().turno] ?? [0, 11];
    const baseTotal = this.horasBase.reduce((total, hora) => total + hora.valor, 0);
    const turnoBase = this.horasBase
      .slice(turnoInicio, turnoFin + 1)
      .reduce((total, hora) => total + hora.valor, 0);
    const share = baseTotal > 0 ? turnoBase / baseTotal : 1;

    return this.basePlatos
      .map((plato) => {
        const unidades = Math.max(1, Math.round(plato.unidades * factor * share));
        const venta = unidades * plato.precio;
        const costo = Math.round(unidades * plato.costoUnitario);
        const margen = venta > 0 ? Math.round(((venta - costo) / venta) * 1000) / 10 : 0;
        return { id: plato.id, nombre: plato.nombre, unidades, venta, costo, margen };
      })
      .sort((a, b) => b.venta - a.venta);
  });

  readonly kpis = computed<KpiItem[]>(() => [
    {
      label: 'Ventas netas',
      value: this.money(this.ventasNetas()),
      detail: `${this.comprobantes()} comprobantes`,
    },
    {
      label: 'Costo de insumos',
      value: this.money(this.costoInsumos()),
      detail: `${this.costoPct().toFixed(1)}% de ventas`,
    },
    {
      label: 'Utilidad bruta',
      value: this.money(this.utilidadBruta()),
      detail: `Margen ${this.margenPct().toFixed(1)}%`,
    },
    {
      label: 'Ticket promedio',
      value: this.money2(this.ticketPromedio()),
      detail: '↑ 7.0% vs. anterior',
    },
  ]);

  readonly resumenFecha = computed(() => this.fechaCorta(this.aplicado().hasta));

  onDesde(event: Event): void {
    this.desde.set((event.target as HTMLInputElement).value);
  }

  onHasta(event: Event): void {
    this.hasta.set((event.target as HTMLInputElement).value);
  }

  onSucursal(event: Event): void {
    this.sucursal.set((event.target as HTMLSelectElement).value);
  }

  onTurno(event: Event): void {
    this.turno.set((event.target as HTMLSelectElement).value);
  }

  aplicar(): void {
    if (!this.desde() || !this.hasta()) {
      this.mostrarAviso('Selecciona las fechas Desde y Hasta.');
      return;
    }
    if (this.desde() > this.hasta()) {
      this.mostrarAviso('La fecha "Desde" no puede ser posterior a la fecha "Hasta".');
      return;
    }

    this.aplicado.set({
      desde: this.desde(),
      hasta: this.hasta(),
      sucursal: this.sucursal(),
      turno: this.turno(),
    });
    this.mostrarAviso(
      `Reporte actualizado: ${this.sucursal()} · ${this.turno().toLowerCase()}.`
    );
  }

  exportarExcel(): void {
    const filtros = this.aplicado();
    const filas = [
      ['REPORTE DE VENTAS - EL HORNERO'],
      ['Sucursal', filtros.sucursal],
      ['Turno', filtros.turno],
      ['Desde', filtros.desde],
      ['Hasta', filtros.hasta],
      [],
      ['VENTAS NETAS', this.ventasNetas()],
      ['COSTO DE INSUMOS', this.costoInsumos()],
      ['UTILIDAD BRUTA', this.utilidadBruta()],
      ['DESCUENTOS', this.descuentos()],
      ['COMPROBANTES', this.comprobantes()],
      [],
      ['PLATO', 'UNIDADES', 'VENTA NETA', 'COSTO', 'MARGEN'],
      ...this.topPlatos().map((plato) => [
        plato.nombre,
        plato.unidades,
        plato.venta,
        plato.costo,
        `${plato.margen}%`,
      ]),
    ];

    const csv = filas.map((fila) => fila.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'reporte-ventas-el-hornero.csv';
    link.click();
    URL.revokeObjectURL(url);

    this.mostrarAviso(`Exportadas ${this.topPlatos().length} filas a CSV.`);
  }

  exportarPdf(): void {
    this.mostrarAviso('La exportación a PDF estará disponible próximamente.');
  }

  money(value: number): string {
    const entero = Math.round(value).toString();
    return `S/ ${entero.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}`;
  }

  money2(value: number): string {
    const [entero, decimal] = value.toFixed(2).split('.');
    return `S/ ${entero.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${decimal}`;
  }

  montoMetodo(metodo: MetodoPago): string {
    return this.money(metodo.monto);
  }

  indexPlato(i: number): string {
    return String(i + 1).padStart(2, '0');
  }

  /** Altura (%) de la barra de una hora dentro del gráfico. */
  altBarras(valor: number): number {
    const max = this.maxHora();
    if (max <= 0) {
      return 4;
    }
    return Math.max(4, (valor / max) * 100);
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

  private diasRango(desde: string, hasta: string): number {
    const inicio = Date.parse(desde);
    const fin = Date.parse(hasta);
    if (Number.isNaN(inicio) || Number.isNaN(fin)) {
      return 1;
    }
    const dias = Math.round((fin - inicio) / 86_400_000) + 1;
    return Math.max(1, dias);
  }

  private fechaCorta(iso: string): string {
    const partes = iso.split('-');
    if (partes.length !== 3) {
      return iso;
    }
    const [anio, mes, dia] = partes;
    const mesIdx = Number(mes) - 1;
    const mesTxt = this.meses[mesIdx] ?? mes;
    return `${dia} ${mesTxt} ${anio}`;
  }

  private mostrarAviso(mensaje: string): void {
    this.aviso.set(mensaje);
    if (this.avisoTimer) {
      clearTimeout(this.avisoTimer);
    }
    this.avisoTimer = setTimeout(() => this.aviso.set(''), 3200);
  }
}
