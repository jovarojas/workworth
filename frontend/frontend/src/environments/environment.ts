declare const WORKWORTH_API_BASE_URL: string | undefined;
declare const WORKWORTH_PRODUCTION: boolean | undefined;
declare const WORKWORTH_AUTH0_DOMAIN: string | undefined;
declare const WORKWORTH_AUTH0_WEB_CLIENT_ID: string | undefined;
declare const WORKWORTH_AUTH0_ANDROID_CLIENT_ID: string | undefined;
declare const WORKWORTH_AUTH0_AUDIENCE: string | undefined;

const apiBaseUrl = typeof WORKWORTH_API_BASE_URL === 'string'
  ? WORKWORTH_API_BASE_URL
  : 'http://localhost:8081/api/v1';

export const environment = {
  production: typeof WORKWORTH_PRODUCTION === 'boolean' ? WORKWORTH_PRODUCTION : false,
  apiBaseUrl,
  auth0: {
    domain: typeof WORKWORTH_AUTH0_DOMAIN === 'string' ? WORKWORTH_AUTH0_DOMAIN : '',
    webClientId: typeof WORKWORTH_AUTH0_WEB_CLIENT_ID === 'string' ? WORKWORTH_AUTH0_WEB_CLIENT_ID : '',
    androidClientId: typeof WORKWORTH_AUTH0_ANDROID_CLIENT_ID === 'string'
      ? WORKWORTH_AUTH0_ANDROID_CLIENT_ID
      : '',
    audience: typeof WORKWORTH_AUTH0_AUDIENCE === 'string' ? WORKWORTH_AUTH0_AUDIENCE : ''
  }
};
