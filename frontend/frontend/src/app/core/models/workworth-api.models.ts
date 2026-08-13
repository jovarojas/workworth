export type EarningStatus = 'AVAILABLE' | 'UNAVAILABLE';
export type EarningPeriod = 'TODAY' | 'WEEK' | 'MONTH' | 'ALL_TIME';
export type ApplicationCurrency = 'EUR' | 'USD';
export type RewardStatus = 'PENDING' | 'ACQUIRED';
export type GoalStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type RewardOutcome = 'AFFORDABLE' | 'SHORTFALL';
export type DashboardMotivationState = 'EMPTY' | 'AVAILABLE' | 'PROGRESS' | 'UNAVAILABLE';
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
  | 'APPLICATION_CURRENCY_LOCKED'
  | 'GOAL_CONFLICT'
  | 'GOAL_PROGRESS_UNAVAILABLE'
  | 'GOAL_CURRENCY_MISMATCH'
  | 'REWARD_CONFLICT'
  | 'REWARD_CURRENCY_MISMATCH'
  | 'WORKDAY_CONFLICT'
  | 'WORKDAY_INTERVAL_INVALID';

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  code?: ApiErrorCode;
  fieldErrors?: Record<string, string>;
}

export interface ApplicationCurrencyResponse {
  currencyCode: ApplicationCurrency;
  changeAllowed: boolean;
}

export interface UpdateApplicationCurrencyRequest {
  currencyCode: ApplicationCurrency;
}

export interface RewardResponse {
  id: number;
  name: string;
  quantity: number;
  price: number;
  currencyCode: ApplicationCurrency;
  status: RewardStatus;
  lastReachedContext: EarningPeriod | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRewardRequest {
  name: string;
  quantity: number;
  price: number;
}

export interface UpdateRewardRequest {
  name: string;
  quantity: number;
  price: number;
}

export interface GoalProgressResponse {
  evaluable: boolean;
  progressAmount: number | null;
  remainingAmount: number | null;
  progressPercentage: number | null;
  reached: boolean | null;
}

export interface GoalResponse {
  id: number;
  title: string;
  targetAmount: number;
  currencyCode: ApplicationCurrency;
  status: GoalStatus;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
  progress: GoalProgressResponse | null;
}

export interface CreateGoalRequest {
  title: string;
  targetAmount: number;
}

export interface UpdateGoalRequest {
  title: string;
  targetAmount: number;
}

export interface RewardRelevanceResponse {
  rewardId: number;
  evaluable: boolean;
  relevantContext: EarningPeriod | null;
  progressContext: EarningPeriod | null;
  outcome: RewardOutcome | null;
  availableAmount: number | null;
  price: number;
  currencyCode: ApplicationCurrency;
  surplus: number | null;
  shortfall: number | null;
  newlyReached: boolean;
  previousReachedContext: EarningPeriod | null;
}

export interface RewardCombinationResponse {
  context: EarningPeriod;
  evaluable: boolean;
  availableAmount: number | null;
  totalPrice: number | null;
  currencyCode: ApplicationCurrency;
  rewards: RewardResponse[];
}

export interface RewardCombinationRelevanceResponse {
  evaluable: boolean;
  combination: RewardCombinationResponse | null;
}

export interface DashboardRewardResponse {
  id: number;
  name: string;
  quantity: number;
  price: number;
  currencyCode: ApplicationCurrency;
  status: RewardStatus;
}

export interface DashboardPrimaryRewardResponse {
  reward: DashboardRewardResponse;
  evaluable: boolean;
  relevantContext: EarningPeriod | null;
  progressContext: EarningPeriod | null;
  outcome: RewardOutcome;
  availableAmount: number;
  surplus: number | null;
  shortfall: number | null;
}

export interface DashboardCombinationResponse {
  context: EarningPeriod;
  availableAmount: number;
  totalPrice: number;
  currencyCode: ApplicationCurrency;
  rewards: DashboardRewardResponse[];
}

export interface DashboardMotivationResponse {
  state: DashboardMotivationState;
  primaryReward: DashboardPrimaryRewardResponse | null;
  combination: DashboardCombinationResponse | null;
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
  status: EarningStatus;
  amount: number | null;
  currencyCode: string | null;
}

export interface EarningResponse {
  localDate: string;
  status: EarningStatus;
  unavailableReason: string | null;
  amount: number | null;
  currencyCode: string | null;
  economicSeconds: number;
}

export interface EarningHistoryResponse {
  items: EarningResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface EarningCorrectionResponse {
  sequence: number;
  cause: 'WORKDAY_CANCELLED' | 'PARTIAL_ABSENCE_CHANGED' | 'MEAL_BREAK_CHANGED';
  previousEconomicSeconds: number;
  newEconomicSeconds: number;
  previousAmount: number | null;
  newAmount: number | null;
  correctedAt: string;
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

export interface CreatePartialAbsenceRequest {
  startedAt: string;
  endedAt: string;
  reason?: string | null;
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
