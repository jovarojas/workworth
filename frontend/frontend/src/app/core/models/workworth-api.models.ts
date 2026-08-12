export type EarningStatus = 'AVAILABLE' | 'UNAVAILABLE';
export type EarningPeriod = 'TODAY' | 'WEEK' | 'MONTH' | 'ALL_TIME';
export type WorkdayStatus =
  | 'SCHEDULED'
  | 'ACTIVE'
  | 'ON_MEAL_BREAK'
  | 'COMPLETED'
  | 'CANCELLED';

export type IncomeSource = 'NET_MONTHLY_REAL' | 'ESTIMATED_NET' | 'UNAVAILABLE';
export type EstimatorStatus = 'NOT_IMPLEMENTED' | 'MISSING_REQUIRED_INPUT' | 'AVAILABLE';
export type ApiErrorCode =
  | 'VALIDATION_ERROR'
  | 'RESOURCE_NOT_FOUND'
  | 'SALARY_PROFILE_CONFLICT'
  | 'SALARY_RATE_UNAVAILABLE'
  | 'SALARY_CONFIGURATION_INCOMPLETE'
  | 'WORKDAY_CONFLICT'
  | 'WORKDAY_INTERVAL_INVALID';

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  code?: ApiErrorCode;
  fieldErrors?: Record<string, string>;
}

export interface CreateSalaryProfileRequest {
  effectiveFrom: string;
  netMonthlyReal: number;
  currencyCode: string;
  payPeriods: number;
}

export interface SalaryProfileResponse {
  id: number;
  effectiveFrom: string;
  grossAnnual: number | null;
  netMonthlyReal: number | null;
  netAnnualReal: number | null;
  currencyCode: string;
  payPeriods: number;
  activeIncomeSource: IncomeSource;
  estimatorStatus: EstimatorStatus;
}

export interface CurrentSalaryProfileResponse {
  month: string;
  salaryProfile: SalaryProfileResponse;
}

export interface MonthlySalaryRateResponse {
  month: string;
  incomeSource: IncomeSource;
  monthlyNetIncome: number;
  standardEconomicHours: number;
  hourlyNetRate: number;
  currencyCode: string;
}

export interface EstimatorStatusResponse {
  fiscalYear: number;
  status: EstimatorStatus;
  requiredInputs: string[];
}

export interface EarningProjectionResponse {
  localDate: string;
  status: EarningStatus;
  economicSeconds: number;
  amount: number | null;
  currencyCode: string | null;
  unavailableReason: string | null;
}

export interface EarningPeriodResponse {
  context: EarningPeriod;
  startDate: string | null;
  endDate: string | null;
  amount: number;
  currencyCode: string;
}

export interface MealBreakResponse {
  id: number;
  startedAt: string;
  endedAt: string | null;
  endedAutomatically: boolean;
}

export interface PartialAbsenceResponse {
  id: number;
  startedAt: string;
  endedAt: string;
  reason: string | null;
}

export interface WorkdayResponse {
  id: number;
  localDate: string;
  timeZone: string;
  status: WorkdayStatus;
  scheduledStart: string;
  scheduledEnd: string;
  maximumEconomicSeconds: number;
  economicSeconds: number;
  mealBreaks: MealBreakResponse[];
  partialAbsences: PartialAbsenceResponse[];
}
