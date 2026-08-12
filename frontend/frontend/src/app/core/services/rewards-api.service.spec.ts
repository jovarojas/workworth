import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { RewardsApiService } from './rewards-api.service';

describe('RewardsApiService', () => {
  let service: RewardsApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: 'http://api.test/api/v1' }
      ]
    });
    service = TestBed.inject(RewardsApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it.each(['PENDING', 'ACQUIRED'] as const)('lists %s rewards with the exact status parameter', (status) => {
    service.list(status).subscribe();

    const request = http.expectOne(`http://api.test/api/v1/rewards?status=${status}`);
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('gets a reward by id', () => {
    service.get(4).subscribe();

    const request = http.expectOne('http://api.test/api/v1/rewards/4');
    expect(request.request.method).toBe('GET');
    request.flush(reward());
  });

  it('creates a reward without a currency code', () => {
    const requestBody = { name: 'Auriculares', quantity: 1, price: 120 };
    service.create(requestBody).subscribe();

    const request = http.expectOne('http://api.test/api/v1/rewards');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(requestBody);
    expect(request.request.body.currencyCode).toBeUndefined();
    request.flush(reward());
  });

  it('updates a reward with its exact request body', () => {
    const requestBody = { name: 'Dos auriculares', quantity: 2, price: 180 };
    service.update(4, requestBody).subscribe();

    const request = http.expectOne('http://api.test/api/v1/rewards/4');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(requestBody);
    request.flush({ ...reward(), ...requestBody });
  });

  it('deletes a reward', () => {
    service.delete(4).subscribe();

    const request = http.expectOne('http://api.test/api/v1/rewards/4');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('marks a reward as acquired with an empty request body', () => {
    service.acquire(4).subscribe();

    const request = http.expectOne('http://api.test/api/v1/rewards/4/acquire');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush({ ...reward(), status: 'ACQUIRED' });
  });

  it('propagates HTTP errors to callers', () => {
    let receivedStatus: number | undefined;
    service.list('PENDING').subscribe({ error: (error) => receivedStatus = error.status });

    const request = http.expectOne('http://api.test/api/v1/rewards?status=PENDING');
    request.flush({ code: 'RESOURCE_NOT_FOUND' }, { status: 404, statusText: 'Not Found' });

    expect(receivedStatus).toBe(404);
  });

  function reward() {
    return {
      id: 4, name: 'Auriculares', quantity: 1, price: 120, currencyCode: 'EUR', status: 'PENDING',
      lastReachedContext: null, createdAt: '2026-08-12T10:00:00Z', updatedAt: '2026-08-12T10:00:00Z'
    };
  }
});
