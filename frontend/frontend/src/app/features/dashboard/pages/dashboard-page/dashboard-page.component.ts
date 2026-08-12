import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize, Subscription, timer } from 'rxjs';
import { EarningPeriodResponse, EarningProjectionResponse, WorkdayResponse, WorkdayStatus } from '../../../../core/models/workworth-api.models';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [CommonModule, CurrencyPipe, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss'
})
export class DashboardPageComponent implements OnInit {
  private readonly earnings = inject(EarningsApiService);
  private readonly workdays = inject(WorkdayApiService);
  private readonly destroyRef = inject(DestroyRef);
  private pollingSubscription?: Subscription;

  readonly projection = signal<EarningProjectionResponse | null>(null);
  readonly today = signal<EarningPeriodResponse | null>(null);
  readonly week = signal<EarningPeriodResponse | null>(null);
  readonly month = signal<EarningPeriodResponse | null>(null);
  readonly allTime = signal<EarningPeriodResponse | null>(null);
  readonly workday = signal<WorkdayResponse | null>(null);

  readonly projectionLoading = signal(true);
  readonly todayLoading = signal(true);
  readonly weekLoading = signal(true);
  readonly monthLoading = signal(true);
  readonly allTimeLoading = signal(true);
  readonly workdayLoading = signal(true);

  readonly projectionError = signal<string | null>(null);
  readonly todayError = signal<string | null>(null);
  readonly weekError = signal<string | null>(null);
  readonly monthError = signal<string | null>(null);
  readonly allTimeError = signal<string | null>(null);
  readonly workdayError = signal<string | null>(null);
  readonly workdayMissing = signal(false);

  readonly isUnavailable = computed(() => this.projection()?.status === 'UNAVAILABLE');

  ngOnInit(): void {
    this.load();
  }

  load(showLoading = true): void {
    this.loadProjection(showLoading);
    this.loadPeriod('TODAY', showLoading);
    this.loadPeriod('WEEK', showLoading);
    this.loadPeriod('MONTH', showLoading);
    this.loadPeriod('ALL_TIME', showLoading);
    this.loadWorkday(showLoading);
  }

  workdayStatusLabel(status: WorkdayStatus): string {
    return {
      SCHEDULED: 'Programada',
      ACTIVE: 'En curso',
      ON_MEAL_BREAK: 'En pausa de comida',
      COMPLETED: 'Finalizada',
      CANCELLED: 'Cancelada'
    }[status];
  }

  private loadProjection(showLoading: boolean): void {
    if (showLoading) {
      this.projectionLoading.set(true);
    }
    this.projectionError.set(null);

    this.earnings.currentProjection()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.projectionLoading.set(false))
      )
      .subscribe({
        next: (projection) => this.projection.set(projection),
        error: (error: unknown) => this.projectionError.set(this.errorMessage(error, 'la ganancia de hoy'))
      });
  }

  private loadPeriod(context: EarningPeriodResponse['context'], showLoading: boolean): void {
    const periodState = {
      TODAY: { loading: this.todayLoading, response: this.today, error: this.todayError, label: 'el resumen de hoy' },
      WEEK: { loading: this.weekLoading, response: this.week, error: this.weekError, label: 'el resumen semanal' },
      MONTH: { loading: this.monthLoading, response: this.month, error: this.monthError, label: 'el resumen mensual' },
      ALL_TIME: { loading: this.allTimeLoading, response: this.allTime, error: this.allTimeError, label: 'el acumulado histórico' }
    }[context];

    if (showLoading) {
      periodState.loading.set(true);
    }
    periodState.error.set(null);

    this.earnings.period(context)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => periodState.loading.set(false))
      )
      .subscribe({
        next: (period) => periodState.response.set(period),
        error: (error: unknown) => periodState.error.set(this.errorMessage(error, periodState.label))
      });
  }

  private loadWorkday(showLoading: boolean): void {
    if (showLoading) {
      this.workdayLoading.set(true);
    }
    this.workdayError.set(null);
    this.workdayMissing.set(false);

    this.workdays.current()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.workdayLoading.set(false))
      )
      .subscribe({
        next: (workday) => {
          this.workday.set(workday);
          this.configurePolling(workday.status);
        },
        error: (error: unknown) => {
          this.workday.set(null);
          this.stopPolling();
          if (error instanceof HttpErrorResponse && error.status === 404) {
            this.workdayMissing.set(true);
            return;
          }
          this.workdayError.set(this.errorMessage(error, 'la jornada de hoy'));
        }
      });
  }

  private configurePolling(status: WorkdayStatus): void {
    if (status !== 'ACTIVE') {
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

  private stopPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = undefined;
  }

  private errorMessage(error: unknown, resource: string): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return `No se puede conectar con WorkWorth para cargar ${resource}.`;
    }
    return `No se ha podido cargar ${resource}. Inténtalo de nuevo más tarde.`;
  }
}
