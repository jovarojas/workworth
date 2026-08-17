# Preparación de despliegue

WorkWorth se desplegará como tres componentes independientes:

- Angular web en Cloudflare Pages.
- API Spring Boot en Railway.
- PostgreSQL gestionado en el mismo proyecto y región de Railway.

La aplicación Android empaquetada con Capacitor consume la misma API HTTPS que la web. El backend y PostgreSQL no forman parte de la APK.

## Backend en Railway

El servicio debe configurarse con directorio raíz `backend` y archivo de configuración `/backend/railway.toml`. El `Dockerfile` incluido compila y ejecuta la API con Java 21.

Antes de desplegar, crea un servicio PostgreSQL gestionado en el mismo proyecto de Railway y configura estas variables en el servicio de API:

| Variable | Valor de producción |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DB_URL` | URL JDBC de PostgreSQL proporcionada por Railway, usando la red privada del proyecto |
| `DB_USERNAME` | Usuario de PostgreSQL proporcionado por Railway |
| `DB_PASSWORD` | Contraseña de PostgreSQL proporcionada por Railway |
| `APP_TIME_ZONE` | `Europe/Madrid` |
| `CORS_ALLOWED_ORIGINS` | Orígenes HTTPS separados por comas autorizados para la web y Capacitor |
| `AUTH0_ISSUER` | `https://<tenant-auth0>/` |
| `AUTH0_JWK_SET_URI` | `https://<tenant-auth0>/.well-known/jwks.json` |
| `AUTH0_AUDIENCE` | Identificador estable de la API de WorkWorth |
| `WORKWORTH_INITIAL_AUTH0_SUB` | `sub` de la primera usuaria autorizada; solo en la inicialización de la base vacía |
| `WORKWORTH_INITIAL_USER_EMAIL` | Correo verificado de esa usuaria; solo en la inicialización de la base vacía |

Railway inyecta `PORT`; la aplicación lo utiliza automáticamente y escucha en `8080` si no se proporciona. Flyway se ejecuta al iniciar el servicio. No publiques PostgreSQL ni copies secretos en el repositorio.

Al asignar un dominio, Railway proporciona TLS automáticamente. El objetivo previsto es `https://api.<dominio>/api/v1`.

## Frontend web en Cloudflare Pages

Configura Cloudflare Pages con:

- Directorio raíz: `frontend/frontend`.
- Versión de Node: `22.14.0`.
- Comando de compilación: `npm ci && npm run build:web:production`.
- Directorio de salida: `dist/frontend/browser`.
- Variables de compilación públicas: `WORKWORTH_API_BASE_URL`, `WORKWORTH_AUTH0_DOMAIN`, `WORKWORTH_AUTH0_AUDIENCE`, `WORKWORTH_AUTH0_WEB_CLIENT_ID` y `WORKWORTH_AUTH0_ANDROID_CLIENT_ID`. La URL HTTPS real de la API termina en `/api/v1`; los demás valores identifican clientes públicos de Auth0, no secretos.

Cloudflare Pages sirve Angular como SPA, por lo que las rutas existentes se resuelven mediante su comportamiento SPA por defecto.

## CORS y Android

Cuando existan dominios reales, `CORS_ALLOWED_ORIGINS` debe contener exactamente los orígenes necesarios, por ejemplo:

```text
https://app.<dominio>,https://localhost
```

`https://localhost` corresponde al origen del WebView de Capacitor configurado con esquema HTTPS. No uses `*` y no uses URLs HTTP locales en compilaciones de producción.

Para una APK de producción:

```bash
cd frontend/frontend
WORKWORTH_API_BASE_URL=https://api.<dominio>/api/v1 npm run build:android:production
cd android
./gradlew assembleDebug
```

No se han creado cuentas, dominios, secretos ni despliegues con esta preparación.
