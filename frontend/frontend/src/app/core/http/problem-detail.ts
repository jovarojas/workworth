import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../models/workworth-api.models';

export function problemDetailFrom(error: unknown): ProblemDetail | null {
  if (!(error instanceof HttpErrorResponse) || !error.error || typeof error.error !== 'object') {
    return null;
  }

  return error.error as ProblemDetail;
}
