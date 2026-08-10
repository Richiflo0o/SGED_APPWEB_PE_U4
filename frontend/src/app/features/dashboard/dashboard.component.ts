import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService, AuthResponse } from '../../auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dashboard">
      <h1>SGED - Sistema de Gestion para Escuela Deportiva</h1>
      <div *ngIf="user" class="user-info">
        <p>Bienvenido, <strong>{{ user.nombre }}</strong></p>
        <p>Rol: {{ user.rol }}</p>
      </div>
      <nav class="menu">
        <a routerLink="/estudiantes">Estudiantes</a>
        <a routerLink="/deportivo-externo">Posiciones y partidos</a>
      </nav>
      <button (click)="logout()">Cerrar sesion</button>
    </div>
  `,
  styles: [`
    .dashboard { max-width: 800px; margin: 40px auto; padding: 2rem; }
    .user-info { margin: 1rem 0; padding: 1rem; background: #f5f5f5; border-radius: 8px; }
    .menu { display: flex; gap: 1rem; margin: 1.5rem 0; }
    .menu a { color: #1976d2; text-decoration: none; font-weight: bold; }
    .menu a:hover { text-decoration: underline; }
    button { padding: 0.5rem 1rem; background: #d32f2f; color: white; border: none; border-radius: 4px; cursor: pointer; }
  `]
})
export class DashboardComponent implements OnInit {
  user: AuthResponse | null = null;

  constructor(private authService: AuthService) { }

  ngOnInit() {
    this.authService.getProfile().subscribe({
      next: (user) => this.user = user,
      error: () => { }
    });
    this.user = this.authService.currentUser();
  }

  logout() {
    this.authService.logout().subscribe();
  }
}