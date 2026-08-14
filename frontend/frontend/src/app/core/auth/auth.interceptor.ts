import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '@auth0/auth0-angular';
import { catchError, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { authConfiguration } from './auth.config';
import { WorkWorthAuthService } from './workworth-auth.service';

export const authenticationInterceptor: HttpInterceptorFn = (request, next) => {
  if (!authConfiguration.configured || !request.url.startsWith(environment.apiBaseUrl)) {
    return next(request);
  }

  const auth0 = inject(AuthService);
  const auth = inject(WorkWorthAuthService);
  return auth0.getAccessTokenSilently().pipe(
    switchMap((accessToken) => next(request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } }))),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.login();
      }
      return throwError(() => error);
    })
  );
};
