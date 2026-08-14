import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';

import { WorkWorthAuthService } from './workworth-auth.service';

export const authenticatedGuard: CanActivateFn = (_route, state) => {
  const auth = inject(WorkWorthAuthService);
  const router = inject(Router);
  if (!auth.configured) {
    return router.createUrlTree(['/login']);
  }
  return auth.isLoading$.pipe(
    filter((loading) => !loading),
    take(1),
    map(() => {
      if (auth.isAuthenticated()) {
        return true;
      }
      auth.login(state.url);
      return router.createUrlTree(['/login']);
    })
  );
};
