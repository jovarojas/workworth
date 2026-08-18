import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize, Observable } from 'rxjs';
import { problemDetailMessage } from '../../../../core/http/problem-detail';
import { goalStatusLabel } from '../../../../core/presentation/display-labels';
import { CreateGoalRequest, GoalResponse } from '../../../../core/models/workworth-api.models';
import { GoalsApiService } from '../../../../core/services/goals-api.service';
import { GoalFormComponent } from '../../components/goal-form/goal-form.component';

@Component({
  selector: 'app-goals-page',
  imports: [
    CommonModule,
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    GoalFormComponent
  ],
  templateUrl: './goals-page.component.html',
  styleUrl: './goals-page.component.scss'
})
export class GoalsPageComponent implements OnInit {
  private readonly goalsApi = inject(GoalsApiService);

  @ViewChild(GoalFormComponent) private goalForm?: GoalFormComponent;

  readonly activeGoals = signal<GoalResponse[]>([]);
  readonly historyGoals = signal<GoalResponse[]>([]);
  readonly activeLoading = signal(true);
  readonly historyLoading = signal(true);
  readonly activeError = signal<string | null>(null);
  readonly historyError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly actionSuccess = signal<string | null>(null);
  readonly activeAction = signal<string | null>(null);
  readonly activeGoalActionIds = signal<Set<number>>(new Set());
  readonly editingGoal = signal<GoalResponse | null>(null);

  readonly formSaving = computed(() => this.activeAction() === 'form');

  ngOnInit(): void {
    this.loadActive();
    this.loadHistory();
  }

  loadActive(showLoading = true): void {
    if (showLoading) {
      this.activeLoading.set(true);
    }
    this.activeError.set(null);
    this.goalsApi.active()
      .pipe(finalize(() => this.activeLoading.set(false)))
      .subscribe({
        next: (goals) => this.activeGoals.set(goals),
        error: (error: unknown) => this.activeError.set(this.errorMessage(error, 'No se han podido cargar los objetivos activos.'))
      });
  }

  loadHistory(showLoading = true): void {
    if (showLoading) {
      this.historyLoading.set(true);
    }
    this.historyError.set(null);
    this.goalsApi.history()
      .pipe(finalize(() => this.historyLoading.set(false)))
      .subscribe({
        next: (goals) => this.historyGoals.set(goals),
        error: (error: unknown) => this.historyError.set(this.errorMessage(error, 'No se ha podido cargar el historial de objetivos.'))
      });
  }

  saveGoal(request: CreateGoalRequest): void {
    if (this.formSaving()) {
      return;
    }
    this.activeAction.set('form');
    this.actionError.set(null);
    this.actionSuccess.set(null);
    const editing = this.editingGoal();
    const action = editing ? this.goalsApi.update(editing.id, request) : this.goalsApi.create(request);
    action.pipe(finalize(() => this.activeAction.set(null))).subscribe({
      next: () => {
        this.actionSuccess.set(editing ? 'Objetivo actualizado.' : 'Objetivo creado.');
        this.editingGoal.set(null);
        this.goalForm?.reset();
        this.loadActive(false);
      },
      error: (error: unknown) => this.actionError.set(this.errorMessage(error,
        editing ? 'No se ha podido actualizar el objetivo.' : 'No se ha podido crear el objetivo.'))
    });
  }

  edit(goal: GoalResponse): void {
    this.actionError.set(null);
    this.actionSuccess.set(null);
    this.editingGoal.set(goal);
  }

  cancelEdit(): void {
    this.editingGoal.set(null);
    this.goalForm?.reset();
  }

  complete(goal: GoalResponse): void {
    if (this.isActionInProgress(goal.id)) {
      return;
    }
    this.runAction(goal.id, this.goalsApi.complete(goal.id), 'Objetivo marcado como completado.',
      'No se ha podido completar el objetivo.');
  }

  cancel(goal: GoalResponse): void {
    if (this.isActionInProgress(goal.id)
      || !window.confirm(`¿Cancelar el objetivo “${goal.title}”?`)) {
      return;
    }
    this.runAction(goal.id, this.goalsApi.cancel(goal.id), 'Objetivo cancelado.', 'No se ha podido cancelar el objetivo.');
  }

  isActionInProgress(id: number): boolean {
    return this.activeGoalActionIds().has(id);
  }

  goalStatusLabel(status: GoalResponse['status']): string {
    return goalStatusLabel(status);
  }

  progressText(goal: GoalResponse): string {
    if (!goal.progress?.evaluable) {
      return 'Ahora mismo no podemos calcular el progreso con las ganancias registradas.';
    }
    if (goal.progress.reached) {
      return 'El objetivo ya ha alcanzado su importe objetivo con lo registrado en WorkWorth.';
    }
    return 'Progreso actual resuelto con las ganancias efectivas registradas en WorkWorth.';
  }

  private runAction(id: number, action: Observable<GoalResponse>, success: string, failure: string): void {
    if (this.isActionInProgress(id)) {
      return;
    }
    this.activeGoalActionIds.update((ids) => new Set(ids).add(id));
    this.actionError.set(null);
    this.actionSuccess.set(null);
    action.pipe(finalize(() => this.activeGoalActionIds.update((ids) => {
      const next = new Set(ids);
      next.delete(id);
      return next;
    }))).subscribe({
      next: () => {
        this.actionSuccess.set(success);
        if (this.editingGoal()?.id === id) {
          this.cancelEdit();
        }
        this.loadActive(false);
        this.loadHistory(false);
      },
      error: (error: unknown) => this.actionError.set(this.errorMessage(error, failure))
    });
  }

  private errorMessage(error: unknown, fallback: string): string {
    const detail = problemDetailMessage(error);
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth. Inténtalo de nuevo más tarde.';
    }
    return fallback;
  }
}
