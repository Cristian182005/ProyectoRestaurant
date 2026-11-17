import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

type KpiTone = 'green' | 'blue' | 'amber' | 'red';
type ComprobanteTipo = 'Factura' | 'Boleta';
type ComprobanteEstado = 'Emitido' | 'Enviado SUNAT' | 'Anulado';
type CategoriaFiltro = 'todas' | ComprobanteTipo;

interface KpiCard {
  label: string;
  value: string;
  detail: string;
  tone: KpiTone;
  icon: string[];
}

interface Comprobante {
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
}

@Component({
  selector: 'app-caja-ventas',
  imports: [NgClass, SidebarComponent],
  templateUrl: './caja-ventas.html',
})
export class CajaVentas {
  private readonly router = inject(Router);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Caja y ventas');
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly searchTerm = signal('');
  readonly categoria = signal<CategoriaFiltro>('todas');
  readonly fecha = signal('01 oct 2026');
  readonly page = signal(1);
  readonly selectedId = signal('f001-00842');

  readonly pageSize = 6;

  readonly dateOptions = ['01 oct 2026', '30 sep 2026', '29 sep 2026'];

  readonly kpis: KpiCard[] = [
    {
      label: 'Venta neta',
      value: 'S/ 18,420',
      detail: '+12.4%',
      tone: 'green',
      icon: ['M3 17l6.5-6.5 4 4L21 7', 'M15 7h6v6'],
    },
    {
      label: 'Comprobantes',
      value: '96',
      detail: '81 boletas · 15 facturas',
      tone: 'blue',
      icon: [
        'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z',
        'M14 3v5h5',
        'M9 13h6',
        'M9 17h4',
      ],
    },
    {
      label: 'IGV',
      value: 'S/ 3,315.60',
      detail: 'Por declarar',
      tone: 'amber',
      icon: ['M3 10 12 3l9 7', 'M5 10v10', 'M9.5 10v10', 'M14.5 10v10', 'M19 10v10', 'M3 20h18'],
    },
    {
      label: 'Anulaciones',
      value: '2',
      detail: 'S/ 341.40',
      tone: 'red',
      icon: [
        'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z',
        'M14 3v5h5',
        'm9.5 12.5 5 5',
        'm14.5 12.5-5 5',
      ],
    },
  ];

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

  readonly comprobantes: Comprobante[] = [
    { id: 'f001-00842', comprobante: 'F001-00842', tipo: 'Factura', cliente: 'Restobar Andino S.A.C.', documento: 'RUC 20604587122', email: 'contabilidad@andino.pe', pago: 'Tarjeta', metodo: 'Visa •••• 4821', total: 286.5, estado: 'Emitido', fecha: '01/10/2026', hora: '12:49', cdr: '12:50' },
    { id: 'b001-04218', comprobante: 'B001-04218', tipo: 'Boleta', cliente: 'María Torres', documento: 'DNI 44871209', email: 'maria.torres@gmail.com', pago: 'Yape', metodo: 'Yape •••• 3391', total: 174.9, estado: 'Emitido', fecha: '01/10/2026', hora: '12:44', cdr: '12:45' },
    { id: 'b001-04217', comprobante: 'B001-04217', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 98, estado: 'Emitido', fecha: '01/10/2026', hora: '12:38', cdr: '12:39' },
    { id: 'f001-00841', comprobante: 'F001-00841', tipo: 'Factura', cliente: 'Logística Sur S.R.L.', documento: 'RUC 20512398744', email: 'administracion@logisticasur.pe', pago: 'Visa', metodo: 'Visa •••• 9017', total: 342.8, estado: 'Enviado SUNAT', fecha: '01/10/2026', hora: '12:31', cdr: '12:32' },
    { id: 'b001-04216', comprobante: 'B001-04216', tipo: 'Boleta', cliente: 'Sofía Mendoza', documento: 'DNI 76321908', email: 'sofia.mendoza@outlook.com', pago: 'Plin', metodo: 'Plin •••• 2074', total: 126, estado: 'Emitido', fecha: '01/10/2026', hora: '12:25', cdr: '12:26' },
    { id: 'b001-04215', comprobante: 'B001-04215', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 215.4, estado: 'Anulado', fecha: '01/10/2026', hora: '12:18', cdr: '12:20' },
    { id: 'f001-00840', comprobante: 'F001-00840', tipo: 'Factura', cliente: 'Hotel Costa Verde E.I.R.L.', documento: 'RUC 20458796311', email: 'reservas@costaverde.pe', pago: 'Tarjeta', metodo: 'Mastercard •••• 5540', total: 512.3, estado: 'Emitido', fecha: '01/10/2026', hora: '12:05', cdr: '12:06' },
    { id: 'b001-04214', comprobante: 'B001-04214', tipo: 'Boleta', cliente: 'Jorge Salazar', documento: 'DNI 40982317', email: 'jorge.salazar@gmail.com', pago: 'Yape', metodo: 'Yape •••• 8812', total: 89.9, estado: 'Emitido', fecha: '01/10/2026', hora: '11:58', cdr: '11:59' },
    { id: 'b001-04213', comprobante: 'B001-04213', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 156, estado: 'Emitido', fecha: '01/10/2026', hora: '11:52', cdr: '11:53' },
    { id: 'f001-00839', comprobante: 'F001-00839', tipo: 'Factura', cliente: 'Mercado Martín S.A.C.', documento: 'RUC 20339874512', email: 'compras@mercadomartin.pe', pago: 'Visa', metodo: 'Visa •••• 3320', total: 1240.8, estado: 'Enviado SUNAT', fecha: '01/10/2026', hora: '11:44', cdr: '11:45' },
    { id: 'b001-04212', comprobante: 'B001-04212', tipo: 'Boleta', cliente: 'Lucía Ferrer', documento: 'DNI 70123456', email: 'lucia.ferrer@gmail.com', pago: 'Plin', metodo: 'Plin •••• 6620', total: 232.5, estado: 'Emitido', fecha: '01/10/2026', hora: '11:37', cdr: '11:38' },
    { id: 'b001-04211', comprobante: 'B001-04211', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 64, estado: 'Emitido', fecha: '01/10/2026', hora: '11:30', cdr: '11:31' },
    { id: 'f001-00838', comprobante: 'F001-00838', tipo: 'Factura', cliente: 'Estudio Jurídico Ramos', documento: 'RUC 20114567893', email: 'contacto@ramosabogados.pe', pago: 'Tarjeta', metodo: 'Visa •••• 2210', total: 398, estado: 'Emitido', fecha: '01/10/2026', hora: '11:22', cdr: '11:23' },
    { id: 'b001-04210', comprobante: 'B001-04210', tipo: 'Boleta', cliente: 'Diego Quispe', documento: 'DNI 45678912', email: 'diego.quispe@outlook.com', pago: 'Yape', metodo: 'Yape •••• 4471', total: 145.6, estado: 'Emitido', fecha: '01/10/2026', hora: '11:15', cdr: '11:16' },
    { id: 'b001-04209', comprobante: 'B001-04209', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 126, estado: 'Anulado', fecha: '01/10/2026', hora: '11:07', cdr: '11:09' },
    { id: 'f001-00837', comprobante: 'F001-00837', tipo: 'Factura', cliente: 'Panadería La Espiga S.A.C.', documento: 'RUC 20778451236', email: 'gerencia@laespiga.pe', pago: 'Plin', metodo: 'Plin •••• 9903', total: 675.4, estado: 'Enviado SUNAT', fecha: '01/10/2026', hora: '10:58', cdr: '10:59' },
    { id: 'b001-04208', comprobante: 'B001-04208', tipo: 'Boleta', cliente: 'Camila Rojas', documento: 'DNI 71456098', email: 'camila.rojas@gmail.com', pago: 'Tarjeta', metodo: 'Mastercard •••• 7734', total: 98.7, estado: 'Emitido', fecha: '01/10/2026', hora: '10:51', cdr: '10:52' },
    { id: 'b001-04207', comprobante: 'B001-04207', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 187.2, estado: 'Emitido', fecha: '01/10/2026', hora: '10:44', cdr: '10:45' },
    { id: 'f001-00836', comprobante: 'F001-00836', tipo: 'Factura', cliente: 'Operadora Turística Andes', documento: 'RUC 20661230987', email: 'ventas@turismoandes.pe', pago: 'Visa', metodo: 'Visa •••• 6180', total: 890, estado: 'Emitido', fecha: '01/10/2026', hora: '10:36', cdr: '10:37' },
    { id: 'b001-04206', comprobante: 'B001-04206', tipo: 'Boleta', cliente: 'Roberto Mendoza', documento: 'DNI 43210987', email: 'roberto.mendoza@gmail.com', pago: 'Yape', metodo: 'Yape •••• 1180', total: 76.5, estado: 'Emitido', fecha: '01/10/2026', hora: '10:29', cdr: '10:30' },
    { id: 'b001-04205', comprobante: 'B001-04205', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 203.8, estado: 'Emitido', fecha: '30/09/2026', hora: '22:14', cdr: '22:15' },
    { id: 'f001-00835', comprobante: 'F001-00835', tipo: 'Factura', cliente: 'Clínica Santa Fe S.A.C.', documento: 'RUC 20554780123', email: 'administracion@santafe.pe', pago: 'Tarjeta', metodo: 'Visa •••• 5067', total: 455.6, estado: 'Enviado SUNAT', fecha: '30/09/2026', hora: '21:58', cdr: '21:59' },
    { id: 'b001-04204', comprobante: 'B001-04204', tipo: 'Boleta', cliente: 'Ana Paredes', documento: 'DNI 48712036', email: 'ana.paredes@outlook.com', pago: 'Plin', metodo: 'Plin •••• 7742', total: 134.9, estado: 'Emitido', fecha: '30/09/2026', hora: '21:41', cdr: '21:42' },
    { id: 'b001-04203', comprobante: 'B001-04203', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 88.4, estado: 'Emitido', fecha: '30/09/2026', hora: '21:22', cdr: '21:23' },
    { id: 'f001-00834', comprobante: 'F001-00834', tipo: 'Factura', cliente: 'Distribuidora El Sol S.R.L.', documento: 'RUC 20441209876', email: 'cuentas@delsol.pe', pago: 'Visa', metodo: 'Visa •••• 8891', total: 768.2, estado: 'Emitido', fecha: '29/09/2026', hora: '20:47', cdr: '20:48' },
    { id: 'b001-04202', comprobante: 'B001-04202', tipo: 'Boleta', cliente: 'Fernando Cárdenas', documento: 'DNI 41209873', email: 'f.cardenas@gmail.com', pago: 'Yape', metodo: 'Yape •••• 2019', total: 165.3, estado: 'Emitido', fecha: '29/09/2026', hora: '20:31', cdr: '20:32' },
    { id: 'b001-04201', comprobante: 'B001-04201', tipo: 'Boleta', cliente: 'Cliente varios', documento: 'Sin identificar', email: '—', pago: 'Efectivo', metodo: 'Efectivo', total: 142, estado: 'Emitido', fecha: '29/09/2026', hora: '20:12', cdr: '20:13' },
    { id: 'b001-04200', comprobante: 'B001-04200', tipo: 'Boleta', cliente: 'Valeria Núñez', documento: 'DNI 75609821', email: 'valeria.nunez@gmail.com', pago: 'Plin', metodo: 'Plin •••• 5508', total: 119.6, estado: 'Emitido', fecha: '29/09/2026', hora: '19:55', cdr: '19:56' },
  ];

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoria = this.categoria();
    const fecha = this.fecha();

    return this.comprobantes.filter((row) => {
      const matchesTerm =
        term === '' ||
        row.comprobante.toLowerCase().includes(term) ||
        row.cliente.toLowerCase().includes(term) ||
        row.pago.toLowerCase().includes(term);
      const matchesCategoria = categoria === 'todas' || row.tipo === categoria;
      const matchesFecha = row.fecha === this.toFechaKey(fecha);
      return matchesTerm && matchesCategoria && matchesFecha;
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
      return 'Mostrando 0–0 de 0 registros';
    }
    const from = (this.page() - 1) * this.pageSize + 1;
    const to = Math.min(this.page() * this.pageSize, total);
    return `Mostrando ${from}–${to} de ${total} registros`;
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

  readonly filterSummary = computed(() => {
    const categoria = this.categoria();
    const label =
      categoria === 'todas' ? 'Todos los comprobantes' : `${categoria}s`;
    return `${this.fecha()} · ${label}`;
  });

  readonly selected = computed(() => {
    return (
      this.comprobantes.find((row) => row.id === this.selectedId()) ??
      this.comprobantes[0]
    );
  });

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
    this.page.set(1);
  }

  onCategoria(event: Event): void {
    this.categoria.set((event.target as HTMLSelectElement).value as CategoriaFiltro);
    this.page.set(1);
  }

  onFecha(event: Event): void {
    this.fecha.set((event.target as HTMLSelectElement).value);
    this.page.set(1);
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.categoria.set('todas');
    this.fecha.set('01 oct 2026');
    this.page.set(1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }
    this.page.set(page);
  }

  select(row: Comprobante): void {
    this.selectedId.set(row.id);
  }

  subtotal(row: Comprobante): string {
    return this.money(Math.round(row.total / 1.18 * 100) / 100);
  }

  igv(row: Comprobante): string {
    return this.money(Math.round((row.total - row.total / 1.18) * 100) / 100);
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
    void this.router.navigate(['/']);
  }

  private toFechaKey(label: string): string {
    const map: Record<string, string> = {
      '01 oct 2026': '01/10/2026',
      '30 sep 2026': '30/09/2026',
      '29 sep 2026': '29/09/2026',
    };
    return map[label] ?? label;
  }
}
