import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { WorkdayResponse } from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class WorkdayApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  current(): Observable<WorkdayResponse> {
    return this.http.get<WorkdayResponse>(`${this.apiBaseUrl}/workdays/current`);
  }
}
