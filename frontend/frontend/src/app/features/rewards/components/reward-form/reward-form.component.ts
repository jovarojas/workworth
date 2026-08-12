import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CreateRewardRequest, RewardResponse } from '../../../../core/models/workworth-api.models';

const nonBlank: ValidatorFn = (control): ValidationErrors | null =>
  String(control.value ?? '').trim() ? null : { required: true };

const positiveInteger: ValidatorFn = (control): ValidationErrors | null =>
  Number.isInteger(Number(control.value)) && Number(control.value) > 0 ? null : { positiveInteger: true };

const moneyScale: ValidatorFn = (control): ValidationErrors | null =>
  /^\d+(?:\.\d{1,2})?$/.test(String(control.value ?? '')) && Number(control.value) > 0
    ? null
    : { moneyScale: true };

@Component({
  selector: 'app-reward-form',
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './reward-form.component.html',
  styleUrl: './reward-form.component.scss'
})
export class RewardFormComponent {
  private currentReward: RewardResponse | null = null;

  @Input() saving = false;
  @Input() set reward(reward: RewardResponse | null) {
    this.currentReward = reward;
    this.reset(reward);
  }
  @Output() submitted = new EventEmitter<CreateRewardRequest>();
  @Output() cancelled = new EventEmitter<void>();

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [nonBlank] }),
    quantity: new FormControl(1, { nonNullable: true, validators: [positiveInteger] }),
    price: new FormControl('', { nonNullable: true, validators: [moneyScale] })
  });

  get editing(): boolean {
    return this.currentReward !== null;
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      name: value.name.trim(),
      quantity: Number(value.quantity),
      price: Number(value.price)
    });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  reset(reward: RewardResponse | null = null): void {
    this.currentReward = reward;
    this.form.reset({
      name: reward?.name ?? '',
      quantity: reward?.quantity ?? 1,
      price: reward ? String(reward.price) : ''
    });
  }

  controlError(controlName: 'name' | 'quantity' | 'price'): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    if (controlName === 'name') {
      return 'Indica un nombre para la recompensa.';
    }
    if (controlName === 'quantity') {
      return 'La cantidad debe ser un entero positivo.';
    }
    return 'Introduce un precio total positivo con un máximo de dos decimales.';
  }
}
