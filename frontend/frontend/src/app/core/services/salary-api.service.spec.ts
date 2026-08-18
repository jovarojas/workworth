import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { SalaryApiService } from './salary-api.service';

describe('SalaryApiService', () => {
  let service: SalaryApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(SalaryApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uses the configured API URL and public salary contracts', () => {
    service.current().subscribe();
    service.create({
      effectiveFrom: '2026-08-01', netMonthlyReal: 1250, currencyCode: 'EUR', payPeriods: 12
    }).subscribe();
    service.rate('2026-08').subscribe();
    service.estimatorStatus().subscribe();

    const current = http.expectOne('http://api.test/api/v1/salary-profiles/current');
    expect(current.request.method).toBe('GET');
    current.flush({ month: '2026-08', salaryProfile: profile() });

    const create = http.expectOne('http://api.test/api/v1/salary-profiles');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual({
      effectiveFrom: '2026-08-01', netMonthlyReal: 1250, currencyCode: 'EUR', payPeriods: 12
    });
    create.flush(profile());

    const rate = http.expectOne('http://api.test/api/v1/salary-rates/2026-08');
    expect(rate.request.method).toBe('GET');
    rate.flush(rateResponse());

    const estimator = http.expectOne('http://api.test/api/v1/salary-estimator/status');
    expect(estimator.request.method).toBe('GET');
    estimator.flush({ fiscalYear: 2026, status: 'NOT_IMPLEMENTED', requiredInputs: ['Fiscal estimator implementation'] });
  });

  it('sends optional current month and fiscal year parameters only when provided', () => {
    service.current('2026-09').subscribe();
    service.estimatorStatus(2026).subscribe();

    const current = http.expectOne('http://api.test/api/v1/salary-profiles/current?month=2026-09');
    current.flush({ month: '2026-09', salaryProfile: profile() });

    const estimator = http.expectOne('http://api.test/api/v1/salary-estimator/status?year=2026');
    estimator.flush({ fiscalYear: 2026, status: 'NOT_IMPLEMENTED', requiredInputs: [] });
  });

  function profile() {
    return {
      id: 1, effectiveFrom: '2026-08-01', grossAnnual: null, netMonthlyReal: 1250,
      netAnnualReal: 15000, currencyCode: 'EUR', payPeriods: 12,
      activeIncomeSource: 'NET_MONTHLY_REAL', estimatorStatus: 'NOT_IMPLEMENTED'
    };
  }

  function rateResponse() {
    return {
      month: '2026-08', incomeSource: 'NET_MONTHLY_REAL', monthlyNetIncome: 1250,
      standardEconomicHours: 160, hourlyNetRate: 7.8125, currencyCode: 'EUR'
    };
  }
});
