import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, Observable, Subscription, timer } from 'rxjs';
import { problemDetailFrom } from '../../../../core/http/problem-detail';
import { MealBreakResponse, WorkdayResponse, WorkdayStatus } from '../../../../core/models/workworth-api.models';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';
import { WorkdayIntervalSerializationError, serializeWorkdayInterval } from '../../serializers/workday-interval.serializer';

@Component({
  selector: 'app-workday-live',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './workday-live.component.html',
  styleUrl: './workday-live.component.scss'
})
export class WorkdayLiveComponent implements OnInit, OnDestroy {
  private readonly workdays = inject(WorkdayApiService);
  private readonly destroyRef = inject(DestroyRef);
  private pollingSubscription?: Subscription;

  readonly workday = signal<WorkdayResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly missing = signal(false);
  readonly actionInProgress = signal<'start-meal-break' | 'end-meal-break' | 'cancel' | 'create-partial-absence' | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly absenceFormVisible = signal(false);
  readonly absenceSubmitted = signal(false);
  readonly absenceFormError = signal<string | null>(null);
  readonly openMealBreak = computed<MealBreakResponse | null>(() =>
    this.workday()?.mealBreaks.find((mealBreak) => mealBreak.endedAt === null) ?? null
  );
  readonly canCreatePartialAbsence = computed(() => {
    const status = this.workday()?.status;
    return status === 'SCHEDULED' || status === 'ACTIVE' || status === 'ON_MEAL_BREAK' || status === 'COMPLETED';
  });

  readonly absenceForm = new FormGroup({
    startedAt: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)] }),
    endedAt: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^([01]\d|2[0-3]):[0-5]\d$/)] }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] })
  });

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  statusLabel(status: WorkdayStatus): string {
    return {
      SCHEDULED: 'Jornada programada',
      ACTIVE: 'Jornada activa',
      ON_MEAL_BREAK: 'En pausa',
      COMPLETED: 'Jornada completada',
      CANCELLED: 'Jornada cancelada'
    }[status];
  }

  load(showLoading = true): void {
    const hasPreviousWorkday = this.workday() !== null;
    if (showLoading) {
      this.loading.set(true);
    }
    this.error.set(null);
    this.missing.set(false);

    this.workdays.current()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (workday) => {
          this.workday.set(workday);
          this.configurePolling(workday.status);
        },
        error: (error: unknown) => {
          if (!showLoading && hasPreviousWorkday) {
            this.error.set(this.errorMessage(error));
            return;
          }

          this.workday.set(null);
          this.stopPolling();
          if (error instanceof HttpErrorResponse && error.status === 404) {
            this.missing.set(true);
            return;
          }
          this.error.set(this.errorMessage(error));
        }
      });
  }

  startMealBreak(workday: WorkdayResponse): void {
    if (this.actionInProgress()) {
      return;
    }

    this.runAction('start-meal-break', this.workdays.startMealBreak(workday.localDate));
  }

  endMealBreak(workday: WorkdayResponse, mealBreak: MealBreakResponse): void {
    if (this.actionInProgress()) {
      return;
    }

    this.runAction('end-meal-break', this.workdays.endMealBreak(workday.localDate, mealBreak.id));
  }

  cancelWorkday(workday: WorkdayResponse): void {
    if (this.actionInProgress() || !window.confirm('¿Quieres marcar esta jornada como no trabajada?')) {
      return;
    }

    this.runAction('cancel', this.workdays.cancel(workday.localDate));
  }

  showAbsenceForm(): void {
    this.absenceFormVisible.set(true);
    this.absenceFormError.set(null);
    this.actionError.set(null);
  }

  hideAbsenceForm(): void {
    if (this.actionInProgress()) {
      return;
    }
    this.resetAbsenceForm();
  }

  private resetAbsenceForm(): void {
    this.absenceFormVisible.set(false);
    this.absenceSubmitted.set(false);
    this.absenceFormError.set(null);
    this.absenceForm.reset();
  }

  createPartialAbsence(workday: WorkdayResponse): void {
    this.absenceSubmitted.set(true);
    this.absenceFormError.set(null);
    this.actionError.set(null);

    if (this.actionInProgress() || this.absenceForm.invalid) {
      this.absenceForm.markAllAsTouched();
      return;
    }

    const value = this.absenceForm.getRawValue();
    if (value.startedAt >= value.endedAt) {
      this.absenceFormError.set('La hora de inicio debe ser anterior a la hora de fin.');
      return;
    }

    try {
      const interval = serializeWorkdayInterval(workday.localDate, value.startedAt, value.endedAt, workday.timeZone);
      this.runAction(
        'create-partial-absence',
        this.workdays.createPartialAbsence(workday.localDate, { ...interval, reason: value.reason || null }),
        () => this.resetAbsenceForm()
      );
    } catch (error) {
      this.absenceFormError.set(
        error instanceof WorkdayIntervalSerializationError
          ? error.message
          : 'No se ha podido preparar la ausencia.'
      );
    }
  }

  absenceControlError(controlName: 'startedAt' | 'endedAt' | 'reason'): string | null {
    const control = this.absenceForm.controls[controlName];
    if (!this.absenceSubmitted() && !control.touched) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('pattern')) {
      return 'Introduce una hora válida.';
    }
    if (control.hasError('maxlength')) {
      return 'El motivo no puede superar 500 caracteres.';
    }
    return null;
  }

  private configurePolling(status: WorkdayStatus): void {
    if (!this.isDynamicStatus(status)) {
      this.stopPolling();
      return;
    }

    if (this.pollingSubscription) {
      return;
    }

    this.pollingSubscription = timer(60_000, 60_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(false));
  }

  private isDynamicStatus(status: WorkdayStatus): boolean {
    return status === 'SCHEDULED' || status === 'ACTIVE' || status === 'ON_MEAL_BREAK';
  }

  private stopPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = undefined;
  }

  private runAction(
    action: 'start-meal-break' | 'end-meal-break' | 'cancel' | 'create-partial-absence',
    request: Observable<unknown>,
    onSuccess?: () => void
  ): void {
    this.actionInProgress.set(action);
    this.actionError.set(null);

    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.actionInProgress.set(null))
      )
      .subscribe({
        next: () => {
          onSuccess?.();
          this.load(false);
        },
        error: (error: unknown) => {
          this.actionError.set(this.actionErrorMessage(error));
          if (problemDetailFrom(error)?.code === 'WORKDAY_CONFLICT') {
            this.load(false);
          }
        }
      });
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth. Comprueba que el backend está en ejecución.';
    }
    return 'No se ha podido cargar la jornada actual. Inténtalo de nuevo más tarde.';
  }

  private actionErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth. La acción no se ha completado.';
    }

    const problem = problemDetailFrom(error);
    switch (problem?.code) {
      case 'VALIDATION_ERROR':
      case 'WORKDAY_INTERVAL_INVALID':
        return problem.detail ?? 'Los datos de la jornada no son válidos.';
      case 'RESOURCE_NOT_FOUND':
        return problem.detail ?? 'La jornada o la pausa ya no existe.';
      case 'WORKDAY_CONFLICT':
        return problem.detail ?? 'Esta acción no es válida para el estado actual de la jornada.';
      default:
        return problem?.detail ?? 'No se ha podido completar la acción de jornada.';
    }
  }
}
