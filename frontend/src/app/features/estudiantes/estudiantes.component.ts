import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
    CategoriaResponse,
    EstadoGeneralResponse,
    EstudianteRequest,
    EstudianteResponse,
    EstudianteService,
} from './estudiante.service';

/**
 * Pantalla CRUD de estudiantes (listar + crear). Bloque B.1 de la
 * Practica Experimental Unidad IV. La edicion/eliminacion ya existen en
 * el backend (EstudianteController) pero no se piden en esta entrega,
 * por lo que la UI se centra en listar y crear con estados de carga y
 * error amigables, igual que la pantalla de la API externa.
 */
@Component({
    selector: 'app-estudiantes',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="page">
      <h1>Estudiantes</h1>

      <section class="panel">
        <h2>Nuevo estudiante</h2>
        <form (ngSubmit)="onCrear()">
          <div class="grid">
            <div>
              <label for="idPersona">ID de persona</label>
              <input id="idPersona" type="number" [(ngModel)]="form.idPersona" name="idPersona" required min="1" />
              <small>La persona debe existir previamente (alta de personas fuera de esta pantalla).</small>
            </div>
            <div>
              <label for="codigoEstudiante">Codigo</label>
              <input id="codigoEstudiante" type="text" [(ngModel)]="form.codigoEstudiante" name="codigoEstudiante" required maxlength="30" />
            </div>
            <div>
              <label for="idCategoria">Categoria</label>
              <select id="idCategoria" [(ngModel)]="form.idCategoria" name="idCategoria" required [disabled]="catalogosCargando">
                <option [ngValue]="null" disabled>Seleccione...</option>
                <option *ngFor="let c of categorias" [ngValue]="c.idCategoria">{{ c.nombre }}</option>
              </select>
            </div>
            <div>
              <label for="idEstadoGeneral">Estado general</label>
              <select id="idEstadoGeneral" [(ngModel)]="form.idEstadoGeneral" name="idEstadoGeneral" required [disabled]="catalogosCargando">
                <option [ngValue]="null" disabled>Seleccione...</option>
                <option *ngFor="let e of estadosGenerales" [ngValue]="e.idEstadoGeneral">{{ e.nombre }}</option>
              </select>
            </div>
            <div>
              <label for="fechaIngreso">Fecha de ingreso</label>
              <input id="fechaIngreso" type="date" [(ngModel)]="form.fechaIngreso" name="fechaIngreso" required />
            </div>
            <div>
              <label for="peso">Peso (kg)</label>
              <input id="peso" type="number" step="0.01" [(ngModel)]="form.peso" name="peso" />
            </div>
            <div>
              <label for="altura">Altura (m)</label>
              <input id="altura" type="number" step="0.01" [(ngModel)]="form.altura" name="altura" />
            </div>
          </div>

          <div *ngIf="catalogosError" class="error">
            No se pudieron cargar categorias/estados. <button type="button" (click)="cargarCatalogos()">Reintentar</button>
          </div>
          <div *ngIf="crearError" class="error">{{ crearError }}</div>
          <div *ngIf="crearOk" class="ok">Estudiante creado correctamente.</div>

          <button type="submit" [disabled]="creando || catalogosCargando">
            {{ creando ? 'Guardando...' : 'Crear estudiante' }}
          </button>
        </form>
      </section>

      <section class="panel">
        <h2>Listado</h2>

        <div *ngIf="cargando" class="estado-carga">Cargando estudiantes...</div>

        <div *ngIf="!cargando && error" class="error">
          No se pudo cargar el listado. <button type="button" (click)="cargarListado()">Reintentar</button>
        </div>

        <div *ngIf="!cargando && !error && estudiantes.length === 0" class="estado-carga">
          No hay estudiantes registrados todavia.
        </div>

        <table *ngIf="!cargando && !error && estudiantes.length > 0">
          <thead>
            <tr>
              <th>Codigo</th>
              <th>Nombre</th>
              <th>Categoria</th>
              <th>Estado</th>
              <th>Ingreso</th>
              <th>Activo</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of estudiantes">
              <td>{{ e.codigoEstudiante }}</td>
              <td>{{ e.nombrePersona }} {{ e.apellidoPersona }}</td>
              <td>{{ e.nombreCategoria }}</td>
              <td>{{ e.nombreEstadoGeneral }}</td>
              <td>{{ e.fechaIngreso }}</td>
              <td>{{ e.activo ? 'Si' : 'No' }}</td>
            </tr>
          </tbody>
        </table>

        <div *ngIf="!cargando && !error && totalPages > 1" class="paginacion">
          <button type="button" (click)="cambiarPagina(page - 1)" [disabled]="page === 0">Anterior</button>
          <span>Pagina {{ page + 1 }} de {{ totalPages }}</span>
          <button type="button" (click)="cambiarPagina(page + 1)" [disabled]="page + 1 >= totalPages">Siguiente</button>
        </div>
      </section>
    </div>
  `,
    styles: [`
    .page { max-width: 900px; margin: 40px auto; padding: 0 1rem; }
    .panel { border: 1px solid #ddd; border-radius: 8px; padding: 1.5rem; margin-bottom: 2rem; }
    .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
    label { display: block; font-weight: bold; margin-bottom: 0.25rem; }
    input, select { width: 100%; padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; }
    small { color: #666; }
    table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
    th, td { text-align: left; padding: 0.5rem; border-bottom: 1px solid #eee; }
    button { padding: 0.6rem 1.2rem; background: #1976d2; color: white; border: none; border-radius: 4px; cursor: pointer; margin-top: 1rem; }
    button:disabled { background: #ccc; cursor: not-allowed; }
    .error { color: #d32f2f; margin-top: 0.75rem; }
    .error button { background: #d32f2f; margin: 0 0 0 0.5rem; padding: 0.3rem 0.7rem; }
    .ok { color: #2e7d32; margin-top: 0.75rem; }
    .estado-carga { color: #666; padding: 1rem 0; }
    .paginacion { display: flex; align-items: center; gap: 1rem; margin-top: 1rem; }
    .paginacion button { margin-top: 0; }
  `]
})
export class EstudiantesComponent implements OnInit {
    estudiantes: EstudianteResponse[] = [];
    categorias: CategoriaResponse[] = [];
    estadosGenerales: EstadoGeneralResponse[] = [];

    page = 0;
    size = 10;
    totalPages = 0;

    cargando = false;
    error = false;

    catalogosCargando = false;
    catalogosError = false;

    creando = false;
    crearError = '';
    crearOk = false;

    form: Partial<EstudianteRequest> = {
        idPersona: undefined,
        idCategoria: null as unknown as number,
        idEstadoGeneral: null as unknown as number,
        codigoEstudiante: '',
        fechaIngreso: '',
        peso: null,
        altura: null,
    };

    constructor(private estudianteService: EstudianteService) { }

    ngOnInit() {
        this.cargarListado();
        this.cargarCatalogos();
    }

    cargarListado() {
        this.cargando = true;
        this.error = false;
        this.estudianteService.listar(this.page, this.size).subscribe({
            next: (resp) => {
                this.estudiantes = resp.content;
                this.totalPages = resp.totalPages;
                this.cargando = false;
            },
            error: () => {
                this.cargando = false;
                this.error = true;
            },
        });
    }

    cargarCatalogos() {
        this.catalogosCargando = true;
        this.catalogosError = false;
        let pendientes = 2;
        const terminar = () => {
            pendientes -= 1;
            if (pendientes === 0) this.catalogosCargando = false;
        };
        this.estudianteService.listarCategoriasActivas().subscribe({
            next: (data) => { this.categorias = data; terminar(); },
            error: () => { this.catalogosError = true; terminar(); },
        });
        this.estudianteService.listarEstadosGenerales().subscribe({
            next: (data) => { this.estadosGenerales = data; terminar(); },
            error: () => { this.catalogosError = true; terminar(); },
        });
    }

    cambiarPagina(nuevaPagina: number) {
        if (nuevaPagina < 0 || nuevaPagina >= this.totalPages) return;
        this.page = nuevaPagina;
        this.cargarListado();
    }

    onCrear() {
        this.creando = true;
        this.crearError = '';
        this.crearOk = false;

        const request: EstudianteRequest = {
            idPersona: Number(this.form.idPersona),
            idCategoria: Number(this.form.idCategoria),
            idEstadoGeneral: Number(this.form.idEstadoGeneral),
            codigoEstudiante: this.form.codigoEstudiante ?? '',
            fechaIngreso: this.form.fechaIngreso ?? '',
            peso: this.form.peso ?? null,
            altura: this.form.altura ?? null,
        };

        this.estudianteService.crear(request).subscribe({
            next: () => {
                this.creando = false;
                this.crearOk = true;
                this.form = {
                    idPersona: undefined,
                    idCategoria: null as unknown as number,
                    idEstadoGeneral: null as unknown as number,
                    codigoEstudiante: '',
                    fechaIngreso: '',
                    peso: null,
                    altura: null,
                };
                this.page = 0;
                this.cargarListado();
            },
            error: (err) => {
                this.creando = false;
                this.crearError = err.status === 400
                    ? 'Datos invalidos. Revisa los campos obligatorios.'
                    : err.status === 409
                        ? 'Ya existe un estudiante con ese codigo.'
                        : 'No se pudo crear el estudiante. Intenta de nuevo.';
            },
        });
    }
}
