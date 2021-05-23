package com.picstar.picstarapp.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.auth0.android.Auth0;
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.adapters.CelebritiesListAdapter;
import com.picstar.picstarapp.adapters.RecommendedCelebritiesAdapter;
import com.picstar.picstarapp.callbacks.OnClickCelebrity;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.mvp.models.categories.CategoriesListResponse;
import com.picstar.picstarapp.mvp.models.categories.Info;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdRequest;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdResponse;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavRequest;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavResponse;
import com.picstar.picstarapp.mvp.presenters.CategoriesPresenter;
import com.picstar.picstarapp.mvp.views.CategoriesView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DashBoardActivity extends BaseActivity implements CategoriesView, NavigationView.OnNavigationItemSelectedListener, OnClickCelebrity, PSR_Utils.OnSingleBtnDialogClick {
    @Nullable
    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.search_option_btn)
    ImageView searchOption;
    @BindView(R.id.title_Tv)
    TextView titleTv;
    @BindView(R.id.search_layout)
    LinearLayout searchLayout;


    private TabLayout tabLayout;
    @BindView(R.id.search_et)
    EditText searchEt;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.noListTv)
    TextView noListTv;
    @BindView(R.id.norecommendListTv)
    TextView noRecommendListTv;

    @BindView(R.id.recommended_recycler_view)
    RecyclerView recommendedRecyclerView;
    @BindView(R.id.recommended_txt)
    TextView recommendedTxt;
    private boolean isSearchVisible = false;
    CategoriesPresenter categoriesPresenter;
    public int categoryId = 0;

    private DrawerLayout drawer;
    private NavigationView navigationView;
    public static int navItemIndex = 0;

    private Handler searchHandler;
    private LinearLayoutManager linearLayoutManager;
    private View footerView;
    private View footerView2;
    private ProgressBarViewHolder footerViewHolder;
    private ProgressBarViewHolder2 footerViewHolder2;
    private CelebritiesListAdapter adapter;
    List<com.picstar.picstarapp.mvp.models.celebrities.Info> celebritiesList = new ArrayList<>();
    List<com.picstar.picstarapp.mvp.models.celebrities.Info> recommendedCelebritiesList = new ArrayList<>();
    private RecommendedCelebritiesAdapter recommendedCelebritiesAdapter;
    private boolean loadMore = false;
    private int currentPage = 1;
    private boolean isAllPagesShown = false;
    private String searchString;
    private boolean isFromFavs = false;
    private ImageView userProfileImgV;
    private int recommendsCurrentPage = 1;
    private boolean isRecommendLoadMore = false;
    private boolean isAllRecommPagesShown = false;
    TextView userNameTv;
    private Auth0 auth0;
    private boolean isOutSideClicked;
    private boolean isDrawerOpen = false;
    private boolean isUserBlocked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);
        ButterKnife.bind(this);
        auth0 = new Auth0(this);
        auth0.setOIDCConformant(true);
        categoriesPresenter = new CategoriesPresenter();
        categoriesPresenter.attachMvpView(this);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = getLayoutInflater().inflate(R.layout.item_loading, recyclerView, false);
        footerViewHolder = new ProgressBarViewHolder(footerView);


        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        adapter = new CelebritiesListAdapter(this, celebritiesList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
        adapter.setFooterView(footerView);


        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recommendedRecyclerView.setLayoutManager(layoutManager2);
        footerView2 = getLayoutInflater().inflate(R.layout.item_loading, recommendedRecyclerView, false);
        footerViewHolder2 = new ProgressBarViewHolder2(footerView2);
        recommendedCelebritiesAdapter = new RecommendedCelebritiesAdapter(this, recommendedCelebritiesList, this);
        recommendedRecyclerView.setAdapter(recommendedCelebritiesAdapter);
        recommendedCelebritiesAdapter.setFooterView(footerView2);

        searchHandler = new Handler();
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout2);
        navigationView = (NavigationView) findViewById(R.id.nav_view);

        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        getAllCategoriesList();
        leftSideMenu.setImageResource(R.drawable.ic_sidemenu);
        searchOption.setImageResource(R.drawable.ic_search);
        titleTv.setText(getString(R.string.home_txt));
        tabLayout = findViewById(R.id.tab_layout);
        setUpNavigationView();
        View headerview = navigationView.getHeaderView(0);
        ImageView closeBtn = (ImageView) headerview.findViewById(R.id.close_btn);
        userProfileImgV = (ImageView) headerview.findViewById(R.id.userprofile_pic);
        userNameTv = (TextView) headerview.findViewById(R.id.username_tv);


        searchEt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;
                if (!searchEt.getText().toString().isEmpty()) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (event.getRawX() >= (searchEt.getRight() - searchEt.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                            // your action here
                            searchEt.setText("");
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        userProfileImgV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.closeDrawers();
                Intent intent = new Intent(DashBoardActivity.this, MyProfileActivity.class);
                startActivity(intent);
            }
        });
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.closeDrawers();
            }
        });


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollHorizontally(1)) {
                    if (!loadMore && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(DashBoardActivity.this)) {
                            if (!isSearchVisible && !isAllPagesShown) {
                                loadMore = true;
                                getCelebritiesListFromServer(categoryId);
                                footerViewHolder.progressBar.setVisibility(View.VISIBLE);

                            } else if (isSearchVisible && !isAllPagesShown) {
                                loadMore = true;
                                searchPagination(searchString);
                            }

                        }
                    }
                }
            }
        });


        recommendedRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recommendedRecyclerView.canScrollHorizontally(1)) {
                    if (!isRecommendLoadMore && !isAllRecommPagesShown) {
                        if (PSR_Utils.isOnline(DashBoardActivity.this)) {
                            isRecommendLoadMore = true;
                            getRecommendedCelebrities(categoryId);
                            footerViewHolder2.progressBar.setVisibility(View.VISIBLE);

                        }
                    }
                }
            }
        });


    }

    public void searchPagination(String searchString) {
        this.searchString = searchString;
        if (PSR_Utils.isOnline(this)) {
            if (categoryId == PSRConstants.FAVOURITES_CATEGORY_ID) {
                categoriesPresenter.searchForCelebrity(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), categoryId, currentPage, searchString, psr_prefsManager.get(PSRConstants.USERID));
            } else {
                categoriesPresenter.searchForCelebrity(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), categoryId, currentPage, searchString, "");
            }

        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }

    }


    @Override
    public void onClickCelebrity(com.picstar.picstarapp.mvp.models.celebrities.Info celebrityDetails) {
        Intent intent = new Intent(this, CelebrityDetailsActivity.class);
        intent.putExtra("CELEBRITY_DETAILS", (Serializable) celebrityDetails);
        startActivity(intent);

    }

    @Override
    public void onClickHeart(com.picstar.picstarapp.mvp.models.celebrities.Info info) {

        if (PSR_Utils.isOnline(this)) {
            PSR_Utils.showProgressDialog(this);
            AddtoFavRequest addtoFavRequest = new AddtoFavRequest();
            addtoFavRequest.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            addtoFavRequest.setCelebrityId(info.getUserId());
            if (categoryId == PSRConstants.FAVOURITES_CATEGORY_ID) {
                addtoFavRequest.setFavStatus(false);
            } else {
                addtoFavRequest.setFavStatus(true);
            }
            categoriesPresenter.addToMyFavs(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), addtoFavRequest, info.getUserId());
        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }

    }

    @Override
    public void onClickRecommendCelebrity(com.picstar.picstarapp.mvp.models.celebrities.Info recommCelebDetails) {
        Intent intent = new Intent(this, CelebrityDetailsActivity.class);
        intent.putExtra("CELEBRITY_DETAILS", (Serializable) recommCelebDetails);
        startActivity(intent);
    }

    @Override
    public void onClickOk() {
        PSR_Utils.navigateToContacUsScreen(this);
    }


    class ProgressBarViewHolder {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        ProgressBarViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    class ProgressBarViewHolder2 {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        ProgressBarViewHolder2(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @OnClick(R.id.left_side_menu_option)
    void onclickMenu(View view) {
        if (!isUserBlocked)
            drawer.openDrawer(GravityCompat.START);
    }


    public void onClickSearch() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (isSearchVisible) {
                    String searchString = s.toString().trim();
                    searchHandler.removeCallbacksAndMessages(null);
                    cancelRequests();
                    searchHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            currentPage = 1;
                            isAllPagesShown = false;
                            searchForCelebrity(searchString);
                        }
                    }, 1000);

                }

                if (s.toString().isEmpty()) {
                    searchEt.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_search), null, null, null);
                } else {
                    searchEt.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_search), null, ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_close3), null);
                }
            }
        });

    }

    public void searchForCelebrity(String searchString) {
        this.searchString = searchString;
        if (PSR_Utils.isOnline(this)) {
            PSR_Utils.showProgressDialog(this);
            //celebritiesList.clear();
            //noListTv.setVisibility(View.GONE);
            if (categoryId == PSRConstants.FAVOURITES_CATEGORY_ID) {
                categoriesPresenter.searchForCelebrity(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), categoryId, currentPage, searchString, psr_prefsManager.get(PSRConstants.USERID));
            } else {
                categoriesPresenter.searchForCelebrity(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), categoryId, currentPage, searchString, "");
            }

        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }
    }

    public void cancelRequests() {
        categoriesPresenter.cancelRequests();
    }

    @Nullable
    @OnClick(R.id.search_option_btn)
    void onClickSearchIcon() {

        if (tabLayout.getTabCount() != 0) {
            if (isSearchVisible) {
                searchLayout.setVisibility(View.GONE);
                isSearchVisible = false;
                // dashBoardFragment.setSearchEnable(false);
            } else {
                searchLayout.setVisibility(View.VISIBLE);
                isSearchVisible = true;
                // dashBoardFragment.setSearchEnable(true);
                onClickSearch();
            }
        }
    }
/*

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isDrawerOpen) { //Your code here to check whether drawer is open or not.

                View content = findViewById(R.id.drawer_layout2); //drawer view id
                int[] contentLocation = new int[2];
                content.getLocationOnScreen(contentLocation);
                Rect rect = new Rect(contentLocation[0],
                        contentLocation[1],
                        contentLocation[0] + content.getWidth(),
                        contentLocation[1] + content.getHeight());

                if (!(rect.contains((int) event.getX(), (int) event.getY()))) {
                    isOutSideClicked = true;
                } else {
                    isOutSideClicked = false;
                }

            } else {
                return super.dispatchTouchEvent(event);
            }
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && isOutSideClicked) {
            isOutSideClicked = false;
            return super.dispatchTouchEvent(event);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && isOutSideClicked) {
            return super.dispatchTouchEvent(event);
        }

        if (isOutSideClicked) {
            return true; //restrict the touch event here
        }else{
            return super.dispatchTouchEvent(event);
        }
    }

*/


    private void setUpNavigationView() {

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {


                switch (menuItem.getItemId()) {

                    case R.id.navigation_home:
                        drawer.closeDrawers();
                        break;
                    case R.id.naviagtion_history:
                        drawer.closeDrawers();
                        navigateToHistory();
                        break;
                    case R.id.naviagtion_settings:
                        drawer.closeDrawers();
                        Intent intent2 = new Intent(DashBoardActivity.this, SettingsActivity.class);
                        startActivity(intent2);
                        //  PSR_Utils.showAlert(DashBoardActivity.this, "Work in Progress", null);
                        break;
                    case R.id.naviagtion_logout:
                        drawer.closeDrawers();
                        openLogoutDialog();
                        break;


                    default:
                        navItemIndex = 0;
                }


                //Checking if the item is in checked state or not, if not make it in checked state
                if (menuItem.isChecked()) {
                    menuItem.setChecked(false);
                } else {
                    menuItem.setChecked(true);
                }
                menuItem.setChecked(true);


                return true;
            }
        });


    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }

    private void navigateToHistory() {
        if (!PSR_Utils.handleDoubleClick(DashBoardActivity.this)) {
            return;
        }

        Intent intent = new Intent(DashBoardActivity.this, HistoryActivity.class);
        startActivity(intent);
    }


    void openLogoutDialog() {
        PSR_Utils.logoutDialog(DashBoardActivity.this, this);
    }

    @Override
    public void onClickLogout() {
        PSR_Utils.doLogout(this, psr_prefsManager);
    }

    void getAllCategoriesList() {
        if (PSR_Utils.isOnline(this)) {
            PSR_Utils.showProgressDialog(this);
            categoriesPresenter.getCategoriesList(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), psr_prefsManager.get(PSRConstants.USERID));
        } else {
            PSR_Utils.showNoNetworkAlert(this);
        }

    }


    @Override
    public void onSessionExpired() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.doLogout(this, psr_prefsManager);
    }

    @Override
    public void onServerError() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onNoInternetConnection() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showNoNetworkAlert(this);
    }

    @Override
    public void onErrorCode(String s) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onGettingList(CategoriesListResponse response) {

        for (Info info : response.getInfo()) {
            tabLayout.addTab(tabLayout.newTab().setTag(info.getCategoryId()).setText(info.getName()));
        }
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        categoryId = response.getInfo().get(0).getCategoryId();
        getCelebritiesByID(categoryId);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isSearchVisible = false;
                searchEt.setText("");
                searchLayout.setVisibility(View.GONE);
                loadMore = false;
                isRecommendLoadMore = false;
                int tag = (Integer) tab.getTag();
                if (PSR_Utils.isOnline(DashBoardActivity.this)) {
                    getCelebritiesByID(tag);
                } else {
                    recommendedTxt.setVisibility(View.GONE);
                    celebritiesList.clear();
                    recommendedCelebritiesList.clear();
                    adapter.notifyDataSetChanged();
                    recommendedCelebritiesAdapter.notifyDataSetChanged();
                    PSR_Utils.showNoNetworkAlert(DashBoardActivity.this);

                }


                Log.e("CATEGORY _ID", tab.getTag().toString());
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
    public void userBlocked(String msg) {
        isUserBlocked = true;
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(this, msg, null, this);
    }


    private void getCelebritiesByID(int id) {
        if (PSR_Utils.isOnline(DashBoardActivity.this)) {
            PSR_Utils.showProgressDialog(DashBoardActivity.this);
            getCelebritiesListFromServer(id);
            if (id == PSRConstants.FAVOURITES_CATEGORY_ID) {
                recommendedRecyclerView.setVisibility(View.GONE);
                recommendedTxt.setVisibility(View.GONE);
                noRecommendListTv.setVisibility(View.GONE);
            } else {
                getRecommendedCelebrities(id);
            }

        } else {
            PSR_Utils.showNoNetworkAlert(DashBoardActivity.this);
        }
    }


    private void getRecommendedCelebrities(int id) {
        if (!isRecommendLoadMore) {
            recommendsCurrentPage = 1;
            isAllRecommPagesShown = false;
            recommendedCelebritiesList.clear();
        }

        CelebritiesByIdRequest recommendRequest = new CelebritiesByIdRequest();
        recommendRequest.setCategoryId(id);
        recommendRequest.setPage(recommendsCurrentPage);
        categoriesPresenter.getRecommCelebrities(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), recommendRequest);

    }

    public void getCelebritiesListFromServer(int id) {
        this.categoryId = id;
        if (!loadMore) {
            currentPage = 1;
            isAllPagesShown = false;
            celebritiesList.clear();
        }
      /*  if (id == PSRConstants.FAVOURITES_CATEGORY_ID) {

            CelebritiesByIdRequest celebritiesByIdRequest = new CelebritiesByIdRequest();
            celebritiesByIdRequest.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            celebritiesByIdRequest.setPerPage(10);
            celebritiesByIdRequest.setPage(currentPage);
            categoriesPresenter.getMyFavCelebrities(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), celebritiesByIdRequest);
        } else {*/
        CelebritiesByIdRequest celebritiesByIdRequest = new CelebritiesByIdRequest();
        celebritiesByIdRequest.setCategoryId(id);
        celebritiesByIdRequest.setPerPage(10);
        celebritiesByIdRequest.setUserId(psr_prefsManager.get(PSRConstants.USERID));
        celebritiesByIdRequest.setPage(currentPage);
        categoriesPresenter.getCelebritiesById(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), celebritiesByIdRequest);
        //   }

    }


    @Override
    protected void onResume() {
        super.onResume();
        Glide.with(this)
                .load(psr_prefsManager.get(PSRConstants.USERPROFILEPIC))
                .centerCrop()
                .placeholder(R.drawable.ic_profilepholder)
                .into(userProfileImgV);
        userNameTv.setText(psr_prefsManager.get(PSRConstants.USERNAME));
    }

    @Override
    public void onFailure(CategoriesListResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage(), null);

    }


    @Override
    public void onGettingCelebritiesFailure(CelebritiesByIdResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage().toString(), null);
    }

    @Override
    public void onAddingToFavsSuccess(AddtoFavResponse response, String celebrityId) {
        isFromFavs = true;
        PSR_Utils.hideProgressDialog();

        if (categoryId == PSRConstants.FAVOURITES_CATEGORY_ID) {
            getCelebritiesListFromServer(categoryId);
        } else {
            if (!response.getMessage().contains("Already") && !response.getMessage().contains("Ya")) {
                for (com.picstar.picstarapp.mvp.models.celebrities.Info celebrity : celebritiesList) {
                    if (celebrity.getUserId().equals(celebrityId)) {
                        celebrity.setFavs(celebrity.getFavs() + 1);
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }

            }
        }

        PSR_Utils.showAlert(this, response.getMessage(), null);
    }

    @Override
    public void onAddingToFavsFailure(AddtoFavResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage(), null);
    }

    @Override
    public void onGettingRecommCelebritiesList(CelebritiesByIdResponse recommCelebrityResponse) {
        PSR_Utils.hideProgressDialog();
        footerViewHolder2.progressBar.setVisibility(View.GONE);
        if (recommCelebrityResponse.getInfo() == null || recommCelebrityResponse.getInfo().size() == 0 || recommCelebrityResponse.getInfo().isEmpty()) {
            isAllRecommPagesShown = true;
        }
        try {
            if (recommCelebrityResponse.getInfo() != null && recommCelebrityResponse.getInfo().size() != 0) {
                recommendsCurrentPage++;
                noRecommendListTv.setVisibility(View.GONE);
                recommendedTxt.setVisibility(View.VISIBLE);
                recommendedRecyclerView.setVisibility(View.VISIBLE);
                recommendedCelebritiesList.addAll(recommCelebrityResponse.getInfo());
                recommendedCelebritiesAdapter.notifyDataSetChanged();
            } else {
                if (recommendsCurrentPage == 1) {
                    recommendedRecyclerView.setVisibility(View.GONE);
                    noRecommendListTv.setVisibility(View.VISIBLE);
                    noRecommendListTv.setText(recommCelebrityResponse.getMessage().toString());
                }
            }
            isRecommendLoadMore = false;
        } catch (Exception e) {
            e.printStackTrace();
            PSR_Utils.hideProgressDialog();
        }

    }

    @Override
    public void onGettimgRecommCelebritiesFailure(CelebritiesByIdResponse recommCelebrityResponse) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, recommCelebrityResponse.getMessage().toString(), null);
    }

    @Override
    public void onGettingSearchListSuccess(CelebritiesByIdResponse response) {
        PSR_Utils.hideProgressDialog();
        if (!loadMore) {
            celebritiesList.clear();
        }
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }

        try {
            if (response.getInfo().size() != 0) {
                currentPage++;
                noListTv.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                noListTv.setVisibility(View.GONE);
                celebritiesList.addAll(response.getInfo());
                adapter.notifyDataSetChanged();

            } else {
                if (currentPage == 1) {
                    recyclerView.setVisibility(View.GONE);
                    noListTv.setVisibility(View.VISIBLE);
                    noListTv.setText(response.getMessage().toString());
                }
            }

            loadMore = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGettingCelebritiesList(CelebritiesByIdResponse response) {

        PSR_Utils.hideProgressDialog();
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }

        if (response.getInfo().size() != 0) {
            currentPage++;
            noListTv.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            celebritiesList.addAll(response.getInfo());
          /*  if (!loadMore) {
                recyclerView.smoothScrollToPosition(0);
            }*/
            adapter.notifyDataSetChanged();
            recyclerView.requestLayout();


        } else {
            if (currentPage == 1) {
                recyclerView.setVisibility(View.GONE);
                noListTv.setVisibility(View.VISIBLE);
                noListTv.setText(response.getMessage().toString());
            }

        }
        loadMore = false;


    }

    @Override
    public void onGettingSearchListFailure(CelebritiesByIdResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage().toString(), null);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show();
        return false;
    }
}
