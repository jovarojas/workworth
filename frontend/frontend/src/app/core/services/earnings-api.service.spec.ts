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

  it.each(['TODAY', 'ALL_TIME'] as const)('requests the %s earning period from the configured API URL', (context) => {
    service.period(context).subscribe();

    const request = http.expectOne(`http://api.test/api/v1/earnings/periods/${context}`);
    expect(request.request.method).toBe('GET');
    request.flush({
      context,
      startDate: context === 'ALL_TIME' ? null : '2026-08-12',
      endDate: context === 'ALL_TIME' ? null : '2026-08-12',
      amount: 123.45,
      currencyCode: 'EUR'
    });
  });

  it('requests paginated earning history with the supplied query parameters', () => {
    service.history(2, 10).subscribe();

    const request = http.expectOne('http://api.test/api/v1/earnings/history?page=2&size=10');
    expect(request.request.method).toBe('GET');
    request.flush({ items: [], page: 2, size: 10, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: true });
  });

  it('requests the effective earning for a workday date', () => {
    service.workday('2026-08-12').subscribe();

    const request = http.expectOne('http://api.test/api/v1/earnings/workdays/2026-08-12');
    expect(request.request.method).toBe('GET');
    request.flush({
      localDate: '2026-08-12', status: 'AVAILABLE', unavailableReason: null,
      amount: 12.5, currencyCode: 'EUR', economicSeconds: 3600
    });
  });

  it('requests the audit corrections for a workday date', () => {
    service.corrections('2026-08-12').subscribe();

    const request = http.expectOne('http://api.test/api/v1/earnings/workdays/2026-08-12/corrections');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
