import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { Capacitor } from '@capacitor/core';
import { BehaviorSubject, Observable, from, of, take } from 'rxjs';
import { map } from 'rxjs/operators';

import { authConfiguration } from './auth.config';
import { NativeAuth } from './native-auth.plugin';

@Injectable({ providedIn: 'root' })
export class WorkWorthAuthService {
  private readonly auth0 = inject(AuthService, { optional: true });
  private readonly nativePlatform = Capacitor.isNativePlatform();
  private readonly authenticated = signal(false);
  private readonly nativeLoading = new BehaviorSubject(this.nativePlatform);
  readonly configured = this.nativePlatform
    ? authConfiguration.androidConfigured
    : authConfiguration.webConfigured;
  readonly isAuthenticated = computed(() => this.authenticated());
  readonly isLoading$ = this.nativePlatform
    ? this.nativeLoading.asObservable()
    : (this.auth0?.isLoading$ ?? of(false));
  readonly isAuthenticated$ = this.nativePlatform
    ? this.nativeLoading.pipe(map(() => this.authenticated()))
    : (this.auth0?.isAuthenticated$ ?? of(false));

  constructor() {
    if (this.nativePlatform) {
      this.restoreNativeSession();
      return;
    }
    this.auth0?.isAuthenticated$.subscribe((authenticated) => this.authenticated.set(authenticated));
  }

  login(returnTo?: string): void {
    if (!this.configured) {
      return;
    }
    if (this.nativePlatform) {
      NativeAuth.login()
        .then(() => this.authenticated.set(true))
        .catch(() => this.authenticated.set(false));
      return;
    }
    this.auth0?.loginWithRedirect({ appState: { target: returnTo ?? '/' } }).pipe(take(1)).subscribe();
  }

  logout(): void {
    if (!this.configured) {
      return;
    }
    if (this.nativePlatform) {
      NativeAuth.logout()
        .finally(() => this.authenticated.set(false));
      return;
    }
    this.auth0?.logout({ logoutParams: { returnTo: window.location.origin } }).pipe(take(1)).subscribe();
  }

  accessToken$(): Observable<string> {
    if (this.nativePlatform) {
      return from(NativeAuth.getAccessToken()).pipe(map((credentials) => credentials.accessToken));
    }
    return this.auth0?.getAccessTokenSilently() ?? of('');
  }

  private restoreNativeSession(): void {
    if (!this.configured) {
      this.nativeLoading.next(false);
      return;
    }
    NativeAuth.isAuthenticated()
      .then(({ authenticated }) => this.authenticated.set(authenticated))
      .catch(() => this.authenticated.set(false))
      .finally(() => this.nativeLoading.next(false));
  }
}
