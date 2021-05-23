package com.picstar.picstarapp.activities;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.utils.PSRConstants;

import java.util.List;


public class ContactUsActivity extends BaseActivity {

    @BindView(R.id.emailTV)
    TextView emailTV;
    @BindView(R.id.phoneNoTV)
    TextView phoneNoTV;
    @BindView(R.id.addressTV)
    TextView addressTV;

    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView titleTv;
    boolean isCameFrmBlockedScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);
        ButterKnife.bind(this);
        if (getIntent() != null) {
            isCameFrmBlockedScreen = getIntent().getBooleanExtra("ISCAME_FROM_BLOCKED_SCREEN", false);
        }
        if (isCameFrmBlockedScreen) {
            leftSideMenu.setVisibility(View.INVISIBLE);
        } else {
            leftSideMenu.setImageResource(R.drawable.ic_back);
        }

        titleTv.setText(getString(R.string.contactus_txt));
        emailTV.setText(psr_prefsManager.get(PSRConstants.CONTACT_US_EMAIL));
        phoneNoTV.setText(psr_prefsManager.get(PSRConstants.CONTACT_US_PHONE_NO));
        addressTV.setText(psr_prefsManager.get(PSRConstants.CONTACT_US_ADDRESS));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }

    @OnClick(R.id.left_side_menu_option)
    void onBackClicked() {
        finish();
    }


    @OnClick(R.id.emailTV)
    void onClickEmail(View v) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{psr_prefsManager.get(PSRConstants.CONTACT_US_EMAIL)});
        // intent.putExtra(Intent.EXTRA_EMAIL, Uri.parse("mailto:" +psr_prefsManager.get(PSRConstants.CONTACT_US_EMAIL)) );
    /*    intent.putExtra(Intent.EXTRA_SUBJECT, "your_subject");
        intent.putExtra(Intent.EXTRA_TEXT, "your_text");*/
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, "Send mail"));
    }

    @OnClick(R.id.phoneNoTV)
    void onClickPhoneNumber(View v) {
        checkRuntTimePermission();
    }


    public void checkRuntTimePermission() {

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CALL_PHONE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + psr_prefsManager.get(PSRConstants.CONTACT_US_PHONE_NO)));
                        startActivity(callIntent);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(ContactUsActivity.this, R.string.phone_call_permiision_alert, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }


}