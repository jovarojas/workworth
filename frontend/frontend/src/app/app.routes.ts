import { Routes } from '@angular/router';
import { authenticatedGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login-page.component')
        .then((module) => module.LoginPageComponent),
    title: 'Acceso | WorkWorth'
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard-page/dashboard-page.component')
        .then((module) => module.DashboardPageComponent),
    title: 'WorkWorth',
    pathMatch: 'full',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'workday',
    loadComponent: () =>
      import('./features/workday/pages/workday-live/workday-live.component')
        .then((module) => module.WorkdayLiveComponent),
    title: 'Jornada | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'salary',
    loadComponent: () =>
      import('./features/salary/pages/salary-profile/salary-profile.component')
        .then((module) => module.SalaryProfileComponent),
    title: 'Salario | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'earnings',
    loadComponent: () =>
      import('./features/earnings/pages/earnings-history/earnings-history.component')
        .then((module) => module.EarningsHistoryComponent),
    title: 'Historial de ganancias | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'earnings/workdays/:date',
    loadComponent: () =>
      import('./features/earnings/pages/earning-detail/earning-detail.component')
        .then((module) => module.EarningDetailComponent),
    title: 'Detalle de ganancia | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'rewards',
    loadComponent: () =>
      import('./features/rewards/pages/rewards-page/rewards-page.component')
        .then((module) => module.RewardsPageComponent),
    title: 'Recompensas | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'goals',
    loadComponent: () =>
      import('./features/goals/pages/goals-page/goals-page.component')
        .then((module) => module.GoalsPageComponent),
    title: 'Objetivos | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'statistics',
    loadComponent: () =>
      import('./features/statistics/pages/statistics-page/statistics-page.component')
        .then((module) => module.StatisticsPageComponent),
    title: 'Estadísticas | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  {
    path: 'preferences/currency',
    loadComponent: () =>
      import('./features/preferences/pages/currency-settings/currency-settings.component')
        .then((module) => module.CurrencySettingsComponent),
    title: 'Ajustes | WorkWorth',
    canActivate: [authenticatedGuard]
  },
  { path: '**', redirectTo: '' }
];
