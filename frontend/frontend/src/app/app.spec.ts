import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter(routes)]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('provides navigation to the dashboard, workday, earnings, rewards, goals, salary and currency settings routes', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(links.map((link) => link.textContent?.trim())).toEqual([
      'WORKWORTH', 'Dashboard', 'Jornada', 'Historial', 'Recompensas', 'Objetivos', 'Salario', 'Ajustes'
    ]);
    expect(links.map((link) => link.getAttribute('href'))).toContain('/workday');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/salary');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/earnings');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/rewards');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/goals');
    expect(links.map((link) => link.getAttribute('href'))).toContain('/preferences/currency');
    expect(routes.some((route) => route.path === 'workday')).toBe(true);
    expect(routes.some((route) => route.path === 'salary')).toBe(true);
    expect(routes.some((route) => route.path === 'earnings')).toBe(true);
    expect(routes.some((route) => route.path === 'earnings/workdays/:date')).toBe(true);
    expect(routes.some((route) => route.path === 'rewards')).toBe(true);
    expect(routes.some((route) => route.path === 'goals')).toBe(true);
    expect(routes.some((route) => route.path === 'preferences/currency')).toBe(true);
    expect(routes.find((route) => route.path === '')?.pathMatch).toBe('full');
  });
});
