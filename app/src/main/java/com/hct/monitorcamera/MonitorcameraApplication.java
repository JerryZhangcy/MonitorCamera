package com.hct.monitorcamera;

import android.app.Application;
import android.content.Context;

public class MonitorcameraApplication extends Application{
    public static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
    }
}
