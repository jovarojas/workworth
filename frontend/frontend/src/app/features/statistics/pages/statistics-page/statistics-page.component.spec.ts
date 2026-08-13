import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { StatisticsResponse } from '../../../../core/models/workworth-api.models';
import { StatisticsApiService } from '../../../../core/services/statistics-api.service';
import { StatisticsPageComponent } from './statistics-page.component';

describe('StatisticsPageComponent', () => {
  let fixture: ComponentFixture<StatisticsPageComponent>;
  let component: StatisticsPageComponent;
  const statisticsApi = { statistics: vi.fn() };

  beforeEach(async () => {
    statisticsApi.statistics.mockReset();
    statisticsApi.statistics.mockReturnValue(of(response()));
    await TestBed.configureTestingModule({
      imports: [StatisticsPageComponent],
      providers: [{ provide: StatisticsApiService, useValue: statisticsApi }]
    }).compileComponents();
    fixture = TestBed.createComponent(StatisticsPageComponent);
    component = fixture.componentInstance;
  });

  it('loads the monthly backend series initially and renders all four metrics', () => {
    fixture.detectChanges();

    expect(statisticsApi.statistics).toHaveBeenCalledWith('MONTH');
    expect(fixture.nativeElement.querySelectorAll('canvas')).toHaveLength(4);
    expect(fixture.nativeElement.textContent).toContain('Horas económicas efectivas');
    expect(fixture.nativeElement.textContent).toContain('Salario medio efectivo por hora');
    expect(fixture.nativeElement.textContent).toContain('Ganancias efectivas totales');
    expect(fixture.nativeElement.textContent).toContain('Goals completados');
  });

  it('requests DAY, WEEK, MONTH and YEAR exactly when the granularity changes', () => {
    fixture.detectChanges();

    component.selectGranularity('DAY');
    component.selectGranularity('WEEK');
    component.selectGranularity('MONTH');
    component.selectGranularity('YEAR');

    expect(statisticsApi.statistics.mock.calls.map((call: unknown[]) => call[0]))
      .toEqual(['MONTH', 'DAY', 'WEEK', 'MONTH', 'YEAR']);
  });

  it('renders zero as a valid value and keeps unavailable metrics distinct', () => {
    statisticsApi.statistics.mockReturnValue(of(response({
      workedHours: { status: 'AVAILABLE', value: 0 },
      averageHourlyEarnings: { status: 'UNAVAILABLE', amount: null, currencyCode: null },
      totalEarnings: { status: 'AVAILABLE', amount: 0, currencyCode: 'EUR' },
      completedGoals: { status: 'AVAILABLE', count: 0 }
    })));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('0.00');
    expect(text).toContain('No disponible');
    expect(text).toContain('EUR');
  });

  it('uses the currency received in the statistics response for EUR and USD', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('EUR');

    statisticsApi.statistics.mockReturnValue(of(response({
      averageHourlyEarnings: { status: 'AVAILABLE', amount: 10, currencyCode: 'USD' },
      totalEarnings: { status: 'AVAILABLE', amount: 20, currencyCode: 'USD' }
    })));
    component.selectGranularity('YEAR');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('USD');
  });

  it('shows an empty state for an empty backend series', () => {
    statisticsApi.statistics.mockReturnValue(of({ granularity: 'MONTH', from: null, to: null, points: [] }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no hay actividad registrada');
  });

  it('shows a contextual HTTP error without treating it as an unavailable metric', () => {
    statisticsApi.statistics.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 400, error: {
      code: 'VALIDATION_ERROR', detail: 'The requested Statistics range exceeds the maximum of 366 points.'
    } })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('The requested Statistics range exceeds the maximum of 366 points.');
    expect(fixture.nativeElement.textContent).not.toContain('No disponible');
  });

  it('depends only on StatisticsApiService and presents the backend point without other data sources', () => {
    fixture.detectChanges();

    expect(TestBed.inject(StatisticsApiService)).toBe(statisticsApi);
    expect(component.statistics()?.points[0]).toEqual(response().points[0]);
  });
});

function response(metrics: Partial<StatisticsResponse['points'][number]> = {}): StatisticsResponse {
  return {
    granularity: 'MONTH', from: null, to: null,
    points: [{
      startDate: '2026-08-01', endDate: '2026-09-01',
      workedHours: { status: 'AVAILABLE', value: 2 },
      averageHourlyEarnings: { status: 'AVAILABLE', amount: 12.5, currencyCode: 'EUR' },
      totalEarnings: { status: 'AVAILABLE', amount: 25, currencyCode: 'EUR' },
      completedGoals: { status: 'AVAILABLE', count: 1 },
      ...metrics
    }]
  };
}
