import { execFileSync } from 'node:child_process';

const apiBaseUrl = process.env.WORKWORTH_API_BASE_URL ?? 'http://localhost:8081/api/v1';
const auth0Domain = process.env.WORKWORTH_AUTH0_DOMAIN;
const auth0Audience = process.env.WORKWORTH_AUTH0_AUDIENCE;
const auth0WebClientId = process.env.WORKWORTH_AUTH0_WEB_CLIENT_ID;
const auth0AndroidClientId = process.env.WORKWORTH_AUTH0_ANDROID_CLIENT_ID;

if (!auth0Domain || !auth0Audience || !auth0WebClientId || !auth0AndroidClientId) {
  throw new Error('Define WORKWORTH_AUTH0_DOMAIN, WORKWORTH_AUTH0_AUDIENCE, WORKWORTH_AUTH0_WEB_CLIENT_ID y WORKWORTH_AUTH0_ANDROID_CLIENT_ID antes de iniciar Auth0 local.');
}

execFileSync(process.execPath, [
  './node_modules/@angular/cli/bin/ng.js',
  'serve',
  '--define', `WORKWORTH_API_BASE_URL=${JSON.stringify(apiBaseUrl)}`,
  '--define', 'WORKWORTH_PRODUCTION=false',
  '--define', `WORKWORTH_AUTH0_DOMAIN=${JSON.stringify(auth0Domain)}`,
  '--define', `WORKWORTH_AUTH0_AUDIENCE=${JSON.stringify(auth0Audience)}`,
  '--define', `WORKWORTH_AUTH0_WEB_CLIENT_ID=${JSON.stringify(auth0WebClientId)}`,
  '--define', `WORKWORTH_AUTH0_ANDROID_CLIENT_ID=${JSON.stringify(auth0AndroidClientId)}`
], { stdio: 'inherit' });
