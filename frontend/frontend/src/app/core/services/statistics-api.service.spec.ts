import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { StatisticsResponse } from '../models/workworth-api.models';
import { StatisticsApiService } from './statistics-api.service';

describe('StatisticsApiService', () => {
  let service: StatisticsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(StatisticsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('queries every supported granularity with the exact request parameters', () => {
    for (const granularity of ['DAY', 'WEEK', 'MONTH', 'YEAR'] as const) {
      service.statistics(granularity, '2026-01-01', '2026-01-31').subscribe();

      const request = http.expectOne(`http://api.test/api/v1/statistics?granularity=${granularity}&from=2026-01-01&to=2026-01-31`);
      expect(request.request.method).toBe('GET');
      request.flush(response(granularity));
    }
  });

  it('keeps an unbounded query free of date parameters', () => {
    service.statistics('MONTH').subscribe();

    const request = http.expectOne('http://api.test/api/v1/statistics?granularity=MONTH');
    expect(request.request.params.has('from')).toBe(false);
    expect(request.request.params.has('to')).toBe(false);
    request.flush(response('MONTH'));
  });

  it('preserves zero values and independently unavailable metrics without local reconstruction', () => {
    let received: StatisticsResponse = response('DAY', {
      workedHours: { status: 'AVAILABLE', value: 0 },
      averageHourlyEarnings: { status: 'UNAVAILABLE', amount: null, currencyCode: null },
      totalEarnings: { status: 'AVAILABLE', amount: 0, currencyCode: 'EUR' },
      completedGoals: { status: 'AVAILABLE', count: 0 }
    });
    service.statistics('DAY').subscribe(result => received = result);

    http.expectOne('http://api.test/api/v1/statistics?granularity=DAY').flush(received);

    expect(received.points[0]).toEqual({
      startDate: '2026-08-10',
      endDate: '2026-08-11',
      workedHours: { status: 'AVAILABLE', value: 0 },
      averageHourlyEarnings: { status: 'UNAVAILABLE', amount: null, currencyCode: null },
      totalEarnings: { status: 'AVAILABLE', amount: 0, currencyCode: 'EUR' },
      completedGoals: { status: 'AVAILABLE', count: 0 }
    });
  });

  it('preserves the currency supplied by the backend for EUR and USD values', () => {
    service.statistics('MONTH').subscribe(result => {
      expect(result.points[0].totalEarnings.currencyCode).toBe('EUR');
    });
    http.expectOne('http://api.test/api/v1/statistics?granularity=MONTH').flush(response('MONTH'));

    service.statistics('MONTH').subscribe(result => {
      expect(result.points[0].totalEarnings.currencyCode).toBe('USD');
    });
    http.expectOne('http://api.test/api/v1/statistics?granularity=MONTH').flush(response('MONTH', {
      totalEarnings: { status: 'AVAILABLE', amount: 20, currencyCode: 'USD' },
      averageHourlyEarnings: { status: 'AVAILABLE', amount: 10, currencyCode: 'USD' }
    }));
  });

  it('propagates a statistics ProblemDetail to the caller', () => {
    let receivedCode: string | undefined;
    service.statistics('DAY').subscribe({ error: error => receivedCode = error.error.code });

    http.expectOne('http://api.test/api/v1/statistics?granularity=DAY').flush(
      { code: 'VALIDATION_ERROR', detail: 'Request validation failed.' },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(receivedCode).toBe('VALIDATION_ERROR');
  });
});

function response(
  granularity: 'DAY' | 'WEEK' | 'MONTH' | 'YEAR',
  metrics: Partial<{
    workedHours: { status: 'AVAILABLE' | 'UNAVAILABLE'; value: number | null };
    averageHourlyEarnings: { status: 'AVAILABLE' | 'UNAVAILABLE'; amount: number | null; currencyCode: 'EUR' | 'USD' | null };
    totalEarnings: { status: 'AVAILABLE' | 'UNAVAILABLE'; amount: number | null; currencyCode: 'EUR' | 'USD' | null };
    completedGoals: { status: 'AVAILABLE' | 'UNAVAILABLE'; count: number | null };
  }> = {}
) {
  return {
    granularity,
    from: null,
    to: null,
    points: [{
      startDate: '2026-08-10',
      endDate: '2026-08-11',
      workedHours: { status: 'AVAILABLE' as const, value: 2 },
      averageHourlyEarnings: { status: 'AVAILABLE' as const, amount: 12.5, currencyCode: 'EUR' as const },
      totalEarnings: { status: 'AVAILABLE' as const, amount: 25, currencyCode: 'EUR' as const },
      completedGoals: { status: 'AVAILABLE' as const, count: 1 },
      ...metrics
    }]
  };
}
