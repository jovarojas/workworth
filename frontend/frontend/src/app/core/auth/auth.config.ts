import { environment } from '../../../environments/environment';

export const authConfiguration = {
  webConfigured: Boolean(environment.auth0.domain && environment.auth0.webClientId && environment.auth0.audience),
  androidConfigured: Boolean(environment.auth0.domain && environment.auth0.androidClientId && environment.auth0.audience),
  domain: environment.auth0.domain || 'workworth.invalid',
  webClientId: environment.auth0.webClientId || 'workworth-placeholder-web-client',
  androidClientId: environment.auth0.androidClientId || 'workworth-placeholder-android-client',
  audience: environment.auth0.audience
};
