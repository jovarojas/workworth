import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { GoalsApiService } from './goals-api.service';

describe('GoalsApiService', () => {
  let service: GoalsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(GoalsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads active goals and closed history from their exact endpoints', () => {
    service.active().subscribe();
    service.history().subscribe();

    const active = http.expectOne('http://api.test/api/v1/goals');
    expect(active.request.method).toBe('GET');
    active.flush([]);
    const history = http.expectOne('http://api.test/api/v1/goals/history');
    expect(history.request.method).toBe('GET');
    history.flush([]);
  });

  it('creates and updates goals without a locally selected currency', () => {
    const body = { title: 'Viaje', targetAmount: 500 };
    service.create(body).subscribe();
    const create = http.expectOne('http://api.test/api/v1/goals');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    expect(create.request.body.currencyCode).toBeUndefined();
    create.flush(goal());

    service.update(4, body).subscribe();
    const update = http.expectOne('http://api.test/api/v1/goals/4');
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(body);
    update.flush(goal());
  });

  it('completes and cancels through the backend lifecycle endpoints', () => {
    service.complete(4).subscribe();
    const complete = http.expectOne('http://api.test/api/v1/goals/4/complete');
    expect(complete.request.method).toBe('POST');
    expect(complete.request.body).toBeNull();
    complete.flush({ ...goal(), status: 'COMPLETED', progress: null });

    service.cancel(4).subscribe();
    const cancel = http.expectOne('http://api.test/api/v1/goals/4/cancel');
    expect(cancel.request.method).toBe('POST');
    expect(cancel.request.body).toBeNull();
    cancel.flush({ ...goal(), status: 'CANCELLED', progress: null });
  });

  it('propagates API errors to the caller', () => {
    let receivedStatus: number | undefined;
    service.complete(4).subscribe({ error: (error) => receivedStatus = error.status });

    const request = http.expectOne('http://api.test/api/v1/goals/4/complete');
    request.flush({ code: 'GOAL_CONFLICT' }, { status: 409, statusText: 'Conflict' });

    expect(receivedStatus).toBe(409);
  });
});

function goal() {
  return {
    id: 4, title: 'Viaje', targetAmount: 500, currencyCode: 'EUR', status: 'ACTIVE',
    createdAt: '2026-08-13T10:00:00Z', updatedAt: '2026-08-13T10:00:00Z', closedAt: null,
    progress: { evaluable: true, progressAmount: 100, remainingAmount: 400, progressPercentage: 20, reached: false }
  };
}
