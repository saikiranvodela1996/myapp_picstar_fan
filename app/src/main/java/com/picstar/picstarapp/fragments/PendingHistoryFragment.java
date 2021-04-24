package com.picstar.picstarapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.activities.CelebrityDetailsActivity;
import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.activities.PaymentActivity;
import com.picstar.picstarapp.adapters.MyHistoryAdapter;
import com.picstar.picstarapp.callbacks.OnClickPhotoSelfieHistory;
import com.picstar.picstarapp.helpers.ProgressBarViewHolder;
import com.picstar.picstarapp.mvp.models.eventhistory.Info;
import com.picstar.picstarapp.mvp.models.eventhistory.PendingHistoryResponse;
import com.picstar.picstarapp.mvp.presenters.PhotoSelfieHistoryPresenter;
import com.picstar.picstarapp.mvp.views.PhotoSelfieHistoryView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PendingHistoryFragment extends BaseFragment implements PhotoSelfieHistoryView, OnClickPhotoSelfieHistory {


    @BindView(R.id.photoselfie_historyRV)
    RecyclerView recyclerView;
    @BindView(R.id.noListTv)
    TextView noListTv;

    PhotoSelfieHistoryPresenter photoSelfieHistoryPresenter;


    private int currentPage = 1;
    private String status;
    private MyHistoryAdapter adapter;
    List<Info> pendingHistoryList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private View footerView;
    public ProgressBarViewHolder footerViewHolder;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.photoselfie_histry_layout, container, false);
        ButterKnife.bind(this, mainView);
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);
        return mainView;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        photoSelfieHistoryPresenter = new PhotoSelfieHistoryPresenter();
        photoSelfieHistoryPresenter.attachMvpView(this);
        footerViewHolder = new ProgressBarViewHolder(footerView);
        adapter = new MyHistoryAdapter(getActivity(), pendingHistoryList, this, false);
        recyclerView.setAdapter(adapter);
        adapter.setFooterView(footerView);
        getPendingHistory(status);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(getActivity())) {
                            isLoading = true;
                            getPendingHistory(status);
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });


    }


    public void getPendingHistory(String status) {
        this.status = status;
        if (PSR_Utils.isOnline(getActivity())) {
            if (noListTv.getVisibility() == View.VISIBLE)
                noListTv.setVisibility(View.GONE);
            if (recyclerView.getVisibility() == View.GONE)
                recyclerView.setVisibility(View.VISIBLE);

            if (!isLoading) {
                PSR_Utils.showProgressDialog(getActivity());
                pendingHistoryList.clear();
                adapter.notifyDataSetChanged();
            }

            photoSelfieHistoryPresenter.getPendingEventsHistory(PSR_Utils.getHeader(psr_prefsManager), PSRConstants.historyPendingKey, psr_prefsManager.get(PSRConstants.USERID), currentPage);
        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }
    }


    @Override
    public void onGettingHistorySuccess(PendingHistoryResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }
        if (response.getInfo() != null && response.getInfo().size() != 0) {
            pendingHistoryList.addAll(response.getInfo());
            adapter.notifyDataSetChanged();
            currentPage++;
        } else if (currentPage == 1) {
            recyclerView.setVisibility(View.GONE);
            noListTv.setVisibility(View.VISIBLE);
            noListTv.setText(response.getMessage().toString());
        }
    }

    @Override
    public void onGettingHistoryFailure(PendingHistoryResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onSessionExpired() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.doLogout(getActivity(), psr_prefsManager);
    }

    @Override
    public void onServerError() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onNoInternetConnection() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showNoNetworkAlert(getActivity());
    }

    @Override
    public void onErrorCode(String s) {
        PSR_Utils.hideProgressDialog();
    }


    @Override
    public void onClickPhotoSelfie(String imagePath, boolean isCameFromCompletedHistory) {
        PSR_Utils.showImageAlert(getActivity(), imagePath, isCameFromCompletedHistory);
        //  PSR_Utils.showAlert(getActivity(), "Work in Progress", null);
    }

    @Override
    public void onClickPaynow(Info info) {

        PSR_Utils.navigatingAccordingly(getActivity(),info);
    }



}
