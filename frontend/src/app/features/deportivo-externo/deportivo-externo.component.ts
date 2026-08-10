import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface FilaTabla {
    posicion: string;
    equipo: string;
    jugados: string;
    ganados: string;
    empatados: string;
    perdidos: string;
    puntos: string;
}

interface FilaPartido {
    evento: string;
    fecha: string;
    local: string;
    visitante: string;
    golesLocal: string;
    golesVisitante: string;
}

/**
 * Pantalla que consume una API externa publica (TheSportsDB, sin backend
 * propio de por medio) para mostrar la tabla de posiciones y los ultimos
 * partidos de una liga de futbol. Bloque B.2 de la Practica Experimental
 * Unidad IV: al ser una llamada de navegador a un tercero, se usa fetch()
 * directo en lugar de HttpClient para no arrastrar el interceptor JWT
 * (auth.interceptor agrega withCredentials a toda peticion de HttpClient,
 * lo que rompe el CORS de un dominio externo que no espera cookies).
 *
 * Nota: TheSportsDB usa la clave de pruebas publica "3" (sin registro).
 * Si el docente pide una API propia con clave, basta con cambiar
 * apiBase/leagueId aqui.
 */
@Component({
    selector: 'app-deportivo-externo',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="page">
      <h1>Posiciones y partidos (API externa)</h1>

      <div class="controles">
        <label for="liga">Liga</label>
        <select id="liga" [(ngModel)]="ligaId" name="liga" (change)="cargarTodo()">
          <option value="4328">Premier League (Inglaterra)</option>
          <option value="4335">La Liga (España)</option>
          <option value="4331">Bundesliga (Alemania)</option>
          <option value="4332">Serie A (Italia)</option>
        </select>
      </div>

      <section class="panel">
        <h2>Tabla de posiciones</h2>

        <div *ngIf="tablaCargando" class="estado-carga">Cargando tabla de posiciones...</div>

        <div *ngIf="!tablaCargando && tablaError" class="error">
          No se pudo cargar la tabla de posiciones. La API externa puede estar caida o sin datos para esta temporada.
          <button type="button" (click)="cargarTabla()">Reintentar</button>
        </div>

        <div *ngIf="!tablaCargando && !tablaError && tabla.length === 0" class="estado-carga">
          No hay datos de posiciones disponibles para esta liga/temporada.
        </div>

        <table *ngIf="!tablaCargando && !tablaError && tabla.length > 0">
          <thead>
            <tr>
              <th>#</th><th>Equipo</th><th>PJ</th><th>G</th><th>E</th><th>P</th><th>Pts</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let f of tabla">
              <td>{{ f.posicion }}</td>
              <td>{{ f.equipo }}</td>
              <td>{{ f.jugados }}</td>
              <td>{{ f.ganados }}</td>
              <td>{{ f.empatados }}</td>
              <td>{{ f.perdidos }}</td>
              <td>{{ f.puntos }}</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="panel">
        <h2>Ultimos partidos</h2>

        <div *ngIf="partidosCargando" class="estado-carga">Cargando partidos...</div>

        <div *ngIf="!partidosCargando && partidosError" class="error">
          No se pudieron cargar los partidos. Intenta de nuevo en unos segundos.
          <button type="button" (click)="cargarPartidos()">Reintentar</button>
        </div>

        <div *ngIf="!partidosCargando && !partidosError && partidos.length === 0" class="estado-carga">
          No hay partidos recientes registrados.
        </div>

        <ul *ngIf="!partidosCargando && !partidosError && partidos.length > 0" class="lista-partidos">
          <li *ngFor="let p of partidos">
            <span class="fecha">{{ p.fecha }}</span>
            <span class="equipos">{{ p.local }} {{ p.golesLocal }} - {{ p.golesVisitante }} {{ p.visitante }}</span>
          </li>
        </ul>
      </section>
    </div>
  `,
    styles: [`
    .page { max-width: 900px; margin: 40px auto; padding: 0 1rem; }
    .controles { margin-bottom: 1.5rem; display: flex; align-items: center; gap: 0.75rem; }
    .controles select { padding: 0.4rem; border-radius: 4px; border: 1px solid #ccc; }
    .panel { border: 1px solid #ddd; border-radius: 8px; padding: 1.5rem; margin-bottom: 2rem; }
    table { width: 100%; border-collapse: collapse; margin-top: 0.5rem; }
    th, td { text-align: left; padding: 0.5rem; border-bottom: 1px solid #eee; }
    .estado-carga { color: #666; padding: 1rem 0; }
    .error { color: #d32f2f; padding: 0.5rem 0; }
    .error button { margin-left: 0.5rem; padding: 0.3rem 0.7rem; background: #d32f2f; color: white; border: none; border-radius: 4px; cursor: pointer; }
    .lista-partidos { list-style: none; padding: 0; margin: 0.5rem 0 0; }
    .lista-partidos li { display: flex; justify-content: space-between; padding: 0.5rem 0; border-bottom: 1px solid #eee; }
    .fecha { color: #666; }
  `]
})
export class DeportivoExternoComponent implements OnInit {
    private readonly apiBase = 'https://www.thesportsdb.com/api/v1/json/3';

    ligaId = '4328';

    tabla: FilaTabla[] = [];
    tablaCargando = false;
    tablaError = false;

    partidos: FilaPartido[] = [];
    partidosCargando = false;
    partidosError = false;

    ngOnInit() {
        this.cargarTodo();
    }

    cargarTodo() {
        this.cargarTabla();
        this.cargarPartidos();
    }

    async cargarTabla() {
        this.tablaCargando = true;
        this.tablaError = false;
        try {
            const resp = await fetch(`${this.apiBase}/lookuptable.php?l=${this.ligaId}`);
            if (!resp.ok) throw new Error('http-error');
            const data = await resp.json();
            const filas = Array.isArray(data?.table) ? data.table : [];
            this.tabla = filas.map((f: any) => ({
                posicion: f.intRank ?? '-',
                equipo: f.strTeam ?? '-',
                jugados: f.intPlayed ?? '-',
                ganados: f.intWin ?? '-',
                empatados: f.intDraw ?? '-',
                perdidos: f.intLoss ?? '-',
                puntos: f.intPoints ?? '-',
            }));
        } catch {
            this.tablaError = true;
            this.tabla = [];
        } finally {
            this.tablaCargando = false;
        }
    }

    async cargarPartidos() {
        this.partidosCargando = true;
        this.partidosError = false;
        try {
            const resp = await fetch(`${this.apiBase}/eventspastleague.php?id=${this.ligaId}`);
            if (!resp.ok) throw new Error('http-error');
            const data = await resp.json();
            const eventos = Array.isArray(data?.events) ? data.events : [];
            this.partidos = eventos.slice(0, 10).map((e: any) => ({
                evento: e.strEvent ?? '-',
                fecha: e.dateEvent ?? '-',
                local: e.strHomeTeam ?? '-',
                visitante: e.strAwayTeam ?? '-',
                golesLocal: e.intHomeScore ?? '-',
                golesVisitante: e.intAwayScore ?? '-',
            }));
        } catch {
            this.partidosError = true;
            this.partidos = [];
        } finally {
            this.partidosCargando = false;
        }
    }
}
