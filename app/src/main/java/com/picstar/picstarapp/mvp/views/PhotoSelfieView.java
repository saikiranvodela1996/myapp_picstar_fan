package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;

public interface PhotoSelfieView extends BaseMvpView {

    void onCreatingServiceReqSuccess(CreateServiceResponse response);

    void userBlocked(String msg);

    void onCreatingServiceReqFailure(CreateServiceResponse response);
}
