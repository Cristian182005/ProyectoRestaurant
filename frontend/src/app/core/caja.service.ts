import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type TipoPago = 'EFECTIVO' | 'TARJETA' | 'YAPE' | 'PLIN' | 'TRANSFERENCIA';

export const TIPOS_PAGO: TipoPago[] = [
  'EFECTIVO',
  'TARJETA',
  'YAPE',
  'PLIN',
  'TRANSFERENCIA',
];

export interface Comprobante {
  id: number;
  fechaEmision: string;
  horaEmision: string;
  gravado: number;
  igv: number;
  precioTotal: number;
  tipoPago: TipoPago;
  vueltoPago: number | null;
  montoRecibido: number;
  cajeroId: number | null;
  pedidoId: number | null;
  numeroComprobante?: string;
  cliente?: string;
  documento?: string;
  email?: string;
  estado?: 'Emitido' | 'Enviado SUNAT' | 'Anulado';
  clienteNaturalId?: number | null;
  clienteJuridicoId?: number | null;
}

export interface ResumenVentas {
  desde: string;
  hasta: string;
  cantidadComprobantes: number;
  totalVentas: number;
  totalIgv: number;
  promedioTicket: number;
}

export interface EmitirComprobanteRequest {
  pedidoId: number;
  cajeroId: number;
  tipoPago: TipoPago;
  montoRecibido: number;
  clienteNaturalId: number | null;
  clienteJuridicoId: number | null;
}

export function fechaISO(valor: string | Date): string {
  if (valor instanceof Date) {
    const anio = valor.getFullYear();
    const mes = String(valor.getMonth() + 1).padStart(2, '0');
    const dia = String(valor.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  const texto = valor.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(texto)) {
    return texto;
  }
  const partes = /^(\d{1,2})[\/-](\d{1,2})[\/-](\d{4})$/.exec(texto);
  if (partes) {
    return `${partes[3]}-${partes[2].padStart(2, '0')}-${partes[1].padStart(2, '0')}`;
  }
  const fecha = new Date(texto);
  return Number.isNaN(fecha.getTime()) ? fechaISO(new Date()) : fechaISO(fecha);
}

@Injectable({ providedIn: 'root' })
export class CajaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/comprobantes`;

  listar(desde?: string, hasta?: string): Observable<Comprobante[]> {
    let params = new HttpParams();
    if (desde) {
      params = params.set('desde', fechaISO(desde));
    }
    if (hasta) {
      params = params.set('hasta', fechaISO(hasta));
    }
    return this.http.get<Comprobante[]>(this.base, { params });
  }

  resumen(desde: string, hasta: string): Observable<ResumenVentas> {
    const params = new HttpParams()
      .set('desde', fechaISO(desde))
      .set('hasta', fechaISO(hasta));
    return this.http.get<ResumenVentas>(`${this.base}/resumen`, { params });
  }

  porPedido(pedidoId: number): Observable<Comprobante> {
    return this.http.get<Comprobante>(`${this.base}/pedido/${pedidoId}`);
  }

  emitir(datos: EmitirComprobanteRequest): Observable<Comprobante> {
    return this.http.post<Comprobante>(this.base, datos);
  }
}
