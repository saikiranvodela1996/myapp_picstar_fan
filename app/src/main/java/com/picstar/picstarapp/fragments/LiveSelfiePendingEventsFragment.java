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
import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.activities.PaymentActivity;
import com.picstar.picstarapp.adapters.CelebrityEventAdapter;
import com.picstar.picstarapp.adapters.PendingLiveSelfieReqAdapter;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.VideoMsgsPendingReq;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.Info;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;
import com.picstar.picstarapp.mvp.presenters.PendingLiveSelfiePresenter;
import com.picstar.picstarapp.mvp.views.PendingLiveSelfieView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LiveSelfiePendingEventsFragment extends BaseFragment implements PendingLiveSelfieView {


    @BindView(R.id.liveselfie_pending_recycler_View)
    RecyclerView recyclerView;

    @BindView(R.id.liveselfie_noListTv)
    TextView noListTv;

    PendingLiveSelfiePresenter pendingLiveSelfiePresenter;

    private String celebrityProfilePicUrl = "";
    private LinearLayoutManager linearLayoutManager;
    private View footerView;
    private ProgressBarViewHolder footerViewHolder;
    private PendingLiveSelfieReqAdapter pendingLiveSelfieReqAdapter;
    List<Info> pendingLiveSelfiesList;
    private boolean isLoading = false;
    private boolean isAllPagesShown = false;
    private String celebrityId;
    private int currentPage = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.liveselfie_pending_evnts_layout, container, false);
        ButterKnife.bind(this, mainView);

        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);
        return mainView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pendingLiveSelfiesList = new ArrayList<>();
        pendingLiveSelfiePresenter = new PendingLiveSelfiePresenter();
        pendingLiveSelfiePresenter.attachMvpView(this);


        footerViewHolder = new ProgressBarViewHolder(footerView);


        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString("USER_ID");
            celebrityProfilePicUrl = args.getString(PSRConstants.PROFILEPICURl);
            pendingLiveSelfieReqAdapter = new PendingLiveSelfieReqAdapter(getActivity(), pendingLiveSelfiesList, celebrityProfilePicUrl, LiveSelfiePendingEventsFragment.this, this);
            recyclerView.setAdapter(pendingLiveSelfieReqAdapter);
            pendingLiveSelfieReqAdapter.setFooterView(footerView);

            if (PSR_Utils.isOnline(getActivity())) {
                PSR_Utils.showProgressDialog(getActivity());
                getPendingLiveSelfies(celebrityId);
            } else {
                PSR_Utils.showNoNetworkAlert(getActivity());
            }
        }


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1)) {
                    if (!isLoading && !isAllPagesShown) {
                        if (PSR_Utils.isOnline(getActivity())) {
                            isLoading = true;
                            getPendingLiveSelfies(celebrityId);
                            footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }

                }

            }
        });


    }


    public void getPendingLiveSelfies(String celebrityId) {
        VideoMsgsPendingReq videoMsgsPendingReq = new VideoMsgsPendingReq();
        videoMsgsPendingReq.setCelebrityId(celebrityId);
        videoMsgsPendingReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
        videoMsgsPendingReq.setStatus(PSRConstants.PENDING);
        videoMsgsPendingReq.setPage(currentPage);
        pendingLiveSelfiePresenter.getPendingLiveSelfieReqs(PSR_Utils.getHeader(psr_prefsManager), videoMsgsPendingReq);
    }


    @Override
    public void onGettingPendingLiveSelfies(LiveSelfiePendingResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if (response.getInfo() == null || response.getInfo().size() == 0 || response.getInfo().isEmpty()) {
            isAllPagesShown = true;
        }

        if (response.getInfo().size() != 0) {
            pendingLiveSelfiesList.addAll(response.getInfo());
            pendingLiveSelfieReqAdapter.notifyDataSetChanged();
            currentPage++;
        } else {
            if (currentPage == 1) {
                recyclerView.setVisibility(View.GONE);
                noListTv.setVisibility(View.VISIBLE);
                noListTv.setText(response.getMessage().toString());
            }
        }


    }


    @Override
    public void onGettingPendingLiveSelfiesFailure(LiveSelfiePendingResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onClickPhotoSelfie(String imagePath, boolean isCameFromCompletedHistory) {
        PSR_Utils.showImageAlert(getActivity(), imagePath, false);
    }

    @Override
    public void onClickPaynow(Info info) {

        navigateAccordingly(info);
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


    class ProgressBarViewHolder {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

        ProgressBarViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    private void navigateAccordingly(Info info) {

        if (info.getLiveEvent() != null && info.getFilePath() != null && !info.getStatus().equalsIgnoreCase(PSRConstants.PAYMENTSUCESS)) {
            /// live selfie taken but payment is not completed......go to payment screen.....
            Intent intent = new Intent(getActivity(), PaymentActivity.class);
            intent.putExtra(PSRConstants.S3UPLOADED_IMAGEURL, info.getFilePath().toString());
            intent.putExtra(PSRConstants.SERVICEREQTYPEID, PSRConstants.LIVESELFIE_SERVICE_REQ_ID);
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
            intent.putExtra(PSRConstants.SERVICEREQID, info.getServiceRequestId());
            intent.putExtra(PSRConstants.EVENTID, info.getLiveEvent().getEventId().toString());
            intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
            intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
            intent.putExtra(PSRConstants.SELECTEDCELEBRITYNAME, info.getCelebrityUser().getUsername());
            startActivity(intent);

        } else if (info.getLiveEvent() != null && info.getFilePath() == null) {
            ///LIVE selfie requested but LIVESELFIE didnt taken with celebrity......

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(getActivity(), LiveSelfieCameraActivity.class);
                    intent.putExtra(PSRConstants.EVENTID, info.getLiveEvent().getEventId().toString());
                    intent.putExtra(PSRConstants.SERVICECOST, info.getAmount().toString());
                    intent.putExtra(PSRConstants.CELEBRITYID, info.getCelebrityId());
                    intent.putExtra(PSRConstants.SELECTEDCELEBRITYNAME, info.getCelebrityUser().getUsername());
                    intent.putExtra(PSRConstants.ISCAMEFROMHISTORY, true);
                    intent.putExtra(PSRConstants.SERVICEREQID, info.getServiceRequestId());
                    startActivity(intent);
                }
            };
            PSR_Utils.checkPermissionToProgress(getActivity(), runnable);

        }

    }
}

