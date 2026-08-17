import { registerPlugin } from '@capacitor/core';

export interface NativeAuthPlugin {
  login(): Promise<void>;
  logout(): Promise<void>;
  isAuthenticated(): Promise<{ authenticated: boolean }>;
  getAccessToken(): Promise<{ accessToken: string }>;
}

export const NativeAuth = registerPlugin<NativeAuthPlugin>('WorkWorthNativeAuth');
