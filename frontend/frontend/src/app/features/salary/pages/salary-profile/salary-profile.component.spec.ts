import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { SalaryApiService } from '../../../../core/services/salary-api.service';
import { SalaryProfileComponent } from './salary-profile.component';

describe('SalaryProfileComponent', () => {
  const salaries = {
    current: vi.fn(),
    create: vi.fn(),
    rate: vi.fn(),
    estimatorStatus: vi.fn()
  };

  beforeEach(async () => {
    Object.values(salaries).forEach((method) => method.mockReset());
    salaries.estimatorStatus.mockReturnValue(of(estimator()));

    await TestBed.configureTestingModule({
      imports: [SalaryProfileComponent],
      providers: [{ provide: SalaryApiService, useValue: salaries }]
    }).compileComponents();
  });

  it('shows the current profile and API-provided monthly rate', () => {
    salaries.current.mockReturnValue(of({ month: '2026-08', salaryProfile: profile() }));
    salaries.rate.mockReturnValue(of(rate()));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('1,250.00');
    expect(content).toContain('15,000.00');
    expect(content).toContain('7.81');
    expect(salaries.rate).toHaveBeenCalledWith('2026-08');
    expect(content).toContain('NOT_IMPLEMENTED');
  });

  it('shows the initial configuration form when the profile does not exist', () => {
    salaries.current.mockReturnValue(throwError(() => problem(404, 'RESOURCE_NOT_FOUND', 'No salary profile is effective.')));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('PRIMERA CONFIGURACIÓN');
    expect(fixture.nativeElement.querySelector('form')).not.toBeNull();
  });

  it('creates a real monthly net profile without gross annual income and loads its rate', () => {
    salaries.current.mockReturnValue(throwError(() => problem(404, 'RESOURCE_NOT_FOUND', 'Not found')));
    salaries.create.mockReturnValue(of(profile()));
    salaries.rate.mockReturnValue(of(rate()));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.patchValue({ effectiveFrom: '2026-08-01', netMonthlyReal: '1250.00' });

    component.submit();
    fixture.detectChanges();

    expect(salaries.create).toHaveBeenCalledWith({
      effectiveFrom: '2026-08-01', netMonthlyReal: 1250, currencyCode: 'EUR', payPeriods: 12
    });
    expect(salaries.rate).toHaveBeenCalledWith('2026-08');
    expect(fixture.nativeElement.textContent).toContain('1,250.00');
  });

  it('keeps the profile visible when the monthly rate is unavailable', () => {
    salaries.current.mockReturnValue(of({ month: '2026-08', salaryProfile: profile() }));
    salaries.rate.mockReturnValue(throwError(() => problem(
      409, 'SALARY_RATE_UNAVAILABLE', 'No standard economic hours are available.'
    )));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();

    const content = fixture.nativeElement.textContent;
    expect(content).toContain('1,250.00');
    expect(content).toContain('No standard economic hours are available.');
  });

  it('shows backend validation field errors without submitting invalid local data', () => {
    salaries.current.mockReturnValue(throwError(() => problem(404, 'RESOURCE_NOT_FOUND', 'Not found')));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.patchValue({ effectiveFrom: '2026-08-02', netMonthlyReal: '12.345' });
    component.submit();
    fixture.detectChanges();

    expect(salaries.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('La fecha debe ser el primer día del mes.');
    expect(fixture.nativeElement.textContent).toContain('Introduce un importe positivo con un máximo de dos decimales.');
  });

  it('shows ProblemDetail field errors returned by the API', () => {
    salaries.current.mockReturnValue(throwError(() => problem(404, 'RESOURCE_NOT_FOUND', 'Not found')));
    salaries.create.mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400,
      error: {
        code: 'VALIDATION_ERROR',
        detail: 'Request validation failed.',
        fieldErrors: { netMonthlyReal: 'must be greater than or equal to 0.01' }
      }
    })));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.patchValue({ effectiveFrom: '2026-08-01', netMonthlyReal: '1250.00' });
    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('must be greater than or equal to 0.01');
    expect(fixture.nativeElement.textContent).toContain('Request validation failed.');
  });

  it('shows a salary profile conflict without inventing a result', () => {
    salaries.current.mockReturnValue(throwError(() => problem(404, 'RESOURCE_NOT_FOUND', 'Not found')));
    salaries.create.mockReturnValue(throwError(() => problem(
      409, 'SALARY_PROFILE_CONFLICT', 'A salary profile already exists for this effective month.'
    )));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.patchValue({ effectiveFrom: '2026-08-01', netMonthlyReal: '1250.00' });
    component.submit();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('A salary profile already exists for this effective month.');
    expect(fixture.nativeElement.textContent).not.toContain('1,250.00');
  });

  it('keeps an unavailable profile visible when the API reports incomplete salary configuration', () => {
    salaries.current.mockReturnValue(of({
      month: '2026-08',
      salaryProfile: { ...profile(), netMonthlyReal: null, netAnnualReal: null, activeIncomeSource: 'UNAVAILABLE' }
    }));
    salaries.rate.mockReturnValue(throwError(() => problem(
      422, 'SALARY_CONFIGURATION_INCOMPLETE', 'A real monthly net income is required.'
    )));

    const fixture = TestBed.createComponent(SalaryProfileComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('UNAVAILABLE');
    expect(fixture.nativeElement.textContent).toContain('A real monthly net income is required.');
  });

  function profile() {
    return {
      id: 1, effectiveFrom: '2026-08-01', grossAnnual: null, netMonthlyReal: 1250,
      netAnnualReal: 15000, currencyCode: 'EUR', payPeriods: 12,
      activeIncomeSource: 'NET_MONTHLY_REAL', estimatorStatus: 'NOT_IMPLEMENTED'
    };
  }

  function rate() {
    return {
      month: '2026-08', incomeSource: 'NET_MONTHLY_REAL', monthlyNetIncome: 1250,
      standardEconomicHours: 160, hourlyNetRate: 7.8125, currencyCode: 'EUR'
    };
  }

  function estimator() {
    return { fiscalYear: 2026, status: 'NOT_IMPLEMENTED', requiredInputs: ['Fiscal estimator implementation'] };
  }

  function problem(status: number, code: string, detail: string): HttpErrorResponse {
    return new HttpErrorResponse({ status, error: { code, detail } });
  }
});
