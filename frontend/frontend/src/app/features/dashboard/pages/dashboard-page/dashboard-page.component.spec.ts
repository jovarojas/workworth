import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DashboardPageComponent } from './dashboard-page.component';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';

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

  it('loads the current workday and earnings summary', () => {
    earnings.currentProjection.mockReturnValue(of({
      localDate: '2026-08-11', status: 'AVAILABLE', economicSeconds: 3600,
      amount: 12.5, currencyCode: 'EUR', unavailableReason: null
    }));
    earnings.period.mockImplementation((context: string) => of({
      context, startDate: '2026-08-10', endDate: '2026-08-17', amount: 50, currencyCode: 'EUR'
    }));
    workdays.current.mockReturnValue(of({
      id: 1, localDate: '2026-08-11', timeZone: 'Europe/Madrid', status: 'ACTIVE',
      scheduledStart: '08:00', scheduledEnd: '17:00', maximumEconomicSeconds: 28800, economicSeconds: 3600
    }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('GANANCIA ACTUAL DE HOY');
    expect(fixture.nativeElement.textContent).toContain('En curso');
    expect(earnings.period).toHaveBeenCalledWith('WEEK');
    expect(earnings.period).toHaveBeenCalledWith('MONTH');
  });

  it('shows an explicit unavailable state without an amount', () => {
    earnings.currentProjection.mockReturnValue(of({
      localDate: '2026-08-11', status: 'UNAVAILABLE', economicSeconds: 0,
      amount: null, currencyCode: null, unavailableReason: 'SALARY_RATE_UNAVAILABLE'
    }));
    earnings.period.mockImplementation((context: string) => of({
      context, startDate: null, endDate: null, amount: 0, currencyCode: 'EUR'
    }));
    workdays.current.mockReturnValue(of({
      id: 1, localDate: '2026-08-11', timeZone: 'Europe/Madrid', status: 'SCHEDULED',
      scheduledStart: '08:00', scheduledEnd: '17:00', maximumEconomicSeconds: 28800, economicSeconds: 0
    }));

    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No disponible');
    expect(fixture.nativeElement.textContent).not.toContain('0,00 €');
  });
});
