import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { finalize } from 'rxjs';
import { problemDetailFrom, problemDetailMessage } from '../../../../core/http/problem-detail';
import {
  ApplicationCurrency,
  ApplicationCurrencyResponse
} from '../../../../core/models/workworth-api.models';
import { PreferencesApiService } from '../../../../core/services/preferences-api.service';

@Component({
  selector: 'app-currency-settings',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './currency-settings.component.html',
  styleUrl: './currency-settings.component.scss'
})
export class CurrencySettingsComponent implements OnInit {
  private readonly preferences = inject(PreferencesApiService);

  readonly settings = signal<ApplicationCurrencyResponse | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly saveError = signal<string | null>(null);
  readonly saved = signal(false);

  readonly currency = new FormControl<ApplicationCurrency>({ value: 'EUR', disabled: true }, {
    nonNullable: true,
    validators: [Validators.required]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.saveError.set(null);
    this.saved.set(false);
    this.currency.disable({ emitEvent: false });

    this.preferences.currency()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (settings) => this.showSettings(settings),
        error: (error: unknown) => this.error.set(this.loadErrorMessage(error))
      });
  }

  save(): void {
    const settings = this.settings();
    if (!settings || !settings.changeAllowed || this.saving() || this.currency.invalid) {
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);
    this.saved.set(false);
    this.preferences.updateCurrency({ currencyCode: this.currency.getRawValue() })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => {
          this.showSettings(updated);
          this.saved.set(true);
        },
        error: (error: unknown) => this.saveError.set(this.saveErrorMessage(error))
      });
  }

  private showSettings(settings: ApplicationCurrencyResponse): void {
    this.settings.set(settings);
    this.currency.setValue(settings.currencyCode, { emitEvent: false });
    if (settings.changeAllowed) {
      this.currency.enable({ emitEvent: false });
    } else {
      this.currency.disable({ emitEvent: false });
    }
  }

  private loadErrorMessage(error: unknown): string {
    const detail = problemDetailMessage(error);
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth para consultar la moneda global.';
    }
    return 'No se ha podido cargar la configuración de moneda.';
  }

  private saveErrorMessage(error: unknown): string {
    const problem = problemDetailFrom(error);
    if (problem?.code === 'APPLICATION_CURRENCY_LOCKED') {
      return problemDetailMessage(error) || 'La moneda está bloqueada porque ya existen datos económicos.';
    }
    return this.loadErrorMessage(error);
  }
}
