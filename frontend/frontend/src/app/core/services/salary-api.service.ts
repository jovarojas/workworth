import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  CreateSalaryProfileRequest,
  CurrentSalaryProfileResponse,
  EstimatorStatusResponse,
  MonthlySalaryRateResponse,
  SalaryProfileResponse
} from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class SalaryApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  current(month?: string): Observable<CurrentSalaryProfileResponse> {
    const options = month ? { params: { month } } : {};
    return this.http.get<CurrentSalaryProfileResponse>(`${this.apiBaseUrl}/salary-profiles/current`, options);
  }

  create(request: CreateSalaryProfileRequest): Observable<SalaryProfileResponse> {
    return this.http.post<SalaryProfileResponse>(`${this.apiBaseUrl}/salary-profiles`, request);
  }

  rate(month: string): Observable<MonthlySalaryRateResponse> {
    return this.http.get<MonthlySalaryRateResponse>(`${this.apiBaseUrl}/salary-rates/${month}`);
  }

  estimatorStatus(year?: number): Observable<EstimatorStatusResponse> {
    const options = year ? { params: { year } } : {};
    return this.http.get<EstimatorStatusResponse>(`${this.apiBaseUrl}/salary-estimator/status`, options);
  }
}
