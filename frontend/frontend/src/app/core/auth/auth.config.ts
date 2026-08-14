import { environment } from '../../../environments/environment';

export const authConfiguration = {
  configured: Boolean(environment.auth0.domain && environment.auth0.clientId && environment.auth0.audience),
  domain: environment.auth0.domain || 'workworth.invalid',
  clientId: environment.auth0.clientId || 'workworth-placeholder-client',
  audience: environment.auth0.audience
};
