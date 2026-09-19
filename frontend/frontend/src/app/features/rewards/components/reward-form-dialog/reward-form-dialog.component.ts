import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs';
import { problemDetailMessage } from '../../../../core/http/problem-detail';
import { CreateRewardRequest, RewardResponse } from '../../../../core/models/workworth-api.models';
import { RewardsApiService } from '../../../../core/services/rewards-api.service';
import { RewardFormComponent } from '../reward-form/reward-form.component';

export interface RewardFormDialogData {
  reward: RewardResponse | null;
}

/**
 * Hosts app-reward-form inside a Material dialog so adding or editing a reward no longer
 * requires scrolling to the bottom of the rewards page. Owns the actual create/update call
 * itself (rather than just re-emitting the form value) so it can stay open and show the error
 * inline when saving fails, and only close once the reward is actually saved.
 */
@Component({
  selector: 'app-reward-form-dialog',
  imports: [CommonModule, MatButtonModule, MatDialogModule, MatIconModule, RewardFormComponent],
  templateUrl: './reward-form-dialog.component.html',
  styleUrl: './reward-form-dialog.component.scss'
})
export class RewardFormDialogComponent {
  private readonly rewards = inject(RewardsApiService);
  private readonly dialogRef = inject(MatDialogRef<RewardFormDialogComponent, boolean>);
  readonly data = inject<RewardFormDialogData>(MAT_DIALOG_DATA);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly editing = this.data.reward !== null;

  save(request: CreateRewardRequest): void {
    if (this.saving()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const editingReward = this.data.reward;
    const action = editingReward ? this.rewards.update(editingReward.id, request) : this.rewards.create(request);

    action.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => this.dialogRef.close(true),
      error: (error: unknown) => this.error.set(this.errorMessage(error))
    });
  }

  close(): void {
    if (this.saving()) {
      return;
    }
    this.dialogRef.close(false);
  }

  private errorMessage(error: unknown): string {
    const detail = problemDetailMessage(error);
    if (detail) {
      return detail;
    }
    return this.editing ? 'No se ha podido actualizar la recompensa.' : 'No se ha podido añadir la recompensa.';
  }
}
