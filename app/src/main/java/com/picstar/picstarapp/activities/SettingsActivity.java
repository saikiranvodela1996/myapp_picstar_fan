package com.picstar.picstarapp.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.callbacks.OnClickCelebrity;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.mvp.models.celebrities.Info;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends BaseActivity implements OnClickCelebrity {
    @BindView(R.id.username_TV)
    TextView userName;
    @BindView(R.id.profilepic_imgView)
    CircleImageView profilePicImg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        userName.setText(psr_prefsManager.get(PSRConstants.USERNAME));
        Glide.with(this)
                .load(psr_prefsManager.get(PSRConstants.USERPROFILEPIC))
                .centerCrop()
                .placeholder(R.drawable.ic_profilepholder)
                .into(profilePicImg);
    }

    @OnClick(R.id.back_btn)
    void onClickBack(View view) {
        finish();
    }

    @OnClick(R.id.username_layout)
    void onClickLayout(View view) {
        Intent intent = new Intent(this, MyProfileActivity.class);
        startActivity(intent);
    }


    @OnClick(R.id.lang_layout)
    void onClickLangLayout(View view) {
        onClickLanguage();
    }
/*
    @OnClick(R.id.notifications_layout)
    void onClickNotfn(View v) {
        PSR_Utils.showAlert(this, "Work in progress", null);
    }*/

    @OnClick(R.id.terms_policy_layout)
    void onClickTerms(View v) {
        Intent intent = new Intent(this, PrivacyPolicyActivity.class);
        startActivity(intent);
    }


    @OnClick(R.id.contact_us_layout)
    void onClickContact(View v) {
        Intent intent = new Intent(this, ContactUsActivity.class);
        startActivity(intent);
    }


    @OnClick(R.id.logout_btn)
    void onClickLogout(View v) {
        PSR_Utils.logoutDialog(this, this);
        // PSR_Utils.showAlert(this, "Work in progress", null);
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }

    public void onClickLanguage() {
        final String[] Language = {"English", "Spanish"};
        final int checkedItem;
        final boolean whichh;
        if (LocaleHelper.getLanguage(this).equals("en")) {
            checkedItem = 0;
            whichh = true;
        } else {
            whichh = false;
            checkedItem = 1;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getResources().getString(R.string.select_language_txt))
                .setSingleChoiceItems(Language, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (Language[which].equals("English") && checkedItem == 1) {
                            LocaleHelper.setLocale(SettingsActivity.this, "en");
                        }
                        //if user select prefered language as Hindi then
                        if (Language[which].equals("Spanish") && checkedItem == 0) {
                            LocaleHelper.setLocale(SettingsActivity.this, "es");
                        }
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        System.exit(1);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onClickCelebrity(Info info) {

    }

    @Override
    public void onClickHeart(Info info) {

    }

    @Override
    public void onClickRecommendCelebrity(Info info) {

    }

    @Override
    public void onClickLogout() {
        PSR_Utils.doLogout(this, psr_prefsManager);
    }
}
