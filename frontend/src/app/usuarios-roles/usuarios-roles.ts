import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MODULOS, Modulo, PermisosService, ROLES_POR_MODULO } from '../core/permisos.service';
import { SesionService } from '../core/sesion.service';
import { RolUsuario, Usuario, UsuarioService } from '../core/usuario.service';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

export type EstadoKpi = 'terra' | 'ok' | 'info';

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

type ModoModal = 'crear' | 'editar' | 'password';

interface KpiItem {
  label: string;
  value: string;
  tone: EstadoKpi;
  icon: string[];
}

const DESCRIPCIONES: Record<RolUsuario, string> = {
  MOZO: 'Gestiona mesas y comandas del salón asignado. No accede a caja, descuentos ni costos.',
  CAJERO: 'Emite boletas y notas de venta, controla la caja del turno y sus arqueos.',
  ADMINISTRADOR: 'Control total del sistema: usuarios, permisos, configuración y trazabilidad.',
};

const ETIQUETAS_MODULO: Record<Modulo, string> = {
  dashboard: 'Dashboard',
  salon: 'Salón y mesas',
  pedidos: 'Comandas y pedidos',
  caja: 'Caja y comprobantes',
  inventario: 'Inventario',
  productos: 'Productos y recetas',
  reportes: 'Reportes',
  usuarios: 'Usuarios y permisos',
};

@Component({
  selector: 'app-usuarios-roles',
  imports: [SidebarComponent],
  templateUrl: './usuarios-roles.html',
  styleUrl: './usuarios-roles.css',
})
export class UsuariosRoles {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);
  private readonly usuariosService = inject(UsuarioService);
  private readonly permisos = inject(PermisosService);

  private avisoTimer: ReturnType<typeof setTimeout> | null = null;

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Usuarios y roles');
  readonly dateLabel = new Date().toLocaleDateString('es-PE', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });

  readonly searchTerm = signal('');
  readonly usuarios = signal<Usuario[]>([]);
  readonly cargando = signal(false);
  readonly cargandoAccion = signal(false);
  readonly errorLista = signal('');

  readonly rolSeleccionado = signal<RolUsuario>('MOZO');
  readonly auditoria = signal<RegistroAuditoria[]>([]);

  readonly modalModo = signal<ModoModal | null>(null);
  readonly usuarioEditado = signal<Usuario | null>(null);
  readonly formNombre = signal('');
  readonly formCorreo = signal('');
  readonly formPassword = signal('');
  readonly formTelefono = signal('');
  readonly formDireccion = signal('');
  readonly formRol = signal<RolUsuario>('MOZO');
  readonly formError = signal('');

  readonly aviso = signal('');

  readonly rolDescripcion = computed(() => DESCRIPCIONES[this.rolSeleccionado()]);

  /** Matriz real de acceso que aplica el backend (roleGuard + SecurityConfig). */
  readonly permisosDelRol = computed<Permiso[]>(() =>
    MODULOS.map((modulo) => ({
      clave: modulo,
      label: ETIQUETAS_MODULO[modulo],
      habilitado: ROLES_POR_MODULO[modulo].includes(this.rolSeleccionado()),
    })),
  );

  readonly permisosActivos = computed(
    () => this.permisosDelRol().filter((permiso) => permiso.habilitado).length,
  );

  readonly usuariosDelRol = computed(
    () => this.usuarios().filter((usuario) => usuario.rol === this.rolSeleccionado()).length,
  );

  readonly filtered = computed(() => this.usuarios());

  readonly kpis = computed<KpiItem[]>(() => {
    const lista = this.usuarios();
    const contar = (rol: RolUsuario) => lista.filter((usuario) => usuario.rol === rol).length;

    return [
      {
        label: 'Usuarios registrados',
        value: String(lista.length),
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

  readonly kpiTone: Record<EstadoKpi, { box: string; value: string }> = {
    terra: { box: 'bg-terra-100 text-terra-600', value: 'text-terra-600' },
    ok: { box: 'bg-ok-100 text-ok-700', value: 'text-ok-700' },
    info: { box: 'bg-info-100 text-info-700', value: 'text-info-700' },
  };

  readonly rolOptions: RolUsuario[] = ['MOZO', 'CAJERO', 'ADMINISTRADOR'];

  constructor() {
    this.cargar();
  }

  // ----------------------------------------------------------------
  // Listado
  // ----------------------------------------------------------------

  cargar(buscar?: string): void {
    this.cargando.set(true);
    this.errorLista.set('');
    this.usuariosService.listar(buscar).subscribe({
      next: (lista) => {
        this.cargando.set(false);
        this.usuarios.set(lista);
      },
      error: (mensaje: string) => {
        this.cargando.set(false);
        this.errorLista.set(mensaje);
      },
    });
  }

  onSearch(event: Event): void {
    const termino = (event.target as HTMLInputElement).value;
    this.searchTerm.set(termino);
    this.cargar(termino.trim() || undefined);
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

  seleccionarUsuario(usuario: Usuario): void {
    if (usuario.rol !== this.rolSeleccionado()) {
      this.rolSeleccionado.set(usuario.rol);
    }
  }

  seleccionarRol(rol: RolUsuario): void {
    this.rolSeleccionado.set(rol);
  }

  inicialesDe(usuario: Usuario): string {
    const origen = (usuario.nombre ?? usuario.email).trim();
    return origen
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte.charAt(0).toUpperCase())
      .join('');
  }

  /** El backend rechaza que un usuario modifique su propio rol. */
  esElUsuarioActual(usuario: Usuario | null): boolean {
    return !!usuario && usuario.id === this.sesion.id();
  }

  // ----------------------------------------------------------------
  // Modal de creación
  // ----------------------------------------------------------------

  abrirCrear(): void {
    this.formNombre.set('');
    this.formCorreo.set('');
    this.formPassword.set('');
    this.formTelefono.set('');
    this.formDireccion.set('');
    this.formRol.set('MOZO');
    this.formError.set('');
    this.usuarioEditado.set(null);
    this.modalModo.set('crear');
  }

  abrirEditar(usuario: Usuario): void {
    this.formNombre.set(usuario.nombre ?? '');
    this.formCorreo.set(usuario.email);
    this.formPassword.set('');
    this.formTelefono.set(usuario.telefono ?? '');
    this.formDireccion.set(usuario.direccion ?? '');
    this.formRol.set(usuario.rol);
    this.formError.set('');
    this.usuarioEditado.set(usuario);
    this.modalModo.set('editar');
  }

  abrirPassword(usuario: Usuario): void {
    this.formPassword.set('');
    this.formError.set('');
    this.usuarioEditado.set(usuario);
    this.modalModo.set('password');
  }

  abrirPasswordDelEditado(): void {
    const usuario = this.usuarioEditado();
    if (usuario) {
      this.abrirPassword(usuario);
    }
  }

  cerrarModal(): void {
    this.modalModo.set(null);
    this.usuarioEditado.set(null);
    this.formError.set('');
  }

  onFormNombre(event: Event): void {
    this.formNombre.set((event.target as HTMLInputElement).value);
  }

  onFormCorreo(event: Event): void {
    this.formCorreo.set((event.target as HTMLInputElement).value);
  }

  onFormPassword(event: Event): void {
    this.formPassword.set((event.target as HTMLInputElement).value);
  }

  onFormTelefono(event: Event): void {
    this.formTelefono.set((event.target as HTMLInputElement).value);
  }

  onFormDireccion(event: Event): void {
    this.formDireccion.set((event.target as HTMLInputElement).value);
  }

  onFormRol(event: Event): void {
    this.formRol.set((event.target as HTMLSelectElement).value as RolUsuario);
  }

  guardar(): void {
    switch (this.modalModo()) {
      case 'crear':
        this.crearUsuario();
        break;
      case 'editar':
        this.editarUsuario();
        break;
      case 'password':
        this.cambiarPassword();
        break;
      default:
        break;
    }
  }

  private crearUsuario(): void {
    const nombre = this.formNombre().trim();
    const correo = this.formCorreo().trim();
    const password = this.formPassword();
    const rol = this.formRol();

    if (!nombre) {
      this.formError.set('Ingresa el nombre completo del usuario.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
      this.formError.set('Ingresa un correo corporativo válido.');
      return;
    }
    if (password.length < 6) {
      this.formError.set('La contraseña debe tener al menos 6 caracteres.');
      return;
    }

    this.formError.set('');
    this.cargandoAccion.set(true);

    this.usuariosService
      .crear({
        email: correo,
        password,
        nombre,
        telefono: this.formTelefono().trim() || null,
        direccion: this.formDireccion().trim() || null,
        rol,
      })
      .subscribe({
        next: (usuario) => {
          this.cargandoAccion.set(false);
          this.cargar();
          this.registrarAuditoria(`Creó el usuario ${usuario.nombre ?? usuario.email}`);
          this.cerrarModal();
          this.mostrarAviso(`Usuario ${usuario.nombre ?? usuario.email} creado correctamente.`);
        },
        error: (mensaje: string) => {
          this.cargandoAccion.set(false);
          this.formError.set(mensaje);
        },
      });
  }

  private editarUsuario(): void {
    const usuario = this.usuarioEditado();
    if (!usuario) {
      return;
    }

    const nombre = this.formNombre().trim();
    const correo = this.formCorreo().trim();
    if (!nombre) {
      this.formError.set('Ingresa el nombre completo del usuario.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) {
      this.formError.set('Ingresa un correo corporativo válido.');
      return;
    }

    this.formError.set('');
    this.cargandoAccion.set(true);

    this.usuariosService
      .actualizar(usuario.id, {
        nombre,
        email: correo,
        telefono: this.formTelefono().trim() || null,
        direccion: this.formDireccion().trim() || null,
      })
      .subscribe({
        next: () => {
          const rolCambiado = this.formRol() !== usuario.rol && !this.esElUsuarioActual(usuario);
          if (rolCambiado) {
            this.aplicarCambioDeRol(usuario);
            return;
          }
          this.finalizarEdicion(usuario, 'Actualizó los datos del usuario');
        },
        error: (mensaje: string) => {
          this.cargandoAccion.set(false);
          this.formError.set(mensaje);
        },
      });
  }

  private aplicarCambioDeRol(usuario: Usuario): void {
    this.usuariosService.cambiarRol(usuario.id, this.formRol()).subscribe({
      next: () => {
        this.finalizarEdicion(
          usuario,
          `Cambió el rol de ${usuario.nombre ?? usuario.email} a ${this.formRol()}`,
        );
      },
      error: (mensaje: string) => {
        this.cargandoAccion.set(false);
        this.formError.set(mensaje);
      },
    });
  }

  private finalizarEdicion(usuario: Usuario, accion: string): void {
    this.cargandoAccion.set(false);
    this.cargar();
    this.registrarAuditoria(accion);
    this.cerrarModal();
    this.mostrarAviso('Usuario actualizado correctamente.');
  }

  private cambiarPassword(): void {
    const usuario = this.usuarioEditado();
    if (!usuario) {
      return;
    }

    const password = this.formPassword();
    if (password.length < 6) {
      this.formError.set('La contraseña debe tener al menos 6 caracteres.');
      return;
    }

    this.formError.set('');
    this.cargandoAccion.set(true);

    this.usuariosService.cambiarPassword(usuario.id, password).subscribe({
      next: () => {
        this.cargandoAccion.set(false);
        this.registrarAuditoria(`Restableció la contraseña de ${usuario.nombre ?? usuario.email}`);
        this.cerrarModal();
        this.mostrarAviso('Contraseña actualizada correctamente.');
      },
      error: (mensaje: string) => {
        this.cargandoAccion.set(false);
        this.formError.set(mensaje);
      },
    });
  }

  // ----------------------------------------------------------------
  // Utilidades
  // ----------------------------------------------------------------

  private registrarAuditoria(accion: string): void {
    const ahora = new Date()
      .toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false })
      .replace('.', ':');

    this.auditoria.update((lista) => [
      { hora: ahora, usuario: this.sesion.email() ?? 'Sistema', accion },
      ...lista,
    ]);
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
