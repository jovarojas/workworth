# Android con Capacitor

WorkWorth empaqueta el frontend Angular como una aplicación Android mediante Capacitor. La APK contiene únicamente los archivos web generados; el backend Spring Boot y PostgreSQL continúan siendo servicios externos.

## Flujo de compilación

Usa Node `22.14.0`, define la URL de la API del entorno y ejecuta:

```bash
WORKWORTH_API_BASE_URL=http://10.0.2.2:8081/api/v1 npm run build:android:development
cd android
./gradlew assembleDebug
```

La APK de depuración se genera en `android/app/build/outputs/apk/debug/app-debug.apk`.

El proyecto Android usa el resultado Angular de `dist/frontend/browser`. Capacitor lo copia a los recursos nativos durante la sincronización y mantiene las rutas de Angular, incluidas `/`, `/salary`, `/workday`, `/earnings`, `/rewards`, `/preferences/currency`, `/goals` y `/statistics`.

## URL de la API

La URL de la API se incorpora en la compilación de Angular a partir de `WORKWORTH_API_BASE_URL`. No se incluye ningún backend ni URL pública en la APK.

- Desarrollo en navegador: `src/environments/environment.ts` usa `http://localhost:8081/api/v1`.
- Desarrollo en emulador Android: antes de una prueba local, configura temporalmente la URL del entorno de compilación a `http://10.0.2.2:8081/api/v1`. `10.0.2.2` apunta al ordenador anfitrión desde el emulador. Esta variante es solo para desarrollo local.
- Producción: cuando exista la API, genera la APK con `WORKWORTH_API_BASE_URL=https://<host-de-la-api>/api/v1 npm run build:android:production`. El comando rechaza URLs que no usen HTTPS.

El backend de producción deberá permitir el origen de Capacitor y servir la API mediante HTTPS. No se habilita tráfico HTTP de producción desde la aplicación Android.

## Requisitos Android

Para compilar una APK se necesita Android SDK con la plataforma `compileSdk 36` y un JDK 21. El proyecto incluye Gradle Wrapper, por lo que no hace falta instalar Gradle de forma independiente. El proyecto generado se puede abrir con `npm run android:open`.

Capacitor Android 8 no compila con JDK 17. Usa un JDK 21 compatible, por ejemplo configurando temporalmente `JAVA_HOME` antes de ejecutar `./gradlew assembleDebug`.
