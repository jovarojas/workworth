import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '@auth0/auth0-angular';
import { of } from 'rxjs';

import { authenticationInterceptor } from './auth.interceptor';
import { authConfiguration } from './auth.config';
import { WorkWorthAuthService } from './workworth-auth.service';

describe('authenticationInterceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  const originalConfigured = authConfiguration.configured;

  beforeEach(() => {
    authConfiguration.configured = true;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authenticationInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getAccessTokenSilently: () => of('access-token') } },
        { provide: WorkWorthAuthService, useValue: { login: vi.fn() } }
      ]
    });
    http = TestBed.inject(HttpClient);
    requests = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    requests.verify();
    authConfiguration.configured = originalConfigured;
  });

  it('adds the Auth0 access token only to the configured API', () => {
    http.get('http://localhost:8081/api/v1/rewards').subscribe();

    const request = requests.expectOne('http://localhost:8081/api/v1/rewards');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush([]);
  });

  it('does not attach credentials to a third-party URL', () => {
    http.get('https://example.test/public').subscribe();

    const request = requests.expectOne('https://example.test/public');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
