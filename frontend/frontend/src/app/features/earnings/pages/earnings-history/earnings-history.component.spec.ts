import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { EarningsHistoryComponent } from './earnings-history.component';

describe('EarningsHistoryComponent', () => {
  const earnings = { history: vi.fn() };

  beforeEach(async () => {
    earnings.history.mockReset();
    await TestBed.configureTestingModule({
      imports: [EarningsHistoryComponent],
      providers: [provideRouter([]), { provide: EarningsApiService, useValue: earnings }]
    }).compileComponents();
  });

  it('renders available and unavailable earnings supplied by the API', () => {
    earnings.history.mockReturnValue(of(history({
      items: [available(), unavailable()]
    })));

    const fixture = TestBed.createComponent(EarningsHistoryComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('2026-08-12');
    expect(content).toContain('12.50');
    expect(content).toContain('EUR');
    expect(content).toContain('No disponible');
    expect(content).toContain('No se puede calcular la tarifa salarial.');
    expect(fixture.nativeElement.querySelector('a[aria-label="Ver detalle de la jornada 2026-08-12"]')?.getAttribute('href'))
      .toBe('/earnings/workdays/2026-08-12');
  });

  it('shows an empty state when the backend returns no earnings', () => {
    earnings.history.mockReturnValue(of(history({ items: [], totalElements: 0, totalPages: 0 })));

    const fixture = TestBed.createComponent(EarningsHistoryComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no hay ganancias registradas.');
  });

  it('uses only hasNext and hasPrevious to paginate', () => {
    earnings.history
      .mockReturnValueOnce(of(history({ page: 1, hasNext: true, hasPrevious: true })))
      .mockReturnValueOnce(of(history({ page: 2, hasNext: false, hasPrevious: true })));

    const fixture = TestBed.createComponent(EarningsHistoryComponent);
    fixture.detectChanges();
    fixture.componentInstance.nextPage();

    expect(earnings.history).toHaveBeenNthCalledWith(1, 0, 20);
    expect(earnings.history).toHaveBeenNthCalledWith(2, 2, 20);
  });

  it('loads the previous page only when hasPrevious is true', () => {
    earnings.history
      .mockReturnValueOnce(of(history({ page: 2, hasNext: true, hasPrevious: true })))
      .mockReturnValueOnce(of(history({ page: 1, hasNext: true, hasPrevious: true })));

    const fixture = TestBed.createComponent(EarningsHistoryComponent);
    fixture.detectChanges();
    fixture.componentInstance.previousPage();

    expect(earnings.history).toHaveBeenNthCalledWith(2, 1, 20);
  });

  it('shows the backend validation detail for a pagination error', () => {
    earnings.history.mockReturnValue(throwError(() => problem(400, 'VALIDATION_ERROR', 'Invalid pagination parameters.')));

    const fixture = TestBed.createComponent(EarningsHistoryComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revisa los datos introducidos.');
  });

  it('shows a connection error without silently hiding it', () => {
    earnings.history.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(EarningsHistoryComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  function available() {
    return { localDate: '2026-08-12', status: 'AVAILABLE' as const, unavailableReason: null, amount: 12.5, currencyCode: 'EUR', economicSeconds: 3600 };
  }

  function unavailable() {
    return { localDate: '2026-08-11', status: 'UNAVAILABLE' as const, unavailableReason: 'SALARY_RATE_UNAVAILABLE', amount: null, currencyCode: null, economicSeconds: 0 };
  }

  function history(overrides: object = {}) {
    return { items: [available()], page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false, ...overrides };
  }

  function problem(status: number, code: string, detail: string): HttpErrorResponse {
    return new HttpErrorResponse({ status, error: { code, detail } });
  }
});
