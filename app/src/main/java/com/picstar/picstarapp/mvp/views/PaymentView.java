package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;

import okhttp3.ResponseBody;

public interface PaymentView extends BaseMvpView {
    void onGettingChargesResponseSuccess(String responseBody);
    void onGettingChargesResponseFailure();

    void onCreatingPaymentServReqSuccess(CreateServiceResponse response);

    void onCreatingPaymentServReqFailure(CreateServiceResponse response);
}
