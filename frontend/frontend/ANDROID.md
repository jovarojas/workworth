# Android con Capacitor

WorkWorth empaqueta el frontend Angular como una aplicación Android mediante Capacitor. La APK contiene únicamente los archivos web generados; el backend Spring Boot y PostgreSQL continúan siendo servicios externos.

## Flujo de compilación

Usa Node `22.14.0` y ejecuta:

```bash
npm run build:android
cd android
./gradlew assembleDebug
```

La APK de depuración se genera en `android/app/build/outputs/apk/debug/app-debug.apk`.

El proyecto Android usa el resultado Angular de `dist/frontend/browser`. Capacitor lo copia a los recursos nativos durante la sincronización y mantiene las rutas de Angular, incluidas `/`, `/salary`, `/workday`, `/earnings`, `/rewards`, `/preferences/currency`, `/goals` y `/statistics`.

## URL de la API

La URL de la API sigue siendo una configuración de Angular. No se incluye ningún backend ni URL pública en la APK.

- Desarrollo en navegador: `src/environments/environment.ts` usa `http://localhost:8081/api/v1`.
- Desarrollo en emulador Android: antes de una prueba local, configura temporalmente la URL del entorno de compilación a `http://10.0.2.2:8081/api/v1`. `10.0.2.2` apunta al ordenador anfitrión desde el emulador. Esta variante es solo para desarrollo local.
- Producción: crea una configuración de entorno de producción con la URL HTTPS real del backend, por ejemplo `https://<host-de-la-api>/api/v1`, cuando dicho despliegue exista. No publiques una APK de producción con una URL HTTP local.

El backend de producción deberá permitir el origen de Capacitor y servir la API mediante HTTPS. No se habilita tráfico HTTP de producción desde la aplicación Android.

## Requisitos Android

Para compilar una APK se necesita Android SDK con la plataforma `compileSdk 36` y un JDK 21. El proyecto incluye Gradle Wrapper, por lo que no hace falta instalar Gradle de forma independiente. El proyecto generado se puede abrir con `npm run android:open`.

Capacitor Android 8 no compila con JDK 17. Usa un JDK 21 compatible, por ejemplo configurando temporalmente `JAVA_HOME` antes de ejecutar `./gradlew assembleDebug`.
