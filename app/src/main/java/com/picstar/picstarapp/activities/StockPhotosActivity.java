package com.picstar.picstarapp.activities;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.adapters.StockPhotosAdapter;
import com.picstar.picstarapp.helpers.LocaleHelper;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdRequest;
import com.picstar.picstarapp.mvp.models.stockphotos.Info;
import com.picstar.picstarapp.mvp.models.stockphotos.StockPhotosResponse;
import com.picstar.picstarapp.mvp.presenters.StockPhotosPresenter;
import com.picstar.picstarapp.mvp.views.StockPhotosView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StockPhotosActivity extends BaseActivity implements StockPhotosView, PSR_Utils.OnSingleBtnDialogClick {
    @BindView(R.id.stockpics_recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView toolbarTitle;
    @BindView(R.id.nostockPics_tv)
    TextView noPicsTv;
    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    private String celebrityID = "";
    StockPhotosPresenter stockPhotosPresenter;
    StockPhotosAdapter stockPhotosAdapter;
    private LinearLayoutManager linearLayoutManager;
    private View footerView;
    private ProgressBarViewHolder footerViewHolder;
    private List<Info> stockPicsList;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;
    private int currentPage = 1;

    Bitmap image = null;
    private String path = "";
    private String photoSelfieCost;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_stockphotos);
        ButterKnife.bind(this);
        leftSideMenu.setImageResource(R.drawable.ic_back);
        toolbarTitle.setText(getString(R.string.photoSelfie_txt));

        stockPicsList = new ArrayList<>();
       /* linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = getLayoutInflater().inflate(R.layout.item_loading, recyclerView, false);

        footerViewHolder = new ProgressBarViewHolder(footerView);*/


        stockPhotosPresenter = new StockPhotosPresenter();
        stockPhotosPresenter.attachMvpView(this);
        if (getIntent() != null) {
            celebrityID = getIntent().getStringExtra(PSRConstants.USERID);
            photoSelfieCost = getIntent().getStringExtra(PSRConstants.PHOTOSELFIECOST);

            GridLayoutManager manager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(manager);
            stockPhotosAdapter = new StockPhotosAdapter(this, stockPicsList);
            recyclerView.setAdapter(stockPhotosAdapter);
            if (PSR_Utils.isOnline(this)) {
                PSR_Utils.showProgressDialog(this);
                gettingAllStockPhotos();
            } else {
                PSR_Utils.showNoNetworkAlert(this);
            }
        }


   /*     for (int i = 0; i < 50; i++) {
            Info info = new Info();
            info.setCreatedAt("");
            info.setPhotoId(0);
            info.setPhotoType(0);
            info.setUpdatedAt("");
            info.setUserId("");
            info.setPhotoUrl("https://picstar.s3-us-west-2.amazonaws.com/Julian+Castro/Cover.jpg");
            stockPicsList.add(info);
        }
        stockPhotosAdapter.notifyDataSetChanged();
*/

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy >= 0) {
                    // Scrolling up
                } else {
                    progressBar.setVisibility(View.GONE);
                    // Scrolling down
                }
            }


            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(StockPhotosActivity.this)) {
                            isLoading = true;
                            gettingAllStockPhotos();
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    }

                }

            }
        });

    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase, LocaleHelper.getLanguage(newBase)));
    }

    public void gettingAllStockPhotos() {
        CelebritiesByIdRequest celebritiesByIdRequest = new CelebritiesByIdRequest();
        celebritiesByIdRequest.setUserId(celebrityID);
        ///this value changes during pagination.
        celebritiesByIdRequest.setPage(currentPage);
        stockPhotosPresenter.getStockPicsOfCelebrity(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), celebritiesByIdRequest);

    }

    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        finish();
    }

    public void onPhotoPicked(Info info) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                getBitmapFromUrl(info);

            }
        };
        PSR_Utils.checkPermissionToProgress(this, runnable);


    }

    private void getBitmapFromUrl(Info info) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            PSR_Utils.showProgressDialog(StockPhotosActivity.this);
                        }
                    });

                    URL url = new URL(info.getPhotoUrl());
                    Bitmap image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    path = saveToInternalStorage(image);
                } catch (IOException e) {
                    PSR_Utils.hideProgressDialog();
                    System.out.println(e);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PSR_Utils.hideProgressDialog();
                        Intent intent = new Intent(StockPhotosActivity.this, PhotoSelfieCameraActivity.class);
                        intent.putExtra(PSRConstants.CELEBRITYID, info.getUserId());
                        intent.putExtra(PSRConstants.CELEBRITYPHOTOID, info.getPhotoId());
                        intent.putExtra(PSRConstants.PHOTOSELFIECOST, photoSelfieCost);
                        intent.putExtra("IMAGEPATH", path);
                        startActivity(intent);

                    }
                });
            }
        }).start();
    }


    private String saveToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "celebrityphoto.jpg");
        if (mypath.exists()) {
            mypath.delete();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mypath.getAbsolutePath();
    }

    @Override
    public void onGettingStockPicsSuccess(StockPhotosResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }

        if (response.getInfo().size() != 0 && !response.getInfo().isEmpty()) {
            currentPage++;
            stockPicsList.addAll(response.getInfo());
            stockPhotosAdapter.notifyDataSetChanged();
        } else {
            if (currentPage == 1) {
                recyclerView.setVisibility(View.GONE);
                noPicsTv.setVisibility(View.VISIBLE);
                noPicsTv.setText(response.getMessage().toString());
            }
        }
    }

    @Override
    public void userBlocked(String msg) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(this, msg, null, this);
    }

    @Override
    public void onGettingStockPicsFailure(StockPhotosResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(this, response.getMessage().toString(), null);
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


}
