import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { EarningDetailComponent } from './earning-detail.component';

describe('EarningDetailComponent', () => {
  const earnings = { workday: vi.fn(), corrections: vi.fn() };

  beforeEach(async () => {
    earnings.workday.mockReset();
    earnings.corrections.mockReset();
    await TestBed.configureTestingModule({
      imports: [EarningDetailComponent],
      providers: [
        provideRouter([]),
        { provide: EarningsApiService, useValue: earnings },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ date: '2026-08-12' }) } } }
      ]
    }).compileComponents();
  });

  it('renders the effective earning and corrections exactly as received', () => {
    earnings.workday.mockReturnValue(of(available()));
    earnings.corrections.mockReturnValue(of(corrections()));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('12.50');
    expect(content).toContain('EUR');
    expect(content).toContain('3600 segundos');
    expect(content).toContain('Jornada cancelada');
    expect(content).toContain('Ausencia parcial modificada');
    expect(content).toContain('Pausa modificada');
    expect(earnings.workday).toHaveBeenCalledWith('2026-08-12');
    expect(earnings.corrections).toHaveBeenCalledWith('2026-08-12');
  });

  it('renders correction amounts with the EUR currency received for the earning', () => {
    earnings.workday.mockReturnValue(of(available()));
    earnings.corrections.mockReturnValue(of(corrections()));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('EUR');
  });

  it('renders correction amounts with the USD currency received for the earning', () => {
    earnings.workday.mockReturnValue(of({ ...available(), currencyCode: 'USD' }));
    earnings.corrections.mockReturnValue(of(corrections()));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('USD');
    expect(content).not.toContain('EUR');
  });

  it('does not invent a currency for correction amounts when the earning currency is missing', () => {
    earnings.workday.mockReturnValue(of({ ...available(), currencyCode: null }));
    earnings.corrections.mockReturnValue(of(corrections()));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No disponible');
    expect(content).not.toContain('EUR');
  });

  it('shows an unavailable earning without inventing an amount', () => {
    earnings.workday.mockReturnValue(of({ ...available(), status: 'UNAVAILABLE', amount: null, currencyCode: null, unavailableReason: 'SALARY_RATE_UNAVAILABLE' }));
    earnings.corrections.mockReturnValue(of([]));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se puede calcular la tarifa salarial.');
    expect(fixture.nativeElement.textContent).toContain('No disponible');
  });

  it('shows the explicit missing earning error for a 404', () => {
    earnings.workday.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No existe una ganancia materializada para esta jornada.');
    expect(earnings.corrections).not.toHaveBeenCalled();
  });

  it('shows a connection error for the earning request', () => {
    earnings.workday.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('shows the empty corrections state', () => {
    earnings.workday.mockReturnValue(of(available()));
    earnings.corrections.mockReturnValue(of([]));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no hay correcciones para esta jornada.');
  });

  it('keeps the effective earning visible when corrections fail independently', () => {
    earnings.workday.mockReturnValue(of(available()));
    earnings.corrections.mockReturnValue(throwError(() => problem(500, 'INTERNAL_ERROR', 'Corrections are unavailable.')));

    const fixture = TestBed.createComponent(EarningDetailComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('12.50');
    expect(content).toContain('No se ha podido completar la operación.');
  });

  function available() {
    return { localDate: '2026-08-12', status: 'AVAILABLE' as const, unavailableReason: null, amount: 12.5, currencyCode: 'EUR', economicSeconds: 3600 };
  }

  function corrections() {
    return [
      { sequence: 3, cause: 'WORKDAY_CANCELLED' as const, previousEconomicSeconds: 3600, newEconomicSeconds: 0, previousAmount: 12.5, newAmount: 0, correctedAt: '2026-08-12T17:00:00Z' },
      { sequence: 2, cause: 'PARTIAL_ABSENCE_CHANGED' as const, previousEconomicSeconds: 4800, newEconomicSeconds: 3600, previousAmount: 16.67, newAmount: 12.5, correctedAt: '2026-08-12T16:00:00Z' },
      { sequence: 1, cause: 'MEAL_BREAK_CHANGED' as const, previousEconomicSeconds: 5400, newEconomicSeconds: 4800, previousAmount: 18.75, newAmount: 16.67, correctedAt: '2026-08-12T15:00:00Z' }
    ];
  }

  function problem(status: number, code: string, detail: string): HttpErrorResponse {
    return new HttpErrorResponse({ status, error: { code, detail } });
  }
});
