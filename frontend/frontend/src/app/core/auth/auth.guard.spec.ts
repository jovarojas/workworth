import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';

import { authenticatedGuard } from './auth.guard';
import { WorkWorthAuthService } from './workworth-auth.service';

describe('authenticatedGuard', () => {
  const router = { createUrlTree: vi.fn(() => ({}) as UrlTree) };

  it('permits an authenticated user after Auth0 finishes loading', async () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: WorkWorthAuthService, useValue: authenticated(true) }
      ]
    });

    const result = await TestBed.runInInjectionContext(() => firstValueFrom(authenticatedGuard({} as never, { url: '/rewards' } as never) as never));

    expect(result).toBe(true);
  });

  it('redirects an unauthenticated user to the access screen while preserving the requested route', async () => {
    const auth = authenticated(false);
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: WorkWorthAuthService, useValue: auth }
      ]
    });

    const result = await TestBed.runInInjectionContext(() => firstValueFrom(authenticatedGuard({} as never, { url: '/goals' } as never) as never));

    expect(auth.login).not.toHaveBeenCalled();
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], { queryParams: { returnTo: '/goals' } });
    expect(result).toEqual({});
  });

  function authenticated(isAuthenticated: boolean) {
    return {
      configured: true,
      isLoading$: of(false),
      isAuthenticated: () => isAuthenticated,
      login: vi.fn()
    };
  }
});
