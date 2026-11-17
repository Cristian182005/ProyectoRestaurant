import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { SidebarComponent } from '../shared/sidebar/sidebar.component';

type EstadoPedido =
  | 'Por cobrar'
  | 'En parrilla'
  | 'Listo'
  | 'En preparación'
  | 'Entregado'
  | 'Anulado';

type CategoriaPedido = 'Parrilla' | 'Entradas' | 'Bebidas' | 'Postres';
type CategoriaFiltro = 'todas' | CategoriaPedido;
type EstadoFiltro = 'todos' | EstadoPedido;
type KpiTone = 'danger' | 'ok' | 'warn';
type AlertTone = 'warn' | 'danger' | 'ok' | 'info' | 'muted';

interface PedidoItem {
  cantidad: number;
  producto: string;
  categoria: CategoriaPedido;
  subtotal: number;
}

interface Pedido {
  id: string;
  hora: string;
  mesa: number;
  personas: number;
  mozo: string;
  apertura: string;
  tiempo: number;
  estado: EstadoPedido;
  subtotal: number;
  igv: number;
  total: number;
  items: PedidoItem[];
  nota: string | null;
  ultimoCambio: string;
}

interface KpiCard {
  label: string;
  value: string;
  detail: string;
  tone: KpiTone;
  icon: string[];
}

interface AlertaEstado {
  mensaje: string;
  accion: string;
  tone: AlertTone;
}

@Component({
  selector: 'app-pedidos',
  imports: [NgClass, SidebarComponent],
  templateUrl: './pedidos.html',
  styleUrl: './pedidos.css',
})
export class Pedidos {
  private readonly router = inject(Router);

  readonly sidebarOpen = signal(false);
  readonly activeSection = signal('Pedidos');
  readonly dateLabel = 'Jue, 01 oct 2026';

  readonly searchTerm = signal('');
  readonly categoria = signal<CategoriaFiltro>('todas');
  readonly estadoFiltro = signal<EstadoFiltro>('todos');
  readonly page = signal(1);
  readonly selectedId = signal('#1052');
  readonly drawerOpen = signal(true);

  readonly pageSize = 6;

  readonly categoriaOptions: { value: CategoriaFiltro; label: string }[] = [
    { value: 'todas', label: 'Todas las categorías' },
    { value: 'Parrilla', label: 'Parrilla' },
    { value: 'Entradas', label: 'Entradas' },
    { value: 'Bebidas', label: 'Bebidas' },
    { value: 'Postres', label: 'Postres' },
  ];

  readonly estadoOptions: { value: EstadoFiltro; label: string }[] = [
    { value: 'todos', label: 'Todos los estados' },
    { value: 'Por cobrar', label: 'Por cobrar' },
    { value: 'En parrilla', label: 'En parrilla' },
    { value: 'En preparación', label: 'En preparación' },
    { value: 'Listo', label: 'Listo' },
    { value: 'Entregado', label: 'Entregado' },
    { value: 'Anulado', label: 'Anulado' },
  ];

  readonly kpis: KpiCard[] = [
    {
      label: 'Activos',
      value: '18',
      detail: '4 nuevos',
      tone: 'danger',
      icon: [
        'M3 10.5 12 3l9 7.5',
        'M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5',
      ],
    },
    {
      label: 'Tiempo promedio',
      value: '14 min',
      detail: 'Meta <= 18',
      tone: 'ok',
      icon: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 7v5l3.5 2'],
    },
    {
      label: 'Por cobrar',
      value: 'S/ 1,842',
      detail: '7 mesas',
      tone: 'warn',
      icon: [
        'M4 5h16a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z',
        'm3.5 6.5 8.5 6 8.5-6',
      ],
    },
  ];

  readonly toneIcon: Record<KpiTone, string> = {
    danger: 'bg-danger-100 text-danger-600',
    ok: 'bg-ok-100 text-ok-600',
    warn: 'bg-warn-100 text-warn-700',
  };

  readonly toneDetail: Record<KpiTone, string> = {
    danger: 'text-danger-700',
    ok: 'text-ok-600',
    warn: 'text-warn-700',
  };

  readonly estadoBadge: Record<EstadoPedido, string> = {
    'Por cobrar': 'bg-warn-100 text-warn-700',
    'En parrilla': 'bg-danger-100 text-danger-700',
    Listo: 'bg-ok-100 text-ok-700',
    'En preparación': 'bg-info-100 text-info-700',
    Entregado: 'bg-cream-300 text-ink-500',
    Anulado: 'bg-danger-100 text-danger-700',
  };

  readonly estadoPunto: Record<EstadoPedido, string> = {
    'Por cobrar': 'bg-[#e0813c]',
    'En parrilla': 'bg-danger-600',
    Listo: 'bg-ok-600',
    'En preparación': 'bg-info-700',
    Entregado: 'bg-ink-400',
    Anulado: 'bg-danger-600',
  };

  readonly alertaEstado: Record<EstadoPedido, AlertaEstado> = {
    'Por cobrar': { mensaje: 'Listo para emitir comprobante.', accion: 'Revisar', tone: 'warn' },
    'En parrilla': { mensaje: 'La parrilla sigue trabajando la comanda.', accion: 'Ver cocina', tone: 'danger' },
    Listo: { mensaje: 'Listo para entregar a la mesa.', accion: 'Entregar', tone: 'ok' },
    'En preparación': { mensaje: 'Comanda en preparación en cocina.', accion: 'Ver cocina', tone: 'info' },
    Entregado: { mensaje: 'Entregado al comensal.', accion: 'Ver comanda', tone: 'muted' },
    Anulado: { mensaje: 'Comanda anulada desde caja.', accion: 'Ver motivo', tone: 'danger' },
  };

  readonly alertaTone: Record<AlertTone, string> = {
    warn: 'border-warn-700/70 bg-warn-100',
    danger: 'border-danger-600/70 bg-danger-100',
    ok: 'border-ok-600/70 bg-ok-50',
    info: 'border-info-700/70 bg-info-100',
    muted: 'border-cream-400 bg-cream-200',
  };

  readonly alertaTexto: Record<AlertTone, string> = {
    warn: 'text-warn-700',
    danger: 'text-danger-700',
    ok: 'text-ok-700',
    info: 'text-info-700',
    muted: 'text-ink-600',
  };

  readonly pedidos: Pedido[] = [
    {
      id: '#1052',
      hora: '12:48',
      mesa: 12,
      personas: 5,
      mozo: 'Lucía Peña',
      apertura: '12:18',
      tiempo: 30,
      estado: 'Por cobrar',
      subtotal: 242.2,
      igv: 44.3,
      total: 286.5,
      items: [
          { cantidad: 2, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 159.8 },
          { cantidad: 1, producto: 'Ensalada parrillera', categoria: 'Entradas', subtotal: 28 },
          { cantidad: 2, producto: 'Chicha morada', categoria: 'Bebidas', subtotal: 24 },
          { cantidad: 2, producto: 'Pisco sour clásico', categoria: 'Bebidas', subtotal: 58 },
        ],
      nota: 'Término 3/4, sin ají en la ensalada.',
      ultimoCambio: 'caja.valeria - 12:47',
    },
    {
      id: '#1051',
      hora: '12:43',
      mesa: 8,
      personas: 2,
      mozo: 'Diego Rojas',
      apertura: '12:29',
      tiempo: 14,
      estado: 'En parrilla',
      subtotal: 148.22,
      igv: 26.68,
      total: 174.9,
      items: [
          { cantidad: 1, producto: 'Inca Kola 1 L', categoria: 'Bebidas', subtotal: 11.5 },
          { cantidad: 2, producto: 'Limonada de jengibre', categoria: 'Bebidas', subtotal: 28 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 108.72 },
        ],
      nota: null,
      ultimoCambio: 'cocina.jorge - 12:42',
    },
    {
      id: '#1050',
      hora: '12:36',
      mesa: 4,
      personas: 3,
      mozo: 'Marcos Vega',
      apertura: '12:14',
      tiempo: 22,
      estado: 'Listo',
      subtotal: 83.05,
      igv: 14.95,
      total: 98,
      items: [
          { cantidad: 2, producto: 'Limonada de jengibre', categoria: 'Bebidas', subtotal: 28 },
          { cantidad: 1, producto: 'Anticucho de corazón', categoria: 'Parrilla', subtotal: 32 },
          { cantidad: 1, producto: 'Causa limeña', categoria: 'Entradas', subtotal: 23.05 },
        ],
      nota: 'Una porción para llevar, usar envase térmico.',
      ultimoCambio: 'salon.marta - 12:35',
    },
    {
      id: '#1049',
      hora: '12:28',
      mesa: 15,
      personas: 6,
      mozo: 'Lucía Peña',
      apertura: '12:20',
      tiempo: 8,
      estado: 'En preparación',
      subtotal: 290.51,
      igv: 52.29,
      total: 342.8,
      items: [
          { cantidad: 1, producto: 'Anticucho de corazón', categoria: 'Parrilla', subtotal: 32 },
          { cantidad: 2, producto: 'Tres leches', categoria: 'Postres', subtotal: 39 },
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 2, producto: 'Causa limeña', categoria: 'Entradas', subtotal: 48 },
          { cantidad: 1, producto: 'Pollo a la brasa 1/4', categoria: 'Parrilla', subtotal: 38 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 89.51 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 12:27',
    },
    {
      id: '#1048',
      hora: '12:17',
      mesa: 3,
      personas: 2,
      mozo: 'Ana Torres',
      apertura: '11:44',
      tiempo: 33,
      estado: 'Entregado',
      subtotal: 106.78,
      igv: 19.22,
      total: 126,
      items: [
          { cantidad: 2, producto: 'Tres leches', categoria: 'Postres', subtotal: 39 },
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 1, producto: 'Causa limeña', categoria: 'Entradas', subtotal: 23.78 },
        ],
      nota: 'Mesa cumpleaños: poner velitas en el postre.',
      ultimoCambio: 'caja.valeria - 12:16',
    },
    {
      id: '#1047',
      hora: '12:05',
      mesa: 9,
      personas: 4,
      mozo: 'Diego Rojas',
      apertura: '11:52',
      tiempo: 13,
      estado: 'Anulado',
      subtotal: 182.54,
      igv: 32.86,
      total: 215.4,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 2, producto: 'Causa limeña', categoria: 'Entradas', subtotal: 48 },
          { cantidad: 1, producto: 'Pollo a la brasa 1/4', categoria: 'Parrilla', subtotal: 38 },
          { cantidad: 1, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 52.54 },
        ],
      nota: null,
      ultimoCambio: 'cocina.jorge - 12:04',
    },
    {
      id: '#1046',
      hora: '11:59',
      mesa: 13,
      personas: 4,
      mozo: 'Marcos Vega',
      apertura: '11:24',
      tiempo: 35,
      estado: 'En parrilla',
      subtotal: 132.75,
      igv: 23.9,
      total: 156.65,
      items: [
          { cantidad: 2, producto: 'Papa a la huancaína', categoria: 'Entradas', subtotal: 44 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 88.75 },
        ],
      nota: 'Término 3/4, sin ají en la ensalada.',
      ultimoCambio: 'salon.marta - 11:57',
    },
    {
      id: '#1045',
      hora: '11:52',
      mesa: 18,
      personas: 5,
      mozo: 'Ana Torres',
      apertura: '11:43',
      tiempo: 9,
      estado: 'Listo',
      subtotal: 74.9,
      igv: 13.48,
      total: 88.38,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 1, producto: 'Tiradito nikkei', categoria: 'Entradas', subtotal: 30.9 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 11:50',
    },
    {
      id: '#1044',
      hora: '11:45',
      mesa: 5,
      personas: 6,
      mozo: 'Rosa Huamán',
      apertura: '11:31',
      tiempo: 14,
      estado: 'En preparación',
      subtotal: 208.4,
      igv: 37.51,
      total: 245.91,
      items: [
          { cantidad: 2, producto: 'Picarones (4 u.)', categoria: 'Postres', subtotal: 36 },
          { cantidad: 1, producto: 'Inca Kola 1 L', categoria: 'Bebidas', subtotal: 11.5 },
          { cantidad: 2, producto: 'Limonada de jengibre', categoria: 'Bebidas', subtotal: 28 },
          { cantidad: 1, producto: 'Anticucho de corazón', categoria: 'Parrilla', subtotal: 32 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 100.9 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 11:43',
    },
    {
      id: '#1043',
      hora: '11:38',
      mesa: 10,
      personas: 2,
      mozo: 'Lucía Peña',
      apertura: '11:20',
      tiempo: 18,
      estado: 'Entregado',
      subtotal: 58.2,
      igv: 10.48,
      total: 68.68,
      items: [
          { cantidad: 1, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 58.2 },
        ],
      nota: 'Una porción para llevar, usar envase térmico.',
      ultimoCambio: 'cocina.jorge - 11:36',
    },
    {
      id: '#1042',
      hora: '11:35',
      mesa: 15,
      personas: 4,
      mozo: 'Marcos Vega',
      apertura: '11:13',
      tiempo: 22,
      estado: 'Por cobrar',
      subtotal: 64.5,
      igv: 11.61,
      total: 76.11,
      items: [
          { cantidad: 2, producto: 'Papa a la huancaína', categoria: 'Entradas', subtotal: 44 },
          { cantidad: 1, producto: 'Tres leches', categoria: 'Postres', subtotal: 20.5 },
        ],
      nota: null,
      ultimoCambio: 'salon.marta - 11:33',
    },
    {
      id: '#1041',
      hora: '11:28',
      mesa: 2,
      personas: 5,
      mozo: 'Ana Torres',
      apertura: '11:02',
      tiempo: 26,
      estado: 'En parrilla',
      subtotal: 88.4,
      igv: 15.91,
      total: 104.31,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 44.4 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 11:26',
    },
    {
      id: '#1040',
      hora: '11:21',
      mesa: 7,
      personas: 6,
      mozo: 'Rosa Huamán',
      apertura: '10:50',
      tiempo: 31,
      estado: 'Listo',
      subtotal: 112.6,
      igv: 20.27,
      total: 132.87,
      items: [
          { cantidad: 2, producto: 'Picarones (4 u.)', categoria: 'Postres', subtotal: 36 },
          { cantidad: 1, producto: 'Inca Kola 1 L', categoria: 'Bebidas', subtotal: 11.5 },
          { cantidad: 1, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 65.1 },
        ],
      nota: 'Término 3/4, sin ají en la ensalada.',
      ultimoCambio: 'caja.valeria - 11:19',
    },
    {
      id: '#1039',
      hora: '11:14',
      mesa: 12,
      personas: 2,
      mozo: 'Lucía Peña',
      apertura: '10:39',
      tiempo: 35,
      estado: 'En preparación',
      subtotal: 148.2,
      igv: 26.68,
      total: 174.88,
      items: [
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 46.5 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 101.7 },
        ],
      nota: null,
      ultimoCambio: 'cocina.jorge - 11:12',
    },
    {
      id: '#1038',
      hora: '11:11',
      mesa: 17,
      personas: 3,
      mozo: 'Diego Rojas',
      apertura: '11:02',
      tiempo: 9,
      estado: 'Entregado',
      subtotal: 96.8,
      igv: 17.42,
      total: 114.22,
      items: [
          { cantidad: 2, producto: 'Papa a la huancaína', categoria: 'Entradas', subtotal: 44 },
          { cantidad: 1, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 52.8 },
        ],
      nota: null,
      ultimoCambio: 'salon.marta - 11:09',
    },
    {
      id: '#1037',
      hora: '11:04',
      mesa: 4,
      personas: 5,
      mozo: 'Ana Torres',
      apertura: '10:50',
      tiempo: 14,
      estado: 'Por cobrar',
      subtotal: 176.3,
      igv: 31.73,
      total: 208.03,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 2, producto: 'Causa limeña', categoria: 'Entradas', subtotal: 48 },
          { cantidad: 1, producto: 'Pollo a la brasa 1/4', categoria: 'Parrilla', subtotal: 38 },
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 46.3 },
        ],
      nota: 'Una porción para llevar, usar envase térmico.',
      ultimoCambio: 'caja.valeria - 11:02',
    },
    {
      id: '#1036',
      hora: '10:57',
      mesa: 9,
      personas: 6,
      mozo: 'Rosa Huamán',
      apertura: '10:39',
      tiempo: 18,
      estado: 'En parrilla',
      subtotal: 132.75,
      igv: 23.9,
      total: 156.65,
      items: [
          { cantidad: 2, producto: 'Picarones (4 u.)', categoria: 'Postres', subtotal: 36 },
          { cantidad: 1, producto: 'Inca Kola 1 L', categoria: 'Bebidas', subtotal: 11.5 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 85.25 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 10:55',
    },
    {
      id: '#1035',
      hora: '10:50',
      mesa: 14,
      personas: 2,
      mozo: 'Lucía Peña',
      apertura: '10:28',
      tiempo: 22,
      estado: 'Listo',
      subtotal: 74.9,
      igv: 13.48,
      total: 88.38,
      items: [
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 46.5 },
          { cantidad: 1, producto: 'Ensalada parrillera', categoria: 'Entradas', subtotal: 28.4 },
        ],
      nota: null,
      ultimoCambio: 'cocina.jorge - 10:48',
    },
    {
      id: '#1034',
      hora: '10:47',
      mesa: 1,
      personas: 3,
      mozo: 'Diego Rojas',
      apertura: '10:21',
      tiempo: 26,
      estado: 'En preparación',
      subtotal: 208.4,
      igv: 37.51,
      total: 245.91,
      items: [
          { cantidad: 2, producto: 'Papa a la huancaína', categoria: 'Entradas', subtotal: 44 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 79.9 },
          { cantidad: 2, producto: 'Mazamorra morada', categoria: 'Postres', subtotal: 28 },
          { cantidad: 1, producto: 'Agua mineral 600 ml', categoria: 'Bebidas', subtotal: 8 },
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 48.5 },
        ],
      nota: 'Término 3/4, sin ají en la ensalada.',
      ultimoCambio: 'salon.marta - 10:45',
    },
    {
      id: '#1033',
      hora: '10:40',
      mesa: 6,
      personas: 4,
      mozo: 'Marcos Vega',
      apertura: '10:09',
      tiempo: 31,
      estado: 'Entregado',
      subtotal: 58.2,
      igv: 10.48,
      total: 68.68,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 1, producto: 'Mazamorra morada', categoria: 'Postres', subtotal: 14.2 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 10:38',
    },
    {
      id: '#1032',
      hora: '10:33',
      mesa: 11,
      personas: 6,
      mozo: 'Rosa Huamán',
      apertura: '09:58',
      tiempo: 35,
      estado: 'Por cobrar',
      subtotal: 64.5,
      igv: 11.61,
      total: 76.11,
      items: [
          { cantidad: 2, producto: 'Picarones (4 u.)', categoria: 'Postres', subtotal: 36 },
          { cantidad: 1, producto: 'Ensalada parrillera', categoria: 'Entradas', subtotal: 28.5 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 10:31',
    },
    {
      id: '#1031',
      hora: '10:26',
      mesa: 16,
      personas: 2,
      mozo: 'Lucía Peña',
      apertura: '10:17',
      tiempo: 9,
      estado: 'En parrilla',
      subtotal: 88.4,
      igv: 15.91,
      total: 104.31,
      items: [
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 46.5 },
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 41.9 },
        ],
      nota: 'Una porción para llevar, usar envase térmico.',
      ultimoCambio: 'cocina.jorge - 10:24',
    },
    {
      id: '#1030',
      hora: '10:23',
      mesa: 3,
      personas: 3,
      mozo: 'Diego Rojas',
      apertura: '10:09',
      tiempo: 14,
      estado: 'Listo',
      subtotal: 112.6,
      igv: 20.27,
      total: 132.87,
      items: [
          { cantidad: 2, producto: 'Papa a la huancaína', categoria: 'Entradas', subtotal: 44 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 68.6 },
        ],
      nota: null,
      ultimoCambio: 'salon.marta - 10:21',
    },
    {
      id: '#1029',
      hora: '10:16',
      mesa: 8,
      personas: 4,
      mozo: 'Marcos Vega',
      apertura: '09:58',
      tiempo: 18,
      estado: 'En preparación',
      subtotal: 148.2,
      igv: 26.68,
      total: 174.88,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 2, producto: 'Causa limeña', categoria: 'Entradas', subtotal: 48 },
          { cantidad: 1, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 56.2 },
        ],
      nota: null,
      ultimoCambio: 'caja.valeria - 10:14',
    },
    {
      id: '#1028',
      hora: '10:09',
      mesa: 13,
      personas: 5,
      mozo: 'Ana Torres',
      apertura: '09:47',
      tiempo: 22,
      estado: 'Entregado',
      subtotal: 96.8,
      igv: 17.42,
      total: 114.22,
      items: [
          { cantidad: 2, producto: 'Picarones (4 u.)', categoria: 'Postres', subtotal: 36 },
          { cantidad: 1, producto: 'Inca Kola 1 L', categoria: 'Bebidas', subtotal: 11.5 },
          { cantidad: 1, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 49.3 },
        ],
      nota: 'Término 3/4, sin ají en la ensalada.',
      ultimoCambio: 'caja.valeria - 10:07',
    },
    {
      id: '#1027',
      hora: '10:02',
      mesa: 18,
      personas: 2,
      mozo: 'Lucía Peña',
      apertura: '09:36',
      tiempo: 26,
      estado: 'Por cobrar',
      subtotal: 176.3,
      igv: 31.73,
      total: 208.03,
      items: [
          { cantidad: 1, producto: 'Costillas BBQ', categoria: 'Parrilla', subtotal: 46.5 },
          { cantidad: 2, producto: 'Chancho al palo', categoria: 'Parrilla', subtotal: 104 },
          { cantidad: 1, producto: 'Palta reina', categoria: 'Entradas', subtotal: 25.8 },
        ],
      nota: null,
      ultimoCambio: 'cocina.jorge - 10:00',
    },
    {
      id: '#1026',
      hora: '09:59',
      mesa: 5,
      personas: 3,
      mozo: 'Diego Rojas',
      apertura: '09:28',
      tiempo: 31,
      estado: 'En parrilla',
      subtotal: 132.75,
      igv: 23.9,
      total: 156.65,
      items: [
          { cantidad: 2, producto: 'Papa a la huancaína', categoria: 'Entradas', subtotal: 44 },
          { cantidad: 1, producto: 'Parrilla El Hornero', categoria: 'Parrilla', subtotal: 88.75 },
        ],
      nota: null,
      ultimoCambio: 'salon.marta - 09:57',
    },
    {
      id: '#1025',
      hora: '09:52',
      mesa: 10,
      personas: 4,
      mozo: 'Marcos Vega',
      apertura: '09:17',
      tiempo: 35,
      estado: 'Listo',
      subtotal: 74.9,
      igv: 13.48,
      total: 88.38,
      items: [
          { cantidad: 1, producto: 'Seco de cabrito', categoria: 'Parrilla', subtotal: 44 },
          { cantidad: 1, producto: 'Tiradito nikkei', categoria: 'Entradas', subtotal: 30.9 },
        ],
      nota: 'Una porción para llevar, usar envase térmico.',
      ultimoCambio: 'caja.valeria - 09:50',
    },
  ];

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const categoria = this.categoria();
    const estado = this.estadoFiltro();

    return this.pedidos.filter((pedido) => {
      const matchesTerm =
        term === '' ||
        pedido.id.toLowerCase().includes(term) ||
        pedido.mozo.toLowerCase().includes(term) ||
        this.mesaLabel(pedido.mesa).toLowerCase().includes(term) ||
        pedido.items.some((item) => item.producto.toLowerCase().includes(term));
      const matchesCategoria =
        categoria === 'todas' || pedido.items.some((item) => item.categoria === categoria);
      const matchesEstado = estado === 'todos' || pedido.estado === estado;
      return matchesTerm && matchesCategoria && matchesEstado;
    });
  });

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filtered().length / this.pageSize))
  );

  readonly pagedRows = computed(() => {
    const start = (this.page() - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  readonly rangeLabel = computed(() => {
    const total = this.filtered().length;
    if (total === 0) {
      return 'Mostrando 0-0 de 0 registros';
    }
    const from = (this.page() - 1) * this.pageSize + 1;
    const to = Math.min(this.page() * this.pageSize, total);
    return `Mostrando ${from}-${to} de ${total} registros`;
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

  readonly shiftLabel = computed(
    () => `Turno almuerzo - ${this.filtered().length} pedidos`
  );

  readonly selected = computed(
    () => this.pedidos.find((pedido) => pedido.id === this.selectedId()) ?? this.pedidos[0]
  );

  readonly selectedItemsCount = computed(() =>
    this.selected().items.reduce((total, item) => total + item.cantidad, 0)
  );

  onSearch(event: Event): void {
    this.searchTerm.set((event.target as HTMLInputElement).value);
    this.page.set(1);
  }

  onCategoria(event: Event): void {
    this.categoria.set((event.target as HTMLSelectElement).value as CategoriaFiltro);
    this.page.set(1);
  }

  onEstado(event: Event): void {
    this.estadoFiltro.set((event.target as HTMLSelectElement).value as EstadoFiltro);
    this.page.set(1);
  }

  resetFilters(): void {
    this.searchTerm.set('');
    this.categoria.set('todas');
    this.estadoFiltro.set('todos');
    this.page.set(1);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) {
      return;
    }
    this.page.set(page);
  }

  select(pedido: Pedido): void {
    this.selectedId.set(pedido.id);
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  mesaLabel(mesa: number): string {
    return `Mesa ${String(mesa).padStart(2, '0')}`;
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
}
