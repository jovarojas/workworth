import { TestBed } from '@angular/core/testing';
import { RewardResponse } from '../../../../core/models/workworth-api.models';
import { RewardFormComponent } from './reward-form.component';

describe('RewardFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RewardFormComponent] }).compileComponents();
  });

  it('defaults quantity to one and emits only the reward request fields', () => {
    const fixture = TestBed.createComponent(RewardFormComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.patchValue({ name: 'Libro', price: '20.00' });
    component.submit();

    expect(component.form.controls.quantity.value).toBe(1);
    expect(submitted).toHaveBeenCalledWith({ name: 'Libro', quantity: 1, price: 20 });
    expect(submitted.mock.calls[0][0].currencyCode).toBeUndefined();
  });

  it.each([
    [{ name: '', quantity: 1, price: '20.00' }],
    [{ name: 'Libro', quantity: 0, price: '20.00' }],
    [{ name: 'Libro', quantity: 1, price: '' }],
    [{ name: 'Libro', quantity: 1, price: '0' }],
    [{ name: 'Libro', quantity: 1, price: '20.999' }]
  ])('rejects invalid local form values', (value) => {
    const fixture = TestBed.createComponent(RewardFormComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const submitted = vi.fn();
    component.submitted.subscribe(submitted);

    component.form.patchValue(value);
    component.submit();

    expect(submitted).not.toHaveBeenCalled();
    expect(component.form.invalid).toBe(true);
  });

  it('preloads a reward for editing', () => {
    const fixture = TestBed.createComponent(RewardFormComponent);
    const component = fixture.componentInstance;
    component.reward = reward();
    fixture.detectChanges();

    expect(component.editing).toBe(true);
    expect(component.form.getRawValue()).toEqual({ name: 'Auriculares', quantity: 2, price: '120' });
  });

  function reward(): RewardResponse {
    return {
      id: 1, name: 'Auriculares', quantity: 2, price: 120, currencyCode: 'EUR', status: 'PENDING',
      lastReachedContext: null, createdAt: '2026-08-12T10:00:00Z', updatedAt: '2026-08-12T10:00:00Z'
    };
  }
});
