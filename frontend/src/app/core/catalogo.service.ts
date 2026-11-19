import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Plato {
  id: number;
  nombre: string;
  descripcion?: string | null;
  imagen?: string | null;
  activo: boolean;
  categoria: string;
  precio: number;
}

export interface Mesa {
  id: number;
  numero: number;
  estado: string;
  capacidad: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  listarPlatosActivos(): Observable<Plato[]> {
    const params = new HttpParams().set('activo', 'true');
    return this.http.get<Plato[]>(`${this.base}/platos`, { params });
  }

  listarMesas(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>(`${this.base}/mesas`);
  }
}
