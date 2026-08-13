import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { DashboardMotivationResponse } from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  motivation(): Observable<DashboardMotivationResponse> {
    return this.http.get<DashboardMotivationResponse>(`${this.apiBaseUrl}/dashboard/motivation`);
  }
}
