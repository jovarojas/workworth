import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../models/workworth-api.models';

export function problemDetailFrom(error: unknown): ProblemDetail | null {
  if (!(error instanceof HttpErrorResponse) || !error.error || typeof error.error !== 'object') {
    return null;
  }

  return error.error as ProblemDetail;
}

export function problemDetailMessage(error: unknown): string | null {
  const code = problemDetailFrom(error)?.code;
  if (!code) {
    return null;
  }

  return {
    VALIDATION_ERROR: 'Revisa los datos introducidos.',
    RESOURCE_NOT_FOUND: 'No se ha encontrado la información solicitada.',
    SALARY_PROFILE_CONFLICT: 'Ya existe un perfil salarial para ese mes.',
    SALARY_RATE_UNAVAILABLE: 'No se puede calcular la tarifa salarial.',
    SALARY_CONFIGURATION_INCOMPLETE: 'La configuración salarial está incompleta.',
    APPLICATION_CURRENCY_LOCKED: 'La moneda no puede cambiarse porque ya existen datos económicos registrados.',
    GOAL_CONFLICT: 'Esta operación no es posible para el estado actual del objetivo.',
    GOAL_PROGRESS_UNAVAILABLE: 'No se puede calcular el progreso del objetivo en este momento.',
    GOAL_CURRENCY_MISMATCH: 'La moneda del objetivo no coincide con la moneda global.',
    STATISTICS_CURRENCY_MISMATCH: 'No se pueden mostrar estadísticas con monedas distintas.',
    REWARD_CONFLICT: 'Esta operación no es posible para el estado actual de la recompensa.',
    REWARD_CURRENCY_MISMATCH: 'La moneda de la recompensa no coincide con la moneda global.',
    WORKDAY_CONFLICT: 'Esta operación no es posible para el estado actual de la jornada.',
    WORKDAY_INTERVAL_INVALID: 'El intervalo indicado no es válido.'
  }[code] ?? 'No se ha podido completar la operación.';
}
