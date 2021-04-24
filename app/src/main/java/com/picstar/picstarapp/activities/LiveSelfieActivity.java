package com.picstar.picstarapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.fragments.CelebrityEventsFragment;
import com.picstar.picstarapp.fragments.LiveSelfieCompltdEventsFragment;
import com.picstar.picstarapp.fragments.LiveSelfiePendingEventsFragment;
import com.picstar.picstarapp.utils.PSRConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LiveSelfieActivity extends BaseActivity {


    @BindView(R.id.liveselfie_tab_layout)
    TabLayout tabLayout;

    @BindView(R.id.liveselfie_frame_layout)
    FrameLayout frameLayout;
    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView toolbarTitle;

    private String profilePicUrl="";
    CelebrityEventsFragment celebrityEventsFragment;
    LiveSelfiePendingEventsFragment liveSelfiePendingEventsFragment;
    LiveSelfieCompltdEventsFragment liveSelfieCompltdEventsFragment;
    private String celebrityId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.liveselfie_layout);
        ButterKnife.bind(this);
        leftSideMenu.setImageResource(R.drawable.ic_back);
        toolbarTitle.setText(getString(R.string.liveselfie_txt));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.events));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.pendingrequest_txt));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.completedrequests_txt));
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        if (getIntent() != null) {
            celebrityId = getIntent().getStringExtra(PSRConstants.USERID);
            profilePicUrl = getIntent().getStringExtra(PSRConstants.PROFILEPICURl);

            CelebrityEventsFragment celebrityEventsFragment = new CelebrityEventsFragment();
            Bundle bundle = new Bundle();
            bundle.putString("USER_ID", celebrityId);
            bundle.putString(PSRConstants.PROFILEPICURl,profilePicUrl);
            celebrityEventsFragment.setArguments( bundle);
            FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.liveselfie_frame_layout, celebrityEventsFragment);
            transaction.commit();
        }


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //viewPager2.setCurrentItem(tab.getPosition());
                switch (tab.getPosition()) {
                    case 0:
                        celebrityEventsFragment = new CelebrityEventsFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("USER_ID", celebrityId);
                        bundle.putString(PSRConstants.PROFILEPICURl,profilePicUrl);
                        celebrityEventsFragment.setArguments( bundle);
                        FragmentTransaction transaction = LiveSelfieActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.liveselfie_frame_layout, celebrityEventsFragment);
                        transaction.commit();
                        break;
                    case 1:

                        liveSelfiePendingEventsFragment = new LiveSelfiePendingEventsFragment();
                        Bundle bundle1 = new Bundle();
                        bundle1.putString("USER_ID", celebrityId);
                        bundle1.putString(PSRConstants.PROFILEPICURl,profilePicUrl);
                        liveSelfiePendingEventsFragment.setArguments( bundle1);
                        FragmentTransaction transaction1 = LiveSelfieActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction1.replace(R.id.liveselfie_frame_layout, liveSelfiePendingEventsFragment);
                        transaction1.commit();
                        break;
                    case 2:

                        liveSelfieCompltdEventsFragment = new LiveSelfieCompltdEventsFragment();
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("USER_ID", celebrityId);
                        bundle2.putString(PSRConstants.PROFILEPICURl,profilePicUrl);
                        liveSelfieCompltdEventsFragment.setArguments( bundle2);
                        FragmentTransaction transaction2 = LiveSelfieActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction2.replace(R.id.liveselfie_frame_layout, liveSelfieCompltdEventsFragment);
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


    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        finish();
    }


























/*
    @OnClick(R.id.eventstab)
    void onClickEvents(View view) {
        eventsTab.setCardBackgroundColor(getResources().getColor(R.color.button_color));
        eventsTv.setTextColor(getResources().getColor(R.color.white));
        eventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_selectedevents, 0, 0);

        pendingEvents.setCardBackgroundColor(getResources().getColor(R.color.white));
        pendingEventsTv.setTextColor(getResources().getColor(R.color.tabstxt_color));
        pendingEventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_unselectedphoto, 0, 0);

        completedEvents.setCardBackgroundColor(getResources().getColor(R.color.white));
        completedEventsTv.setTextColor(getResources().getColor(R.color.tabstxt_color));
        completedEventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_unselectedphoto, 0, 0);
    }

    @OnClick(R.id.pendingevnts_tab)
    void onClickPendingEvents(View view) {
        pendingEvents.setCardBackgroundColor(getResources().getColor(R.color.button_color));
        pendingEventsTv.setTextColor(getResources().getColor(R.color.white));
        pendingEventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_selected_pendingevents, 0, 0);

        eventsTab.setCardBackgroundColor(getResources().getColor(R.color.white));
        eventsTv.setTextColor(getResources().getColor(R.color.tabstxt_color));
        eventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_unselectedevents, 0, 0);

        completedEvents.setCardBackgroundColor(getResources().getColor(R.color.white));
        completedEventsTv.setTextColor(getResources().getColor(R.color.tabstxt_color));
        completedEventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_unselectedphoto, 0, 0);
    }

    @OnClick(R.id.completedEvents_tab)
    void onClickCompletdEvents(View view) {
        completedEvents.setCardBackgroundColor(getResources().getColor(R.color.button_color));
        completedEventsTv.setTextColor(getResources().getColor(R.color.white));
        completedEventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_selected_pendingevents, 0, 0);

        eventsTab.setCardBackgroundColor(getResources().getColor(R.color.white));
        eventsTv.setTextColor(getResources().getColor(R.color.tabstxt_color));
        eventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_unselectedevents, 0, 0);

        pendingEvents.setCardBackgroundColor(getResources().getColor(R.color.white));
        pendingEventsTv.setTextColor(getResources().getColor(R.color.tabstxt_color));
        pendingEventsTv.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_unselectedphoto, 0, 0);
    }*/
}
