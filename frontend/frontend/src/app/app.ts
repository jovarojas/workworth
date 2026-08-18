import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';

import { WorkWorthAuthService } from './core/auth/workworth-auth.service';

@Component({
  selector: 'app-root',
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatTooltipModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  readonly auth = inject(WorkWorthAuthService);
  readonly navigation = [
    { path: '/', label: 'Inicio', icon: 'home', exact: true },
    { path: '/workday', label: 'Jornada', icon: 'schedule' },
    { path: '/earnings', label: 'Historial', icon: 'receipt_long' },
    { path: '/rewards', label: 'Recompensas', icon: 'redeem' },
    { path: '/goals', label: 'Objetivos', icon: 'flag' },
    { path: '/statistics', label: 'Estadísticas', icon: 'insights' },
    { path: '/salary', label: 'Salario', icon: 'payments' },
    { path: '/preferences/currency', label: 'Ajustes', icon: 'settings' }
  ];

  logout(menu?: MatSidenav): void {
    menu?.close();
    this.auth.logout();
  }
}
