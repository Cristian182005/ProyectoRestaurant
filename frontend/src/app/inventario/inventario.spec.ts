import { TestBed } from '@angular/core/testing';
import { Inventario } from './inventario';

describe('Inventario', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inventario],
    }).compileComponents();
  });

  it('should create the inventory view', () => {
    const fixture = TestBed.createComponent(Inventario);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the KPI cards and the sample rows', () => {
    const fixture = TestBed.createComponent(Inventario);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Existencias valorizadas');
    expect(compiled.textContent).toContain('S/ 38,420');
    expect(compiled.querySelectorAll('tbody tr').length).toBe(8);
    expect(compiled.textContent).toContain('Bife ancho crudo');
    expect(compiled.textContent).toContain('CRÍTICO');
  });

  it('should filter rows by search term', () => {
    const fixture = TestBed.createComponent(Inventario);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.searchTerm.set('lomo');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Lomo fino');
  });

  it('should register a salida movement in real time', () => {
    const fixture = TestBed.createComponent(Inventario);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.formCodigo.set('INS-101');
    component.formTipo.set('salida');
    component.formCantidad.set(0.6);
    component.registrarMovimiento();

    fixture.detectChanges();

    const bife = component.insumos().find((item) => item.codigo === 'INS-101');
    expect(bife?.existencia).toBe(18);
    expect(component.movimientosHoy()).toBe(125);
    expect(component.movimientos()[0].insumo).toBe('Bife ancho crudo');
    expect(component.movimientos()[0].variacion).toBe('-0.6 kg');
  });

  it('should block a salida greater than the available stock', () => {
    const fixture = TestBed.createComponent(Inventario);
    const component = fixture.componentInstance;

    component.formCodigo.set('INS-102');
    component.formTipo.set('salida');
    component.formCantidad.set(99);
    component.registrarMovimiento();

    expect(component.formError()).toContain('Solo hay');
    expect(component.insumos()[1].existencia).toBe(3.4);
  });

  it('should keep three critical supplies in the side panel', () => {
    const fixture = TestBed.createComponent(Inventario);
    const component = fixture.componentInstance;

    expect(component.criticos().length).toBe(3);
    expect(component.criticos().map((item) => item.nombre)).toEqual([
      'Lomo fino',
      'Chorizo artesanal',
      'Carbón vegetal',
    ]);
  });
});
