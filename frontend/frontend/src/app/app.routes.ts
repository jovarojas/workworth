import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/dashboard/pages/dashboard-page/dashboard-page.component')
        .then((module) => module.DashboardPageComponent),
    title: 'WorkWorth',
    pathMatch: 'full'
  },
  {
    path: 'workday',
    loadComponent: () =>
      import('./features/workday/pages/workday-live/workday-live.component')
        .then((module) => module.WorkdayLiveComponent),
    title: 'Jornada | WorkWorth'
  },
  {
    path: 'salary',
    loadComponent: () =>
      import('./features/salary/pages/salary-profile/salary-profile.component')
        .then((module) => module.SalaryProfileComponent),
    title: 'Salario | WorkWorth'
  },
  {
    path: 'earnings',
    loadComponent: () =>
      import('./features/earnings/pages/earnings-history/earnings-history.component')
        .then((module) => module.EarningsHistoryComponent),
    title: 'Historial de ganancias | WorkWorth'
  },
  {
    path: 'earnings/workdays/:date',
    loadComponent: () =>
      import('./features/earnings/pages/earning-detail/earning-detail.component')
        .then((module) => module.EarningDetailComponent),
    title: 'Detalle de ganancia | WorkWorth'
  },
  {
    path: 'rewards',
    loadComponent: () =>
      import('./features/rewards/pages/rewards-page/rewards-page.component')
        .then((module) => module.RewardsPageComponent),
    title: 'Recompensas | WorkWorth'
  },
  {
    path: 'goals',
    loadComponent: () =>
      import('./features/goals/pages/goals-page/goals-page.component')
        .then((module) => module.GoalsPageComponent),
    title: 'Objetivos | WorkWorth'
  },
  {
    path: 'preferences/currency',
    loadComponent: () =>
      import('./features/preferences/pages/currency-settings/currency-settings.component')
        .then((module) => module.CurrencySettingsComponent),
    title: 'Ajustes | WorkWorth'
  },
  { path: '**', redirectTo: '' }
];
