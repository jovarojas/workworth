import {
  EarningPeriod,
  EarningStatus,
  EstimatorStatus,
  GoalStatus,
  IncomeSource,
  RewardStatus,
  StatisticsGranularity,
  WorkdayStatus
} from '../models/workworth-api.models';

export function earningStatusLabel(status: EarningStatus): string {
  return status === 'AVAILABLE' ? 'Disponible' : 'No disponible';
}

export function earningPeriodLabel(period: EarningPeriod): string {
  return {
    TODAY: 'Hoy',
    WEEK: 'Esta semana',
    MONTH: 'Este mes',
    ALL_TIME: 'Todo el historial'
  }[period];
}

export function earningPeriodContextLabel(period: EarningPeriod): string {
  return {
    TODAY: 'hoy',
    WEEK: 'esta semana',
    MONTH: 'este mes',
    ALL_TIME: 'en todo el historial'
  }[period];
}

export function rewardStatusLabel(status: RewardStatus): string {
  return status === 'PENDING' ? 'Pendiente' : 'Conseguida';
}

export function goalStatusLabel(status: GoalStatus): string {
  return {
    ACTIVE: 'Activo',
    COMPLETED: 'Completado',
    CANCELLED: 'Cancelado'
  }[status];
}

export function workdayStatusLabel(status: WorkdayStatus): string {
  return {
    SCHEDULED: 'Programada',
    ACTIVE: 'En curso',
    ON_MEAL_BREAK: 'En pausa',
    COMPLETED: 'Finalizada',
    CANCELLED: 'Cancelada'
  }[status];
}

export function statisticsGranularityLabel(granularity: StatisticsGranularity): string {
  return {
    DAY: 'Día',
    WEEK: 'Semana',
    MONTH: 'Mes',
    YEAR: 'Año'
  }[granularity];
}

export function incomeSourceLabel(source: IncomeSource): string {
  return {
    NET_MONTHLY_REAL: 'Ganancias netas mensuales reales',
    ESTIMATED_NET: 'Estimación de ganancias netas',
    UNAVAILABLE: 'No disponible'
  }[source];
}

export function estimatorStatusLabel(status: EstimatorStatus): string {
  return {
    NOT_IMPLEMENTED: 'No disponible',
    MISSING_REQUIRED_INPUT: 'Faltan datos necesarios',
    AVAILABLE: 'Disponible'
  }[status];
}

export function earningUnavailableReasonLabel(reason: string | null): string | null {
  if (!reason) {
    return null;
  }
  return {
    SALARY_PROFILE_NOT_FOUND: 'No hay un perfil salarial disponible.',
    SALARY_CONFIGURATION_INCOMPLETE: 'La configuración salarial está incompleta.',
    SALARY_RATE_UNAVAILABLE: 'No se puede calcular la tarifa salarial.'
  }[reason] ?? 'No se puede calcular esta ganancia con la información disponible.';
}

export function earningCorrectionCauseLabel(cause: string): string {
  return {
    WORKDAY_CANCELLED: 'Jornada cancelada',
    PARTIAL_ABSENCE_CHANGED: 'Ausencia parcial modificada',
    MEAL_BREAK_CHANGED: 'Pausa modificada'
  }[cause] ?? 'Corrección de jornada';
}
