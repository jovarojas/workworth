import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  CreateRewardRequest,
  RewardResponse,
  RewardStatus,
  UpdateRewardRequest
} from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class RewardsApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  list(status?: RewardStatus): Observable<RewardResponse[]> {
    const options = status ? { params: new HttpParams().set('status', status) } : {};
    return this.http.get<RewardResponse[]>(`${this.apiBaseUrl}/rewards`, options);
  }

  get(id: number): Observable<RewardResponse> {
    return this.http.get<RewardResponse>(`${this.apiBaseUrl}/rewards/${id}`);
  }

  create(request: CreateRewardRequest): Observable<RewardResponse> {
    return this.http.post<RewardResponse>(`${this.apiBaseUrl}/rewards`, request);
  }

  update(id: number, request: UpdateRewardRequest): Observable<RewardResponse> {
    return this.http.put<RewardResponse>(`${this.apiBaseUrl}/rewards/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/rewards/${id}`);
  }

  acquire(id: number): Observable<RewardResponse> {
    return this.http.post<RewardResponse>(`${this.apiBaseUrl}/rewards/${id}/acquire`, null);
  }
}
