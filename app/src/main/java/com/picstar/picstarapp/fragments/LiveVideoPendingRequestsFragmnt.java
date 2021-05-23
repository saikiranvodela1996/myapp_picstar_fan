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


public class LiveVideoPendingRequestsFragmnt extends BaseFragment implements LiveVideoPendingView, PSR_Utils.OnSingleBtnDialogClick {

    @BindView(R.id.recycler_View)
    RecyclerView recyclerView;


    @BindView(R.id.noListTv)
    TextView noListTv;
    PendingLiveVideoReqPresenter pendingLiveVideoReqPresenter;
    private String celebrityId = "";
    PendingLiveVideosAdapter pendingLiveVideosAdapter;
    private View footerView;
    private FooterViewHolder footerViewHolder;
    private LinearLayoutManager linearLayoutManager;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;
    private int currentPage = 1;
    List<Info> pendinglivevideos;


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
        View mainView = inflater.inflate(R.layout.fragment_live_video_pending_requests_fragmnt, container, false);
        ButterKnife.bind(this, mainView);
        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);
        return mainView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pendinglivevideos = new ArrayList<>();
        pendingLiveVideoReqPresenter = new PendingLiveVideoReqPresenter();
        pendingLiveVideoReqPresenter.attachMvpView(this);

        footerViewHolder = new FooterViewHolder(footerView);
        pendingLiveVideosAdapter = new PendingLiveVideosAdapter(getActivity(), pendinglivevideos, false, this);
        recyclerView.setAdapter(pendingLiveVideosAdapter);

        pendingLiveVideosAdapter.setFooterView(footerView);
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
        LiveVideoPendingReq liveVideoPendingReq = new LiveVideoPendingReq();
        liveVideoPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
        liveVideoPendingReq.setCelebrityId(celebrityId);
        liveVideoPendingReq.setStatus(PSRConstants.PENDING);
        liveVideoPendingReq.setPage(currentPage);
        pendingLiveVideoReqPresenter.getPendingVideoMsgs(psr_prefsManager.get(PSRConstants.SELECTED_LANGUAGE), PSR_Utils.getHeader(psr_prefsManager), liveVideoPendingReq);
    }


    public void getPendingVideoMsgs() {
        if (PSR_Utils.isOnline(getActivity())) {
            PSR_Utils.showProgressDialog(getActivity());
            LiveVideoPendingReq liveVideoPendingReq = new LiveVideoPendingReq();
            liveVideoPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            liveVideoPendingReq.setCelebrityId(celebrityId);
            liveVideoPendingReq.setStatus(PSRConstants.PENDING);
            liveVideoPendingReq.setPage(1);
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
            pendinglivevideos.addAll(response.getInfo());
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
        if (currentPage > 1) {
            currentPage--;
        }
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onClickPaynow(com.picstar.picstarapp.mvp.models.livevideo.pending.Info info) {

        if (info.getServiceRequestTypeId() == Integer.parseInt(PSRConstants.LIVE_VIDEO_SERVICE_REQ_ID) && !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            ///LIVEVIDEO IS REQUESTED BUT PAYMENT IS NOT DONE...So....navigating to payment...
            Intent intent = new Intent(getActivity(), PaymentActivity.class);
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
            intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
            intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
            intent.putExtra(PSRConstants.SERVICEREQID, info.getServiceRequestId());
            intent.putExtra(PSRConstants.SERVICEREQTYPEID, PSRConstants.LIVE_VIDEO_SERVICE_REQ_ID);
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
