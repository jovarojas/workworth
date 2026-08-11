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

  it('provides navigation to the dashboard and live workday route', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];
    expect(links.map((link) => link.textContent?.trim())).toEqual(['WORKWORTH', 'Dashboard', 'Jornada']);
    expect(links.map((link) => link.getAttribute('href'))).toContain('/workday');
    expect(routes.some((route) => route.path === 'workday')).toBe(true);
    expect(routes.find((route) => route.path === '')?.pathMatch).toBe('full');
  });
});
