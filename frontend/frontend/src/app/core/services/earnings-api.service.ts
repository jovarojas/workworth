import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  EarningCorrectionResponse,
  EarningHistoryResponse,
  EarningPeriod,
  EarningPeriodResponse,
  EarningProjectionResponse,
  EarningResponse
} from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class EarningsApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  currentProjection(): Observable<EarningProjectionResponse> {
    return this.http.get<EarningProjectionResponse>(
      `${this.apiBaseUrl}/earnings/current/projection`
    );
  }

  period(context: EarningPeriod): Observable<EarningPeriodResponse> {
    return this.http.get<EarningPeriodResponse>(`${this.apiBaseUrl}/earnings/periods/${context}`);
  }

  history(page: number, size: number): Observable<EarningHistoryResponse> {
    return this.http.get<EarningHistoryResponse>(`${this.apiBaseUrl}/earnings/history`, {
      params: { page, size }
    });
  }

  workday(date: string): Observable<EarningResponse> {
    return this.http.get<EarningResponse>(`${this.apiBaseUrl}/earnings/workdays/${date}`);
  }

  corrections(date: string): Observable<EarningCorrectionResponse[]> {
    return this.http.get<EarningCorrectionResponse[]>(`${this.apiBaseUrl}/earnings/workdays/${date}/corrections`);
  }
}
