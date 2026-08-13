import { execFileSync } from 'node:child_process';

const [target, environment] = process.argv.slice(2);
const apiBaseUrl = process.env.WORKWORTH_API_BASE_URL;

if (!['web', 'android'].includes(target) || !['development', 'production'].includes(environment)) {
  throw new Error('Uso: node scripts/build-with-api.mjs <web|android> <development|production>');
}

if (!apiBaseUrl) {
  throw new Error('WORKWORTH_API_BASE_URL es obligatoria para compilar una variante configurable.');
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
  `WORKWORTH_PRODUCTION=${environment === 'production'}`
];

execFileSync(process.execPath, buildArguments, { stdio: 'inherit' });

if (target === 'android') {
  execFileSync(process.execPath, ['./node_modules/@capacitor/cli/bin/capacitor', 'sync', 'android'], {
    stdio: 'inherit'
  });
}
