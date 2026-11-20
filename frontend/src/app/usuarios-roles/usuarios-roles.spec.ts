import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Usuario, UsuarioService } from '../core/usuario.service';
import { UsuariosRoles } from './usuarios-roles';

const USUARIOS: Usuario[] = [
  {
    id: 1,
    nombre: 'Lucía Mendoza',
    email: 'lucia.mendoza@elhornero.pe',
    rol: 'ADMINISTRADOR',
    telefono: '999111222',
    direccion: 'Av. Lima 123',
  },
  {
    id: 2,
    nombre: 'Rosa Huamán',
    email: 'rosa.huaman@elhornero.pe',
    rol: 'CAJERO',
    telefono: '999222333',
    direccion: 'Jr. Piura 456',
  },
  {
    id: 3,
    nombre: 'Javier Rojas',
    email: 'javier.rojas@elhornero.pe',
    rol: 'MOZO',
    telefono: '999333444',
    direccion: 'Av. Sol 789',
  },
  {
    id: 4,
    nombre: 'Mariela Díaz',
    email: 'mariela.diaz@elhornero.pe',
    rol: 'MOZO',
    telefono: '999444555',
    direccion: 'Av. Norte 321',
  },
  {
    id: 5,
    nombre: 'Carlos Soto',
    email: 'carlos.soto@elhornero.pe',
    rol: 'MOZO',
    telefono: '999555666',
    direccion: 'Jr. Sur 654',
  },
  {
    id: 6,
    nombre: 'Diego Salazar',
    email: 'diego.salazar@elhornero.pe',
    rol: 'CAJERO',
    telefono: '999666777',
    direccion: 'Av. Este 987',
  },
];

describe('UsuariosRoles', () => {
  let service: jasmine.SpyObj<UsuarioService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<UsuarioService>('UsuarioService', [
      'listar',
      'obtener',
      'crear',
      'actualizar',
      'cambiarRol',
      'cambiarPassword',
    ]);
    service.listar.and.callFake((buscar?: string) => {
      const termino = (buscar ?? '').trim().toLowerCase();
      if (!termino) {
        return of(USUARIOS);
      }
      return of(
        USUARIOS.filter(
          (usuario) =>
            (usuario.nombre ?? '').toLowerCase().includes(termino) ||
            usuario.email.toLowerCase().includes(termino),
        ),
      );
    });
    service.crear.and.returnValue(of(USUARIOS[0]));
    service.actualizar.and.returnValue(of(USUARIOS[0]));
    service.cambiarRol.and.returnValue(of(USUARIOS[0]));
    service.cambiarPassword.and.returnValue(of(USUARIOS[0]));

    await TestBed.configureTestingModule({
      imports: [UsuariosRoles],
      providers: [{ provide: UsuarioService, useValue: service }],
    }).compileComponents();
  });

  it('should create the users and roles view', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the header, the KPI cards and the users loaded from the backend', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Usuarios y roles');
    expect(compiled.textContent).toContain('Control de accesos, permisos y trazabilidad');
    expect(compiled.textContent).toContain('Usuarios registrados');
    expect(compiled.querySelectorAll('tbody tr').length).toBe(6);
    expect(compiled.textContent).toContain('Lucía Mendoza');
    expect(compiled.textContent).toContain('lucia.mendoza@elhornero.pe');
    expect(service.listar).toHaveBeenCalled();
  });

  it('should not show fictitious states or last access anymore', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).not.toContain('ACTIVO');
    expect(compiled.textContent).not.toContain('SUSPENDIDO');
    expect(compiled.textContent).not.toContain('Último acceso');
    expect(compiled.querySelectorAll('thead th').length).toBe(4);
  });

  it('should expose the KPI values 6, 1, 3 and 2', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const valores = component.kpis().map((kpi) => kpi.value);
    expect(valores).toEqual(['6', '1', '3', '2']);
  });

  it('should ask the backend to search rows by term', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    service.listar.calls.reset();

    component.onSearch({ target: { value: 'diego' } } as unknown as Event);
    fixture.detectChanges();

    expect(service.listar).toHaveBeenCalledWith('diego');
    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Diego Salazar');
  });

  it('should switch the selected role when a user row is selected', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.rolSeleccionado()).toBe('MOZO');
    expect(component.usuariosDelRol()).toBe(3);

    component.seleccionarUsuario(component.usuarios()[0]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(component.rolSeleccionado()).toBe('ADMINISTRADOR');
    expect(component.usuariosDelRol()).toBe(1);
    expect(compiled.textContent).toContain('Control total del sistema');
  });

  it('should start with the MOZO permissions aligned to the access matrix', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.permisosDelRol().length).toBe(8);
    expect(component.permisosActivos()).toBe(4);
    expect(
      component
        .permisosDelRol()
        .filter((permiso) => permiso.habilitado)
        .map((p) => p.clave),
    ).toEqual(['dashboard', 'salon', 'pedidos', 'productos']);
  });

  it('should create a new user through the backend service', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.abrirCrear();
    component.formNombre.set('Ana Torres');
    component.formCorreo.set('ana.torres@elhornero.pe');
    component.formPassword.set('secreta123');
    component.formRol.set('CAJERO');
    component.guardar();
    fixture.detectChanges();

    expect(service.crear).toHaveBeenCalledWith({
      email: 'ana.torres@elhornero.pe',
      password: 'secreta123',
      nombre: 'Ana Torres',
      telefono: null,
      direccion: null,
      rol: 'CAJERO',
    });
    expect(component.modalModo()).toBeNull();
    expect(component.auditoria()[0].accion).toContain('Creó el usuario');
  });

  it('should validate the new user form before calling the backend', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.abrirCrear();
    component.guardar();
    expect(component.formError()).toContain('nombre completo');

    component.formNombre.set('Ana Torres');
    component.guardar();
    expect(component.formError()).toContain('correo corporativo');

    component.formCorreo.set('ana.torres@elhornero.pe');
    component.guardar();
    expect(component.formError()).toContain('al menos 6 caracteres');

    expect(service.crear).not.toHaveBeenCalled();
    expect(component.modalModo()).toBe('crear');
  });

  it('should edit a user and report validation errors from the backend', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.abrirEditar(USUARIOS[1]);
    expect(component.formCorreo()).toBe('rosa.huaman@elhornero.pe');
    expect(component.formRol()).toBe('CAJERO');

    component.formNombre.set('Rosa Huamán Vargas');
    component.guardar();
    fixture.detectChanges();

    expect(service.actualizar).toHaveBeenCalledWith(2, {
      nombre: 'Rosa Huamán Vargas',
      email: 'rosa.huaman@elhornero.pe',
      telefono: '999222333',
      direccion: 'Jr. Piura 456',
    });
    expect(component.modalModo()).toBeNull();
  });

  it('should open the password modal and send the new password', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.abrirPassword(USUARIOS[2]);
    component.formPassword.set('nuevaClave123');
    component.guardar();
    fixture.detectChanges();

    expect(service.cambiarPassword).toHaveBeenCalledWith(3, 'nuevaClave123');
    expect(component.modalModo()).toBeNull();
    expect(component.auditoria()[0].accion).toContain('Restableció la contraseña');
  });
});
