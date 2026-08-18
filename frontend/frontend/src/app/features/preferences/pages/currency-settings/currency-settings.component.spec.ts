import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PreferencesApiService } from '../../../../core/services/preferences-api.service';
import { CurrencySettingsComponent } from './currency-settings.component';

describe('CurrencySettingsComponent', () => {
  const preferences = {
    currency: vi.fn(),
    updateCurrency: vi.fn()
  };

  beforeEach(async () => {
    preferences.currency.mockReset();
    preferences.updateCurrency.mockReset();
    preferences.currency.mockReturnValue(of(settings('EUR', true)));

    await TestBed.configureTestingModule({
      imports: [CurrencySettingsComponent],
      providers: [{ provide: PreferencesApiService, useValue: preferences }]
    }).compileComponents();
  });

  it.each(['EUR', 'USD'] as const)('renders %s as the global currency', (currencyCode) => {
    preferences.currency.mockReturnValue(of(settings(currencyCode, true)));

    const fixture = TestBed.createComponent(CurrencySettingsComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(currencyCode);
    expect(fixture.componentInstance.currency.getRawValue()).toBe(currencyCode);
  });

  it('updates the currency when change is allowed', () => {
    preferences.updateCurrency.mockReturnValue(of(settings('USD', true)));
    const fixture = TestBed.createComponent(CurrencySettingsComponent);
    fixture.detectChanges();

    fixture.componentInstance.currency.setValue('USD');
    fixture.componentInstance.save();
    fixture.detectChanges();

    expect(preferences.updateCurrency).toHaveBeenCalledWith({ currencyCode: 'USD' });
    expect(fixture.nativeElement.textContent).toContain('Moneda global actualizada.');
  });

  it('does not allow a change when the backend reports the currency is locked', () => {
    preferences.currency.mockReturnValue(of(settings('EUR', false)));
    const fixture = TestBed.createComponent(CurrencySettingsComponent);
    fixture.detectChanges();

    fixture.componentInstance.save();

    expect(fixture.componentInstance.currency.disabled).toBe(true);
    expect(preferences.updateCurrency).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('La moneda está bloqueada');
  });

  it('shows an APPLICATION_CURRENCY_LOCKED response without replacing the current currency', () => {
    preferences.updateCurrency.mockReturnValue(throwError(() => problem(
      409, 'APPLICATION_CURRENCY_LOCKED', 'Currency cannot change after economic data exists.'
    )));
    const fixture = TestBed.createComponent(CurrencySettingsComponent);
    fixture.detectChanges();

    fixture.componentInstance.save();
    fixture.detectChanges();

    expect(fixture.componentInstance.settings()?.currencyCode).toBe('EUR');
    expect(fixture.nativeElement.textContent).toContain('La moneda no puede cambiarse porque ya existen datos económicos registrados.');
  });

  it('shows a connection error when the global currency cannot be loaded', () => {
    preferences.currency.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const fixture = TestBed.createComponent(CurrencySettingsComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se puede conectar con WorkWorth');
  });

  function settings(currencyCode: 'EUR' | 'USD', changeAllowed: boolean) {
    return { currencyCode, changeAllowed };
  }

  function problem(status: number, code: string, detail: string): HttpErrorResponse {
    return new HttpErrorResponse({ status, error: { code, detail } });
  }
});
