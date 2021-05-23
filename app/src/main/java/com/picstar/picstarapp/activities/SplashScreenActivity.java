package com.picstar.picstarapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.utils.PSRConstants;

public class SplashScreenActivity  extends BaseActivity{
    long SPLASH_SCREEN_DURATION = 1000;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(psr_prefsManager.getBoolean(PSRConstants.ISLOGGEDIN))
                {
                    Intent loginActivityIntent = new Intent(SplashScreenActivity.this, DashBoardActivity.class);
                    startActivity(loginActivityIntent);
                    finish();
                }else
                {
                    Intent loginActivityIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    startActivity(loginActivityIntent);
                    finish();
                }

            }
        }, SPLASH_SCREEN_DURATION);
    }

}
