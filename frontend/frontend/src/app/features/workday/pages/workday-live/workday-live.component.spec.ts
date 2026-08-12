import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { MealBreakResponse, WorkdayResponse, WorkdayStatus } from '../../../../core/models/workworth-api.models';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';
import { WorkdayLiveComponent } from './workday-live.component';

describe('WorkdayLiveComponent', () => {
  const workdays = {
    current: vi.fn(),
    startMealBreak: vi.fn(),
    endMealBreak: vi.fn(),
    cancel: vi.fn()
  };

  beforeEach(async () => {
    Object.values(workdays).forEach((method) => method.mockReset());

    await TestBed.configureTestingModule({
      imports: [WorkdayLiveComponent],
      providers: [{ provide: WorkdayApiService, useValue: workdays }]
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it.each([
    ['SCHEDULED', 'Jornada programada'],
    ['ACTIVE', 'Jornada activa'],
    ['ON_MEAL_BREAK', 'En pausa'],
    ['COMPLETED', 'Jornada completada'],
    ['CANCELLED', 'Jornada cancelada']
  ] as const)('shows the real %s state', (status, label) => {
    workdays.current.mockReturnValue(of(workday(status)));

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).toContain(label);
    expect(fixture.nativeElement.textContent).toContain(status);
  });

  it('shows economic seconds exactly as received without calculating them', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE', { economicSeconds: 4_321, maximumEconomicSeconds: 28_800 })));

    const fixture = createComponent();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('4321 segundos');
    expect(content).toContain('Máximo programado: 28800 segundos');
  });

  it('starts a meal break from an active workday and refreshes the persisted response', () => {
    const mealBreak = openMealBreak(21);
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('ON_MEAL_BREAK', { mealBreaks: [mealBreak] })));
    workdays.startMealBreak.mockReturnValue(of(mealBreak));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(workdays.startMealBreak).toHaveBeenCalledWith('2026-08-12');
    expect(workdays.current).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Finalizar pausa');
  });

  it('recovers the open meal-break identifier after a reload and uses it to end the break', () => {
    const mealBreak = openMealBreak(44);
    workdays.current
      .mockReturnValueOnce(of(workday('ON_MEAL_BREAK', { mealBreaks: [mealBreak] })))
      .mockReturnValueOnce(of(workday('ACTIVE', { mealBreaks: [closedMealBreak(44)] })));
    workdays.endMealBreak.mockReturnValue(of(closedMealBreak(44)));

    const fixture = createComponent();
    click(fixture, '[data-testid="end-meal-break"]');
    fixture.detectChanges();

    expect(workdays.endMealBreak).toHaveBeenCalledWith('2026-08-12', 44);
    expect(workdays.current).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Iniciar pausa');
  });

  it('does not offer to end an already closed meal break', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE', { mealBreaks: [closedMealBreak(8)] })));

    const fixture = createComponent();

    expect(fixture.nativeElement.querySelector('[data-testid="end-meal-break"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="start-meal-break"]')).not.toBeNull();
  });

  it('cancels a confirmed workday and refreshes its cancelled state', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('CANCELLED', { economicSeconds: 0 })));
    workdays.cancel.mockReturnValue(of(void 0));

    const fixture = createComponent();
    click(fixture, '[data-testid="cancel-workday"]');
    fixture.detectChanges();

    expect(workdays.cancel).toHaveBeenCalledWith('2026-08-12');
    expect(fixture.nativeElement.textContent).toContain('Jornada cancelada');
    expect(fixture.nativeElement.querySelector('[data-testid="cancel-workday"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="start-meal-break"]')).toBeNull();
  });

  it('does not cancel when confirmation is declined', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    workdays.current.mockReturnValue(of(workday('ACTIVE')));

    const fixture = createComponent();
    click(fixture, '[data-testid="cancel-workday"]');

    expect(workdays.cancel).not.toHaveBeenCalled();
  });

  it('prevents a duplicate action while a meal-break request is in progress', () => {
    const pending = new Subject<MealBreakResponse>();
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    workdays.startMealBreak.mockReturnValue(pending);

    const fixture = createComponent();
    const button = fixture.nativeElement.querySelector('[data-testid="start-meal-break"]') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    button.click();

    expect(workdays.startMealBreak).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBe(true);

    pending.complete();
  });

  it.each([
    [400, 'VALIDATION_ERROR', 'La petición no es válida.'],
    [404, 'RESOURCE_NOT_FOUND', 'La jornada no existe.'],
    [409, 'WORKDAY_CONFLICT', 'La pausa ya está abierta.'],
    [422, 'WORKDAY_INTERVAL_INVALID', 'El intervalo no es válido.']
  ] as const)('shows the public %i %s error without hiding the workday', (status, code, detail) => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    workdays.startMealBreak.mockReturnValue(throwError(() => new HttpErrorResponse({ status, error: { code, detail } })));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(detail);
    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
  });

  it('shows a contextual connection error without hiding the workday', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    workdays.startMealBreak.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('La acción no se ha completado');
    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
  });

  it('shows an empty state when the initial GET reports no workday', () => {
    workdays.current.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).toContain('No hay jornada para hoy');
  });

  it('keeps polling after a successful mutation while the workday remains dynamic', () => {
    vi.useFakeTimers();
    const mealBreak = openMealBreak(21);
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('ON_MEAL_BREAK', { mealBreaks: [mealBreak] })))
      .mockReturnValue(of(workday('ON_MEAL_BREAK', { mealBreaks: [mealBreak] })));
    workdays.startMealBreak.mockReturnValue(of(mealBreak));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(3);
  });

  it('does not render manual workday start or completion actions', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).not.toContain('Iniciar jornada');
    expect(fixture.nativeElement.textContent).not.toContain('Finalizar jornada');
  });

  it.each(['SCHEDULED', 'ACTIVE', 'ON_MEAL_BREAK'] as const)('polls after sixty seconds for %s workdays', (status) => {
    vi.useFakeTimers();
    workdays.current.mockReturnValue(of(workday(status)));

    createComponent();
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(2);
  });

  it.each(['COMPLETED', 'CANCELLED'] as const)('does not poll for %s workdays', (status) => {
    vi.useFakeTimers();
    workdays.current.mockReturnValue(of(workday(status)));

    createComponent();
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(1);
  });

  it('cleans up polling when the component is destroyed', () => {
    vi.useFakeTimers();
    workdays.current.mockReturnValue(of(workday('ACTIVE')));

    const fixture = createComponent();
    fixture.destroy();
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(1);
  });

  function createComponent() {
    const fixture = TestBed.createComponent(WorkdayLiveComponent);
    fixture.detectChanges();
    return fixture;
  }

  function click(fixture: ReturnType<typeof createComponent>, selector: string): void {
    (fixture.nativeElement.querySelector(selector) as HTMLButtonElement).click();
  }

  function workday(
    status: WorkdayStatus,
    overrides: Partial<WorkdayResponse> = {}
  ): WorkdayResponse {
    return {
      id: 1,
      localDate: '2026-08-12',
      timeZone: 'Europe/Madrid',
      status,
      scheduledStart: '08:00',
      scheduledEnd: '17:00',
      maximumEconomicSeconds: 28_800,
      economicSeconds: 3_600,
      mealBreaks: [],
      partialAbsences: [],
      ...overrides
    };
  }

  function openMealBreak(id: number): MealBreakResponse {
    return { id, startedAt: '2026-08-12T10:00:00Z', endedAt: null, endedAutomatically: false };
  }

  function closedMealBreak(id: number): MealBreakResponse {
    return { id, startedAt: '2026-08-12T10:00:00Z', endedAt: '2026-08-12T10:30:00Z', endedAutomatically: false };
  }
});
