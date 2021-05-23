package com.picstar.picstarapp.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.fragments.LiveVideoCompltdRequestsFragment;
import com.picstar.picstarapp.fragments.LiveVideoNewRequestFragment;
import com.picstar.picstarapp.fragments.LiveVideoPendingRequestsFragmnt;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.utils.PSRConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LiveVideoActivity extends AppCompatActivity {

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView toolbarTitle;
    @BindView(R.id.videomsgsframe_Layout)
    FrameLayout frameLayout;

    private String celebrityID = "";
    LiveVideoNewRequestFragment liveVideoNewRequestFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_video);
        ButterKnife.bind(this);

        leftSideMenu.setImageResource(R.drawable.ic_back);
        toolbarTitle.setText(getString(R.string.live_video_txt));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.newrequest_txt));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.pendingrequest_txt));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.completedrequests_txt));


        if (getIntent() != null) {
            celebrityID = getIntent().getStringExtra(PSRConstants.USERID);
        }

        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        liveVideoNewRequestFragment = new LiveVideoNewRequestFragment();
        Bundle bundle = new Bundle();
        bundle.putString(PSRConstants.SELECTED_CELEBRITYID, celebrityID);
        liveVideoNewRequestFragment.setArguments(bundle);
        FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.videomsgsframe_Layout, liveVideoNewRequestFragment);
        transaction.commit();


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //viewPager2.setCurrentItem(tab.getPosition());
                switch (tab.getPosition()) {
                    case 0:
                        liveVideoNewRequestFragment = new LiveVideoNewRequestFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString(PSRConstants.SELECTED_CELEBRITYID, celebrityID);
                        liveVideoNewRequestFragment.setArguments(bundle);
                        FragmentTransaction transaction = LiveVideoActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.videomsgsframe_Layout, liveVideoNewRequestFragment);
                        transaction.commit();
                        break;
                    case 1:

                        LiveVideoPendingRequestsFragmnt videoMsgPendingRequestsFragmnt = new LiveVideoPendingRequestsFragmnt();
                        Bundle bundle1 = new Bundle();
                        bundle1.putString(PSRConstants.SELECTED_CELEBRITYID, celebrityID);
                        videoMsgPendingRequestsFragmnt.setArguments(bundle1);

                        FragmentTransaction transaction1 = LiveVideoActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction1.replace(R.id.videomsgsframe_Layout, videoMsgPendingRequestsFragmnt);
                        transaction1.commit();
                        break;
                    case 2:

                        LiveVideoCompltdRequestsFragment videoMsgCompltdRequestsFragment = new LiveVideoCompltdRequestsFragment();
                        Bundle bundle2 = new Bundle();
                        bundle2.putString(PSRConstants.SELECTED_CELEBRITYID, celebrityID);
                        videoMsgCompltdRequestsFragment.setArguments(bundle2);

                        FragmentTransaction transaction2 = LiveVideoActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction2.replace(R.id.videomsgsframe_Layout, videoMsgCompltdRequestsFragment);
                        transaction2.commit();
                        break;

                }


            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }

    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        finish();
    }
}
