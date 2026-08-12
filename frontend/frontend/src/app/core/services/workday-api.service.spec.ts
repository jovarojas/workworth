import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../config/api.config';
import { WorkdayApiService } from './workday-api.service';

describe('WorkdayApiService', () => {
  let service: WorkdayApiService;
  let http: HttpTestingController;
  const apiBaseUrl = 'http://api.test/api/v1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: apiBaseUrl }
      ]
    });
    service = TestBed.inject(WorkdayApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the current workday', () => {
    service.current().subscribe();

    const request = http.expectOne(`${apiBaseUrl}/workdays/current`);
    expect(request.request.method).toBe('GET');
    request.flush(workdayResponse());
  });

  it('starts a meal break with the exact POST contract', () => {
    service.startMealBreak('2026-08-12').subscribe();

    const request = http.expectOne(`${apiBaseUrl}/workdays/2026-08-12/meal-breaks/start`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(openMealBreak());
  });

  it('ends a meal break with its backend identifier', () => {
    service.endMealBreak('2026-08-12', 42).subscribe();

    const request = http.expectOne(`${apiBaseUrl}/workdays/2026-08-12/meal-breaks/42/end`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({ ...openMealBreak(), endedAt: '2026-08-12T10:30:00Z' });
  });

  it('cancels a workday with the exact 204 POST contract', () => {
    service.cancel('2026-08-12').subscribe();

    const request = http.expectOne(`${apiBaseUrl}/workdays/2026-08-12/cancel`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('creates a partial absence with the exact persisted interval contract', () => {
    const absence = {
      startedAt: '2026-08-12T08:30:00.000Z',
      endedAt: '2026-08-12T09:15:00.000Z',
      reason: 'Cita médica'
    };
    service.createPartialAbsence('2026-08-12', absence).subscribe();

    const request = http.expectOne(`${apiBaseUrl}/workdays/2026-08-12/partial-absences`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(absence);
    request.flush({ id: 14, ...absence });
  });

  function workdayResponse() {
    return {
      id: 1,
      localDate: '2026-08-12',
      timeZone: 'Europe/Madrid',
      status: 'ACTIVE',
      scheduledStart: '08:00:00',
      scheduledEnd: '15:00:00',
      maximumEconomicSeconds: 25_200,
      economicSeconds: 0,
      mealBreaks: [],
      partialAbsences: []
    };
  }

  function openMealBreak() {
    return { id: 42, startedAt: '2026-08-12T10:00:00Z', endedAt: null, endedAutomatically: false };
  }
});
