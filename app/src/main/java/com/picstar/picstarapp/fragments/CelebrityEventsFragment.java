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
import com.picstar.picstarapp.adapters.CelebrityEventAdapter;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsRequest;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.Info;
import com.picstar.picstarapp.mvp.models.celebrityevents.RequestLiveSelfieRespnse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.presenters.CelebrityEventsPresenter;
import com.picstar.picstarapp.mvp.views.CelebrityEventsView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CelebrityEventsFragment extends BaseFragment implements CelebrityEventsView {


    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.noevents_tv)
    TextView noEventsTv;


    CelebrityEventsPresenter celebrityEventsPresenter;
    private String profilePicUrl = "";
    private String celebrityId = "";
    private CelebrityEventAdapter celebrityEventAdapter;
    List<Info> celebrityEvents;
    private LinearLayoutManager linearLayoutManager;
    private View footerView;
    private ProgressBarViewHolder footerViewHolder;
    private boolean isLoading=false;
    private boolean isAllPagesShown=false;
    private int currentPage=1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.celebrity_events_layout, container, false);
        ButterKnife.bind(this, mainView);

        linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        footerView = inflater.inflate(R.layout.item_loading, recyclerView, false);

        return mainView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        celebrityEvents = new ArrayList<>();
        celebrityEventsPresenter = new CelebrityEventsPresenter();
        celebrityEventsPresenter.attachMvpView(this);

        footerViewHolder = new ProgressBarViewHolder(footerView);

        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString("USER_ID");
            profilePicUrl = args.getString(PSRConstants.PROFILEPICURl);

            celebrityEventAdapter = new CelebrityEventAdapter(getActivity(), celebrityEvents, profilePicUrl, CelebrityEventsFragment.this);
            recyclerView.setAdapter(celebrityEventAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            celebrityEventAdapter.setFooterView(footerView);


            if (PSR_Utils.isOnline(getActivity())) {
                PSR_Utils.showProgressDialog(getActivity());
                getCelebrityEvents(celebrityId);
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
                            getCelebrityEvents(celebrityId);
                          footerViewHolder.progressBar.setVisibility(View.VISIBLE);
                        }
                    }

                }


            }
        });






    }

    public void getCelebrityEvents(String celebrityId) {
        CelebrityEventsRequest celebrityEventsRequest = new CelebrityEventsRequest();
        celebrityEventsRequest.setUser_id(psr_prefsManager.get(PSRConstants.USERID));
        celebrityEventsRequest.setCelebrity_id(celebrityId);
        celebrityEventsRequest.setPage(currentPage);
        celebrityEventsPresenter.getCelebrityEvents(PSR_Utils.getHeader(psr_prefsManager), celebrityEventsRequest);

    }


    public void onClickCelebrityEvent(Info item) {
        if (PSR_Utils.isOnline(getActivity())) {
            PSR_Utils.showProgressDialog(getActivity());
            CreateServiceReq createServiceReq = new CreateServiceReq();
            createServiceReq.setCelebrityId(celebrityId);
            createServiceReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            createServiceReq.setServiceRequestTypeId(Integer.parseInt(PSRConstants.LIVESELFIE_SERVICE_REQ_ID));
            createServiceReq.setEventId(item.getEventId());
            celebrityEventsPresenter.requestForLiveSelfie(PSR_Utils.getHeader(psr_prefsManager), createServiceReq, item.getEventId());
        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }

    }

    @Override
    public void onGettingCelebrityEventsSuccess(CelebrityEventsResponse response) {
        PSR_Utils.hideProgressDialog();
        isLoading = false;
        footerViewHolder.progressBar.setVisibility(View.GONE);
        if(response.getInfo()==null||response.getInfo().size()==0||response.getInfo().isEmpty())
        {
            isAllPagesShown = true;
        }
        if (response.getInfo().size() != 0) {
            currentPage++;
            celebrityEvents.addAll(response.getInfo());
            celebrityEventAdapter.notifyDataSetChanged();

        } else {
            if(currentPage==1) {
                recyclerView.setVisibility(View.GONE);
                noEventsTv.setVisibility(View.VISIBLE);
                noEventsTv.setText(response.getMessage().toString());
            }
        }
    }

    @Override
    public void onGettingCelebrityEventsFailure(CelebrityEventsResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage().toString(), null);
    }

    @Override
    public void onRequestingSelfieSuccess(CreateServiceResponse requestLiveSelfieRespnse, int eventId) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), requestLiveSelfieRespnse.getMessage(), null);
        for (Info info : celebrityEvents) {
            if (info.getEventId() == eventId) {
                info.setCreatedRequests(1);
            }
            celebrityEventAdapter.notifyDataSetChanged();

        }
    }

    @Override
    public void onRequestingSelfieFailure(CreateServiceResponse requestLiveSelfieRespnse) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), requestLiveSelfieRespnse.getMessage(), null);
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



     class ProgressBarViewHolder {
        @BindView(R.id.progressBar)
        ProgressBar progressBar;

         ProgressBarViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }


}
