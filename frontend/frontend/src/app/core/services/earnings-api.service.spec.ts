import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../config/api.config';
import { EarningsApiService } from './earnings-api.service';

describe('EarningsApiService', () => {
  let service: EarningsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(EarningsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the current projection from the configured API base URL', () => {
    service.currentProjection().subscribe();

    const request = http.expectOne('http://api.test/api/v1/earnings/current/projection');
    expect(request.request.method).toBe('GET');
    request.flush({
      localDate: '2026-08-11', status: 'AVAILABLE', economicSeconds: 0,
      amount: 0, currencyCode: 'EUR', unavailableReason: null
    });
  });
});
