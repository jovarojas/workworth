import { HttpErrorResponse } from '@angular/common/http';
import { problemDetailFrom, problemDetailMessage } from './problem-detail';

describe('problemDetailFrom', () => {
  it('extracts the public ProblemDetail fields', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: {
        code: 'VALIDATION_ERROR',
        detail: 'Request validation failed.',
        fieldErrors: { startedAt: 'must not be null' }
      }
    });

    expect(problemDetailFrom(error)).toEqual({
      code: 'VALIDATION_ERROR',
      detail: 'Request validation failed.',
      fieldErrors: { startedAt: 'must not be null' }
    });
  });

  it('returns null for a non-HTTP error', () => {
    expect(problemDetailFrom(new Error('network'))).toBeNull();
  });

  it('provides a Spanish public message for known API error codes', () => {
    const error = new HttpErrorResponse({
      status: 409,
      error: { code: 'APPLICATION_CURRENCY_LOCKED', detail: 'Currency cannot change after economic data exists.' }
    });

    expect(problemDetailMessage(error)).toBe('La moneda no puede cambiarse porque ya existen datos económicos registrados.');
  });
});
