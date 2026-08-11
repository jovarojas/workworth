export type EarningStatus = 'AVAILABLE' | 'UNAVAILABLE';
export type EarningPeriod = 'TODAY' | 'WEEK' | 'MONTH' | 'ALL_TIME';
export type WorkdayStatus =
  | 'SCHEDULED'
  | 'ACTIVE'
  | 'ON_MEAL_BREAK'
  | 'COMPLETED'
  | 'CANCELLED';

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

export interface WorkdayResponse {
  id: number;
  localDate: string;
  timeZone: string;
  status: WorkdayStatus;
  scheduledStart: string;
  scheduledEnd: string;
  maximumEconomicSeconds: number;
  economicSeconds: number;
}
