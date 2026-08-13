import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { DashboardApiService } from '../../../../core/services/dashboard-api.service';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';
import { DashboardPageComponent } from './dashboard-page.component';

describe('DashboardPageComponent', () => {
  const earnings = {
    currentProjection: vi.fn(),
    period: vi.fn()
  };
  const workdays = { current: vi.fn() };
  const dashboard = { motivation: vi.fn() };

  beforeEach(async () => {
    earnings.currentProjection.mockReset();
    earnings.period.mockReset();
    workdays.current.mockReset();
    dashboard.motivation.mockReset();

    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [
        { provide: EarningsApiService, useValue: earnings },
        { provide: DashboardApiService, useValue: dashboard },
        { provide: WorkdayApiService, useValue: workdays },
        provideRouter([])
      ]
    }).compileComponents();
  });

  afterEach(() => vi.useRealTimers());

  it('shows API-provided earning, workday and all four period summaries literally', () => {
    mockAvailableDashboard();

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('12.50');
    expect(content).toContain('10.11');
    expect(content).toContain('50.00');
    expect(content).toContain('120.75');
    expect(content).toContain('3,456.78');
    expect(content).toContain('EUR');
    expect(content).toContain('En curso');
    expect(content).toContain('08:00 – 17:00');
    expect(earnings.period).toHaveBeenCalledWith('TODAY');
    expect(earnings.period).toHaveBeenCalledWith('WEEK');
    expect(earnings.period).toHaveBeenCalledWith('MONTH');
    expect(earnings.period).toHaveBeenCalledWith('ALL_TIME');
    expect(dashboard.motivation).toHaveBeenCalledTimes(1);
  });

  it('uses USD supplied by the backend for the projection and all period summaries', () => {
    mockAvailableDashboard({ currencyCode: 'USD' });
    earnings.period.mockImplementation((context: string) => of({
      ...period(context, periodAmount(context)), currencyCode: 'USD'
    }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('USD');
    expect(fixture.nativeElement.textContent).not.toContain('EUR');
  });

  it('does not invent EUR when an available response has no currency code', () => {
    mockAvailableDashboard({ currencyCode: null });
    earnings.period.mockImplementation((context: string) => of({
      ...period(context, periodAmount(context)), currencyCode: null
    }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No hay una moneda disponible para la ganancia de hoy.');
    expect(content).not.toContain('EUR');
    expect(content).not.toContain('€');
  });

  it('renders the EMPTY motivation state returned by the backend', () => {
    mockAvailableDashboard();
    dashboard.motivation.mockReturnValue(of({ state: 'EMPTY', primaryReward: null, combination: null }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Añade una recompensa');
  });

  it('renders an AVAILABLE motivation without selecting reward contexts locally', () => {
    mockAvailableDashboard();
    dashboard.motivation.mockReturnValue(of(motivation('AVAILABLE', {
      relevantContext: 'WEEK', outcome: 'AFFORDABLE', surplus: 0, shortfall: null
    })));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Ya puedes conseguir Auriculares');
    expect(content).toContain('esta semana');
    expect(fixture.nativeElement.querySelector('.motivation-combination')).toBeNull();
    expect(fixture.nativeElement.querySelector('a[href="/rewards"]')).not.toBeNull();
  });

  it('renders the backend-provided PROGRESS shortfall without calculating it', () => {
    mockAvailableDashboard();
    dashboard.motivation.mockReturnValue(of(motivation('PROGRESS', {
      relevantContext: null, progressContext: 'TODAY', outcome: 'SHORTFALL', surplus: null, shortfall: 35
    })));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Auriculares está cada vez más cerca');
    expect(content).toContain('35.00');
    expect(content).toContain('hoy');
  });

  it('renders the UNAVAILABLE motivation state without inventing an amount', () => {
    mockAvailableDashboard();
    dashboard.motivation.mockReturnValue(of({ state: 'UNAVAILABLE', primaryReward: null, combination: null }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('Ahora mismo no podemos evaluar tus recompensas');
    expect(content).not.toContain('€0.00');
  });

  it('renders the optional backend-provided combination without summing or grouping rewards', () => {
    mockAvailableDashboard();
    dashboard.motivation.mockReturnValue(of({
      ...motivation('AVAILABLE', { relevantContext: 'MONTH', outcome: 'AFFORDABLE', surplus: 5, shortfall: null }),
      combination: {
        context: 'MONTH', availableAmount: 95, totalPrice: 90, currencyCode: 'USD',
        rewards: [
          { id: 7, name: 'Hamburguesas', quantity: 2, price: 30, currencyCode: 'USD', status: 'PENDING' },
          { id: 8, name: 'Funkos de Shakira', quantity: 2, price: 60, currencyCode: 'USD', status: 'PENDING' }
        ]
      }
    }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('2 Hamburguesas');
    expect(content).toContain('2 Funkos de Shakira');
    expect(content).toContain('Total:');
    expect(content).toContain('Disponible:');
    expect(content).toContain('USD');
  });

  it('keeps earning and workday data visible when motivation fails', () => {
    mockAvailableDashboard();
    dashboard.motivation.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 500,
      error: { detail: 'No se pudo resolver la motivación.' }
    })));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No se ha podido cargar la motivación. Inténtalo de nuevo más tarde.');
    expect(content).toContain('12.50');
    expect(content).toContain('En curso');
    expect(content).toContain('50.00');
  });

  it('shows an explicit unavailable projection state without inventing an amount', () => {
    mockAvailableDashboard({
      localDate: '2026-08-11', status: 'UNAVAILABLE', economicSeconds: 0,
      amount: null, currencyCode: null, unavailableReason: 'SALARY_RATE_UNAVAILABLE'
    });

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No disponible');
    expect(content).toContain('No se puede calcular la tarifa salarial.');
    expect(content).not.toContain('0,00');
  });

  it('shows an unavailable period without passing a null amount to the currency formatter', () => {
    mockAvailableDashboard();
    earnings.period.mockImplementation((context: string) => of({
      ...period(context, periodAmount(context)),
      status: context === 'MONTH' ? 'UNAVAILABLE' : 'AVAILABLE',
      amount: context === 'MONTH' ? null : periodAmount(context),
      currencyCode: context === 'MONTH' ? null : 'EUR'
    }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No hay un importe evaluable para este mes');
    expect(content).toContain('10.11');
    expect(content).toContain('3,456.78');
  });

  it('keeps earnings visible when there is no current workday', () => {
    mockAvailableDashboard();
    workdays.current.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No hay jornada para hoy');
    expect(content).toContain('12.50');
    expect(content).toContain('50.00');
  });

  it('keeps the monthly summary visible when the weekly endpoint fails', () => {
    mockAvailableDashboard();
    earnings.period.mockImplementation((context: string) => context === 'WEEK'
      ? throwError(() => new HttpErrorResponse({ status: 500 }))
      : of(period('MONTH', 120.75)));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No se ha podido cargar el resumen semanal');
    expect(content).toContain('120.75');
  });

  it('keeps the other periods visible when TODAY fails', () => {
    mockAvailableDashboard();
    earnings.period.mockImplementation((context: string) => context === 'TODAY'
      ? throwError(() => new HttpErrorResponse({ status: 500 }))
      : of(period(context, periodAmount(context))));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No se ha podido cargar el resumen de hoy');
    expect(content).toContain('50.00');
    expect(content).toContain('120.75');
    expect(content).toContain('3,456.78');
  });

  it('keeps the other periods visible when ALL_TIME fails', () => {
    mockAvailableDashboard();
    earnings.period.mockImplementation((context: string) => context === 'ALL_TIME'
      ? throwError(() => new HttpErrorResponse({ status: 500 }))
      : of(period(context, periodAmount(context))));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No se ha podido cargar el acumulado histórico');
    expect(content).toContain('10.11');
    expect(content).toContain('50.00');
    expect(content).toContain('120.75');
  });

  it('shows independent errors when TODAY and ALL_TIME are unavailable', () => {
    mockAvailableDashboard();
    earnings.period.mockImplementation((context: string) => ['TODAY', 'ALL_TIME'].includes(context)
      ? throwError(() => new HttpErrorResponse({ status: 0 }))
      : of(period(context, periodAmount(context))));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('No se puede conectar con WorkWorth para cargar el resumen de hoy');
    expect(content).toContain('No se puede conectar con WorkWorth para cargar el acumulado histórico');
    expect(content).toContain('50.00');
    expect(content).toContain('120.75');
  });

  it('shows independent connection errors when the backend is unavailable', () => {
    const unavailable = () => throwError(() => new HttpErrorResponse({ status: 0 }));
    earnings.currentProjection.mockImplementation(unavailable);
    earnings.period.mockImplementation(unavailable);
    workdays.current.mockImplementation(unavailable);
    dashboard.motivation.mockImplementation(unavailable);

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('polls all dashboard data after sixty seconds while the workday is active', () => {
    vi.useFakeTimers();
    mockAvailableDashboard();

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    vi.advanceTimersByTime(60_000);

    expect(earnings.currentProjection).toHaveBeenCalledTimes(2);
    expect(workdays.current).toHaveBeenCalledTimes(2);
  });

  it('stops polling when the refreshed workday is no longer active', () => {
    vi.useFakeTimers();
    mockAvailableDashboard();
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('COMPLETED')));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();
    vi.advanceTimersByTime(60_000);
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(2);
    expect(earnings.currentProjection).toHaveBeenCalledTimes(2);
  });

  function mockAvailableDashboard(projectionOverride?: object): void {
    earnings.currentProjection.mockReturnValue(of({
      localDate: '2026-08-11', status: 'AVAILABLE', economicSeconds: 3600,
      amount: 12.5, currencyCode: 'EUR', unavailableReason: null,
      ...projectionOverride
    }));
    earnings.period.mockImplementation((context: string) => of(period(context, periodAmount(context))));
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    dashboard.motivation.mockReturnValue(of({ state: 'EMPTY', primaryReward: null, combination: null }));
  }

  function motivation(state: 'AVAILABLE' | 'PROGRESS', overrides: object) {
    return {
      state,
      primaryReward: {
        reward: { id: 4, name: 'Auriculares', quantity: 1, price: 120, currencyCode: 'EUR', status: 'PENDING' },
        evaluable: true,
        relevantContext: 'WEEK',
        progressContext: null,
        outcome: 'AFFORDABLE',
        availableAmount: 120,
        surplus: 0,
        shortfall: null,
        ...overrides
      },
      combination: null
    };
  }

  function period(context: string, amount: number) {
    return {
      context,
      startDate: '2026-08-10',
      endDate: '2026-08-17',
      status: 'AVAILABLE',
      amount,
      currencyCode: 'EUR'
    };
  }

  function periodAmount(context: string): number {
    return {
      TODAY: 10.11,
      WEEK: 50,
      MONTH: 120.75,
      ALL_TIME: 3456.78
    }[context] ?? 0;
  }

  function workday(status: string) {
    return {
      id: 1,
      localDate: '2026-08-11',
      timeZone: 'Europe/Madrid',
      status,
      scheduledStart: '08:00',
      scheduledEnd: '17:00',
      maximumEconomicSeconds: 28800,
      economicSeconds: 3600
    };
  }
});
