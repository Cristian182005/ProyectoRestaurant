import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ClienteResponse {
  id: number;
  nombre: string;
  tipoDocumento: string;
  numeroDocumento: string;
  email: string;
  telefono: string;
  direccion: string;
}

export interface ProveedorResponse {
  id: number;
  nombre: string;
  ruc: string;
  email: string;
  telefono: string;
  direccion: string;
}

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl;

  listarNaturales(): Observable<ClienteResponse[]> {
    return this.http.get<ClienteResponse[]>(`${this.base}/clientes/naturales`);
  }

  listarJuridicos(): Observable<ClienteResponse[]> {
    return this.http.get<ClienteResponse[]>(`${this.base}/clientes/juridicos`);
  }

  listarProveedores(): Observable<ProveedorResponse[]> {
    return this.http.get<ProveedorResponse[]>(`${this.base}/proveedores`);
  }
}
