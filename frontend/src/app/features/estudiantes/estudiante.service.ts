import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

export interface EstudianteResponse {
    idEstudiante: number;
    nombrePersona: string;
    apellidoPersona: string;
    nombreCategoria: string;
    nombreEstadoGeneral: string;
    codigoEstudiante: string;
    fechaIngreso: string;
    peso: number | null;
    altura: number | null;
    activo: boolean;
    createdAt: string;
}

export interface EstudiantePageResponse {
    content: EstudianteResponse[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface EstudianteRequest {
    idPersona: number;
    idCategoria: number;
    idEstadoGeneral: number;
    codigoEstudiante: string;
    fechaIngreso: string;
    peso: number | null;
    altura: number | null;
}

export interface CategoriaResponse {
    idCategoria: number;
    nombre: string;
    edadMin: number;
    edadMax: number;
    descripcion: string;
    activo: boolean;
    createdAt: string;
}

export interface EstadoGeneralResponse {
    idEstadoGeneral: number;
    nombre: string;
}

/**
 * Consume el CRUD de estudiantes (/api/estudiantes) y los catalogos de
 * apoyo (categorias activas, estados generales) que necesita el
 * formulario de alta. El JWT viaja en cookie HttpOnly: el interceptor
 * global ya adjunta withCredentials, aqui solo se arman las peticiones.
 */
@Injectable({ providedIn: 'root' })
export class EstudianteService {
    private readonly apiUrl = '/api/estudiantes';

    constructor(private http: HttpClient) { }

    listar(page: number, size: number) {
        const params = new HttpParams()
            .set('page', page)
            .set('size', size)
            .set('sort', 'idEstudiante,desc');
        return this.http.get<EstudiantePageResponse>(this.apiUrl, { params });
    }

    crear(request: EstudianteRequest) {
        return this.http.post<EstudianteResponse>(this.apiUrl, request);
    }

    listarCategoriasActivas() {
        return this.http.get<CategoriaResponse[]>('/api/categorias/activas');
    }

    listarEstadosGenerales() {
        return this.http.get<EstadoGeneralResponse[]>('/api/estados_generales');
    }
}
