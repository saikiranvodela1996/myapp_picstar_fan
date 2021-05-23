package com.picstar.picstarapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.utils.PSRConstants;


public class PrivacyPolicyActivity extends BaseActivity {

    @BindView(R.id.webView)
    WebView webView;
    @BindView(R.id.progressbar)
    ProgressBar progressbar;

    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView titleTv;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);
        ButterKnife.bind(this);
        leftSideMenu.setImageResource(R.drawable.ic_back);
        titleTv.setText(getString(R.string.privacy_policy));
        url = psr_prefsManager.get(PSRConstants.PRIVACY_POLICY_URL);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressbar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(PrivacyPolicyActivity.this, "Error:" + description, Toast.LENGTH_SHORT).show();
            }

        });
        webView.loadUrl(url);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }
    @OnClick(R.id.left_side_menu_option)
    void onBackClicked() {
        finish();
    }
}