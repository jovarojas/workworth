import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { problemDetailFrom } from '../../../../core/http/problem-detail';
import { EarningHistoryResponse } from '../../../../core/models/workworth-api.models';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';

const HISTORY_PAGE_SIZE = 20;

@Component({
  selector: 'app-earnings-history',
  imports: [CommonModule, CurrencyPipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './earnings-history.component.html',
  styleUrl: './earnings-history.component.scss'
})
export class EarningsHistoryComponent implements OnInit {
  private readonly earnings = inject(EarningsApiService);

  readonly history = signal<EarningHistoryResponse | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.earnings.history(page, HISTORY_PAGE_SIZE)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (history) => this.history.set(history),
        error: (error: unknown) => this.error.set(this.errorMessage(error))
      });
  }

  previousPage(): void {
    const current = this.history();
    if (current?.hasPrevious) {
      this.loadPage(current.page - 1);
    }
  }

  nextPage(): void {
    const current = this.history();
    if (current?.hasNext) {
      this.loadPage(current.page + 1);
    }
  }

  private errorMessage(error: unknown): string {
    const detail = problemDetailFrom(error)?.detail;
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth para cargar el historial de ganancias.';
    }
    return 'No se ha podido cargar el historial de ganancias. Inténtalo de nuevo más tarde.';
  }
}
