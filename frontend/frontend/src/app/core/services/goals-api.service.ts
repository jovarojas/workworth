import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  CreateGoalRequest,
  GoalResponse,
  UpdateGoalRequest
} from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class GoalsApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  active(): Observable<GoalResponse[]> {
    return this.http.get<GoalResponse[]>(`${this.apiBaseUrl}/goals`);
  }

  history(): Observable<GoalResponse[]> {
    return this.http.get<GoalResponse[]>(`${this.apiBaseUrl}/goals/history`);
  }

  create(request: CreateGoalRequest): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(`${this.apiBaseUrl}/goals`, request);
  }

  update(id: number, request: UpdateGoalRequest): Observable<GoalResponse> {
    return this.http.put<GoalResponse>(`${this.apiBaseUrl}/goals/${id}`, request);
  }

  complete(id: number): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(`${this.apiBaseUrl}/goals/${id}/complete`, null);
  }

  cancel(id: number): Observable<GoalResponse> {
    return this.http.post<GoalResponse>(`${this.apiBaseUrl}/goals/${id}/cancel`, null);
  }
}
