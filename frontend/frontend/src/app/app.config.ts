import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAuth0 } from '@auth0/auth0-angular';

import { authenticationInterceptor } from './core/auth/auth.interceptor';
import { authConfiguration } from './core/auth/auth.config';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authenticationInterceptor])),
    provideRouter(routes),
    provideAuth0({
      domain: authConfiguration.domain,
      clientId: authConfiguration.clientId,
      authorizationParams: {
        audience: authConfiguration.audience || undefined,
        redirect_uri: window.location.origin
      },
      useRefreshTokens: true
    }),
    { provide: LOCALE_ID, useValue: 'es-ES' }
  ]
};
