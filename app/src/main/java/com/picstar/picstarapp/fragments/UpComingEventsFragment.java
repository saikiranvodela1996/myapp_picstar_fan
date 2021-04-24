package com.picstar.picstarapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.adapters.MyHistoryAdapter;
import com.picstar.picstarapp.callbacks.OnClickPhotoSelfieHistory;
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

public class UpComingEventsFragment extends BaseFragment  implements PhotoSelfieHistoryView, OnClickPhotoSelfieHistory {
    @BindView(R.id.historyRV)
    RecyclerView historyRV;
    @BindView(R.id.noListTv)
    TextView noListTv;

    private MyHistoryAdapter adapter;
    List<Info> historyList = new ArrayList<>();
    private int currentPage=1;
    private boolean isLoading=false;
    private boolean isAllPagesShown=false;
    private LinearLayoutManager linearLayoutManager;
    private View footerView;
    private ProgressBarViewHolder footerViewHolder;
    private PhotoSelfieHistoryPresenter photoSelfieHistoryPresenter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.history_events_layout, container, false);
        ButterKnife.bind(this, mainView);
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        historyRV.setLayoutManager(linearLayoutManager);
        footerView = inflater.inflate(R.layout.item_loading, historyRV, false);
        return  mainView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        photoSelfieHistoryPresenter = new PhotoSelfieHistoryPresenter();
        photoSelfieHistoryPresenter.attachMvpView(this);

        footerViewHolder = new ProgressBarViewHolder(footerView);
        adapter = new MyHistoryAdapter(getActivity(), historyList,this,false);
        historyRV.setAdapter(adapter);
        adapter.setFooterView(footerView);

        getUpComingEventsHistory();



        historyRV.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(getActivity())) {
                            isLoading = true;
                            getUpComingEventsHistory();
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }

    public void getUpComingEventsHistory() {
        if (PSR_Utils.isOnline(getActivity())) {
            if(noListTv.getVisibility()==View.VISIBLE)
                noListTv.setVisibility(View.GONE);
            if(historyRV.getVisibility()==View.GONE)
                historyRV.setVisibility(View.VISIBLE);

            if(!isLoading) {
                PSR_Utils.showProgressDialog(getActivity());
                historyList.clear();
                adapter.notifyDataSetChanged();
            }

            photoSelfieHistoryPresenter.getPendingEventsHistory(PSR_Utils.getHeader(psr_prefsManager),PSRConstants.historyUpcomingKey, psr_prefsManager.get(PSRConstants.USERID), currentPage);
        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }
    }

    @Override
    public void onGettingHistorySuccess(PendingHistoryResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if(response.getInfo()==null||response.getInfo().size()==0||response.getInfo().isEmpty())
        {
            isAllPagesShown = true;
        }
        if(response.getInfo()!=null && response.getInfo().size()!=0) {
            historyList.addAll(response.getInfo());
            adapter.notifyDataSetChanged();
            currentPage++;
        }else if(currentPage==1)
        {
            historyRV.setVisibility(View.GONE);
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
        PSR_Utils.showAlert(getActivity(),getResources().getString(R.string.somethingwnt_wrong_txt),null);
    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(),getResources().getString(R.string.somethingwnt_wrong_txt),null);
    }

    @Override
    public void onNoInternetConnection() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showNoNetworkAlert(getActivity());
    }

    @Override
    public void onErrorCode(String s) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(),getResources().getString(R.string.somethingwnt_wrong_txt),null);
    }

    @Override
    public void onClickPhotoSelfie(String imagePath,boolean isCameFromCompletedHistory) {
        PSR_Utils.showImageAlert(getActivity(), imagePath,isCameFromCompletedHistory);
    }

    @Override
    public void onClickPaynow(Info info) {
        PSR_Utils.navigatingAccordingly(getActivity(),info);
    }


    class ProgressBarViewHolder {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        ProgressBarViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

}
