package com.picstar.picstarapp.app;

import android.app.Application;
import android.os.StrictMode;


import com.picstar.picstarapp.utils.PSR_Utils;

import java.io.File;

public class PicstarApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        createAppFolderIfRequired();
    }

    private void createAppFolderIfRequired() {
        File appFolder = new File(PSR_Utils.createFilePath(getApplicationContext()));
        if (!appFolder.exists()) {
            appFolder.mkdir();
        }
    }
}
