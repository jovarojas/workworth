import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { MealBreakResponse, WorkdayResponse } from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class WorkdayApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  current(): Observable<WorkdayResponse> {
    return this.http.get<WorkdayResponse>(`${this.apiBaseUrl}/workdays/current`);
  }

  startMealBreak(localDate: string): Observable<MealBreakResponse> {
    return this.http.post<MealBreakResponse>(`${this.apiBaseUrl}/workdays/${localDate}/meal-breaks/start`, null);
  }

  endMealBreak(localDate: string, mealBreakId: number): Observable<MealBreakResponse> {
    return this.http.post<MealBreakResponse>(`${this.apiBaseUrl}/workdays/${localDate}/meal-breaks/${mealBreakId}/end`, null);
  }

  cancel(localDate: string): Observable<void> {
    return this.http.post<void>(`${this.apiBaseUrl}/workdays/${localDate}/cancel`, null);
  }
}
