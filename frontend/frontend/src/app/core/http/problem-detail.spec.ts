import { HttpErrorResponse } from '@angular/common/http';
import { problemDetailFrom } from './problem-detail';

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
});
