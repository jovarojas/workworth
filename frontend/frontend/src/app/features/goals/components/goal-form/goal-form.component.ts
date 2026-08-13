import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CreateGoalRequest, GoalResponse } from '../../../../core/models/workworth-api.models';

const nonBlank: ValidatorFn = (control): ValidationErrors | null =>
  String(control.value ?? '').trim() ? null : { required: true };

const moneyScale: ValidatorFn = (control): ValidationErrors | null =>
  /^\d+(?:\.\d{1,2})?$/.test(String(control.value ?? '')) && Number(control.value) > 0
    ? null
    : { moneyScale: true };

@Component({
  selector: 'app-goal-form',
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './goal-form.component.html',
  styleUrl: './goal-form.component.scss'
})
export class GoalFormComponent {
  private currentGoal: GoalResponse | null = null;

  @Input() saving = false;
  @Input() set goal(goal: GoalResponse | null) {
    this.currentGoal = goal;
    this.reset(goal);
  }
  @Output() submitted = new EventEmitter<CreateGoalRequest>();
  @Output() cancelled = new EventEmitter<void>();

  readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [nonBlank] }),
    targetAmount: new FormControl('', { nonNullable: true, validators: [moneyScale] })
  });

  get editing(): boolean {
    return this.currentGoal !== null;
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitted.emit({ title: value.title.trim(), targetAmount: Number(value.targetAmount) });
  }

  cancel(): void {
    this.cancelled.emit();
  }

  reset(goal: GoalResponse | null = null): void {
    this.currentGoal = goal;
    this.form.reset({ title: goal?.title ?? '', targetAmount: goal ? String(goal.targetAmount) : '' });
  }

  controlError(controlName: 'title' | 'targetAmount'): string | null {
    const control = this.form.controls[controlName];
    if (!control.touched || control.valid) {
      return null;
    }
    return controlName === 'title'
      ? 'Indica un título para el objetivo.'
      : 'Introduce un importe objetivo positivo con un máximo de dos decimales.';
  }
}
