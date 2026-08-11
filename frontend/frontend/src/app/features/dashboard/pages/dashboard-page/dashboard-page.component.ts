import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin, Subscription, timer } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';
import {
  EarningPeriodResponse,
  EarningProjectionResponse,
  WorkdayResponse,
  WorkdayStatus
} from '../../../../core/models/workworth-api.models';

interface DashboardData {
  projection: EarningProjectionResponse;
  week: EarningPeriodResponse;
  month: EarningPeriodResponse;
  workday: WorkdayResponse;
}

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

  readonly data = signal<DashboardData | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly isUnavailable = computed(() => this.data()?.projection.status === 'UNAVAILABLE');

  ngOnInit(): void {
    this.load();
  }

  load(showLoading = true): void {
    if (showLoading) {
      this.loading.set(true);
    }
    this.error.set(null);

    forkJoin({
      projection: this.earnings.currentProjection(),
      week: this.earnings.period('WEEK'),
      month: this.earnings.period('MONTH'),
      workday: this.workdays.current()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.data.set(data);
          this.loading.set(false);
          this.configurePolling(data.workday.status);
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.stopPolling();
          this.error.set(this.errorMessage(error));
        }
      });
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

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return 'No se puede conectar con WorkWorth. Comprueba que el backend está en ejecución.';
      }
      if (error.status === 404) {
        return 'No hay una jornada disponible para hoy.';
      }
    }
    return 'No se ha podido cargar el Dashboard. Inténtalo de nuevo más tarde.';
  }
}
