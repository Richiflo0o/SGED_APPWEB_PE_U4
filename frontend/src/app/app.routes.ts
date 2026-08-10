import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'registro', loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent) },
  {
    path: '', canActivate: [authGuard], children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
    ]
  },
  // --- init nuevas rutas ---
  { path: 'estudiantes', loadComponent: () => import('./features/estudiantes/estudiantes.component').then(m => m.EstudiantesComponent) },
  { path: 'deportivo-externo', loadComponent: () => import('./features/deportivo-externo/deportivo-externo.component').then(m => m.DeportivoExternoComponent) },
  // --- end nuevas rutas ---
  { path: '**', redirectTo: '' }
];
