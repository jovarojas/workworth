import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { problemDetailFrom } from '../../../../core/http/problem-detail';
import { EarningCorrectionResponse, EarningResponse } from '../../../../core/models/workworth-api.models';
import { EarningsApiService } from '../../../../core/services/earnings-api.service';

@Component({
  selector: 'app-earning-detail',
  imports: [CommonModule, CurrencyPipe, MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './earning-detail.component.html',
  styleUrl: './earning-detail.component.scss'
})
export class EarningDetailComponent implements OnInit {
  private readonly earnings = inject(EarningsApiService);
  private readonly route = inject(ActivatedRoute);

  readonly earning = signal<EarningResponse | null>(null);
  readonly corrections = signal<EarningCorrectionResponse[]>([]);
  readonly loading = signal(true);
  readonly correctionsLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly correctionsError = signal<string | null>(null);
  readonly date = signal<string | null>(null);

  ngOnInit(): void {
    const date = this.route.snapshot.paramMap.get('date');
    this.date.set(date);
    if (date) {
      this.loadEarning(date);
    } else {
      this.loading.set(false);
      this.error.set('La fecha de la jornada no es válida.');
    }
  }

  loadEarning(date: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.earning.set(null);
    this.corrections.set([]);
    this.correctionsError.set(null);

    this.earnings.workday(date)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (earning) => {
          this.earning.set(earning);
          this.loadCorrections(date);
        },
        error: (error: unknown) => this.error.set(this.earningErrorMessage(error))
      });
  }

  private loadCorrections(date: string): void {
    this.correctionsLoading.set(true);
    this.correctionsError.set(null);

    this.earnings.corrections(date)
      .pipe(finalize(() => this.correctionsLoading.set(false)))
      .subscribe({
        next: (corrections) => this.corrections.set(corrections),
        error: (error: unknown) => this.correctionsError.set(this.correctionsErrorMessage(error))
      });
  }

  private earningErrorMessage(error: unknown): string {
    const detail = problemDetailFrom(error)?.detail;
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 404) {
      return 'No existe una ganancia materializada para esta jornada.';
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth para cargar esta ganancia.';
    }
    return 'No se ha podido cargar la ganancia de esta jornada. Inténtalo de nuevo más tarde.';
  }

  private correctionsErrorMessage(error: unknown): string {
    const detail = problemDetailFrom(error)?.detail;
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth para cargar las correcciones.';
    }
    return 'No se han podido cargar las correcciones de esta jornada.';
  }
}
