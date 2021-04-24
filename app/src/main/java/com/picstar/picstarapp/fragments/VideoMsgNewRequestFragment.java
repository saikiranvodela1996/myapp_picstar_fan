package com.picstar.picstarapp.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgRequest;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.presenters.VideoMsgPresenter;
import com.picstar.picstarapp.mvp.views.VideoEventView;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoMsgNewRequestFragment extends BaseFragment implements VideoEventView {


    @BindView(R.id.eventname_Et)
    EditText eventNameEt;


    @BindView(R.id.event_date_Tv)
    TextView eventDateTv;
    @BindView(R.id.event_descriptn_Et)
    EditText eventDescriptnEt;
    DatePickerDialog picker;
    VideoMsgPresenter videoMsgPresenter;
    private String celebrityId = "";
    String eventDate = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.videomsg_newrequest, container, false);
        ButterKnife.bind(this, mainView);
        return mainView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        videoMsgPresenter = new VideoMsgPresenter();
        videoMsgPresenter.attachMvpView(this);

        if (getArguments() != null) {
            Bundle args = getArguments();
            celebrityId = args.getString(PSRConstants.SELECTED_CELEBRITYID);
        }

    }


    @OnClick(R.id.event_date_Tv)
    void onClickDate(View view) {
        final Calendar cldr = Calendar.getInstance();

        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        picker = new DatePickerDialog(getActivity(),

                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        int month = monthOfYear + 1;

                        eventDateTv.setText(appendZero(dayOfMonth + "") + "" + "/" + appendZero(month + "") + "/" + year);
                        eventDate = year + "" + "-" + month + "-" + dayOfMonth;
                        eventDateTv.setTextColor(getResources().getColor(R.color.white));
                    }
                }, year, month, day);
        picker.getDatePicker().setMinDate(new Date().getTime());
        picker.show();
    }


    private String appendZero(String month) {
        String m1 = "";
        if (month.length() == 1) {
            m1 = "0" + month;
        } else {
            return month;
        }

        return m1;
    }


    @OnClick(R.id.request_btn)
    void onClickVideoRequest(View view) {
        String eventName = eventNameEt.getText().toString();
        String enteredDate = eventDateTv.getText().toString();
        String eventDescriptn = eventDescriptnEt.getText().toString();

        if (eventName.trim().isEmpty() && eventDescriptn.trim().isEmpty() && enteredDate.equalsIgnoreCase(getResources().getString(R.string.dateformat_hinttxt))) {
            PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.fill_all_details_txt), null);
            return;
        }
        if (eventName.trim().isEmpty()) {
            PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.enter_eventname_txt), null);
            return;
        }
        if (enteredDate.equalsIgnoreCase(getResources().getString(R.string.dateformat_hinttxt))) {
            PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.enter_date_alertmsg), null);
            return;
        }

        if (eventDescriptn.trim().isEmpty()) {
            PSR_Utils.showAlert(getActivity(), getResources().getString(R.string.enter_descrptn_alertmsg), null);
            return;
        }


        createVideoMsgRequest(eventName, eventDescriptn);
    }


    public void createVideoMsgRequest(String eventName, String eventDescriptn) {
        if (PSR_Utils.isOnline(getActivity())) {
            PSR_Utils.showProgressDialog(getActivity());


            VideoMsgRequest videoMsgRequest = new VideoMsgRequest();
            videoMsgRequest.setVideoEventId("0");
            videoMsgRequest.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            videoMsgRequest.setCelebrityId(celebrityId);
            videoMsgRequest.setVideoEventName(eventName.trim());
            videoMsgRequest.setVideoEventDesc(eventDescriptn.trim());
            videoMsgRequest.setVideoEventDate(eventDate + "T23:59:59.999+00:00");
            videoMsgPresenter.videoMsgRequest(PSR_Utils.getHeader(psr_prefsManager), videoMsgRequest);

        } else {
            PSR_Utils.showNoNetworkAlert(getActivity());
        }
    }

    @Override
    public void onCreatingvideoEvntSuccess(VideoMsgResponse response) {

        if (PSR_Utils.isOnline(getActivity())) {
            CreateServiceReq createServiceReq = new CreateServiceReq();
            createServiceReq.setCelebrityId(celebrityId);
            createServiceReq.setUserId(psr_prefsManager.get(PSRConstants.USERID));
            createServiceReq.setVideoEventId(response.getInfo().getVideoEventId());
            createServiceReq.setServiceRequestTypeId(Integer.parseInt(PSRConstants.VIDEOMSGS_SERVICE_REQ_ID));
            videoMsgPresenter.createRequest(PSR_Utils.getHeader(psr_prefsManager), createServiceReq);
        } else {
            PSR_Utils.hideProgressDialog();
            PSR_Utils.showNoNetworkAlert(getActivity());
        }


    }

    @Override
    public void onCreatingvideoEvntFailure(VideoMsgResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage(), null);
    }

    @Override
    public void onCreatingServiceReqSuccess(CreateServiceResponse response) {
        PSR_Utils.hideProgressDialog();
        eventNameEt.setText("");
        eventDateTv.setText(getResources().getString(R.string.dateformat_hinttxt));
        eventDateTv.setTextColor(getResources().getColor(R.color.et_hint_color));
        eventDescriptnEt.setText("");
        PSR_Utils.showAlert(getActivity(), response.getMessage(), null);

    }

    @Override
    public void onCreatingServiceReqFailure(CreateServiceResponse response) {
        PSR_Utils.hideProgressDialog();
        PSR_Utils.showAlert(getActivity(), response.getMessage(), null);
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
