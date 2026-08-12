import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  ApplicationCurrencyResponse,
  UpdateApplicationCurrencyRequest
} from '../models/workworth-api.models';

@Injectable({ providedIn: 'root' })
export class PreferencesApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  currency(): Observable<ApplicationCurrencyResponse> {
    return this.http.get<ApplicationCurrencyResponse>(`${this.apiBaseUrl}/application-settings/currency`);
  }

  updateCurrency(request: UpdateApplicationCurrencyRequest): Observable<ApplicationCurrencyResponse> {
    return this.http.put<ApplicationCurrencyResponse>(`${this.apiBaseUrl}/application-settings/currency`, request);
  }
}
