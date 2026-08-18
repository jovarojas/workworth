import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';

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

const androidDevelopmentCleartextApi = target === 'android'
  && environment === 'development'
  && parsedApiBaseUrl.protocol === 'http:';

if (androidDevelopmentCleartextApi
  && ['10.0.2.2', '127.0.0.1', 'localhost'].includes(parsedApiBaseUrl.hostname)) {
  throw new Error('La APK de desarrollo para un dispositivo físico requiere la IP LAN del host en WORKWORTH_API_BASE_URL.');
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
  const auth0ConfigurationPath = new URL('../android/app/build/generated/auth0.properties', import.meta.url);
  const networkSecurityConfigPath = new URL(
    '../android/app/build/generated/res/developmentNetworkSecurity/xml/debug_network_security_config.xml',
    import.meta.url
  );

  mkdirSync(new URL('.', auth0ConfigurationPath), { recursive: true });
  writeFileSync(auth0ConfigurationPath, [
    `auth0Domain=${auth0Domain}`,
    `auth0Audience=${auth0Audience}`,
    `auth0ClientId=${auth0AndroidClientId}`
  ].join('\n'));

  mkdirSync(new URL('.', networkSecurityConfigPath), { recursive: true });
  writeFileSync(networkSecurityConfigPath, androidDevelopmentCleartextApi
    ? `<network-security-config>\n    <base-config cleartextTrafficPermitted="false" />\n    <domain-config cleartextTrafficPermitted="true">\n        <domain includeSubdomains="false">${parsedApiBaseUrl.hostname}</domain>\n    </domain-config>\n</network-security-config>\n`
    : '<network-security-config>\n    <base-config cleartextTrafficPermitted="false" />\n</network-security-config>\n');

  execFileSync(process.execPath, ['./node_modules/@capacitor/cli/bin/capacitor', 'sync', 'android'], {
    stdio: 'inherit',
    env: {
      ...process.env,
      WORKWORTH_ANDROID_ALLOW_MIXED_CONTENT: String(androidDevelopmentCleartextApi)
    }
  });
}
