import { execFileSync } from 'node:child_process';

const [target, environment] = process.argv.slice(2);
const apiBaseUrl = process.env.WORKWORTH_API_BASE_URL;
const auth0Domain = process.env.WORKWORTH_AUTH0_DOMAIN;
const auth0Audience = process.env.WORKWORTH_AUTH0_AUDIENCE;
const auth0WebClientId = process.env.WORKWORTH_AUTH0_WEB_CLIENT_ID;
const auth0AndroidClientId = process.env.WORKWORTH_AUTH0_ANDROID_CLIENT_ID;

if (!['web', 'android'].includes(target) || !['development', 'production'].includes(environment)) {
  throw new Error('Uso: node scripts/build-with-api.mjs <web|android> <development|production>');
}

if (!apiBaseUrl) {
  throw new Error('WORKWORTH_API_BASE_URL es obligatoria para compilar una variante configurable.');
}

if (!auth0Domain || !auth0Audience || !auth0WebClientId || !auth0AndroidClientId) {
  throw new Error('Las variables públicas WORKWORTH_AUTH0_DOMAIN, WORKWORTH_AUTH0_AUDIENCE, WORKWORTH_AUTH0_WEB_CLIENT_ID y WORKWORTH_AUTH0_ANDROID_CLIENT_ID son obligatorias.');
}

let parsedApiBaseUrl;
try {
  parsedApiBaseUrl = new URL(apiBaseUrl);
} catch {
  throw new Error('WORKWORTH_API_BASE_URL debe ser una URL absoluta válida.');
}

if (environment === 'production' && parsedApiBaseUrl.protocol !== 'https:') {
  throw new Error('La compilación de producción requiere una API HTTPS.');
}

if (environment === 'development' && !['http:', 'https:'].includes(parsedApiBaseUrl.protocol)) {
  throw new Error('La API de desarrollo debe usar HTTP o HTTPS.');
}

const buildArguments = [
  './node_modules/@angular/cli/bin/ng.js',
  'build',
  '--configuration',
  environment === 'production' ? 'production' : 'development',
  '--define',
  `WORKWORTH_API_BASE_URL=${JSON.stringify(apiBaseUrl)}`,
  '--define',
  `WORKWORTH_PRODUCTION=${environment === 'production'}`,
  '--define',
  `WORKWORTH_AUTH0_DOMAIN=${JSON.stringify(auth0Domain)}`,
  '--define',
  `WORKWORTH_AUTH0_AUDIENCE=${JSON.stringify(auth0Audience)}`,
  '--define',
  `WORKWORTH_AUTH0_WEB_CLIENT_ID=${JSON.stringify(auth0WebClientId)}`,
  '--define',
  `WORKWORTH_AUTH0_ANDROID_CLIENT_ID=${JSON.stringify(auth0AndroidClientId)}`
];

execFileSync(process.execPath, buildArguments, { stdio: 'inherit' });

if (target === 'android') {
  execFileSync(process.execPath, ['./node_modules/@capacitor/cli/bin/capacitor', 'sync', 'android'], {
    stdio: 'inherit'
  });
}
