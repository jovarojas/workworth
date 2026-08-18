package com.workworth.app;

import com.getcapacitor.BridgeActivity;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        registerPlugin(WorkWorthNativeAuthPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
