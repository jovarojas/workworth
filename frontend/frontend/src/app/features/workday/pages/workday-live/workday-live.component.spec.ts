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
    cancel: vi.fn(),
    createPartialAbsence: vi.fn()
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
  ] as const)('shows the translated %s state', (status, label) => {
    workdays.current.mockReturnValue(of(workday(status)));

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).toContain(label);
    expect(fixture.nativeElement.textContent).not.toContain(status);
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
  ] as const)('shows the localized %i %s error without hiding the workday', (status, code, detail) => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    workdays.startMealBreak.mockReturnValue(throwError(() => new HttpErrorResponse({ status, error: { code, detail } })));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(localizedMessage(code));
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

  it('shows an initial GET error without a workday', () => {
    workdays.current.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).toContain('No se ha podido cargar la jornada actual');
    expect(fixture.nativeElement.textContent).not.toContain('Jornada activa');
  });

  it('preserves the current workday when the refresh after starting a meal break fails', () => {
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })));
    workdays.startMealBreak.mockReturnValue(of(openMealBreak(10)));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('preserves the current workday when the refresh after ending a meal break fails', () => {
    const mealBreak = openMealBreak(10);
    workdays.current
      .mockReturnValueOnce(of(workday('ON_MEAL_BREAK', { mealBreaks: [mealBreak] })))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    workdays.endMealBreak.mockReturnValue(of(closedMealBreak(10)));

    const fixture = createComponent();
    click(fixture, '[data-testid="end-meal-break"]');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('En pausa');
    expect(fixture.nativeElement.textContent).toContain('No se ha podido cargar la jornada actual');
  });

  it('preserves the current workday when the refresh after cancellation fails', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    workdays.cancel.mockReturnValue(of(void 0));

    const fixture = createComponent();
    click(fixture, '[data-testid="cancel-workday"]');
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
    expect(fixture.nativeElement.textContent).toContain('No se ha podido cargar la jornada actual');
  });

  it('reconciles the current workday after a conflict', () => {
    const mealBreak = openMealBreak(77);
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('ON_MEAL_BREAK', { mealBreaks: [mealBreak] })));
    workdays.startMealBreak.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { code: 'WORKDAY_CONFLICT', detail: 'La pausa ya está abierta.' }
    })));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(workdays.current).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('En pausa');
    expect(fixture.nativeElement.textContent).toContain('Esta operación no es posible para el estado actual de la jornada.');
    expect(fixture.nativeElement.querySelector('[data-testid="end-meal-break"]')).not.toBeNull();
  });

  it('preserves the current workday when reconciliation after a conflict fails', () => {
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })));
    workdays.startMealBreak.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409,
      error: { code: 'WORKDAY_CONFLICT', detail: 'La pausa ya está abierta.' }
    })));

    const fixture = createComponent();
    click(fixture, '[data-testid="start-meal-break"]');
    fixture.detectChanges();

    expect(workdays.current).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
    expect(fixture.nativeElement.textContent).toContain('Esta operación no es posible para el estado actual de la jornada.');
    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
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

  it('preserves the current workday when polling fails', () => {
    vi.useFakeTimers();
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 0 })));

    const fixture = createComponent();
    vi.advanceTimersByTime(60_000);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('stops polling after a successful cancellation refreshes to CANCELLED', () => {
    vi.useFakeTimers();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('CANCELLED', { economicSeconds: 0 })));
    workdays.cancel.mockReturnValue(of(void 0));

    const fixture = createComponent();
    click(fixture, '[data-testid="cancel-workday"]');
    vi.advanceTimersByTime(60_000);

    expect(workdays.current).toHaveBeenCalledTimes(2);
  });

  it('does not render manual workday start or completion actions', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));

    const fixture = createComponent();

    expect(fixture.nativeElement.textContent).not.toContain('Iniciar jornada');
    expect(fixture.nativeElement.textContent).not.toContain('Finalizar jornada');
  });

  it.each(['SCHEDULED', 'ACTIVE', 'ON_MEAL_BREAK', 'COMPLETED'] as const)(
    'offers partial absence registration for a %s workday',
    (status) => {
      workdays.current.mockReturnValue(of(workday(status)));

      const fixture = createComponent();

      expect(fixture.nativeElement.querySelector('[data-testid="show-partial-absence-form"]')).not.toBeNull();
    }
  );

  it('does not offer partial absence registration for a cancelled workday', () => {
    workdays.current.mockReturnValue(of(workday('CANCELLED')));

    const fixture = createComponent();

    expect(fixture.nativeElement.querySelector('[data-testid="show-partial-absence-form"]')).toBeNull();
  });

  it('serializes the form interval in the workday zone, creates the absence, and refreshes from the backend', () => {
    const absence = {
      id: 31,
      startedAt: '2026-08-12T08:30:00.000Z',
      endedAt: '2026-08-12T09:15:00.000Z',
      reason: 'Cita médica'
    };
    workdays.current
      .mockReturnValueOnce(of(workday('ACTIVE')))
      .mockReturnValueOnce(of(workday('ACTIVE', { partialAbsences: [absence] })));
    workdays.createPartialAbsence.mockReturnValue(of(absence));

    const fixture = createComponent();
    click(fixture, '[data-testid="show-partial-absence-form"]');
    fixture.componentInstance.absenceForm.setValue({
      startedAt: '10:30',
      endedAt: '11:15',
      reason: 'Cita médica'
    });
    fixture.componentInstance.createPartialAbsence(fixture.componentInstance.workday()!);
    fixture.detectChanges();

    expect(workdays.createPartialAbsence).toHaveBeenCalledWith('2026-08-12', {
      startedAt: '2026-08-12T08:30:00.000Z',
      endedAt: '2026-08-12T09:15:00.000Z',
      reason: 'Cita médica'
    });
    expect(workdays.current).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance.workday()?.partialAbsences).toEqual([absence]);
    expect(fixture.nativeElement.querySelector('[data-testid="submit-partial-absence"]')).toBeNull();
  });

  it('does not send an invalid local interval to the backend', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));

    const fixture = createComponent();
    fixture.componentInstance.showAbsenceForm();
    fixture.componentInstance.absenceForm.setValue({ startedAt: '12:00', endedAt: '12:00', reason: '' });
    fixture.componentInstance.createPartialAbsence(fixture.componentInstance.workday()!);
    fixture.detectChanges();

    expect(workdays.createPartialAbsence).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('La hora de inicio debe ser anterior a la hora de fin.');
  });

  it('keeps the backend workday visible when absence creation is rejected', () => {
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    workdays.createPartialAbsence.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 422,
      error: { code: 'WORKDAY_INTERVAL_INVALID', detail: 'El intervalo se solapa con una pausa.' }
    })));

    const fixture = createComponent();
    fixture.componentInstance.showAbsenceForm();
    fixture.componentInstance.absenceForm.setValue({ startedAt: '10:00', endedAt: '11:00', reason: '' });
    fixture.componentInstance.createPartialAbsence(fixture.componentInstance.workday()!);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('El intervalo indicado no es válido.');
    expect(fixture.nativeElement.textContent).toContain('Jornada activa');
  });

  it('prevents a duplicate partial absence submission while the request is in progress', () => {
    const pending = new Subject();
    workdays.current.mockReturnValue(of(workday('ACTIVE')));
    workdays.createPartialAbsence.mockReturnValue(pending);

    const fixture = createComponent();
    fixture.componentInstance.showAbsenceForm();
    fixture.componentInstance.absenceForm.setValue({ startedAt: '10:00', endedAt: '11:00', reason: '' });
    const currentWorkday = fixture.componentInstance.workday()!;
    fixture.componentInstance.createPartialAbsence(currentWorkday);
    fixture.componentInstance.createPartialAbsence(currentWorkday);

    expect(workdays.createPartialAbsence).toHaveBeenCalledTimes(1);

    pending.complete();
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

  function localizedMessage(code: string): string {
    return {
      VALIDATION_ERROR: 'Revisa los datos introducidos.',
      RESOURCE_NOT_FOUND: 'No se ha encontrado la información solicitada.',
      WORKDAY_CONFLICT: 'Esta operación no es posible para el estado actual de la jornada.',
      WORKDAY_INTERVAL_INVALID: 'El intervalo indicado no es válido.'
    }[code] ?? 'No se ha podido completar la operación.';
  }

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
