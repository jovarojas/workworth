import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  EarningPeriod,
  EarningPeriodResponse,
  EarningProjectionResponse
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

  period(context: Extract<EarningPeriod, 'WEEK' | 'MONTH'>): Observable<EarningPeriodResponse> {
    return this.http.get<EarningPeriodResponse>(`${this.apiBaseUrl}/earnings/periods/${context}`);
  }
}
