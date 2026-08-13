import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GoalResponse } from '../../../../core/models/workworth-api.models';
import { GoalFormComponent } from './goal-form.component';

describe('GoalFormComponent', () => {
  let fixture: ComponentFixture<GoalFormComponent>;
  let component: GoalFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [GoalFormComponent] }).compileComponents();
    fixture = TestBed.createComponent(GoalFormComponent);
    component = fixture.componentInstance;
  });

  it('requires a title and a positive target with no more than two decimals', () => {
    component.form.setValue({ title: '', targetAmount: '0' });
    expect(component.form.invalid).toBe(true);

    component.form.setValue({ title: 'Viaje', targetAmount: '10.123' });
    expect(component.form.invalid).toBe(true);

    component.form.setValue({ title: 'Viaje', targetAmount: '10.50' });
    expect(component.form.valid).toBe(true);
  });

  it('preloads an active goal and emits only editable backend fields', () => {
    component.goal = goal();
    let emitted: unknown;
    component.submitted.subscribe((value) => emitted = value);

    component.submit();

    expect(component.form.getRawValue()).toEqual({ title: 'Viaje', targetAmount: '500' });
    expect(emitted).toEqual({ title: 'Viaje', targetAmount: 500 });
    expect(emitted).not.toHaveProperty('currencyCode');
    expect(emitted).not.toHaveProperty('progress');
  });
});

function goal(): GoalResponse {
  return {
    id: 1, title: 'Viaje', targetAmount: 500, currencyCode: 'EUR', status: 'ACTIVE',
    createdAt: '2026-08-13T10:00:00Z', updatedAt: '2026-08-13T10:00:00Z', closedAt: null,
    progress: { evaluable: true, progressAmount: 150, remainingAmount: 350, progressPercentage: 30, reached: false }
  };
}
