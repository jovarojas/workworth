import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { routes } from './app.routes';
import { WorkWorthAuthService } from './core/auth/workworth-auth.service';

describe('App', () => {
  const auth = {
    configured: true,
    isLoading$: of(false),
    isLoading: () => false,
    isAuthenticated: () => true,
    login: vi.fn(),
    logout: vi.fn()
  };

  beforeEach(async () => {
    auth.logout.mockClear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes),
        { provide: WorkWorthAuthService, useValue: auth }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('provides navigation to the dashboard, workday, earnings, rewards, goals, statistics, salary and currency settings routes', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(links.map((link) => link.getAttribute('href'))).toContain('/workday');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/salary');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/earnings');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/rewards');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/goals');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/statistics');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/preferences/currency');
    expect(routes.some((route) => route.path === 'workday')).toBe(true);
    expect(routes.some((route) => route.path === 'salary')).toBe(true);
    expect(routes.some((route) => route.path === 'earnings')).toBe(true);
    expect(routes.some((route) => route.path === 'earnings/workdays/:date')).toBe(true);
    expect(routes.some((route) => route.path === 'rewards')).toBe(true);
    expect(routes.some((route) => route.path === 'goals')).toBe(true);
    expect(routes.some((route) => route.path === 'statistics')).toBe(true);
    expect(routes.some((route) => route.path === 'preferences/currency')).toBe(true);
    expect(routes.find((route) => route.path === '')?.pathMatch).toBe('full');
  });

  it('opens the mobile navigation and closes it after selecting a route', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const menuButton = fixture.nativeElement.querySelector('.app-nav__menu-button') as HTMLButtonElement;
    menuButton.click();
    await fixture.whenStable();

    const drawer = fixture.nativeElement.querySelector('.app-nav__drawer') as HTMLElement;
    expect(drawer.classList).toContain('mat-drawer-opened');

    const rewardsLink = drawer.querySelector('a[href="/rewards"]') as HTMLAnchorElement;
    rewardsLink.click();
    await fixture.whenStable();

    expect(drawer.classList).not.toContain('mat-drawer-opened');
  });

  it('uses an accessible logout icon instead of a textual navigation entry', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const logout = fixture.nativeElement.querySelector('.app-nav__logout') as HTMLButtonElement;
    expect(logout.getAttribute('aria-label')).toBe('Cerrar sesión');
    expect(logout.textContent?.trim()).not.toContain('Cerrar sesión');

    logout.click();
    expect(auth.logout).toHaveBeenCalledOnce();
  });
});
