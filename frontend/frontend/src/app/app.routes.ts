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
  { path: '**', redirectTo: '' }
];
