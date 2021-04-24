package com.picstar.picstarapp.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.helpers.PSR_PrefsManager;

class BaseActivity  extends AppCompatActivity {
PSR_PrefsManager psr_prefsManager;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getSupportActionBar().hide();
        psr_prefsManager = new PSR_PrefsManager(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolBar_bgcolor, this.getTheme()));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolBar_bgcolor));
        }
    }

    public void showToast(final String msg){
       runOnUiThread(new Runnable() {
           @Override
           public void run() {
               Toast.makeText(BaseActivity.this, msg, Toast.LENGTH_LONG).show();
           }
       });
    }
}
