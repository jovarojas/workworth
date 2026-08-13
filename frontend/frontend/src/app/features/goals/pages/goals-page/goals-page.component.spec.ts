import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { GoalResponse } from '../../../../core/models/workworth-api.models';
import { GoalsApiService } from '../../../../core/services/goals-api.service';
import { GoalsPageComponent } from './goals-page.component';

describe('GoalsPageComponent', () => {
  let fixture: ComponentFixture<GoalsPageComponent>;
  let component: GoalsPageComponent;
  const goalsApi = {
    active: vi.fn(), history: vi.fn(), create: vi.fn(), update: vi.fn(), complete: vi.fn(), cancel: vi.fn()
  };

  const activeGoal = goal(1, 'Viaje', 'ACTIVE', progress(false));
  const completedGoal = goal(2, 'Curso', 'COMPLETED', null);
  const cancelledGoal = goal(3, 'Libro', 'CANCELLED', null);

  beforeEach(async () => {
    Object.values(goalsApi).forEach((method) => method.mockReset());
    goalsApi.active.mockReturnValue(of([activeGoal]));
    goalsApi.history.mockReturnValue(of([completedGoal, cancelledGoal]));
    goalsApi.create.mockReturnValue(of(activeGoal));
    goalsApi.update.mockReturnValue(of(activeGoal));
    goalsApi.complete.mockReturnValue(of(completedGoal));
    goalsApi.cancel.mockReturnValue(of(cancelledGoal));

    await TestBed.configureTestingModule({
      imports: [GoalsPageComponent],
      providers: [{ provide: GoalsApiService, useValue: goalsApi }]
    }).compileComponents();
    fixture = TestBed.createComponent(GoalsPageComponent);
    component = fixture.componentInstance;
  });

  it('loads active goals and closed history separately', () => {
    fixture.detectChanges();

    expect(goalsApi.active).toHaveBeenCalledTimes(1);
    expect(goalsApi.history).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.textContent).toContain('En marcha');
    expect(fixture.nativeElement.textContent).toContain('Cerrados');
    expect(fixture.nativeElement.textContent).toContain('Completado');
    expect(fixture.nativeElement.textContent).toContain('Cancelado');
  });

  it('presents progress supplied by the backend without consulting Earnings', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Registrado');
    expect(fixture.nativeElement.textContent).toContain('Restante');
    expect(fixture.nativeElement.textContent).toContain('25%');
    expect(fixture.nativeElement.textContent).toContain('Progreso actual resuelto con las ganancias efectivas');
    expect(goalsApi).not.toHaveProperty('earnings');
  });

  it('shows unavailable progress without inventing money values', () => {
    goalsApi.active.mockReturnValue(of([goal(1, 'Viaje', 'ACTIVE', {
      evaluable: false, progressAmount: null, remainingAmount: null, progressPercentage: null, reached: null
    })]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ahora mismo no podemos calcular el progreso');
    expect(fixture.nativeElement.textContent).not.toContain('Registrado');
  });

  it('creates and edits active goals through the backend', () => {
    fixture.detectChanges();
    component.saveGoal({ title: 'Viaje', targetAmount: 500 });
    component.edit(activeGoal);
    component.saveGoal({ title: 'Viaje actualizado', targetAmount: 600 });

    expect(goalsApi.create).toHaveBeenCalledWith({ title: 'Viaje', targetAmount: 500 });
    expect(goalsApi.update).toHaveBeenCalledWith(1, { title: 'Viaje actualizado', targetAmount: 600 });
  });

  it('completes and cancels through backend lifecycle operations and refreshes both lists', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fixture.detectChanges();

    component.complete(activeGoal);
    component.cancel(activeGoal);

    expect(goalsApi.complete).toHaveBeenCalledWith(1);
    expect(goalsApi.cancel).toHaveBeenCalledWith(1);
    expect(goalsApi.active).toHaveBeenCalledTimes(3);
    expect(goalsApi.history).toHaveBeenCalledTimes(3);
  });

  it('does not render mutable actions for completed and cancelled goals', () => {
    goalsApi.active.mockReturnValue(of([]));
    fixture.detectChanges();

    const historyCards = fixture.nativeElement.querySelectorAll('.goal-card--closed');
    expect(historyCards).toHaveLength(2);
    Array.from(historyCards).forEach((card: unknown) => {
      expect((card as HTMLElement).textContent).not.toContain('Editar');
      expect((card as HTMLElement).textContent).not.toContain('Cancelar');
      expect((card as HTMLElement).textContent).not.toContain('Marcar como completado');
    });
  });

  it('offers completion only when the backend marks active progress as reached', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Marcar como completado');

    goalsApi.active.mockReturnValue(of([goal(1, 'Viaje', 'ACTIVE', progress(true))]));
    component.loadActive();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Marcar como completado');
  });

  it('renders independent empty states for active goals and history', () => {
    goalsApi.active.mockReturnValue(of([]));
    goalsApi.history.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no tienes objetivos activos.');
    expect(fixture.nativeElement.textContent).toContain('Los objetivos completados o cancelados aparecerán aquí.');
  });

  it('keeps already visible lists when a lifecycle action fails', () => {
    goalsApi.complete.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409, error: {
      code: 'GOAL_CONFLICT', detail: 'Only a reached active goal can be completed.'
    } })));
    fixture.detectChanges();

    component.complete(activeGoal);
    fixture.detectChanges();

    expect(component.activeGoals()).toEqual([activeGoal]);
    expect(component.historyGoals()).toEqual([completedGoal, cancelledGoal]);
    expect(fixture.nativeElement.textContent).toContain('Esta operación no es posible para el estado actual del objetivo.');
  });

  it('prevents duplicate lifecycle requests while an action is pending', () => {
    const completion = new Subject<GoalResponse>();
    goalsApi.complete.mockReturnValue(completion);
    fixture.detectChanges();

    component.complete(activeGoal);
    component.complete(activeGoal);

    expect(goalsApi.complete).toHaveBeenCalledTimes(1);
    completion.next(completedGoal);
    completion.complete();
  });

  it('keeps each goal action disabled independently while concurrent actions are pending', () => {
    const firstCompletion = new Subject<GoalResponse>();
    const secondGoal = goal(4, 'Libro', 'ACTIVE', progress(false));
    const secondCancellation = new Subject<GoalResponse>();
    goalsApi.complete.mockReturnValue(firstCompletion);
    goalsApi.cancel.mockReturnValue(secondCancellation);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fixture.detectChanges();

    component.complete(activeGoal);
    component.cancel(secondGoal);
    component.complete(activeGoal);

    expect(component.isActionInProgress(activeGoal.id)).toBe(true);
    expect(component.isActionInProgress(secondGoal.id)).toBe(true);
    expect(goalsApi.complete).toHaveBeenCalledTimes(1);
    firstCompletion.complete();
    secondCancellation.complete();
  });
});

function goal(id: number, title: string, status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED',
  currentProgress: GoalResponse['progress']): GoalResponse {
  return {
    id, title, targetAmount: 500, currencyCode: 'EUR', status,
    createdAt: '2026-08-13T10:00:00Z', updatedAt: '2026-08-13T10:00:00Z',
    closedAt: status === 'ACTIVE' ? null : '2026-08-13T12:00:00Z', progress: currentProgress
  };
}

function progress(reached: boolean): NonNullable<GoalResponse['progress']> {
  return { evaluable: true, progressAmount: 125, remainingAmount: 375, progressPercentage: 25, reached };
}
