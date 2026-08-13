import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.workworth.app',
  appName: 'WorkWorth',
  webDir: 'dist/frontend/browser',
  server: {
    androidScheme: 'https'
  }
};

export default config;
