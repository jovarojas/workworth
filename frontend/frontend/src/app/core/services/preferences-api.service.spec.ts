import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { PreferencesApiService } from './preferences-api.service';

describe('PreferencesApiService', () => {
  let service: PreferencesApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(PreferencesApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gets the global application currency', () => {
    service.currency().subscribe();

    const request = http.expectOne('http://api.test/api/v1/application-settings/currency');
    expect(request.request.method).toBe('GET');
    request.flush({ currencyCode: 'EUR', changeAllowed: true });
  });

  it('updates the global application currency', () => {
    service.updateCurrency({ currencyCode: 'USD' }).subscribe();

    const request = http.expectOne('http://api.test/api/v1/application-settings/currency');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ currencyCode: 'USD' });
    request.flush({ currencyCode: 'USD', changeAllowed: true });
  });
});
