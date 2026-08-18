import {
  earningCorrectionCauseLabel,
  earningPeriodLabel,
  earningStatusLabel,
  earningUnavailableReasonLabel,
  estimatorStatusLabel,
  goalStatusLabel,
  incomeSourceLabel,
  rewardStatusLabel,
  statisticsGranularityLabel,
  workdayStatusLabel
} from './display-labels';

describe('display labels', () => {
  it('translates user-visible states and periods without changing their internal values', () => {
    expect(earningPeriodLabel('WEEK')).toBe('Esta semana');
    expect(earningStatusLabel('AVAILABLE')).toBe('Disponible');
    expect(rewardStatusLabel('PENDING')).toBe('Pendiente');
    expect(rewardStatusLabel('ACQUIRED')).toBe('Conseguida');
    expect(goalStatusLabel('COMPLETED')).toBe('Completado');
    expect(workdayStatusLabel('ON_MEAL_BREAK')).toBe('En pausa');
    expect(statisticsGranularityLabel('YEAR')).toBe('Año');
  });

  it('translates technical salary and earnings codes for presentation', () => {
    expect(incomeSourceLabel('NET_MONTHLY_REAL')).toBe('Ganancias netas mensuales reales');
    expect(estimatorStatusLabel('NOT_IMPLEMENTED')).toBe('No disponible');
    expect(earningUnavailableReasonLabel('SALARY_RATE_UNAVAILABLE')).toBe('No se puede calcular la tarifa salarial.');
    expect(earningCorrectionCauseLabel('WORKDAY_CANCELLED')).toBe('Jornada cancelada');
  });
});
