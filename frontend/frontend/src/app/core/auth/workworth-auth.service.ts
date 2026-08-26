import { Injectable, Injector, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { Capacitor } from '@capacitor/core';
import { BehaviorSubject, Observable, from, of, take } from 'rxjs';
import { map } from 'rxjs/operators';

import { authConfiguration } from './auth.config';
import { NativeAuth } from './native-auth.plugin';

@Injectable({ providedIn: 'root' })
export class WorkWorthAuthService {
  private readonly injector = inject(Injector);
  private readonly router = inject(Router);
  private readonly nativePlatform = Capacitor.isNativePlatform();
  private readonly auth0 = this.nativePlatform
    ? null
    : this.injector.get(AuthService, null);
  private readonly authenticated = signal(false);
  private readonly loading = signal(this.nativePlatform);
  private readonly nativeLoading = new BehaviorSubject(this.nativePlatform);
  readonly configured = this.nativePlatform
    ? authConfiguration.androidConfigured
    : authConfiguration.webConfigured;
  readonly isAuthenticated = computed(() => this.authenticated());
  readonly isLoading = computed(() => this.loading());
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
    this.auth0?.isLoading$.subscribe((loading) => this.loading.set(loading));
  }

  login(returnTo?: string): void {
    if (!this.configured) {
      return;
    }
    if (this.nativePlatform) {
      NativeAuth.login()
        .then(() => {
          this.authenticated.set(true);
          return this.router.navigateByUrl(this.validReturnTo(returnTo));
        })
        .catch(() => this.authenticated.set(false));
      return;
    }
    this.auth0?.loginWithRedirect({ appState: { target: returnTo ?? '/' } }).pipe(take(1)).subscribe();
  }

  logout(): void {
    this.authenticated.set(false);
    void this.router.navigateByUrl('/login');

    if (!this.configured) {
      return;
    }
    if (this.nativePlatform) {
      NativeAuth.logout()
        .catch(() => undefined);
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
      this.loading.set(false);
      this.nativeLoading.next(false);
      return;
    }
    NativeAuth.isAuthenticated()
      .then(({ authenticated }) => this.authenticated.set(authenticated))
      .catch(() => this.authenticated.set(false))
      .finally(() => {
        this.loading.set(false);
        this.nativeLoading.next(false);
      });
  }

  private validReturnTo(returnTo?: string): string {
    return returnTo?.startsWith('/') && !returnTo.startsWith('//') ? returnTo : '/';
  }
}
