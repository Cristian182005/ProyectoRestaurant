import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SesionService } from '../core/sesion.service';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

type EstadoProducto = 'disponible' | 'pausado';

interface InsumoPorcion {
  insumo: string;
  cantidad: string;
  costo: number;
}

interface Producto {
  id: string;
  codigo: string;
  nombre: string;
  categoria: string;
  precio: number;
  estado: EstadoProducto;
  imagen: string;
  porciones: number;
  minutos: number;
  preparacion: string;
  receta: InsumoPorcion[];
}

interface CategoriaChip {
  key: string;
  label: string;
  count: number;
}

@Component({
  selector: 'app-productos-recetas',
  imports: [SidebarComponent],
  templateUrl: './productos-recetas.html',
})
export class ProductosRecetas {
  private readonly router = inject(Router);
  private readonly sesion = inject(SesionService);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Productos y recetas');
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly searchTerm = signal('');
  readonly searchOpen = signal(false);
  readonly categoriaActiva = signal('todos');
  readonly selectedId = signal('PAR-001');

  readonly aviso = signal('');
  readonly failedImages = signal<Record<string, boolean>>({});

  private avisoTimer: ReturnType<typeof setTimeout> | null = null;

  readonly productos = signal<Producto[]>([
    {
      id: 'PAR-001',
      codigo: 'PAR-001',
      nombre: 'Bife Ancho',
      categoria: 'Parrilla',
      precio: 99,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1600891964092-4316c288032e?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 18,
      preparacion:
        'Sellar a brasas alta, finalizar a término solicitado y reposar 3 minutos. Servir con papa dorada y mantequilla de hierbas.',
      receta: [
        { insumo: 'Bife ancho crudo', cantidad: '0.350 kg', costo: 29.4 },
        { insumo: 'Papa amarilla', cantidad: '0.220 kg', costo: 1.76 },
        { insumo: 'Mantequilla de hierbas', cantidad: '0.030 kg', costo: 1.65 },
        { insumo: 'Chimichurri de la casa', cantidad: '0.025 L', costo: 1.1 },
        { insumo: 'Sal parrillera', cantidad: '0.008 kg', costo: 0.39 },
        { insumo: 'Carbón vegetal', cantidad: '0.120 kg', costo: 0.5 },
      ],
    },
    {
      id: 'PAR-002',
      codigo: 'PAR-002',
      nombre: 'Parrillada El Hornero',
      categoria: 'Parrilla',
      precio: 149,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?auto=format&fit=crop&w=640&q=60',
      porciones: 2,
      minutos: 35,
      preparacion:
        'Cocinar cortes por separado según su punto, dejar reposar 5 minutos y emplatar en tabla de madera con guarniciones calientes.',
      receta: [
        { insumo: 'Bife ancho crudo', cantidad: '0.480 kg', costo: 40.32 },
        { insumo: 'Lomo fino', cantidad: '0.250 kg', costo: 12.0 },
        { insumo: 'Papa amarilla', cantidad: '0.400 kg', costo: 3.2 },
        { insumo: 'Chimichurri de la casa', cantidad: '0.040 L', costo: 1.6 },
        { insumo: 'Sal parrillera', cantidad: '0.010 kg', costo: 0.48 },
        { insumo: 'Carbón vegetal', cantidad: '0.160 kg', costo: 0.8 },
      ],
    },
    {
      id: 'PAR-003',
      codigo: 'PAR-003',
      nombre: 'Lomo fino',
      categoria: 'Parrilla',
      precio: 119,
      estado: 'pausado',
      imagen:
        'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 22,
      preparacion:
        'Sellar 2 minutos por lado, llevar a horno 6 minutos a 180 °C y reposar antes de cortar. Acompañar con champiñones salteados.',
      receta: [
        { insumo: 'Lomo fino', cantidad: '0.650 kg', costo: 32.0 },
        { insumo: 'Papa amarilla', cantidad: '0.450 kg', costo: 3.6 },
        { insumo: 'Mantequilla de hierbas', cantidad: '0.060 kg', costo: 3.3 },
        { insumo: 'Champiñones', cantidad: '0.180 kg', costo: 4.5 },
        { insumo: 'Sal parrillera', cantidad: '0.025 kg', costo: 1.2 },
        { insumo: 'Carbón vegetal', cantidad: '0.500 kg', costo: 2.5 },
      ],
    },
    {
      id: 'PAR-004',
      codigo: 'PAR-004',
      nombre: 'Bife de chorizo',
      categoria: 'Parrilla',
      precio: 109,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1558030006-450675393462?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 16,
      preparacion:
        'Brasear a fuego medio-alto volteando una sola vez. Reposar 4 minutos y terminar con sal en escamas y chimichurri.',
      receta: [
        { insumo: 'Bife de chorizo crudo', cantidad: '0.500 kg', costo: 37.8 },
        { insumo: 'Papa amarilla', cantidad: '0.250 kg', costo: 2.0 },
        { insumo: 'Chimichurri de la casa', cantidad: '0.040 L', costo: 1.6 },
        { insumo: 'Sal parrillera', cantidad: '0.010 kg', costo: 0.48 },
        { insumo: 'Carbón vegetal', cantidad: '0.150 kg', costo: 0.75 },
      ],
    },
    {
      id: 'PAR-005',
      codigo: 'PAR-005',
      nombre: 'Costillar de cerdo',
      categoria: 'Parrilla',
      precio: 89,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1529692236671-f1f6cf9683ba?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 40,
      preparacion:
        'Cocinar a fuego bajo 35 minutos, glasear con salsa BBQ a fuego alto y cortar en porciones antes de servir.',
      receta: [
        { insumo: 'Costillar de cerdo', cantidad: '0.700 kg', costo: 25.2 },
        { insumo: 'Salsa BBQ de la casa', cantidad: '0.080 kg', costo: 2.4 },
        { insumo: 'Papa amarilla', cantidad: '0.300 kg', costo: 2.4 },
        { insumo: 'Carbón vegetal', cantidad: '0.200 kg', costo: 1.0 },
        { insumo: 'Sal parrillera', cantidad: '0.015 kg', costo: 0.72 },
      ],
    },
    {
      id: 'PAR-006',
      codigo: 'PAR-006',
      nombre: 'Pollo a las brasas',
      categoria: 'Parrilla',
      precio: 79,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?auto=format&fit=crop&w=640&q=60',
      porciones: 2,
      minutos: 45,
      preparacion:
        'Marinar 12 horas, asar girando cada 10 minutos hasta dorar y servir con papa dorada y ají golf.',
      receta: [
        { insumo: 'Pollo entero', cantidad: '1.200 kg', costo: 16.0 },
        { insumo: 'Papa amarilla', cantidad: '0.400 kg', costo: 3.2 },
        { insumo: 'Ají golf', cantidad: '0.050 kg', costo: 1.5 },
        { insumo: 'Carbón vegetal', cantidad: '0.250 kg', costo: 1.25 },
        { insumo: 'Sal parrillera', cantidad: '0.015 kg', costo: 0.72 },
      ],
    },
    {
      id: 'ENT-001',
      codigo: 'ENT-001',
      nombre: 'Anticuchos',
      categoria: 'Entradas',
      precio: 59,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 25,
      preparacion:
        'Marinar en ají panca al menos 4 horas, cocinar a brasas medias girando y servir con papa y cebolla encurtida.',
      receta: [
        { insumo: 'Corazón de res', cantidad: '0.400 kg', costo: 8.4 },
        { insumo: 'Papa amarilla', cantidad: '0.350 kg', costo: 2.8 },
        { insumo: 'Ají panca', cantidad: '0.100 kg', costo: 3.2 },
        { insumo: 'Cebolla roja', cantidad: '0.250 kg', costo: 2.4 },
        { insumo: 'Sal parrillera', cantidad: '0.012 kg', costo: 0.6 },
        { insumo: 'Carbón vegetal', cantidad: '0.240 kg', costo: 1.2 },
      ],
    },
    {
      id: 'ENT-002',
      codigo: 'ENT-002',
      nombre: 'Chorizo artesanal',
      categoria: 'Entradas',
      precio: 32,
      estado: 'pausado',
      imagen:
        'https://images.unsplash.com/photo-1613514785940-daed07799d9b?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 12,
      preparacion:
        'Cocinar a brasas medias volteando con frecuencia hasta dorar. Servir en pan crujiente con ají amarillo y cebolla.',
      receta: [
        { insumo: 'Chorizo fresco', cantidad: '0.350 kg', costo: 7.0 },
        { insumo: 'Pan francés', cantidad: '2 und', costo: 1.0 },
        { insumo: 'Ají amarillo', cantidad: '0.050 kg', costo: 1.2 },
        { insumo: 'Cebolla roja', cantidad: '0.150 kg', costo: 0.6 },
        { insumo: 'Carbón vegetal', cantidad: '0.280 kg', costo: 1.4 },
      ],
    },
    {
      id: 'ENT-003',
      codigo: 'ENT-003',
      nombre: 'Provoleta a la brasa',
      categoria: 'Entradas',
      precio: 42,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 10,
      preparacion:
        'Derretir el provolone en plancha con brasa, terminar con tomate, albahaca y aceite de oliva. Servir de inmediato.',
      receta: [
        { insumo: 'Queso provolone', cantidad: '0.250 kg', costo: 9.5 },
        { insumo: 'Tomate', cantidad: '0.150 kg', costo: 1.2 },
        { insumo: 'Albahaca fresca', cantidad: '0.020 kg', costo: 1.0 },
        { insumo: 'Aceite de oliva', cantidad: '0.020 L', costo: 1.4 },
        { insumo: 'Pan rústico', cantidad: '1 und', costo: 1.0 },
        { insumo: 'Carbón vegetal', cantidad: '0.120 kg', costo: 0.6 },
      ],
    },
    {
      id: 'ENT-004',
      codigo: 'ENT-004',
      nombre: 'Papa rellena',
      categoria: 'Entradas',
      precio: 18,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1565299507177-b0ac66763828?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 20,
      preparacion:
        'Rellenar con carne guisada, empanizar y freír hasta dorar. Escurrir y servir con salsa huancaína.',
      receta: [
        { insumo: 'Papa amarilla', cantidad: '0.300 kg', costo: 2.4 },
        { insumo: 'Carne molida', cantidad: '0.150 kg', costo: 4.5 },
        { insumo: 'Huevo', cantidad: '1 und', costo: 0.6 },
        { insumo: 'Ají panca', cantidad: '0.030 kg', costo: 0.6 },
        { insumo: 'Aceite de fritura', cantidad: '0.040 L', costo: 1.2 },
      ],
    },
    {
      id: 'ENT-005',
      codigo: 'ENT-005',
      nombre: 'Causa limeña',
      categoria: 'Entradas',
      precio: 22,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 15,
      preparacion:
        'Montar en capas de papa amarilla templada con relleno frío, decorar con palta y huevo duro.',
      receta: [
        { insumo: 'Papa amarilla', cantidad: '0.350 kg', costo: 2.8 },
        { insumo: 'Palta', cantidad: '0.150 kg', costo: 3.0 },
        { insumo: 'Atún en aceite', cantidad: '0.120 kg', costo: 4.2 },
        { insumo: 'Huevo', cantidad: '1 und', costo: 0.6 },
        { insumo: 'Mayonesa', cantidad: '0.030 kg', costo: 0.9 },
      ],
    },
    {
      id: 'ENT-006',
      codigo: 'ENT-006',
      nombre: 'Papa a la huancaína',
      categoria: 'Entradas',
      precio: 20,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 15,
      preparacion:
        'Licuar ají amarillo con queso y galleta, coccionar a fuego bajo y servir sobre papa amarilla con huevo duro.',
      receta: [
        { insumo: 'Papa amarilla', cantidad: '0.300 kg', costo: 2.4 },
        { insumo: 'Queso fresco', cantidad: '0.100 kg', costo: 2.6 },
        { insumo: 'Ají amarillo', cantidad: '0.080 kg', costo: 1.6 },
        { insumo: 'Galleta soda', cantidad: '0.040 kg', costo: 0.5 },
        { insumo: 'Leche evaporada', cantidad: '0.060 L', costo: 0.4 },
      ],
    },
    {
      id: 'GRU-001',
      codigo: 'GRU-001',
      nombre: 'Papa amarilla frita',
      categoria: 'Guarniciones',
      precio: 12,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 8,
      preparacion:
        'Doble cocción: blanquear 4 minutos y freír a 180 °C hasta dorar. Salar al momento de servir.',
      receta: [
        { insumo: 'Papa amarilla', cantidad: '0.300 kg', costo: 2.4 },
        { insumo: 'Aceite de fritura', cantidad: '0.060 L', costo: 1.8 },
        { insumo: 'Sal parrillera', cantidad: '0.008 kg', costo: 0.39 },
      ],
    },
    {
      id: 'GRU-002',
      codigo: 'GRU-002',
      nombre: 'Yuca crocante',
      categoria: 'Guarniciones',
      precio: 12,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1541544181051-e46607bc22a4?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 10,
      preparacion:
        'Hervir hasta que abra, secar bien y freír a fuego alto para lograr el exterior crujiente. Servir con salsa de ají.',
      receta: [
        { insumo: 'Yuca', cantidad: '0.300 kg', costo: 2.1 },
        { insumo: 'Aceite de fritura', cantidad: '0.060 L', costo: 1.8 },
        { insumo: 'Sal parrillera', cantidad: '0.008 kg', costo: 0.39 },
      ],
    },
    {
      id: 'GRU-003',
      codigo: 'GRU-003',
      nombre: 'Ensalada mixta',
      categoria: 'Guarniciones',
      precio: 14,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 6,
      preparacion:
        'Lavar y cortar vegetales al momento, aliñar con aceite, limón y sal antes de emplatar.',
      receta: [
        { insumo: 'Lechuga criolla', cantidad: '0.150 kg', costo: 1.2 },
        { insumo: 'Tomate', cantidad: '0.120 kg', costo: 0.96 },
        { insumo: 'Cebolla roja', cantidad: '0.050 kg', costo: 0.3 },
        { insumo: 'Aceite de oliva', cantidad: '0.030 L', costo: 0.9 },
        { insumo: 'Limón', cantidad: '0.020 kg', costo: 0.2 },
      ],
    },
    {
      id: 'GRU-004',
      codigo: 'GRU-004',
      nombre: 'Camote frito',
      categoria: 'Guarniciones',
      precio: 14,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1630384060421-cb20d0e0649d?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 9,
      preparacion:
        'Cortar en gajos gruesos, freír hasta dorar y escurrir sobre papel. Salar ligeramente al servir.',
      receta: [
        { insumo: 'Camote', cantidad: '0.300 kg', costo: 2.4 },
        { insumo: 'Aceite de fritura', cantidad: '0.060 L', costo: 1.8 },
        { insumo: 'Sal parrillera', cantidad: '0.008 kg', costo: 0.39 },
      ],
    },
    {
      id: 'BEB-001',
      codigo: 'BEB-001',
      nombre: 'Chicha morada',
      categoria: 'Bebidas',
      precio: 10,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=640&q=60',
      porciones: 4,
      minutos: 30,
      preparacion:
        'Hervir maíz morado con piña y canela, endulzar, enfriar y servir con hielo y trozos de piña.',
      receta: [
        { insumo: 'Maíz morado', cantidad: '0.120 kg', costo: 3.6 },
        { insumo: 'Piña', cantidad: '0.100 kg', costo: 1.0 },
        { insumo: 'Limón', cantidad: '0.040 kg', costo: 0.4 },
        { insumo: 'Azúcar blanca', cantidad: '0.080 kg', costo: 0.48 },
      ],
    },
    {
      id: 'BEB-002',
      codigo: 'BEB-002',
      nombre: 'Limonada de hierbabuena',
      categoria: 'Bebidas',
      precio: 12,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1621263764928-df1444c5e859?auto=format&fit=crop&w=640&q=60',
      porciones: 2,
      minutos: 5,
      preparacion:
        'Prensar limones, mezclar con agua fría, azúcar y hierbabuena fresca. Servir con hielo.',
      receta: [
        { insumo: 'Limón', cantidad: '0.120 kg', costo: 1.2 },
        { insumo: 'Azúcar blanca', cantidad: '0.060 kg', costo: 0.36 },
        { insumo: 'Hierbabuena', cantidad: '0.010 kg', costo: 0.6 },
        { insumo: 'Agua purificada', cantidad: '0.400 L', costo: 0.12 },
      ],
    },
    {
      id: 'BEB-003',
      codigo: 'BEB-003',
      nombre: 'Cerveza artesanal negra',
      categoria: 'Bebidas',
      precio: 16,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1608270586620-248524c67de9?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 2,
      preparacion: 'Servir entre 6 y 8 °C en vaso frío, sin vertir el sedimentio del fondo.',
      receta: [
        { insumo: 'Cerveza negra 330 ml', cantidad: '1 und', costo: 7.5 },
        { insumo: 'Vaso desechable', cantidad: '1 und', costo: 0.3 },
      ],
    },
    {
      id: 'BEB-004',
      codigo: 'BEB-004',
      nombre: 'Agua mineral 500 ml',
      categoria: 'Bebidas',
      precio: 6,
      estado: 'disponible',
      imagen:
        'https://images.unsplash.com/photo-1548839140-29a749e1cf4d?auto=format&fit=crop&w=640&q=60',
      porciones: 1,
      minutos: 1,
      preparacion: 'Servir fría con hielo y rodaja de limón.',
      receta: [{ insumo: 'Agua mineral 500 ml', cantidad: '1 und', costo: 2.1 }],
    },
  ]);

  readonly chips = computed<CategoriaChip[]>(() => {
    const productos = this.productos();
    const total = productos.length;
    const base: CategoriaChip[] = [{ key: 'todos', label: 'Todos', count: total }];

    for (const producto of productos) {
      const existente = base.find((chip) => chip.key === producto.categoria);
      if (existente) {
        existente.count += 1;
      } else {
        base.push({ key: producto.categoria, label: producto.categoria, count: 1 });
      }
    }

    return base;
  });

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoria = this.categoriaActiva();

    return this.productos().filter((producto) => {
      const matchesCategoria = categoria === 'todos' || producto.categoria === categoria;
      const matchesTerm =
        term === '' ||
        producto.nombre.toLowerCase().includes(term) ||
        producto.codigo.toLowerCase().includes(term) ||
        producto.categoria.toLowerCase().includes(term);
      return matchesCategoria && matchesTerm;
    });
  });

  readonly seleccionado = computed(
    () => this.productos().find((producto) => producto.id === this.selectedId()) ?? null
  );

  readonly pausados = computed(() =>
    this.productos().filter((producto) => producto.estado === 'pausado')
  );

  readonly estadoBadge: Record<EstadoProducto, string> = {
    disponible: 'bg-ok-50 text-ok-700',
    pausado: 'bg-danger-100 text-danger-700',
  };

  readonly estadoPunto: Record<EstadoProducto, string> = {
    disponible: 'bg-ok-600',
    pausado: 'bg-danger-600',
  };

  costoDe(producto: Producto): number {
    const total = producto.receta.reduce((sum, item) => sum + item.costo, 0);
    return Math.round(total * 100) / 100;
  }

  margenDe(producto: Producto): number {
    if (producto.precio <= 0) {
      return 0;
    }
    const margen = ((producto.precio - this.costoDe(producto)) / producto.precio) * 100;
    return Math.round(margen * 10) / 10;
  }

  money(value: number): string {
    return `S/ ${value.toFixed(2)}`;
  }

  moneyCard(value: number): string {
    const entero = Number.isInteger(value) ? value.toString() : value.toFixed(2);
    return `S/ ${entero}`;
  }

  seleccionar(id: string): void {
    this.selectedId.set(id);
  }

  setCategoria(key: string): void {
    this.categoriaActiva.set(key);
  }

  toggleSearch(): void {
    this.searchOpen.update((open) => !open);
    if (!this.searchOpen()) {
      this.searchTerm.set('');
    }
  }

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
  }

  markFailed(id: string): void {
    this.failedImages.update((map) => ({ ...map, [id]: true }));
  }

  actualizarCostos(): void {
    this.mostrarAviso('Costos recalculados con las existencias actuales del inventario.');
  }

  nuevoProducto(): void {
    this.mostrarAviso('Formulario de nuevo producto (próximamente).');
  }

  editarReceta(): void {
    const producto = this.seleccionado();
    this.mostrarAviso(
      producto ? `Editando receta de ${producto.nombre} (próximamente).` : 'Selecciona un producto.'
    );
  }

  duplicar(): void {
    const producto = this.seleccionado();
    this.mostrarAviso(
      producto
        ? `${producto.nombre} duplicado como borrador (próximamente).`
        : 'Selecciona un producto.'
    );
  }

  revisarStock(): void {
    void this.router.navigate(['/inventario']);
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

  private mostrarAviso(mensaje: string): void {
    this.aviso.set(mensaje);
    if (this.avisoTimer) {
      clearTimeout(this.avisoTimer);
    }
    this.avisoTimer = setTimeout(() => this.aviso.set(''), 3200);
  }
}
