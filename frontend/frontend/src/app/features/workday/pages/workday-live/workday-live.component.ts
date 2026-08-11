import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize, Subscription, timer } from 'rxjs';
import { WorkdayResponse, WorkdayStatus } from '../../../../core/models/workworth-api.models';
import { WorkdayApiService } from '../../../../core/services/workday-api.service';

@Component({
  selector: 'app-workday-live',
  imports: [CommonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
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

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth. Comprueba que el backend está en ejecución.';
    }
    return 'No se ha podido cargar la jornada actual. Inténtalo de nuevo más tarde.';
  }
}
