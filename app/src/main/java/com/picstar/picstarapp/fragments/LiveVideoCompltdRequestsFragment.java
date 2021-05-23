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
import com.picstar.picstarapp.adapters.PendingLiveVideosAdapter;
import com.picstar.picstarapp.mvp.models.livevideo.pending.Info;
import com.picstar.picstarapp.mvp.models.livevideo.pending.LiveVideoPendingReq;
import com.picstar.picstarapp.mvp.models.livevideo.pending.PendingLiveVideoResponse;
import com.picstar.picstarapp.mvp.presenters.PendingLiveVideoReqPresenter;
import com.picstar.picstarapp.mvp.views.LiveVideoPendingView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LiveVideoCompltdRequestsFragment extends BaseFragment implements LiveVideoPendingView, PSR_Utils.OnSingleBtnDialogClick {

    @BindView(R.id.recycler_View)
    RecyclerView recyclerView;
    @BindView(R.id.noListTv)
    TextView noListTv;
    private String celebrityId;
    private PendingLiveVideoReqPresenter pendingLiveVideoReqPresenter;
    private PendingLiveVideosAdapter pendingLiveVideosAdapter;
    List<Info> compltedvideoMsgs;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;
    private int currentPage = 1;
    private View footerView;
    private FooterViewHolder footerViewHolder;


    class FooterViewHolder {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        private FooterViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.videomsg_pendingrequests, container, false);
        ButterKnife.bind(this, mainView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);

        return mainView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        compltedvideoMsgs = new ArrayList<>();

        pendingLiveVideoReqPresenter = new PendingLiveVideoReqPresenter();
        pendingLiveVideoReqPresenter.attachMvpView(this);


        footerViewHolder = new FooterViewHolder(footerView);

        pendingLiveVideosAdapter = new PendingLiveVideosAdapter(getActivity(), compltedvideoMsgs, true, this);
        recyclerView.setAdapter(pendingLiveVideosAdapter);
        pendingLiveVideosAdapter.setFooterView(footerView);
        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString(PSRConstants.SELECTED_CELEBRITYID);
            getCompletedLiveVideos();
        }


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(getActivity())) {
                            isLoading = true;
                            currentPage++;
                            getCompletedLiveVideos();
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }


    public void getCompletedLiveVideos() {
        if (PSR_Utils.isOnline(getActivity())) {
            if (currentPage == 1) {
                PSR_Utils.showProgressDialog(getActivity());
            }
            LiveVideoPendingReq liveVideoPendingReq = new LiveVideoPendingReq();
            liveVideoPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            liveVideoPendingReq.setCelebrityId(celebrityId);
            liveVideoPendingReq.setStatus(PSRConstants.CLOSED);
            liveVideoPendingReq.setPage(currentPage);
            pendingLiveVideoReqPresenter.getPendingVideoMsgs(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), liveVideoPendingReq);

        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }
    }

    @Override
    public void gettingPendingorcompltdLiveVideoSuccess(PendingLiveVideoResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }


        if (response.getInfo().size() != 0) {
            compltedvideoMsgs.addAll(response.getInfo());
            pendingLiveVideosAdapter.notifyDataSetChanged();

        } else {
            if (currentPage == 1) {
                recyclerView.setVisibility(View.GONE);
                noListTv.setVisibility(View.VISIBLE);
                noListTv.setText(response.getMessage().toString());
            }
        }
    }

    @Override
    public void userBlocked(String msg) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.singleBtnAlert(getActivity(), msg, null, this);
    }

    @Override
    public void onClickOk() {
        PSR_Utils.navigateToContacUsScreen(getActivity());
    }

    @Override
    public void gettingPendingorcompltdLiveVideoFailure(PendingLiveVideoResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onClickPaynow(Info info) {
////DO NOTHING>>>
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
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }
}