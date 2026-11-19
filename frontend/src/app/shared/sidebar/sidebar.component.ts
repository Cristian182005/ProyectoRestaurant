import { Component, EventEmitter, Input, Output, computed, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Modulo, PermisosService } from '../../core/permisos.service';
import { SesionService } from '../../core/sesion.service';

interface NavItem {
  label: string;
  icon: string[];
  route?: string;
  modulo: Modulo;
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
})
export class SidebarComponent {
  private readonly router = inject(Router);
  private readonly permisos = inject(PermisosService);
  readonly sesion = inject(SesionService);

  @Input() activeSection = 'Resumen';
  @Input() open = false;

  @Output() sectionChange = new EventEmitter<string>();
  @Output() close = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  readonly navItems: NavItem[] = [
    {
      label: 'Resumen',
      route: '/dashboard',
      modulo: 'dashboard',
      icon: ['M4 4h6v6H4z', 'M14 4h6v6h-6z', 'M14 14h6v6h-6z', 'M4 14h6v6H4z'],
    },
    {
      label: 'Salón',
      route: '/salon',
      modulo: 'salon',
      icon: [
        'M12 3v18',
        'M3 9h18',
        'M9 21V9',
        'M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z',
      ],
    },
    {
      label: 'Caja y ventas',
      route: '/caja-ventas',
      modulo: 'caja',
      icon: [
        'M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z',
        'M13.5 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z',
        'M6 9.5h.01',
        'M18 14.5h.01',
      ],
    },
    {
      label: 'Pedidos',
      route: '/pedidos',
      modulo: 'pedidos',
      icon: [
        'M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z',
        'M14 3v5h5',
        'M9 13h6',
        'M9 17h4',
      ],
    },
    {
      label: 'Inventario',
      route: '/inventario',
      modulo: 'inventario',
      icon: ['M21 8l-9-5-9 5 9 5 9-5z', 'M3 8v8l9 5 9-5V8', 'M12 13v8'],
    },
    {
      label: 'Productos y recetas',
      route: '/productos-recetas',
      modulo: 'productos',
      icon: [
        'M3 2v7a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V2',
        'M7 2v20',
        'M21 15V2a5 5 0 0 0-5 5v6a2 2 0 0 0 2 2h3Z',
        'M21 15v7',
      ],
    },
    {
      label: 'Reportes',
      route: '/reportes',
      modulo: 'reportes',
      icon: ['M3 3v18h18', 'M18 17V9', 'M13 17V6', 'M8 17v-3'],
    },
    {
      label: 'Usuarios y roles',
      route: '/usuarios-roles',
      modulo: 'usuarios',
      icon: [
        'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2',
        'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
        'M22 21v-2a4 4 0 0 0-3-3.87',
        'M16 3.13a4 4 0 0 1 0 7.75',
      ],
    },
  ];

  readonly navItemsVisibles = computed(() =>
    this.navItems.filter((item) => this.permisos.puede(item.modulo)),
  );

  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (!(event instanceof NavigationEnd)) {
        return;
      }
      const section = this.sectionFromUrl();
      if (section && section !== this.activeSection) {
        this.sectionChange.emit(section);
      }
    });
  }

  selectSection(item: NavItem): void {
    this.sectionChange.emit(item.label);
    this.close.emit();
    if (item.route) {
      void this.router.navigate([item.route]);
    }
  }

  /** Cierra la sesión (limpia el JWT) y notifica a la página para navegar al login. */
  selectLogout(): void {
    this.sesion.cerrarSesion();
    this.close.emit();
    this.logout.emit();
  }

  private sectionFromUrl(): string | null {
    if (this.router.url.startsWith('/salon')) {
      return 'Salón';
    }
    if (this.router.url.startsWith('/caja-ventas')) {
      return 'Caja y ventas';
    }
    if (this.router.url.startsWith('/pedidos')) {
      return 'Pedidos';
    }
    if (this.router.url.startsWith('/inventario')) {
      return 'Inventario';
    }
    if (this.router.url.startsWith('/productos-recetas')) {
      return 'Productos y recetas';
    }
    if (this.router.url.startsWith('/reportes')) {
      return 'Reportes';
    }
    if (this.router.url.startsWith('/usuarios-roles')) {
      return 'Usuarios y roles';
    }
    if (this.router.url.startsWith('/dashboard')) {
      return 'Resumen';
    }
    return null;
  }
}
