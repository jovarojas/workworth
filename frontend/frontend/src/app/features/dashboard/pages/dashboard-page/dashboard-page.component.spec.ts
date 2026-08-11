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

  it('shows the API-provided earning, workday, week and month data', () => {
    mockAvailableDashboard();

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('12.50');
    expect(content).toContain('50.00');
    expect(content).toContain('120.75');
    expect(content).toContain('En curso');
    expect(content).toContain('08:00 – 17:00');
    expect(earnings.period).toHaveBeenCalledWith('WEEK');
    expect(earnings.period).toHaveBeenCalledWith('MONTH');
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
    earnings.period.mockImplementation((context: string) => of(period(context, context === 'WEEK' ? 50 : 120.75)));
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
  }

  function period(context: string, amount: number) {
    return {
      context,
      startDate: '2026-08-10',
      endDate: '2026-08-17',
      amount,
      currencyCode: 'EUR'
    };
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
