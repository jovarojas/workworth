import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';
import { WorkdayLiveComponent } from './workday-live.component';

describe('WorkdayLiveComponent', () => {
  const workdays = { current: vi.fn() };

  beforeEach(async () => {
    workdays.current.mockReset();

    await TestBed.configureTestingModule({
      imports: [WorkdayLiveComponent],
      providers: [{ provide: WorkdayApiService, useValue: workdays }]
    }).compileComponents();
  });

  afterEach(() => vi.useRealTimers());

  it.each([
    ['SCHEDULED', 'Jornada programada'],
    ['ACTIVE', 'Jornada activa'],
    ['ON_MEAL_BREAK', 'En pausa'],
    ['COMPLETED', 'Jornada completada'],
    ['CANCELLED', 'Jornada cancelada']
  ])('shows the real %s state', (status, label) => {
    workdays.current.mockReturnValue(of(workday(status)));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(label);
    expect(fixture.nativeElement.textContent).toContain(status);
  });

  it('shows economic seconds exactly as received without calculating them', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE', 4_321, 28_800)));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('4321 segundos');
    expect(content).toContain('Máximo programado: 28800 segundos');
  });

  it('shows an empty state when the backend reports no workday', () => {
    workdays.current.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay jornada para hoy');
  });

  it('shows a connection error when the backend is unavailable', () => {
    workdays.current.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('does not render pause resume or other workday action buttons', () => {
    workdays.current.mockReturnValue(of(workday('ON_MEAL_BREAK')));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('button')).toHaveLength(0);
    expect(fixture.nativeElement.textContent).not.toContain('Reanudar');
  });

  it.each(['SCHEDULED', 'ACTIVE', 'ON_MEAL_BREAK'])('polls after sixty seconds for %s workdays', (status) => {
    vi.useFakeTimers();
    workdays.current.mockReturnValue(of(workday(status)));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(2);
  });

  it.each(['COMPLETED', 'CANCELLED'])('does not poll for %s workdays', (status) => {
    vi.useFakeTimers();
    workdays.current.mockReturnValue(of(workday(status)));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(1);
  });

  it('cleans up polling when the component is destroyed', () => {
    vi.useFakeTimers();
    workdays.current.mockReturnValue(of(workday('ACTIVE')));

    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();
    fixture.destroy();
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(1);
  });

  function workday(status: string, economicSeconds = 3_600, maximumEconomicSeconds = 28_800) {
    return {
      id: 1,
      localDate: '2026-08-11',
      timeZone: 'Europe/Madrid',
      status,
      scheduledStart: '08:00',
      scheduledEnd: '17:00',
      maximumEconomicSeconds,
      economicSeconds
    };
  }
});
