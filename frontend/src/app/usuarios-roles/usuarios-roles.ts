import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SesionService } from '../core/sesion.service';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

export type RolUsuario = 'ADMINISTRADOR' | 'CAJERO' | 'MOZO';
export type EstadoUsuario = 'ACTIVO' | 'SUSPENDIDO';
export type TonoKpi = 'terra' | 'ok' | 'info';

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  iniciales: string;
  rol: RolUsuario;
  estado: EstadoUsuario;
  ultimoAcceso: string;
}

export interface Permiso {
  clave: string;
  label: string;
  habilitado: boolean;
}

export interface RegistroAuditoria {
  hora: string;
  usuario: string;
  accion: string;
}

interface KpiItem {
  label: string;
  value: string;
  tone: TonoKpi;
  icon: string[];
}

const DESCRIPCIONES: Record<RolUsuario, string> = {
  MOZO: 'Gestiona mesas y comandas del salón asignado. No accede a caja, descuentos ni costos.',
  CAJERO: 'Emite boletas y notas de venta, controla la caja del turno y sus arqueos.',
  ADMINISTRADOR: 'Control total del sistema: usuarios, permisos, configuración y trazabilidad.',
};

const PERMISOS_BASE: Record<RolUsuario, Permiso[]> = {
  ADMINISTRADOR: [
    { clave: 'dashboard', label: 'Resumen', habilitado: true },
    { clave: 'salon', label: 'Salón y mesas', habilitado: true },
    { clave: 'pedidos', label: 'Comandas y pedidos', habilitado: true },
    { clave: 'caja', label: 'Caja y comprobantes', habilitado: true },
    { clave: 'inventario', label: 'Inventario', habilitado: true },
    { clave: 'productos', label: 'Productos y recetas', habilitado: true },
    { clave: 'reportes', label: 'Reportes', habilitado: true },
    { clave: 'usuarios', label: 'Usuarios y permisos', habilitado: true },
  ],
  CAJERO: [
    { clave: 'dashboard', label: 'Resumen', habilitado: true },
    { clave: 'salon', label: 'Salón y mesas', habilitado: false },
    { clave: 'pedidos', label: 'Comandas y pedidos', habilitado: false },
    { clave: 'caja', label: 'Caja y comprobantes', habilitado: true },
    { clave: 'inventario', label: 'Inventario', habilitado: false },
    { clave: 'productos', label: 'Productos y recetas', habilitado: false },
    { clave: 'reportes', label: 'Reportes', habilitado: true },
    { clave: 'usuarios', label: 'Usuarios y permisos', habilitado: false },
  ],
  MOZO: [
    { clave: 'dashboard', label: 'Resumen', habilitado: true },
    { clave: 'salon', label: 'Salón y mesas', habilitado: true },
    { clave: 'pedidos', label: 'Comandas y pedidos', habilitado: true },
    { clave: 'caja', label: 'Caja y comprobantes', habilitado: false },
    { clave: 'inventario', label: 'Inventario', habilitado: false },
    { clave: 'productos', label: 'Productos y recetas', habilitado: true },
    { clave: 'reportes', label: 'Reportes', habilitado: false },
    { clave: 'usuarios', label: 'Usuarios y permisos', habilitado: false },
  ],
};

const USUARIOS_BASE: Usuario[] = [
  {
    id: 1,
    nombre: 'Lucía Mendoza',
    email: 'lucia.mendoza@elhornero.pe',
    iniciales: 'LM',
    rol: 'ADMINISTRADOR',
    estado: 'ACTIVO',
    ultimoAcceso: 'Hoy · 18:02',
  },
  {
    id: 2,
    nombre: 'Rosa Huamán',
    email: 'rosa.huaman@elhornero.pe',
    iniciales: 'RH',
    rol: 'CAJERO',
    estado: 'ACTIVO',
    ultimoAcceso: 'Hoy · 17:48',
  },
  {
    id: 3,
    nombre: 'Javier Rojas',
    email: 'javier.rojas@elhornero.pe',
    iniciales: 'JR',
    rol: 'MOZO',
    estado: 'ACTIVO',
    ultimoAcceso: 'Hoy · 17:55',
  },
  {
    id: 4,
    nombre: 'Mariela Díaz',
    email: 'mariela.diaz@elhornero.pe',
    iniciales: 'MD',
    rol: 'MOZO',
    estado: 'ACTIVO',
    ultimoAcceso: 'Hoy · 18:06',
  },
  {
    id: 5,
    nombre: 'Carlos Soto',
    email: 'carlos.soto@elhornero.pe',
    iniciales: 'CS',
    rol: 'MOZO',
    estado: 'ACTIVO',
    ultimoAcceso: 'Hoy · 18:10',
  },
  {
    id: 6,
    nombre: 'Diego Salazar',
    email: 'diego.salazar@elhornero.pe',
    iniciales: 'DS',
    rol: 'CAJERO',
    estado: 'SUSPENDIDO',
    ultimoAcceso: '28 sep · 23:40',
  },
];

const AUDITORIA_BASE: RegistroAuditoria[] = [
  {
    hora: '20:43',
    usuario: 'Rosa Huamán',
    accion: 'Emitió boleta B001-008421',
  },
  {
    hora: '20:41',
    usuario: 'Lucía Mendoza',
    accion: 'Autorizó descuento 10%',
  },
  {
    hora: '19:43',
    usuario: 'Javier Rojas',
    accion: 'Envió comanda C-1048',
  },
  {
    hora: '18:06',
    usuario: 'Mariela Díaz',
    accion: 'Inició turno · Tablet 02',
  },
];

@Component({
  selector: 'app-usuarios-roles',
  imports: [SidebarComponent],
  templateUrl: './usuarios-roles.html',
  styleUrl: './usuarios-roles.css',
})
export class UsuariosRoles {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);

  private avisoTimer: ReturnType<typeof setTimeout> | null = null;

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Usuarios y roles');
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly searchTerm = signal('');
  readonly usuarios = signal<Usuario[]>(USUARIOS_BASE);
  readonly rolSeleccionado = signal<RolUsuario>('MOZO');
  readonly permisosPorRol = signal<Record<RolUsuario, Permiso[]>>({
    ADMINISTRADOR: PERMISOS_BASE['ADMINISTRADOR'].map((p) => ({ ...p })),
    CAJERO: PERMISOS_BASE['CAJERO'].map((p) => ({ ...p })),
    MOZO: PERMISOS_BASE['MOZO'].map((p) => ({ ...p })),
  });
  readonly editandoPermisos = signal(false);
  readonly auditoria = signal<RegistroAuditoria[]>(AUDITORIA_BASE);

  readonly modalAbierto = signal(false);
  readonly formNombre = signal('');
  readonly formCorreo = signal('');
  readonly formRol = signal<RolUsuario>('MOZO');
  readonly formError = signal('');

  readonly aviso = signal('');

  readonly rolDescripcion = computed(() => DESCRIPCIONES[this.rolSeleccionado()]);

  readonly permisosDelRol = computed(() => this.permisosPorRol()[this.rolSeleccionado()]);

  readonly permisosActivos = computed(
    () => this.permisosDelRol().filter((permiso) => permiso.habilitado).length,
  );

  readonly usuariosDelRol = computed(
    () => this.usuarios().filter((usuario) => usuario.rol === this.rolSeleccionado()).length,
  );

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) {
      return this.usuarios();
    }
    return this.usuarios().filter(
      (usuario) =>
        usuario.nombre.toLowerCase().includes(term) ||
        usuario.email.toLowerCase().includes(term) ||
        usuario.rol.toLowerCase().includes(term) ||
        usuario.estado.toLowerCase().includes(term),
    );
  });

  readonly kpis = computed<KpiItem[]>(() => {
    const lista = this.usuarios();
    const contar = (rol: RolUsuario) => lista.filter((usuario) => usuario.rol === rol).length;

    return [
      {
        label: 'Usuarios activos',
        value: String(lista.filter((usuario) => usuario.estado === 'ACTIVO').length),
        tone: 'terra',
        icon: [
          'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2',
          'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
          'M22 21v-2a4 4 0 0 0-3-3.87',
          'M16 3.13a4 4 0 0 1 0 7.75',
        ],
      },
      {
        label: 'Administradores',
        value: String(contar('ADMINISTRADOR')),
        tone: 'ok',
        icon: ['M12 3l7 3v5c0 4.4-3 8.3-7 9.5-4-1.2-7-5.1-7-9.5V6z', 'M9.4 12.2l1.9 1.9 3.5-3.6'],
      },
      {
        label: 'Mozos',
        value: String(contar('MOZO')),
        tone: 'terra',
        icon: [
          'M9 4H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2',
          'M9 4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v1H9z',
          'M9 11h6',
          'M9 15h4',
        ],
      },
      {
        label: 'Cajeros',
        value: String(contar('CAJERO')),
        tone: 'info',
        icon: [
          'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z',
          'M12 7v10',
          'M14.5 9.2c-.5-.8-1.5-1.3-2.6-1.3-1.4 0-2.5.8-2.5 1.9 0 1.2 1.1 1.7 2.5 2 1.5.3 2.6.8 2.6 2s-1.2 2-2.6 2c-1.1 0-2.1-.5-2.6-1.3',
        ],
      },
    ];
  });

  readonly kpiTone: Record<TonoKpi, { box: string; value: string }> = {
    terra: { box: 'bg-terra-100 text-terra-600', value: 'text-terra-600' },
    ok: { box: 'bg-ok-100 text-ok-700', value: 'text-ok-700' },
    info: { box: 'bg-info-100 text-info-700', value: 'text-info-700' },
  };

  readonly rolOptions: RolUsuario[] = ['MOZO', 'CAJERO', 'ADMINISTRADOR'];

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

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  seleccionarUsuario(usuario: Usuario): void {
    if (usuario.rol !== this.rolSeleccionado()) {
      this.rolSeleccionado.set(usuario.rol);
      this.editandoPermisos.set(false);
    }
  }

  seleccionarRol(rol: RolUsuario): void {
    if (rol !== this.rolSeleccionado()) {
      this.rolSeleccionado.set(rol);
      this.editandoPermisos.set(false);
    }
  }

  alternarPermiso(clave: string): void {
    if (!this.editandoPermisos()) {
      return;
    }
    const rol = this.rolSeleccionado();
    this.permisosPorRol.update((mapa) => ({
      ...mapa,
      [rol]: mapa[rol].map((permiso) =>
        permiso.clave === clave ? { ...permiso, habilitado: !permiso.habilitado } : permiso,
      ),
    }));
  }

  onEditarPermisos(): void {
    const rol = this.rolSeleccionado();

    if (!this.editandoPermisos()) {
      this.editandoPermisos.set(true);
      return;
    }

    this.editandoPermisos.set(false);
    this.registrarAuditoria(`Actualizó permisos del rol ${rol}`);
    this.mostrarAviso(`Permisos de ${rol} actualizados correctamente.`);
  }

  abrirModal(): void {
    this.formNombre.set('');
    this.formCorreo.set('');
    this.formRol.set('MOZO');
    this.formError.set('');
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.formError.set('');
  }

  onFormNombre(event: Event): void {
    this.formNombre.set((event.target as HTMLInputElement).value);
  }

  onFormCorreo(event: Event): void {
    this.formCorreo.set((event.target as HTMLInputElement).value);
  }

  onFormRol(event: Event): void {
    this.formRol.set((event.target as HTMLSelectElement).value as RolUsuario);
  }

  crearUsuario(): void {
    const nombre = this.formNombre().trim();
    const correo = this.formCorreo().trim().toLowerCase();
    const rol = this.formRol();

    if (!nombre) {
      this.formError.set('Ingresa el nombre completo del usuario.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
      this.formError.set('Ingresa un correo corporativo válido.');
      return;
    }

    if (this.usuarios().some((usuario) => usuario.email === correo)) {
      this.formError.set('Ya existe un usuario registrado con ese correo.');
      return;
    }

    const siguienteId = this.usuarios().reduce((max, usuario) => Math.max(max, usuario.id), 0) + 1;

    const nuevo: Usuario = {
      id: siguienteId,
      nombre,
      email: correo,
      iniciales: this.calcularIniciales(nombre),
      rol,
      estado: 'ACTIVO',
      ultimoAcceso: 'Sin acceso aún',
    };

    this.usuarios.update((lista) => [...lista, nuevo]);
    this.registrarAuditoria(`Creó el usuario ${nombre}`);
    this.cerrarModal();
    this.mostrarAviso(`Usuario ${nombre} creado correctamente.`);
  }

  private calcularIniciales(nombre: string): string {
    return nombre
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte.charAt(0).toUpperCase())
      .join('');
  }

  private registrarAuditoria(accion: string): void {
    const ahora = new Date()
      .toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false })
      .replace('.', ':');

    this.auditoria.update((lista) => [{ hora: ahora, usuario: 'Lucía Mendoza', accion }, ...lista]);
  }

  private mostrarAviso(texto: string): void {
    this.aviso.set(texto);

    if (this.avisoTimer) {
      clearTimeout(this.avisoTimer);
    }

    this.avisoTimer = setTimeout(() => {
      this.aviso.set('');
      this.avisoTimer = null;
    }, 3200);
  }
}
