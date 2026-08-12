import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize, Observable } from 'rxjs';
import { problemDetailFrom } from '../../../../core/http/problem-detail';
import {
  CreateRewardRequest,
  RewardResponse
} from '../../../../core/models/workworth-api.models';
import { RewardsApiService } from '../../../../core/services/rewards-api.service';
import { RewardFormComponent } from '../../components/reward-form/reward-form.component';

@Component({
  selector: 'app-rewards-page',
  imports: [
    CommonModule,
    CurrencyPipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RewardFormComponent
  ],
  templateUrl: './rewards-page.component.html',
  styleUrl: './rewards-page.component.scss'
})
export class RewardsPageComponent implements OnInit {
  private readonly rewards = inject(RewardsApiService);

  @ViewChild(RewardFormComponent) private rewardForm?: RewardFormComponent;

  readonly pending = signal<RewardResponse[]>([]);
  readonly acquired = signal<RewardResponse[]>([]);
  readonly pendingLoading = signal(true);
  readonly acquiredLoading = signal(true);
  readonly pendingError = signal<string | null>(null);
  readonly acquiredError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly actionSuccess = signal<string | null>(null);
  readonly editingReward = signal<RewardResponse | null>(null);
  readonly activeAction = signal<string | null>(null);

  readonly formSaving = computed(() => this.activeAction() === 'form');

  ngOnInit(): void {
    this.loadPending();
    this.loadAcquired();
  }

  loadPending(showLoading = true): void {
    if (showLoading) {
      this.pendingLoading.set(true);
    }
    this.pendingError.set(null);
    this.rewards.list('PENDING')
      .pipe(finalize(() => this.pendingLoading.set(false)))
      .subscribe({
        next: (rewards) => this.pending.set(rewards),
        error: (error: unknown) => this.pendingError.set(this.errorMessage(error, 'las recompensas pendientes'))
      });
  }

  loadAcquired(showLoading = true): void {
    if (showLoading) {
      this.acquiredLoading.set(true);
    }
    this.acquiredError.set(null);
    this.rewards.list('ACQUIRED')
      .pipe(finalize(() => this.acquiredLoading.set(false)))
      .subscribe({
        next: (rewards) => this.acquired.set(rewards),
        error: (error: unknown) => this.acquiredError.set(this.errorMessage(error, 'las recompensas conseguidas'))
      });
  }

  saveReward(request: CreateRewardRequest): void {
    if (this.formSaving()) {
      return;
    }

    this.activeAction.set('form');
    this.actionError.set(null);
    this.actionSuccess.set(null);
    const editing = this.editingReward();
    const action = editing ? this.rewards.update(editing.id, request) : this.rewards.create(request);

    action.pipe(finalize(() => this.activeAction.set(null))).subscribe({
      next: () => {
        this.actionSuccess.set(editing ? 'Recompensa actualizada.' : 'Recompensa añadida.');
        this.editingReward.set(null);
        this.rewardForm?.reset();
        this.refreshLists();
      },
      error: (error: unknown) => this.actionError.set(this.errorMessage(
        error, editing ? 'No se ha podido actualizar la recompensa.' : 'No se ha podido añadir la recompensa.'
      ))
    });
  }

  edit(reward: RewardResponse): void {
    this.actionError.set(null);
    this.actionSuccess.set(null);
    this.editingReward.set(reward);
  }

  cancelEdit(): void {
    this.editingReward.set(null);
    this.rewardForm?.reset();
  }

  confirmDelete(reward: RewardResponse): void {
    if (this.isActionInProgress(reward.id)
      || !window.confirm(`¿Eliminar la recompensa “${this.rewardLabel(reward)}”?`)) {
      return;
    }

    this.runAction(reward.id, this.rewards.delete(reward.id), 'Recompensa eliminada.', 'No se ha podido eliminar la recompensa.');
  }

  acquire(reward: RewardResponse): void {
    if (this.isActionInProgress(reward.id)) {
      return;
    }

    this.runAction(reward.id, this.rewards.acquire(reward.id), 'Recompensa marcada como conseguida.',
      'No se ha podido marcar la recompensa como conseguida.');
  }

  isActionInProgress(id: number): boolean {
    return this.activeAction() === String(id);
  }

  rewardLabel(reward: RewardResponse): string {
    return reward.quantity > 1 ? `${reward.quantity} ${reward.name}` : reward.name;
  }

  private runAction(id: number, action: Observable<unknown>, success: string, failure: string): void {
    this.activeAction.set(String(id));
    this.actionError.set(null);
    this.actionSuccess.set(null);
    action.pipe(finalize(() => this.activeAction.set(null))).subscribe({
      next: () => {
        this.actionSuccess.set(success);
        if (this.editingReward()?.id === id) {
          this.cancelEdit();
        }
        this.refreshLists();
      },
      error: (error: unknown) => this.actionError.set(this.errorMessage(error, failure))
    });
  }

  private refreshLists(): void {
    this.loadPending(false);
    this.loadAcquired(false);
  }

  private errorMessage(error: unknown, fallback: string): string {
    const detail = problemDetailFrom(error)?.detail;
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth. Inténtalo de nuevo más tarde.';
    }
    return fallback;
  }
}
