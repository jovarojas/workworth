import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { take } from 'rxjs';

import { authConfiguration } from './auth.config';

@Injectable({ providedIn: 'root' })
export class WorkWorthAuthService {
  private readonly auth0 = inject(AuthService);
  private readonly authenticated = signal(false);
  readonly configured = authConfiguration.configured;
  readonly isAuthenticated = computed(() => this.authenticated());
  readonly isLoading$ = this.auth0.isLoading$;
  readonly isAuthenticated$ = this.auth0.isAuthenticated$;

  constructor() {
    this.auth0.isAuthenticated$.subscribe((authenticated) => this.authenticated.set(authenticated));
  }

  login(returnTo?: string): void {
    if (!this.configured) {
      return;
    }
    this.auth0.loginWithRedirect({ appState: { target: returnTo ?? '/' } }).pipe(take(1)).subscribe();
  }

  logout(): void {
    if (!this.configured) {
      return;
    }
    this.auth0.logout({ logoutParams: { returnTo: window.location.origin } }).pipe(take(1)).subscribe();
  }
}
