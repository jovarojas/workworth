import { Component, inject } from '@angular/core';

import { WorkWorthAuthService } from '../../../../core/auth/workworth-auth.service';

@Component({
  selector: 'app-login-page',
  template: `
    <main class="login-page" aria-labelledby="login-title">
      <h1 id="login-title">Accede a WorkWorth</h1>
      <p>Inicia sesión con tu cuenta autorizada para continuar.</p>
      @if (auth.configured) {
        <button type="button" (click)="auth.login()">Iniciar sesión</button>
      } @else {
        <p role="alert">La autenticación todavía no está configurada para este entorno.</p>
      }
    </main>
  `,
  styles: `:host { display: block; } .login-page { max-width: 36rem; margin: 4rem auto; padding: 1rem; }`
})
export class LoginPageComponent {
  readonly auth = inject(WorkWorthAuthService);
}
