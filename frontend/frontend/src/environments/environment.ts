declare const WORKWORTH_API_BASE_URL: string | undefined;
declare const WORKWORTH_PRODUCTION: boolean | undefined;

const apiBaseUrl = typeof WORKWORTH_API_BASE_URL === 'string'
  ? WORKWORTH_API_BASE_URL
  : 'http://localhost:8081/api/v1';

export const environment = {
  production: typeof WORKWORTH_PRODUCTION === 'boolean' ? WORKWORTH_PRODUCTION : false,
  apiBaseUrl
};
