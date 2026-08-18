import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { StatisticsGranularity, StatisticsResponse } from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class StatisticsApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  statistics(granularity: StatisticsGranularity, from?: string, to?: string): Observable<StatisticsResponse> {
    let params = new HttpParams().set('granularity', granularity);
    if (from !== undefined) {
      params = params.set('from', from);
    }
    if (to !== undefined) {
      params = params.set('to', to);
    }
    return this.http.get<StatisticsResponse>(`${this.apiBaseUrl}/statistics`, { params });
  }
}
