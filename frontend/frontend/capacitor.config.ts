import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.workworth.app',
  appName: 'WorkWorth',
  webDir: 'dist/frontend/browser',
  server: {
    androidScheme: 'https'
  },
  android: {
    allowMixedContent: process.env.WORKWORTH_ANDROID_ALLOW_MIXED_CONTENT === 'true'
  }
};

export default config;
