import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { CreateRewardRequest, RewardResponse, RewardStatus } from '../../../../core/models/workworth-api.models';
import { RewardsApiService } from '../../../../core/services/rewards-api.service';
import { RewardsPageComponent } from './rewards-page.component';

describe('RewardsPageComponent', () => {
  let fixture: ComponentFixture<RewardsPageComponent>;
  let component: RewardsPageComponent;
  const rewardsApi = {
    list: vi.fn(),
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    acquire: vi.fn()
  };

  const pendingReward = reward(1, 'Auriculares', 'PENDING', 1, 120);
  const acquiredReward = reward(2, 'Libro', 'ACQUIRED', 2, 30);

  beforeEach(async () => {
    Object.values(rewardsApi).forEach((method) => method.mockReset());
    rewardsApi.list.mockImplementation((status?: RewardStatus) => of(status === 'PENDING' ? [pendingReward] : [acquiredReward]));
    rewardsApi.create.mockReturnValue(of(pendingReward));
    rewardsApi.update.mockReturnValue(of(pendingReward));
    rewardsApi.delete.mockReturnValue(of(void 0));
    rewardsApi.acquire.mockReturnValue(of(acquiredReward));

    await TestBed.configureTestingModule({
      imports: [RewardsPageComponent],
      providers: [{ provide: RewardsApiService, useValue: rewardsApi }]
    }).compileComponents();

    fixture = TestBed.createComponent(RewardsPageComponent);
    component = fixture.componentInstance;
  });

  it('loads and keeps pending and acquired rewards in separate sections', () => {
    fixture.detectChanges();

    expect(rewardsApi.list).toHaveBeenCalledWith('PENDING');
    expect(rewardsApi.list).toHaveBeenCalledWith('ACQUIRED');
    expect(fixture.nativeElement.textContent).toContain('PENDIENTES');
    expect(fixture.nativeElement.textContent).toContain('CONSEGUIDAS');
    expect(fixture.nativeElement.textContent).toContain('Auriculares');
    expect(fixture.nativeElement.textContent).toContain('2 Libro');
  });

  it('shows independent empty states', () => {
    rewardsApi.list.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aún no tienes recompensas pendientes.');
    expect(fixture.nativeElement.textContent).toContain('Aún no has marcado recompensas como conseguidas.');
  });

  it('creates a reward and refreshes both lists', () => {
    fixture.detectChanges();
    const request: CreateRewardRequest = { name: 'Cena', quantity: 1, price: 50 };

    component.saveReward(request);

    expect(rewardsApi.create).toHaveBeenCalledWith(request);
    expect(rewardsApi.list).toHaveBeenCalledTimes(4);
    expect(component.actionSuccess()).toBe('Recompensa añadida.');
  });

  it('updates the selected pending reward', () => {
    fixture.detectChanges();
    const request: CreateRewardRequest = { name: 'Auriculares nuevos', quantity: 1, price: 130 };

    component.edit(pendingReward);
    component.saveReward(request);

    expect(rewardsApi.update).toHaveBeenCalledWith(pendingReward.id, request);
    expect(component.actionSuccess()).toBe('Recompensa actualizada.');
  });

  it('does not delete when confirmation is declined', () => {
    fixture.detectChanges();
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    component.confirmDelete(pendingReward);

    expect(rewardsApi.delete).not.toHaveBeenCalled();
  });

  it('deletes after confirmation and refreshes both lists', () => {
    fixture.detectChanges();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.confirmDelete(pendingReward);

    expect(rewardsApi.delete).toHaveBeenCalledWith(pendingReward.id);
    expect(rewardsApi.list).toHaveBeenCalledTimes(4);
  });

  it('acquires a reward and refreshes the pending and acquired lists', () => {
    fixture.detectChanges();

    component.acquire(pendingReward);

    expect(rewardsApi.acquire).toHaveBeenCalledWith(pendingReward.id);
    expect(rewardsApi.list).toHaveBeenCalledTimes(4);
    expect(component.actionSuccess()).toBe('Recompensa marcada como conseguida.');
  });

  it('does not render mutable actions for acquired rewards', () => {
    rewardsApi.list.mockImplementation((status?: RewardStatus) => of(status === 'PENDING' ? [] : [acquiredReward]));
    fixture.detectChanges();

    const acquiredSection = fixture.nativeElement.querySelector(
      'section[aria-labelledby="acquired-heading"]'
    ) as HTMLElement;
    expect(acquiredSection.textContent).not.toContain('Editar');
    expect(acquiredSection.textContent).not.toContain('Eliminar');
    expect(acquiredSection.textContent).not.toContain('Marcar como conseguida');
  });

  it('keeps acquired rewards visible when loading pending rewards fails', () => {
    rewardsApi.list.mockImplementation((status?: RewardStatus) => status === 'PENDING'
      ? throwError(() => new HttpErrorResponse({ status: 0 }))
      : of([acquiredReward]));
    fixture.detectChanges();

    expect(component.pending()).toEqual([]);
    expect(component.acquired()).toEqual([acquiredReward]);
    expect(component.pendingError()).toContain('No se puede conectar');
  });

  it('keeps an action disabled while its request is pending', () => {
    const deletion = new Subject<void>();
    rewardsApi.delete.mockReturnValue(deletion);
    fixture.detectChanges();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.confirmDelete(pendingReward);

    expect(component.isActionInProgress(pendingReward.id)).toBe(true);
    component.confirmDelete(pendingReward);
    expect(rewardsApi.delete).toHaveBeenCalledTimes(1);
    deletion.complete();
  });
});

function reward(
  id: number,
  name: string,
  status: RewardStatus,
  quantity: number,
  price: number
): RewardResponse {
  return {
    id,
    name,
    quantity,
    price,
    currencyCode: 'EUR',
    status,
    lastReachedContext: null,
    createdAt: '2026-08-12T09:00:00Z',
    updatedAt: '2026-08-12T09:00:00Z'
  };
}
