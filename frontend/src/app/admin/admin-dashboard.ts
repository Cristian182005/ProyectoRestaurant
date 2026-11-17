import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

interface NavItem {
  label: string;
  icon: string[];
}

interface KpiCard {
  label: string;
  value: string;
  detail: string;
  tone: 'terracotta' | 'blue' | 'green';
  icon: string[];
}

interface HourBar {
  label: string;
  height: number;
  peak?: boolean;
  tag?: string;
}

interface PaymentMethod {
  label: string;
  amount: string;
  value: number;
  color: string;
}

interface TopDish {
  name: string;
  portions: number;
  amount: string;
}

interface StockAlert {
  name: string;
  minimum: string;
  current: string;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.css',
})
export class AdminDashboard {
  private readonly router = inject(Router);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Resumen');
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly navItems: NavItem[] = [
    {
      label: 'Resumen',
      icon: [
        'M4 4h6v6H4z',
        'M14 4h6v6h-6z',
        'M14 14h6v6h-6z',
        'M4 14h6v6H4z',
      ],
    },
    {
      label: 'Salón',
      icon: [
        'M12 3v18',
        'M3 9h18',
        'M9 21V9',
        'M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z',
      ],
    },
    {
      label: 'Caja y ventas',
      icon: [
        'M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
        'M13.5 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z',
        'M6 9.5h.01',
        'M18 14.5h.01',
      ],
    },
    {
      label: 'Pedidos',
      icon: [
        'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z',
        'M14 3v5h5',
        'M9 13h6',
        'M9 17h4',
      ],
    },
    {
      label: 'Inventario',
      icon: ['M21 8l-9-5-9 5 9 5 9-5z', 'M3 8v8l9 5 9-5V8', 'M12 13v8'],
    },
    {
      label: 'Productos y recetas',
      icon: [
        'M3 2v7a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V2',
        'M7 2v20',
        'M21 15V2a5 5 0 0 0-5 5v6a2 2 0 0 0 2 2h3Z',
        'M21 15v7',
      ],
    },
    {
      label: 'Reportes',
      icon: ['M3 3v18h18', 'M18 17V9', 'M13 17V6', 'M8 17v-3'],
    },
    {
      label: 'Usuarios y roles',
      icon: [
        'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2',
        'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
        'M22 21v-2a4 4 0 0 0-3-3.87',
        'M16 3.13a4 4 0 0 1 0 7.75',
      ],
    },
  ];

  readonly kpis: KpiCard[] = [
    {
      label: 'Ventas del día',
      value: 'S/ 18,460',
      detail: '↑ 12.4% vs. jueves anterior',
      tone: 'terracotta',
      icon: [
        'M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
        'M3 10h18',
      ],
    },
    {
      label: 'Pedidos activos',
      value: '14',
      detail: '5 en cocina · 4 listos',
      tone: 'blue',
      icon: [
        'M9 4H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2',
        'M9 4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v1H9z',
        'M9 12h6',
        'M9 16h4',
      ],
    },
    {
      label: 'Ocupación de mesas',
      value: '18 / 24',
      detail: '75% · 3 por cobrar',
      tone: 'green',
      icon: [
        'M4 18h16',
        'M6 18c0-4.4 2.7-8 6-8s6 3.6 6 8',
        'M12 6v4',
      ],
    },
    {
      label: 'Ticket promedio',
      value: 'S/ 128.20',
      detail: '↑ S/ 8.40 en el turno',
      tone: 'terracotta',
      icon: ['M22 7l-8.5 8.5-5-5L2 17', 'M16 7h6v6'],
    },
  ];

  readonly hourBars: HourBar[] = [
    { label: '12 h', height: 48 },
    { label: '13 h', height: 78 },
    { label: '14 h', height: 64 },
    { label: '18 h', height: 54 },
    { label: '19 h', height: 88 },
    { label: '20 h', height: 130, peak: true, tag: 'S/ 3.6k' },
    { label: '21 h', height: 100 },
    { label: '22 h', height: 70 },
  ];

  readonly payments: PaymentMethod[] = [
    { label: 'Tarjeta', amount: 'S/ 8,740', value: 8740, color: '#b85327' },
    { label: 'Efectivo', amount: 'S/ 5,190', value: 5190, color: '#2b2018' },
    { label: 'Yape / Plin', amount: 'S/ 4,530', value: 4530, color: '#e0a94e' },
  ];

  readonly topDishes: TopDish[] = [
    { name: 'Bife Ancho', portions: 32, amount: 'S/ 3,168' },
    { name: 'Parrillada El Hornero', portions: 21, amount: 'S/ 3,129' },
    { name: 'Anticuchos', portions: 28, amount: 'S/ 1,652' },
    { name: 'Lomo fino', portions: 14, amount: 'S/ 1,666' },
  ];

  readonly stockAlerts: StockAlert[] = [
    { name: 'Lomo fino', minimum: 'Mín. 5 kg', current: '3.4 KG' },
    { name: 'Carbón vegetal', minimum: 'Mín. 12 kg', current: '8 KG' },
    { name: 'Chorizo artesanal', minimum: 'Mín. 18 und', current: '10 UND' },
  ];

  readonly maxPortions = this.topDishes.reduce(
    (max, dish) => Math.max(max, dish.portions),
    0,
  );

  private readonly totalPayments = this.payments.reduce(
    (sum, method) => sum + method.value,
    0,
  );

  readonly topShare = Math.round((this.payments[0].value / this.totalPayments) * 100);

  readonly donutGradient = this.buildDonutGradient();

  selectSection(item: NavItem): void {
    this.activeSection.set(item.label);
    this.sidebarOpen.set(false);
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

  indexLabel(position: number): string {
    return String(position).padStart(2, '0');
  }

  dishBarWidth(portions: number): number {
    if (this.maxPortions === 0) {
      return 0;
    }
    return Math.round((portions / this.maxPortions) * 62);
  }

  private buildDonutGradient(): string {
    const gap = 1.4;
    let cursor = 0;

    const stops = this.payments.map((method) => {
      const share = (method.value / this.totalPayments) * 100;
      const from = cursor + gap / 2;
      const to = cursor + share - gap / 2;
      cursor += share;
      return `${method.color} ${from.toFixed(2)}% ${to.toFixed(2)}%`;
    });

    return `conic-gradient(${stops.join(', ')})`;
  }
}
