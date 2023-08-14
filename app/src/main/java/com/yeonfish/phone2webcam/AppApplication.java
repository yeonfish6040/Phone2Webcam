package com.yeonfish.phone2webcam;

import androidx.multidex.MultiDexApplication;

public class AppApplication extends MultiDexApplication {
    boolean isRunning = false;
    private AppApplication instance = null;

    public AppApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
    }
}
