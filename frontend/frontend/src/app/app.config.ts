import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { AuthService, provideAuth0 } from '@auth0/auth0-angular';
import { Capacitor } from '@capacitor/core';

import { authenticationInterceptor } from './core/auth/auth.interceptor';
import { authConfiguration } from './core/auth/auth.config';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authenticationInterceptor])),
    provideRouter(routes),
    ...(Capacitor.isNativePlatform()
      ? [{ provide: AuthService, useValue: null }]
      : [provideAuth0({
        domain: authConfiguration.domain,
        clientId: authConfiguration.webClientId,
        authorizationParams: {
          audience: authConfiguration.audience || undefined,
          redirect_uri: window.location.origin
        },
        useRefreshTokens: true
      })]),
    { provide: LOCALE_ID, useValue: 'es-ES' }
  ]
};
