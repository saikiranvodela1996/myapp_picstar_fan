package com.picstar.picstarapp.fragments;

import android.content.Context;
import android.content.Intent;
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
import com.picstar.picstarapp.activities.PaymentActivity;
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

public class VideoMsgPendingRequestsFragmnt extends BaseFragment implements VideoMsgsPendingView {

    @BindView(R.id.recycler_View)
    RecyclerView recyclerView;


    @BindView(R.id.noListTv)
    TextView noListTv;
    VideoMsgsPendingReqPresenter videoMsgsPendingReqPresenter;
    private String celebrityId = "";
    PendingVideoMsgsAdapter pendingVideoMsgsAdapter;
    private View footerView;
    private FooterViewHolder footerViewHolder;
    private LinearLayoutManager linearLayoutManager;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;
    private int currentPage = 1;
    List<Info> pendingvideoMsgs;
    ///List<Info> pendingvideoMsgs;

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
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);
        return mainView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pendingvideoMsgs = new ArrayList<>();
        videoMsgsPendingReqPresenter = new VideoMsgsPendingReqPresenter();
        videoMsgsPendingReqPresenter.attachMvpView(this);

        footerViewHolder = new FooterViewHolder(footerView);
        pendingVideoMsgsAdapter = new PendingVideoMsgsAdapter(getActivity(), pendingvideoMsgs, false,this);
        recyclerView.setAdapter(pendingVideoMsgsAdapter);

        pendingVideoMsgsAdapter.setFooterView(footerView);
        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString(PSRConstants.SELECTED_CELEBRITYID);
            getPendingVideoMsgs();
        }


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                int currentItems = linearLayoutManager.getChildCount();
                int totalItems = linearLayoutManager.getItemCount();
                int scrolledoutItems = linearLayoutManager.findFirstVisibleItemPosition();
                //  if ( (currentItems + scrolledoutItems == totalItems)) {

                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(getActivity())) {
                            isLoading = true;
                            currentPage++;
                            loadNextPage();
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);


                        }
                    }

                }

                // }
            }
        });

    }

    public void loadNextPage() {
        VideoMsgsPendingReq videoMsgsPendingReq = new VideoMsgsPendingReq();
        videoMsgsPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
        videoMsgsPendingReq.setCelebrityId(celebrityId);
        videoMsgsPendingReq.setStatus(PSRConstants.PENDING);
        videoMsgsPendingReq.setPage(currentPage);
        videoMsgsPendingReqPresenter.getPendingVideoMsgs(PSR_Utils.getHeader(psr_prefsManager), videoMsgsPendingReq);
    }


    public void getPendingVideoMsgs() {
        if (PSR_Utils.isOnline(getActivity())) {
            PSR_Utils.showProgressDialog(getActivity());
            VideoMsgsPendingReq videoMsgsPendingReq = new VideoMsgsPendingReq();
            videoMsgsPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            videoMsgsPendingReq.setCelebrityId(celebrityId);
            videoMsgsPendingReq.setStatus(PSRConstants.PENDING);
            videoMsgsPendingReq.setPage(1);
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
            pendingvideoMsgs.addAll(response.getInfo());
            pendingVideoMsgsAdapter.notifyDataSetChanged();
        } else {
            if(currentPage==1)
            {
                recyclerView.setVisibility(View.GONE);
                noListTv.setVisibility(View.VISIBLE);
                noListTv.setText(response.getMessage().toString());
            }

        }
    }

    @Override
    public void gettingPendingorcompltdVideoMsgsFailure(PendingVideoMsgsResponse response) {
        PSR_Utils.hideProgressDialog();
        if (currentPage > 1) {
            currentPage--;
        }
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onClickPayNow(Info info) {
        if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.VIDEOMSGS_SERVICE_REQ_ID) && !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            ///VIDEOMSG IS REQUESTED BUT PAYMENT IS NOT DONE..So....navigating to payment...
            Intent intent = new Intent(getActivity(), PaymentActivity.class);
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
            intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
            intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
            intent.putExtra(PSRConstants.SERVICEREQID, info.getServiceRequestId());
            intent.putExtra(PSRConstants.SERVICEREQTYPEID, PSRConstants.VIDEOMSGS_SERVICE_REQ_ID);
            startActivity(intent);
        }
    }

    @Override
    public void onSessionExpired() {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.doLogout(getActivity(), psr_prefsManager);
    }

    @Override
    public void onServerError() {
        PSR_Utils.hideProgressDialog();
        if (currentPage > 1) {
            currentPage--;
        }
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public Context getMvpContext() {
        return null;
    }

    @Override
    public void onError(Throwable throwable) {
        PSR_Utils.hideProgressDialog();
        if (currentPage > 1) {
            currentPage--;
        }
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }

    @Override
    public void onNoInternetConnection() {
        PSR_Utils.hideProgressDialog();
        if (currentPage > 1) {
            currentPage--;
        }
        PSR_Utils.showNoNetworkAlert(getActivity());
    }

    @Override
    public void onErrorCode(String s) {
        PSR_Utils.hideProgressDialog();
        if (currentPage > 1) {
            currentPage--;
        }
        PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.somethingwnt_wrong_txt), null);
    }
}
