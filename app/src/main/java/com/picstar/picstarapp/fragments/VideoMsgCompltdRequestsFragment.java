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
import com.picstar.picstarapp.adapters.PendingVideoMsgsAdapter;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.Info;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.VideoMsgsPendingReq;
import com.picstar.picstarapp.mvp.presenters.VideoMsgsPendingReqPresenter;
import com.picstar.picstarapp.mvp.views.VideoMsgsPendingView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VideoMsgCompltdRequestsFragment extends BaseFragment implements VideoMsgsPendingView {


    @BindView(R.id.recycler_View)
    RecyclerView recyclerView;
    @BindView(R.id.noListTv)
    TextView noListTv;
    private String celebrityId;
    private VideoMsgsPendingReqPresenter videoMsgsPendingReqPresenter;
    private PendingVideoMsgsAdapter pendingVideoMsgsAdapter;
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

        videoMsgsPendingReqPresenter = new VideoMsgsPendingReqPresenter();
        videoMsgsPendingReqPresenter.attachMvpView(this);


        footerViewHolder = new FooterViewHolder(footerView);

        pendingVideoMsgsAdapter = new PendingVideoMsgsAdapter(getActivity(), compltedvideoMsgs, true,this);
        recyclerView.setAdapter(pendingVideoMsgsAdapter);
        pendingVideoMsgsAdapter.setFooterView(footerView);
        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString(PSRConstants.SELECTED_CELEBRITYID);
            getCompletedVideoMsgs();
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
                            getCompletedVideoMsgs();
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }


    public void getCompletedVideoMsgs() {
        if (PSR_Utils.isOnline(getActivity())) {
            if(currentPage==1) {
                PSR_Utils.showProgressDialog(getActivity());
            }
            VideoMsgsPendingReq videoMsgsPendingReq = new VideoMsgsPendingReq();
            videoMsgsPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            videoMsgsPendingReq.setCelebrityId(celebrityId);
            videoMsgsPendingReq.setStatus(PSRConstants.CLOSED);
            videoMsgsPendingReq.setPage(currentPage);
            videoMsgsPendingReqPresenter.getPendingVideoMsgs(PSR_Utils.getHeader(psr_prefsManager), videoMsgsPendingReq);

        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }
    }

    @Override
    public void gettingPendingorcompltdVideoMsgsSuccess(PendingVideoMsgsResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo()==null||response.getInfo().size()==0||response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }


        if (response.getInfo().size() != 0) {
            compltedvideoMsgs.addAll(response.getInfo());
            pendingVideoMsgsAdapter.notifyDataSetChanged();

        } else {
            if (currentPage == 1) {
                recyclerView.setVisibility(View.GONE);
                noListTv.setVisibility(View.VISIBLE);
                noListTv.setText(response.getMessage().toString());
            }
        }
    }

    @Override
    public void gettingPendingorcompltdVideoMsgsFailure(PendingVideoMsgsResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onClickPayNow(Info info) {
///DO nothing
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
