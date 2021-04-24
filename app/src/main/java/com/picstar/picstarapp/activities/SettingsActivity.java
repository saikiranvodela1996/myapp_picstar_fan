package com.picstar.picstarapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.utils.PSRConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends BaseActivity {
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

    @OnClick(R.id.back_btn) void onClickBack(View view)
    {
        finish();
    }
    @OnClick(R.id.username_layout) void onClickLayout(View view)
    {
        Intent intent=new Intent(this,MyProfileActivity.class);
        startActivity(intent);
    }
}
