import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type EstadoPedido = 'PENDIENTE' | 'EN_PREPARACION' | 'ENTREGADO' | 'CANCELADO';

export type EstadoPedidoVisual =
  | 'Por cobrar'
  | 'En parrilla'
  | 'Listo'
  | 'En preparación'
  | 'Entregado'
  | 'Anulado';

export interface DetallePedido {
  id: number;
  pedidoId: number | null;
  platoId: number;
  platoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  categoria?: string;
}

export interface Pedido {
  id: number;
  estado: EstadoPedido;
  horaTomadaDelPedido: string;
  mesaId: number | null;
  mesaNumero: number | null;
  empleadoId: number | null;
  total: number;
  detalles: DetallePedido[];
}

export interface ItemPedidoRequest {
  platoId: number;
  cantidad: number;
}

export interface CrearPedidoRequest {
  mesaId: number;
  empleadoId: number;
  items: ItemPedidoRequest[];
}

export interface AgregarDetalleRequest {
  pedidoId: number;
  platoId: number;
  cantidad: number;
}

const ESTADOS_VISUALES: Record<EstadoPedido, EstadoPedidoVisual> = {
  PENDIENTE: 'Por cobrar',
  EN_PREPARACION: 'En preparación',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Anulado',
};

const ESTADOS_BACKEND: Record<EstadoPedidoVisual, EstadoPedido> = {
  'Por cobrar': 'PENDIENTE',
  'En parrilla': 'EN_PREPARACION',
  Listo: 'ENTREGADO',
  'En preparación': 'EN_PREPARACION',
  Entregado: 'ENTREGADO',
  Anulado: 'CANCELADO',
};

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/pedidos`;
  private readonly baseDetalles = `${environment.apiUrl}/detalles-pedido`;

  listar(estado?: EstadoPedido | null): Observable<Pedido[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<Pedido[]>(this.base, params ? { params } : {});
  }

  obtener(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.base}/${id}`);
  }

  detalles(id: number): Observable<DetallePedido[]> {
    return this.http.get<DetallePedido[]>(`${this.base}/${id}/detalles`);
  }

  crear(datos: CrearPedidoRequest): Observable<Pedido> {
    return this.http.post<Pedido>(this.base, datos);
  }

  agregarDetalle(datos: AgregarDetalleRequest): Observable<DetallePedido> {
    return this.http.post<DetallePedido>(this.baseDetalles, datos);
  }

  actualizarDetalle(id: number, cantidad: number): Observable<DetallePedido> {
    return this.http.put<DetallePedido>(`${this.baseDetalles}/${id}`, { cantidad });
  }

  eliminarDetalle(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseDetalles}/${id}`);
  }

  cambiarEstado(id: number, estado: EstadoPedido): Observable<Pedido> {
    return this.http.put<Pedido>(`${this.base}/${id}/estado`, { estado });
  }

  etiquetaEstado(estado: EstadoPedido): EstadoPedidoVisual {
    return ESTADOS_VISUALES[estado] ?? 'Por cobrar';
  }

  estadoDesdeEtiqueta(etiqueta: EstadoPedidoVisual | string): EstadoPedido {
    return ESTADOS_BACKEND[etiqueta as EstadoPedidoVisual] ?? 'PENDIENTE';
  }
}
