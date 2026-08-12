import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';
import {
  CurrentSalaryProfileResponse,
  EstimatorStatusResponse,
  MonthlySalaryRateResponse,
  ProblemDetail,
  SalaryProfileResponse
} from '../../../../core/models/workworth-api.models';
import { SalaryApiService } from '../../../../core/services/salary-api.service';

const firstDayOfMonth: ValidatorFn = (control): ValidationErrors | null =>
  /^\d{4}-\d{2}-01$/.test(control.value ?? '') ? null : { firstDayOfMonth: true };

const moneyScale: ValidatorFn = (control): ValidationErrors | null =>
  /^\d+(?:\.\d{1,2})?$/.test(control.value ?? '') && Number(control.value) >= 0.01
    ? null
    : { moneyScale: true };

@Component({
  selector: 'app-salary-profile',
  imports: [
    CommonModule,
    CurrencyPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './salary-profile.component.html',
  styleUrl: './salary-profile.component.scss'
})
export class SalaryProfileComponent implements OnInit {
  private readonly salaries = inject(SalaryApiService);

  readonly profile = signal<SalaryProfileResponse | null>(null);
  readonly profileMonth = signal<string | null>(null);
  readonly rate = signal<MonthlySalaryRateResponse | null>(null);
  readonly estimator = signal<EstimatorStatusResponse | null>(null);

  readonly loadingProfile = signal(true);
  readonly loadingRate = signal(false);
  readonly loadingEstimator = signal(true);
  readonly saving = signal(false);

  readonly profileMissing = signal(false);
  readonly profileError = signal<string | null>(null);
  readonly rateError = signal<string | null>(null);
  readonly estimatorError = signal<string | null>(null);
  readonly submitError = signal<string | null>(null);
  readonly fieldErrors = signal<Record<string, string>>({});
  readonly submitted = signal(false);

  readonly estimatorNotImplemented = computed(() => this.estimator()?.status === 'NOT_IMPLEMENTED');

  readonly form = new FormGroup({
    effectiveFrom: new FormControl(this.currentMonthFirstDay(), {
      nonNullable: true,
      validators: [Validators.required, firstDayOfMonth]
    }),
    netMonthlyReal: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, moneyScale]
    }),
    currencyCode: new FormControl('EUR', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]
    }),
    payPeriods: new FormControl(12, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(12), Validators.max(12)]
    })
  });

  ngOnInit(): void {
    this.loadCurrentProfile();
    this.loadEstimatorStatus();
  }

  loadCurrentProfile(): void {
    this.loadingProfile.set(true);
    this.profileError.set(null);
    this.profileMissing.set(false);

    this.salaries.current()
      .pipe(finalize(() => this.loadingProfile.set(false)))
      .subscribe({
        next: (current) => this.showProfile(current),
        error: (error: unknown) => {
          this.profile.set(null);
          this.rate.set(null);
          if (this.isNotFound(error)) {
            this.profileMissing.set(true);
            return;
          }
          this.profileError.set(this.errorDetail(error, 'No se ha podido cargar el perfil salarial vigente.'));
        }
      });
  }

  loadEstimatorStatus(): void {
    this.loadingEstimator.set(true);
    this.estimatorError.set(null);

    this.salaries.estimatorStatus()
      .pipe(finalize(() => this.loadingEstimator.set(false)))
      .subscribe({
        next: (estimator) => this.estimator.set(estimator),
        error: (error: unknown) => this.estimatorError.set(
          this.errorDetail(error, 'No se ha podido consultar el estado del estimador fiscal.')
        )
      });
  }

  submit(): void {
    this.submitted.set(true);
    this.submitError.set(null);
    this.fieldErrors.set({});

    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.saving.set(true);
    this.salaries.create({
      effectiveFrom: value.effectiveFrom,
      netMonthlyReal: Number(value.netMonthlyReal),
      currencyCode: value.currencyCode,
      payPeriods: value.payPeriods
    })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (profile) => {
          this.profileMissing.set(false);
          this.profile.set(profile);
          this.profileMonth.set(profile.effectiveFrom.slice(0, 7));
          this.loadRate(profile.effectiveFrom.slice(0, 7));
        },
        error: (error: unknown) => this.handleSubmissionError(error)
      });
  }

  controlError(controlName: 'effectiveFrom' | 'netMonthlyReal' | 'currencyCode' | 'payPeriods'): string | null {
    const control = this.form.controls[controlName];
    const backendError = this.fieldErrors()[controlName];
    if (backendError) {
      return backendError;
    }
    if (!control.touched && !this.submitted()) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('moneyScale')) {
      return 'Introduce un importe positivo con un máximo de dos decimales.';
    }
    if (control.hasError('firstDayOfMonth')) {
      return 'La fecha debe ser el primer día del mes.';
    }
    if (control.invalid) {
      return 'El valor no es válido.';
    }
    return null;
  }

  private showProfile(current: CurrentSalaryProfileResponse): void {
    this.profile.set(current.salaryProfile);
    this.profileMonth.set(current.month);
    this.loadRate(current.month);
  }

  private loadRate(month: string): void {
    this.loadingRate.set(true);
    this.rateError.set(null);
    this.rate.set(null);

    this.salaries.rate(month)
      .pipe(finalize(() => this.loadingRate.set(false)))
      .subscribe({
        next: (rate) => this.rate.set(rate),
        error: (error: unknown) => this.rateError.set(
          this.errorDetail(error, 'La tarifa mensual no está disponible.')
        )
      });
  }

  private handleSubmissionError(error: unknown): void {
    const problem = this.problemDetail(error);
    this.fieldErrors.set(problem?.fieldErrors ?? {});
    this.submitError.set(this.errorDetail(error, 'No se ha podido guardar el perfil salarial.'));
  }

  private isNotFound(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 404 && this.problemDetail(error)?.code === 'RESOURCE_NOT_FOUND';
  }

  private errorDetail(error: unknown, fallback: string): string {
    const detail = this.problemDetail(error)?.detail;
    return detail || fallback;
  }

  private problemDetail(error: unknown): ProblemDetail | null {
    if (!(error instanceof HttpErrorResponse) || !error.error || typeof error.error !== 'object') {
      return null;
    }
    return error.error as ProblemDetail;
  }

  private currentMonthFirstDay(): string {
    const current = new Date();
    const year = current.getFullYear();
    const month = String(current.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}-01`;
  }
}
