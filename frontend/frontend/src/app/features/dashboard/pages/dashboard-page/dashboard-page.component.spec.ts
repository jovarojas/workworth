import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';
import { DashboardPageComponent } from './dashboard-page.component';

describe('DashboardPageComponent', () => {
  const earnings = {
    currentProjection: vi.fn(),
    period: vi.fn()
  };
  const workdays = { current: vi.fn() };

  beforeEach(async () => {
    earnings.currentProjection.mockReset();
    earnings.period.mockReset();
    workdays.current.mockReset();

    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [
        { provide: EarningsApiService, useValue: earnings },
        { provide: WorkdayApiService, useValue: workdays }
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
    expect(content).toContain('En curso');
    expect(content).toContain('08:00 – 17:00');
    expect(earnings.period).toHaveBeenCalledWith('TODAY');
    expect(earnings.period).toHaveBeenCalledWith('WEEK');
    expect(earnings.period).toHaveBeenCalledWith('MONTH');
    expect(earnings.period).toHaveBeenCalledWith('ALL_TIME');
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
    expect(content).toContain('SALARY_RATE_UNAVAILABLE');
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
