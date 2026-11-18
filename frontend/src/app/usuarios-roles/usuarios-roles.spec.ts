import { TestBed } from '@angular/core/testing';
import { UsuariosRoles } from './usuarios-roles';

describe('UsuariosRoles', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsuariosRoles],
    }).compileComponents();
  });

  it('should create the users and roles view', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the header, the KPI cards and the six sample users', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Usuarios y roles');
    expect(compiled.textContent).toContain('Control de accesos, permisos y trazabilidad');
    expect(compiled.textContent).toContain('Usuarios activos');
    expect(compiled.querySelectorAll('tbody tr').length).toBe(6);
    expect(compiled.textContent).toContain('Lucía Mendoza');
    expect(compiled.textContent).toContain('SUSPENDIDO');
    expect(compiled.textContent).toContain('lucia.mendoza@elhornero.pe');
  });

  it('should expose the KPI values 5, 1, 3 and 2', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;

    const valores = component.kpis().map((kpi) => kpi.value);
    expect(valores).toEqual(['5', '1', '3', '2']);
  });

  it('should filter rows by search term', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.searchTerm.set('diego');
    fixture.detectChanges();

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

  it('should start with two permissions enabled for the MOZO role', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;

    expect(component.permisosDelRol().length).toBe(7);
    expect(component.permisosActivos()).toBe(2);
    expect(
      component
        .permisosDelRol()
        .filter((permiso) => permiso.habilitado)
        .map((p) => p.clave),
    ).toEqual(['salon', 'comandas']);
  });

  it('should toggle permissions only in edit mode and save them', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.alternarPermiso('caja');
    expect(component.permisosActivos()).toBe(2);

    component.onEditarPermisos();
    expect(component.editandoPermisos()).toBeTrue();

    component.alternarPermiso('caja');
    expect(component.permisosActivos()).toBe(3);

    component.onEditarPermisos();
    fixture.detectChanges();

    expect(component.editandoPermisos()).toBeFalse();
    expect(component.aviso()).toContain('Permisos de MOZO actualizados');
    expect(component.auditoria()[0].accion).toContain('Actualizó permisos del rol MOZO');
  });

  it('should create a new user from the modal and refresh the KPIs', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.abrirModal();
    component.formNombre.set('Ana Torres');
    component.formCorreo.set('ana.torres@elhornero.pe');
    component.formRol.set('CAJERO');
    component.crearUsuario();
    fixture.detectChanges();

    expect(component.usuarios().length).toBe(7);
    expect(component.modalAbierto()).toBeFalse();
    expect(component.kpis()[0].value).toBe('6');
    expect(component.kpis()[3].value).toBe('3');
    expect(component.auditoria()[0].accion).toContain('Creó el usuario Ana Torres');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Ana Torres');
    expect(compiled.querySelectorAll('tbody tr').length).toBe(7);
  });

  it('should validate the new user form', () => {
    const fixture = TestBed.createComponent(UsuariosRoles);
    const component = fixture.componentInstance;

    component.abrirModal();
    component.crearUsuario();
    expect(component.formError()).toContain('nombre completo');

    component.formNombre.set('Ana Torres');
    component.crearUsuario();
    expect(component.formError()).toContain('correo corporativo');

    component.formCorreo.set('lucia.mendoza@elhornero.pe');
    component.crearUsuario();
    expect(component.formError()).toContain('Ya existe');

    expect(component.usuarios().length).toBe(6);
  });
});
