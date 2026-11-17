import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

type TableStatus = 'libre' | 'ocupada' | 'reservada' | 'por-cobrar';
type ZoneKey = 'todas' | 'principal' | 'terraza' | 'privado';

interface SalonTable {
  id: string;
  label: string;
  zone: string;
  zoneKey: Exclude<ZoneKey, 'todas'>;
  capacity: number;
  status: TableStatus;
  title: string;
  subtitle: string;
  amount: string;
}

interface MetricCard {
  value: string;
  label: string;
  icon: string[];
}

interface ZoneFilter {
  key: ZoneKey;
  label: string;
}

@Component({
  selector: 'app-salon',
  imports: [SidebarComponent],
  templateUrl: './salon.component.html',
  styleUrl: './salon.component.css',
})
export class SalonComponent {
  private readonly router = inject(Router);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Salón');
  readonly activeZone = signal<ZoneKey>('todas');
  readonly draggingId = signal<string | null>(null);
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly zoneFilters: ZoneFilter[] = [
    { key: 'todas', label: 'Todas' },
    { key: 'principal', label: 'Salón principal' },
    { key: 'terraza', label: 'Terraza' },
    { key: 'privado', label: 'Privado' },
  ];

  readonly metrics: MetricCard[] = [
    {
      value: '86',
      label: 'Comensales en salón',
      icon: [
        'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2',
        'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
        'M22 21v-2a4 4 0 0 0-3-3.87',
        'M16 3.13a4 4 0 0 1 0 7.75',
      ],
    },
    {
      value: '1 h 12 min',
      label: 'Rotación promedio',
      icon: [
        'M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8',
        'M21 3v5h-5',
        'M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16',
        'M3 21v-5h5',
      ],
    },
    {
      value: '3',
      label: 'Reservas próximas',
      icon: [
        'M8 2v4',
        'M16 2v4',
        'M3 10h18',
        'M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z',
      ],
    },
    {
      value: 'S/ 5,945',
      label: 'Venta abierta',
      icon: [
        'M9 4H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2',
        'M9 4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v1H9z',
        'M9 12h6',
        'M9 16h4',
      ],
    },
  ];

  readonly statusLabels: Record<TableStatus, string> = {
    libre: 'Libre',
    ocupada: 'Ocupada',
    reservada: 'Reservada',
    'por-cobrar': 'Por cobrar',
  };

  readonly tables = signal<SalonTable[]>([
    { id: 'm01', label: '01', zone: 'Salón principal', zoneKey: 'principal', capacity: 4, status: 'ocupada', title: 'J. Rojas', subtitle: '42 min', amount: 'S/ 186' },
    { id: 'm02', label: '02', zone: 'Salón principal', zoneKey: 'principal', capacity: 2, status: 'por-cobrar', title: 'M. Díaz', subtitle: '58 min', amount: 'S/ 94' },
    { id: 'm03', label: '03', zone: 'Salón principal', zoneKey: 'principal', capacity: 4, status: 'ocupada', title: 'J. Rojas', subtitle: '31 min', amount: 'S/ 321' },
    { id: 'm04', label: '04', zone: 'Salón principal', zoneKey: 'principal', capacity: 6, status: 'reservada', title: '20:00 · Salazar', subtitle: 'Salón principal', amount: '—' },
    { id: 'm05', label: '05', zone: 'Salón principal', zoneKey: 'principal', capacity: 4, status: 'ocupada', title: 'A. León', subtitle: '27 min', amount: 'S/ 146' },
    { id: 'm06', label: '06', zone: 'Salón principal', zoneKey: 'principal', capacity: 4, status: 'libre', title: 'Disponible para asignar', subtitle: 'Salón principal', amount: '—' },
    { id: 'm07', label: '07', zone: 'Salón principal', zoneKey: 'principal', capacity: 6, status: 'ocupada', title: 'M. Díaz', subtitle: '49 min', amount: 'S/ 372' },
    { id: 'm08', label: '08', zone: 'Salón principal', zoneKey: 'principal', capacity: 2, status: 'ocupada', title: 'A. León', subtitle: '22 min', amount: 'S/ 118' },
    { id: 'm09', label: '09', zone: 'Salón principal', zoneKey: 'principal', capacity: 4, status: 'por-cobrar', title: 'J. Rojas', subtitle: '71 min', amount: 'S/ 214' },
    { id: 'm10', label: '10', zone: 'Salón principal', zoneKey: 'principal', capacity: 4, status: 'ocupada', title: 'M. Díaz', subtitle: '36 min', amount: 'S/ 172' },
    { id: 'm11', label: '11', zone: 'Salón principal', zoneKey: 'principal', capacity: 2, status: 'reservada', title: '20:30 · Paredes', subtitle: 'Salón principal', amount: '—' },
    { id: 'm12', label: '12', zone: 'Salón principal', zoneKey: 'principal', capacity: 8, status: 'ocupada', title: 'A. León', subtitle: '53 min', amount: 'S/ 488' },
    { id: 't01', label: 'T1', zone: 'Terraza', zoneKey: 'terraza', capacity: 4, status: 'ocupada', title: 'C. Soto', subtitle: '33 min', amount: 'S/ 198' },
    { id: 't02', label: 'T2', zone: 'Terraza', zoneKey: 'terraza', capacity: 4, status: 'ocupada', title: 'C. Soto', subtitle: '26 min', amount: 'S/ 162' },
    { id: 't03', label: 'T3', zone: 'Terraza', zoneKey: 'terraza', capacity: 2, status: 'libre', title: 'Disponible para asignar', subtitle: 'Terraza', amount: '—' },
    { id: 't04', label: 'T4', zone: 'Terraza', zoneKey: 'terraza', capacity: 6, status: 'ocupada', title: 'M. Díaz', subtitle: '44 min', amount: 'S/ 321' },
    { id: 't05', label: 'T5', zone: 'Terraza', zoneKey: 'terraza', capacity: 4, status: 'ocupada', title: 'C. Soto', subtitle: '19 min', amount: 'S/ 154' },
    { id: 't06', label: 'T6', zone: 'Terraza', zoneKey: 'terraza', capacity: 4, status: 'por-cobrar', title: 'A. León', subtitle: '66 min', amount: 'S/ 208' },
    { id: 't07', label: 'T7', zone: 'Terraza', zoneKey: 'terraza', capacity: 6, status: 'ocupada', title: 'C. Soto', subtitle: '39 min', amount: 'S/ 292' },
    { id: 't08', label: 'T8', zone: 'Terraza', zoneKey: 'terraza', capacity: 2, status: 'reservada', title: '21:00 · Vega', subtitle: 'Terraza', amount: '—' },
    { id: 'p01', label: 'P1', zone: 'Privado', zoneKey: 'privado', capacity: 8, status: 'ocupada', title: 'J. Rojas', subtitle: '55 min', amount: 'S/ 536' },
    { id: 'p02', label: 'P2', zone: 'Privado', zoneKey: 'privado', capacity: 10, status: 'ocupada', title: 'M. Díaz', subtitle: '47 min', amount: 'S/ 624' },
    { id: 'p03', label: 'P3', zone: 'Privado', zoneKey: 'privado', capacity: 8, status: 'libre', title: 'Disponible para asignar', subtitle: 'Privado', amount: '—' },
    { id: 'p04', label: 'P4', zone: 'Privado', zoneKey: 'privado', capacity: 12, status: 'ocupada', title: 'A. León', subtitle: '61 min', amount: 'S/ 792' },
  ]);

  readonly filteredTables = computed(() => {
    const zone = this.activeZone();
    return zone === 'todas'
      ? this.tables()
      : this.tables().filter((table) => table.zoneKey === zone);
  });

  readonly zoneCounts = computed(() => {
    const counts: Record<ZoneKey, number> = { todas: 0, principal: 0, terraza: 0, privado: 0 };
    for (const table of this.tables()) {
      counts.todas += 1;
      counts[table.zoneKey] += 1;
    }
    return counts;
  });

  readonly statusCounts = computed(() => {
    const counts: Record<TableStatus, number> = { libre: 0, ocupada: 0, reservada: 0, 'por-cobrar': 0 };
    for (const table of this.filteredTables()) {
      counts[table.status] += 1;
    }
    return counts;
  });

  onDragStart(event: DragEvent, id: string): void {
    this.draggingId.set(id);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', id);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  onDrop(targetId: string): void {
    const sourceId = this.draggingId();
    this.draggingId.set(null);
    if (!sourceId || sourceId === targetId) {
      return;
    }

    this.tables.update((tables) => {
      const next = [...tables];
      const from = next.findIndex((table) => table.id === sourceId);
      if (from === -1) {
        return tables;
      }
      const [moved] = next.splice(from, 1);
      const to = next.findIndex((table) => table.id === targetId);
      next.splice(to === -1 ? next.length : to, 0, moved);
      return next;
    });
  }

  onDragEnd(): void {
    this.draggingId.set(null);
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
}
