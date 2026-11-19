import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface InsumoBackend {
  id: number;
  nombre: string;
  descripcion?: string | null;
  stockActual: number;
  stockMinimo: number;
  unidadMedida: string;
  imagen?: string | null;
}

export interface Merma {
  id: number;
  insumo?: { id: number; nombre: string } | null;
  insumoId?: number | null;
  cantidad: number;
  motivo?: string | null;
  observacion?: string | null;
  empleadoId?: number | null;
  fechaRegistro?: string | null;
}

export interface DetalleIngreso {
  id: number;
  insumoId: number;
  cantidad: number;
}

export interface Ingreso {
  id: number;
  fechaIngreso: string;
  proveedorId: number | null;
  empleadoId: number | null;
  detalles: DetalleIngreso[];
}

export interface RegistrarMermaRequest {
  insumoId: number;
  cantidad: number;
  motivo: string;
  observacion: string;
  empleadoId: number;
}

export interface RegistrarIngresoRequest {
  proveedorId: number;
  empleadoId: number;
  detalles: { insumoId: number; cantidad: number }[];
}

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}`;

  listarInsumos(): Observable<InsumoBackend[]> {
    return this.http.get<InsumoBackend[]>(`${this.base}/insumos`);
  }

  listarStockBajo(): Observable<InsumoBackend[]> {
    return this.http.get<InsumoBackend[]>(`${this.base}/insumos/stock-bajo`);
  }

  listarMermas(): Observable<Merma[]> {
    return this.http.get<Merma[]>(`${this.base}/mermas`);
  }

  listarIngresos(): Observable<Ingreso[]> {
    return this.http.get<Ingreso[]>(`${this.base}/ingresos`);
  }

  registrarMerma(datos: RegistrarMermaRequest): Observable<Merma> {
    return this.http.post<Merma>(`${this.base}/mermas`, datos);
  }

  registrarIngreso(datos: RegistrarIngresoRequest): Observable<Ingreso> {
    return this.http.post<Ingreso>(`${this.base}/ingresos`, datos);
  }
}
