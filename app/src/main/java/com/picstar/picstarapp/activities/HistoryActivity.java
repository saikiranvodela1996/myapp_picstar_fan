package com.picstar.picstarapp.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.picstar.picstarapp.R;

import com.picstar.picstarapp.fragments.CompletedEventsHistoryFragment;
import com.picstar.picstarapp.fragments.UpComingEventsFragment;
import com.picstar.picstarapp.fragments.PendingHistoryFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryActivity extends BaseActivity {


    @BindView(R.id.history_tabs_layout)
    TabLayout tabLayout;
    @BindView(R.id.history_frame_layout)
    FrameLayout frameLayout;
    @BindView(R.id.toolBar)
    Toolbar toolbar;
    @BindView(R.id.toolbar_title)
    TextView toolbar_title;



    private UpComingEventsFragment upComingEventsFragment;
    private PendingHistoryFragment pendingHistoryFragment;
    private CompletedEventsHistoryFragment completedEventsHistoryFragment;
    private int tag = 2;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        ButterKnife.bind(this);
        toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
      ///  toolbar.setOverflowIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_filter));
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.upcoming_txt)).setTag(0));
        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.pending_txt)).setTag(1));
        tabLayout.addTab(tabLayout.newTab().setText(getResources().getString(R.string.completed_txt)).setTag(2));
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        upComingEventsFragment = new UpComingEventsFragment();
        FragmentTransaction transaction = HistoryActivity.this.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.history_frame_layout, upComingEventsFragment);
        transaction.commit();


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tag = Integer.parseInt(tab.getTag().toString());

                switch (tag) {
                    case 0:
                        upComingEventsFragment = new UpComingEventsFragment();
                        FragmentTransaction transaction = HistoryActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.history_frame_layout, upComingEventsFragment);
                        transaction.commit();
                        break;
                    case 1:
                        pendingHistoryFragment = new PendingHistoryFragment();
                        FragmentTransaction transaction1 = HistoryActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction1.replace(R.id.history_frame_layout, pendingHistoryFragment);
                        transaction1.commit();
                        break;
                    case 2:
                        completedEventsHistoryFragment = new CompletedEventsHistoryFragment();
                        FragmentTransaction transaction2 = HistoryActivity.this.getSupportFragmentManager().beginTransaction();
                        transaction2.replace(R.id.history_frame_layout, completedEventsHistoryFragment);
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



}







