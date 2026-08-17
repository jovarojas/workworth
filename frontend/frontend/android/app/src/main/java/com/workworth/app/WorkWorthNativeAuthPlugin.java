package com.workworth.app;

import androidx.annotation.NonNull;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SecureCredentialsManager;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.Callback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "WorkWorthNativeAuth")
public class WorkWorthNativeAuthPlugin extends Plugin {

    private static final String AUTH0_SCHEME = "com.workworth.app";

    private Auth0 account;
    private SecureCredentialsManager credentials;

    @Override
    public void load() {
        if (!isConfigured()) {
            return;
        }
        account = Auth0.getInstance(BuildConfig.AUTH0_CLIENT_ID, BuildConfig.AUTH0_DOMAIN);
        credentials = new SecureCredentialsManager(
            getContext(), account, new SharedPreferencesStorage(getContext()));
    }

    @PluginMethod
    public void login(PluginCall call) {
        if (!isReady(call)) {
            return;
        }
        WebAuthProvider.login(account)
            .withScheme(AUTH0_SCHEME)
            .withAudience(BuildConfig.AUTH0_AUDIENCE)
            .withScope("openid profile email offline_access")
            .start(getActivity(), new Callback<Credentials, AuthenticationException>() {
                @Override
                public void onSuccess(Credentials result) {
                    credentials.saveCredentials(result);
                    call.resolve();
                }

                @Override
                public void onFailure(@NonNull AuthenticationException error) {
                    call.reject("No se pudo iniciar sesión.", "AUTHENTICATION_FAILED", error);
                }
            });
    }

    @PluginMethod
    public void isAuthenticated(PluginCall call) {
        JSObject result = new JSObject();
        result.put("authenticated", credentials != null && credentials.hasValidCredentials());
        call.resolve(result);
    }

    @PluginMethod
    public void getAccessToken(PluginCall call) {
        if (!isReady(call)) {
            return;
        }
        credentials.getCredentials(new Callback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(Credentials result) {
                JSObject response = new JSObject();
                response.put("accessToken", result.getAccessToken());
                call.resolve(response);
            }

            @Override
            public void onFailure(@NonNull CredentialsManagerException error) {
                call.reject("No hay una sesión Android válida.", "SESSION_UNAVAILABLE", error);
            }
        });
    }

    @PluginMethod
    public void logout(PluginCall call) {
        if (!isReady(call)) {
            return;
        }
        WebAuthProvider.logout(account)
            .withScheme(AUTH0_SCHEME)
            .start(getActivity(), new Callback<Void, AuthenticationException>() {
                @Override
                public void onSuccess(Void result) {
                    credentials.clearCredentials();
                    call.resolve();
                }

                @Override
                public void onFailure(@NonNull AuthenticationException error) {
                    credentials.clearCredentials();
                    call.reject("No se pudo cerrar sesión en el proveedor.", "LOGOUT_FAILED", error);
                }
            });
    }

    private boolean isReady(PluginCall call) {
        if (credentials != null) {
            return true;
        }
        call.reject("La autenticación Android no está configurada.", "AUTHENTICATION_NOT_CONFIGURED");
        return false;
    }

    private boolean isConfigured() {
        return !BuildConfig.AUTH0_DOMAIN.isBlank()
            && !BuildConfig.AUTH0_CLIENT_ID.isBlank()
            && !BuildConfig.AUTH0_AUDIENCE.isBlank();
    }
}
