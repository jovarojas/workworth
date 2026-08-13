import { CommonModule, CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize, Observable } from 'rxjs';
import { problemDetailMessage } from '../../../../core/http/problem-detail';
import { earningPeriodContextLabel, rewardStatusLabel } from '../../../../core/presentation/display-labels';
import {
  CreateRewardRequest,
  RewardCombinationResponse,
  RewardRelevanceResponse,
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
  private pendingRequestGeneration = 0;
  private acquiredRequestGeneration = 0;
  private relevanceRequestGeneration = 0;
  private combinationRequestGeneration = 0;

  @ViewChild(RewardFormComponent) private rewardForm?: RewardFormComponent;

  readonly pending = signal<RewardResponse[]>([]);
  readonly acquired = signal<RewardResponse[]>([]);
  readonly relevanceByRewardId = signal<Record<number, RewardRelevanceResponse>>({});
  readonly relevanceErrors = signal<Record<number, string>>({});
  readonly relevanceLoadingIds = signal<Set<number>>(new Set());
  readonly relevantCombination = signal<RewardCombinationResponse | null>(null);
  readonly combinationEvaluable = signal<boolean | null>(null);
  readonly combinationLoading = signal(true);
  readonly combinationError = signal<string | null>(null);
  readonly otherCombinationUnavailable = signal(false);

  readonly pendingLoading = signal(true);
  readonly acquiredLoading = signal(true);
  readonly pendingError = signal<string | null>(null);
  readonly acquiredError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly actionSuccess = signal<string | null>(null);
  readonly editingReward = signal<RewardResponse | null>(null);
  readonly activeAction = signal<string | null>(null);

  readonly formSaving = computed(() => this.activeAction() === 'form');
  readonly recentlyReached = computed(() => this.pending().filter((reward) =>
    this.relevanceByRewardId()[reward.id]?.newlyReached
  ));

  ngOnInit(): void {
    this.loadPending();
    this.loadAcquired();
    this.loadRelevantCombination();
  }

  loadPending(showLoading = true): void {
    const requestGeneration = ++this.pendingRequestGeneration;
    this.invalidateRelevances();
    if (showLoading) {
      this.pendingLoading.set(true);
    }
    this.pendingError.set(null);
    this.rewards.list('PENDING')
      .pipe(finalize(() => {
        if (requestGeneration === this.pendingRequestGeneration) {
          this.pendingLoading.set(false);
        }
      }))
      .subscribe({
        next: (rewards) => {
          if (requestGeneration !== this.pendingRequestGeneration) {
            return;
          }
          this.pending.set(rewards);
          this.loadRelevances(rewards);
        },
        error: (error: unknown) => {
          if (requestGeneration === this.pendingRequestGeneration) {
            this.pendingError.set(this.errorMessage(error, 'las recompensas pendientes'));
          }
        }
      });
  }

  loadAcquired(showLoading = true): void {
    const requestGeneration = ++this.acquiredRequestGeneration;
    if (showLoading) {
      this.acquiredLoading.set(true);
    }
    this.acquiredError.set(null);
    this.rewards.list('ACQUIRED')
      .pipe(finalize(() => {
        if (requestGeneration === this.acquiredRequestGeneration) {
          this.acquiredLoading.set(false);
        }
      }))
      .subscribe({
        next: (rewards) => {
          if (requestGeneration === this.acquiredRequestGeneration) {
            this.acquired.set(rewards);
          }
        },
        error: (error: unknown) => {
          if (requestGeneration === this.acquiredRequestGeneration) {
            this.acquiredError.set(this.errorMessage(error, 'las recompensas conseguidas'));
          }
        }
      });
  }

  loadRelevantCombination(): void {
    const requestGeneration = ++this.combinationRequestGeneration;
    this.combinationLoading.set(true);
    this.combinationError.set(null);
    this.otherCombinationUnavailable.set(false);
    this.rewards.relevantCombination()
      .pipe(finalize(() => {
        if (requestGeneration === this.combinationRequestGeneration) {
          this.combinationLoading.set(false);
        }
      }))
      .subscribe({
        next: (response) => {
          if (requestGeneration !== this.combinationRequestGeneration) {
            return;
          }
          this.combinationEvaluable.set(response.evaluable);
          this.relevantCombination.set(this.isVisibleCombination(response.combination) ? response.combination : null);
        },
        error: (error: unknown) => {
          if (requestGeneration === this.combinationRequestGeneration) {
            this.combinationError.set(this.errorMessage(error, 'No se ha podido consultar la combinación de recompensas.'));
          }
        }
      });
  }

  requestAnotherCombination(): void {
    const combination = this.relevantCombination();
    if (!combination || this.combinationLoading()) {
      return;
    }

    const requestGeneration = ++this.combinationRequestGeneration;
    this.combinationLoading.set(true);
    this.combinationError.set(null);
    this.otherCombinationUnavailable.set(false);
    this.rewards.combination(combination.context, combination.rewards.map((reward) => reward.id))
      .pipe(finalize(() => {
        if (requestGeneration === this.combinationRequestGeneration) {
          this.combinationLoading.set(false);
        }
      }))
      .subscribe({
        next: (alternative) => {
          if (requestGeneration !== this.combinationRequestGeneration) {
            return;
          }
          if (this.isVisibleCombination(alternative)) {
            this.relevantCombination.set(alternative);
          } else {
            this.otherCombinationUnavailable.set(true);
          }
        },
        error: (error: unknown) => {
          if (requestGeneration === this.combinationRequestGeneration) {
            this.combinationError.set(this.errorMessage(error, 'No se ha podido buscar otra combinación.'));
          }
        }
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
        this.refreshRewards();
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

  rewardStatusLabel(status: RewardResponse['status']): string {
    return rewardStatusLabel(status);
  }

  contextLabel(context: RewardCombinationResponse['context']): string {
    return earningPeriodContextLabel(context);
  }

  relevanceMessage(reward: RewardResponse): string | null {
    const relevance = this.relevanceByRewardId()[reward.id];
    if (!relevance) {
      return null;
    }
    if (!relevance.evaluable) {
      return 'Ahora mismo no podemos evaluar esta recompensa con las ganancias registradas.';
    }
    if (relevance.newlyReached && relevance.relevantContext) {
      if (relevance.previousReachedContext) {
        return `Antes la alcanzabas ${earningPeriodContextLabel(relevance.previousReachedContext)}; ahora también la alcanzas ${earningPeriodContextLabel(relevance.relevantContext)}.`;
      }
      return `Ahora puedes conseguir ${this.rewardLabel(reward)} (${relevance.price} ${relevance.currencyCode}).`;
    }
    if (relevance.outcome === 'AFFORDABLE' && relevance.relevantContext) {
      return `Puedes conseguirla con lo registrado ${earningPeriodContextLabel(relevance.relevantContext)}.`;
    }
    if (relevance.outcome === 'SHORTFALL' && relevance.shortfall !== null) {
      return `Te faltan ${relevance.shortfall} ${relevance.currencyCode} para conseguirla.`;
    }
    return null;
  }

  private loadRelevances(rewards: RewardResponse[]): void {
    const requestGeneration = this.relevanceRequestGeneration;
    const ids = new Set(rewards.map((reward) => reward.id));
    this.relevanceByRewardId.set(Object.fromEntries(
      Object.entries(this.relevanceByRewardId()).filter(([id]) => ids.has(Number(id)))
    ));
    this.relevanceErrors.set(Object.fromEntries(
      Object.entries(this.relevanceErrors()).filter(([id]) => ids.has(Number(id)))
    ));
    rewards.forEach((reward) => this.loadRelevance(reward, requestGeneration));
  }

  private loadRelevance(reward: RewardResponse, requestGeneration: number): void {
    this.relevanceLoadingIds.update((ids) => new Set(ids).add(reward.id));
    this.relevanceErrors.update((errors) => {
      const { [reward.id]: _, ...remaining } = errors;
      return remaining;
    });
    this.rewards.relevance(reward.id)
      .pipe(finalize(() => this.relevanceLoadingIds.update((ids) => {
        if (requestGeneration !== this.relevanceRequestGeneration) {
          return ids;
        }
        const next = new Set(ids);
        next.delete(reward.id);
        return next;
      })))
      .subscribe({
        next: (relevance) => {
          if (requestGeneration === this.relevanceRequestGeneration) {
            this.relevanceByRewardId.update((all) => ({ ...all, [reward.id]: relevance }));
          }
        },
        error: (error: unknown) => {
          if (requestGeneration === this.relevanceRequestGeneration) {
            this.relevanceErrors.update((all) => ({
              ...all,
              [reward.id]: this.errorMessage(error, 'No se ha podido actualizar la relevancia de esta recompensa.')
            }));
          }
        }
      });
  }

  private invalidateRelevances(): void {
    this.relevanceRequestGeneration++;
    this.relevanceByRewardId.set({});
    this.relevanceErrors.set({});
    this.relevanceLoadingIds.set(new Set());
  }

  private isVisibleCombination(combination: RewardCombinationResponse | null): combination is RewardCombinationResponse {
    return combination !== null && combination.evaluable && combination.rewards.length >= 2;
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
        this.refreshRewards();
      },
      error: (error: unknown) => this.actionError.set(this.errorMessage(error, failure))
    });
  }

  private refreshRewards(): void {
    this.loadPending(false);
    this.loadAcquired(false);
    this.loadRelevantCombination();
  }

  private errorMessage(error: unknown, fallback: string): string {
    const detail = problemDetailMessage(error);
    if (detail) {
      return detail;
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'No se puede conectar con WorkWorth. Inténtalo de nuevo más tarde.';
    }
    return fallback;
  }
}
