import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import {
  CreateRewardRequest,
  EarningPeriod,
  RewardCombinationResponse,
  RewardRelevanceResponse,
  RewardResponse,
  RewardStatus
} from '../../../../core/models/workworth-api.models';
import { RewardsApiService } from '../../../../core/services/rewards-api.service';
import { RewardsPageComponent } from './rewards-page.component';

describe('RewardsPageComponent', () => {
  let fixture: ComponentFixture<RewardsPageComponent>;
  let component: RewardsPageComponent;
  const rewardsApi = {
    list: vi.fn(), get: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn(), acquire: vi.fn(),
    relevance: vi.fn(), relevantCombination: vi.fn(), combination: vi.fn()
  };

  const pendingReward = reward(1, 'Auriculares', 'PENDING', 1, 120);
  const secondPendingReward = reward(3, 'Libro', 'PENDING', 2, 30);
  const acquiredReward = reward(2, 'Cena', 'ACQUIRED', 1, 40);

  beforeEach(async () => {
    Object.values(rewardsApi).forEach((method) => method.mockReset());
    rewardsApi.list.mockImplementation((status?: RewardStatus) => of(status === 'PENDING' ? [pendingReward] : [acquiredReward]));
    rewardsApi.create.mockReturnValue(of(pendingReward));
    rewardsApi.update.mockReturnValue(of(pendingReward));
    rewardsApi.delete.mockReturnValue(of(void 0));
    rewardsApi.acquire.mockReturnValue(of(acquiredReward));
    rewardsApi.relevance.mockReturnValue(of(relevance(pendingReward.id)));
    rewardsApi.relevantCombination.mockReturnValue(of({ evaluable: true, combination: null }));
    rewardsApi.combination.mockReturnValue(of(noCombination('WEEK')));

    await TestBed.configureTestingModule({
      imports: [RewardsPageComponent],
      providers: [{ provide: RewardsApiService, useValue: rewardsApi }]
    }).compileComponents();

    fixture = TestBed.createComponent(RewardsPageComponent);
    component = fixture.componentInstance;
  });

  it('loads relevance once per pending reward and keeps the CRUD lists separated', () => {
    rewardsApi.list.mockImplementation((status?: RewardStatus) => of(status === 'PENDING'
      ? [pendingReward, secondPendingReward] : [acquiredReward]));
    rewardsApi.relevance.mockImplementation((id: number) => of(relevance(id)));
    fixture.detectChanges();

    expect(rewardsApi.relevance).toHaveBeenCalledTimes(2);
    expect(rewardsApi.relevance).toHaveBeenCalledWith(pendingReward.id);
    expect(rewardsApi.relevance).toHaveBeenCalledWith(secondPendingReward.id);
    expect(fixture.nativeElement.textContent).toContain('PENDIENTES');
    expect(fixture.nativeElement.textContent).toContain('CONSEGUIDAS');
  });

  it('presents an affordable relevance decided by the backend', () => {
    rewardsApi.relevance.mockReturnValue(of(relevance(pendingReward.id, { relevantContext: 'WEEK', outcome: 'AFFORDABLE' })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Puedes conseguirla con lo registrado esta semana.');
  });

  it('presents the backend shortfall without calculating it locally', () => {
    rewardsApi.relevance.mockReturnValue(of(relevance(pendingReward.id, {
      progressContext: 'TODAY', outcome: 'SHORTFALL', shortfall: 35, relevantContext: null
    })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Te faltan 35 EUR para conseguirla.');
  });

  it('highlights a newly reached reward without a previous context', () => {
    rewardsApi.relevance.mockReturnValue(of(relevance(pendingReward.id, { newlyReached: true, relevantContext: 'MONTH' })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ahora están a tu alcance');
    expect(fixture.nativeElement.textContent).toContain('Ahora puedes conseguir Auriculares (120 EUR).');
  });

  it('shows a context improvement for a newly reached reward', () => {
    rewardsApi.relevance.mockReturnValue(of(relevance(pendingReward.id, {
      newlyReached: true, relevantContext: 'WEEK', previousReachedContext: 'MONTH'
    })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Antes la alcanzabas este mes; ahora también la alcanzas esta semana.');
  });

  it('keeps an unevaluable reward visible with its contextual message', () => {
    rewardsApi.relevance.mockReturnValue(of(relevance(pendingReward.id, {
      evaluable: false, relevantContext: null, progressContext: null, outcome: null
    })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Auriculares');
    expect(fixture.nativeElement.textContent).toContain('Ahora mismo no podemos evaluar esta recompensa');
  });

  it('keeps a reward visible when its relevance request fails', () => {
    rewardsApi.relevance.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Auriculares');
    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('keeps several newly reached rewards in backend list order', () => {
    rewardsApi.list.mockImplementation((status?: RewardStatus) => of(status === 'PENDING'
      ? [pendingReward, secondPendingReward] : []));
    rewardsApi.relevance.mockImplementation((id: number) => of(relevance(id, { newlyReached: true, relevantContext: 'WEEK' })));
    fixture.detectChanges();

    expect(component.recentlyReached().map((reward) => reward.id)).toEqual([1, 3]);
  });

  it('shows a relevant combination supplied by the backend', () => {
    rewardsApi.relevantCombination.mockReturnValue(of({ evaluable: true, combination: combination('MONTH') }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Con lo registrado este mes puedes conseguir:');
    expect(fixture.nativeElement.textContent).toContain('Total:');
    expect(fixture.nativeElement.textContent).toContain('Disponible:');
  });

  it('distinguishes evaluable contexts with no relevant combination', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no hay una combinación de recompensas disponible.');
  });

  it('distinguishes all contexts being unavailable', () => {
    rewardsApi.relevantCombination.mockReturnValue(of({ evaluable: false, combination: null }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ahora mismo no podemos evaluar combinaciones');
  });

  it('requests another combination with the current context and visible reward ids', () => {
    rewardsApi.relevantCombination.mockReturnValue(of({ evaluable: true, combination: combination('WEEK') }));
    rewardsApi.combination.mockReturnValue(of(combination('WEEK', [reward(8, 'Museo', 'PENDING', 1, 25), reward(9, 'Cine', 'PENDING', 1, 20)])));
    fixture.detectChanges();

    component.requestAnotherCombination();

    expect(rewardsApi.combination).toHaveBeenCalledWith('WEEK', [1, 3]);
    expect(component.relevantCombination()?.rewards.map((reward) => reward.id)).toEqual([8, 9]);
  });

  it('keeps the visible combination when there is no alternative', () => {
    rewardsApi.relevantCombination.mockReturnValue(of({ evaluable: true, combination: combination('WEEK') }));
    rewardsApi.combination.mockReturnValue(of(noCombination('WEEK')));
    fixture.detectChanges();

    component.requestAnotherCombination();
    fixture.detectChanges();

    expect(component.relevantCombination()?.rewards.map((reward) => reward.id)).toEqual([1, 3]);
    expect(fixture.nativeElement.textContent).toContain('No hay otra combinación válida para este contexto.');
  });

  it('keeps reward lists visible when the relevant combination request fails', () => {
    rewardsApi.relevantCombination.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Auriculares');
    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  it('prevents duplicate requests while an alternative combination is pending', () => {
    const nextCombination = new Subject<RewardCombinationResponse>();
    rewardsApi.relevantCombination.mockReturnValue(of({ evaluable: true, combination: combination('WEEK') }));
    rewardsApi.combination.mockReturnValue(nextCombination);
    fixture.detectChanges();

    component.requestAnotherCombination();
    component.requestAnotherCombination();

    expect(rewardsApi.combination).toHaveBeenCalledTimes(1);
    nextCombination.complete();
  });

  it('keeps the post-acquisition pending list when an older pending response arrives late', () => {
    const olderPending = new Subject<RewardResponse[]>();
    let pendingCalls = 0;
    rewardsApi.list.mockImplementation((status?: RewardStatus) => {
      if (status === 'PENDING') {
        pendingCalls++;
        return pendingCalls === 1 ? olderPending : of([]);
      }
      return of([acquiredReward]);
    });
    fixture.detectChanges();

    component.acquire(pendingReward);
    olderPending.next([pendingReward]);

    expect(component.pending()).toEqual([]);
  });

  it('keeps the post-deletion pending list when an older pending response arrives late', () => {
    const olderPending = new Subject<RewardResponse[]>();
    let pendingCalls = 0;
    rewardsApi.list.mockImplementation((status?: RewardStatus) => {
      if (status === 'PENDING') {
        pendingCalls++;
        return pendingCalls === 1 ? olderPending : of([]);
      }
      return of([acquiredReward]);
    });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fixture.detectChanges();

    component.confirmDelete(pendingReward);
    olderPending.next([pendingReward]);

    expect(component.pending()).toEqual([]);
  });

  it('keeps the post-acquisition acquired list when an older acquired response arrives late', () => {
    const olderAcquired = new Subject<RewardResponse[]>();
    let acquiredCalls = 0;
    rewardsApi.list.mockImplementation((status?: RewardStatus) => {
      if (status === 'ACQUIRED') {
        acquiredCalls++;
        return acquiredCalls === 1 ? olderAcquired : of([acquiredReward]);
      }
      return of([]);
    });
    fixture.detectChanges();

    component.acquire(pendingReward);
    olderAcquired.next([]);

    expect(component.acquired()).toEqual([acquiredReward]);
  });

  it('ignores an older pending error after a newer pending response succeeds', () => {
    const olderPending = new Subject<RewardResponse[]>();
    let pendingCalls = 0;
    rewardsApi.list.mockImplementation((status?: RewardStatus) => {
      if (status === 'PENDING') {
        pendingCalls++;
        return pendingCalls === 1 ? olderPending : of([]);
      }
      return of([]);
    });
    fixture.detectChanges();

    component.loadPending();
    olderPending.error(new HttpErrorResponse({ status: 0 }));

    expect(component.pendingError()).toBeNull();
  });

  it('keeps the newest relevant combination when an older response arrives late', () => {
    const olderCombination = new Subject<{ evaluable: boolean; combination: RewardCombinationResponse | null }>();
    const newerCombination = new Subject<{ evaluable: boolean; combination: RewardCombinationResponse | null }>();
    let requests = 0;
    rewardsApi.relevantCombination.mockImplementation(() => ++requests === 1 ? olderCombination : newerCombination);
    fixture.detectChanges();

    component.loadRelevantCombination();
    newerCombination.next({ evaluable: true, combination: combination('MONTH') });
    olderCombination.next({ evaluable: true, combination: combination('WEEK') });

    expect(component.relevantCombination()?.context).toBe('MONTH');
  });

  it('keeps the newest relevance for a reward when an older response arrives late', () => {
    const olderRelevance = new Subject<RewardRelevanceResponse>();
    const newerRelevance = new Subject<RewardRelevanceResponse>();
    let relevanceCalls = 0;
    rewardsApi.relevance.mockImplementation(() => ++relevanceCalls === 1 ? olderRelevance : newerRelevance);
    fixture.detectChanges();

    component.loadPending();
    newerRelevance.next(relevance(pendingReward.id, { relevantContext: 'MONTH', outcome: 'AFFORDABLE' }));
    olderRelevance.next(relevance(pendingReward.id, { relevantContext: 'WEEK', outcome: 'AFFORDABLE' }));

    expect(component.relevanceByRewardId()[pendingReward.id].relevantContext).toBe('MONTH');
  });

  it('still refreshes CRUD lists after an acquisition', () => {
    fixture.detectChanges();
    component.acquire(pendingReward);

    expect(rewardsApi.acquire).toHaveBeenCalledWith(pendingReward.id);
    expect(rewardsApi.list).toHaveBeenCalledTimes(4);
  });
});

function reward(id: number, name: string, status: RewardStatus, quantity: number, price: number): RewardResponse {
  return {
    id, name, status, quantity, price, currencyCode: 'EUR', lastReachedContext: null,
    createdAt: '2026-08-13T09:00:00Z', updatedAt: '2026-08-13T09:00:00Z'
  };
}

function relevance(rewardId: number, changes: Partial<RewardRelevanceResponse> = {}): RewardRelevanceResponse {
  return {
    rewardId, evaluable: true, relevantContext: 'WEEK', progressContext: null, outcome: 'AFFORDABLE',
    availableAmount: 140, price: 120, currencyCode: 'EUR', surplus: 20, shortfall: null,
    newlyReached: false, previousReachedContext: null, ...changes
  };
}

function combination(context: EarningPeriod, rewards: RewardResponse[] = [
  reward(1, 'Auriculares', 'PENDING', 1, 60), reward(3, 'Libro', 'PENDING', 2, 30)
]): RewardCombinationResponse {
  return { context, evaluable: true, availableAmount: 120, totalPrice: 90, currencyCode: 'EUR', rewards };
}

function noCombination(context: EarningPeriod): RewardCombinationResponse {
  return { context, evaluable: true, availableAmount: 120, totalPrice: null, currencyCode: 'EUR', rewards: [] };
}
