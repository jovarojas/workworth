package com.workworth.app;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        registerPlugin(WorkWorthNativeAuthPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
