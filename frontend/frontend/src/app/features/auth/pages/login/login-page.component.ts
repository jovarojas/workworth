import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute } from '@angular/router';

import { WorkWorthAuthService } from '../../../../core/auth/workworth-auth.service';

@Component({
  selector: 'app-login-page',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <main class="login-page" aria-labelledby="login-title">
      <section class="login-page__card">
        <p class="login-page__brand">WORKWORTH</p>
        <mat-icon class="login-page__icon" aria-hidden="true">account_circle</mat-icon>
        <h1 id="login-title">Tu trabajo, en tus manos</h1>
        <p class="login-page__description">Inicia sesión para consultar tu jornada, ganancias y objetivos personales.</p>
        @if (auth.configured) {
          <button mat-flat-button type="button" class="login-page__button" (click)="login()">
            Iniciar sesión
          </button>
        } @else {
          <p class="login-page__error" role="alert">La autenticación todavía no está configurada para este entorno.</p>
        }
      </section>
    </main>
  `,
  styles: `
    :host { display: block; width: 100%; }
    .login-page { display: grid; min-height: 100dvh; padding: 1.5rem; place-items: center; }
    .login-page__card { background: #fff; border: 1px solid #dce2ef; border-radius: 1.25rem; box-shadow: 0 1rem 3rem rgb(32 47 83 / .1); max-width: 27rem; padding: 2.5rem 2rem; text-align: center; width: 100%; }
    .login-page__brand { color: #273a8a; font-size: .8rem; font-weight: 800; letter-spacing: .15em; margin: 0 0 1.75rem; }
    .login-page__icon { color: #596fd4; font-size: 3.5rem; height: 3.5rem; margin-bottom: 1rem; width: 3.5rem; }
    h1 { color: #172033; font-size: 1.7rem; line-height: 1.2; margin: 0; }
    .login-page__description { color: #56627a; line-height: 1.55; margin: 1rem 0 1.75rem; }
    .login-page__button { min-height: 3rem; padding-inline: 1.5rem; width: 100%; }
    .login-page__error { color: #a42b2b; font-weight: 700; margin: 1.5rem 0 0; }
  `
})
export class LoginPageComponent {
  readonly auth = inject(WorkWorthAuthService);
  private readonly route = inject(ActivatedRoute);

  login(): void {
    this.auth.login(this.route.snapshot.queryParamMap.get('returnTo') ?? undefined);
  }
}
