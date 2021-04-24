package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.livevideo.create.LiveVideoResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;

public interface LiveVideoEventView extends BaseMvpView {

    void onCreatingvideoEvntSuccess(LiveVideoResponse response);

    void onCreatingvideoEvntFailure(LiveVideoResponse response);

    void onCreatingServiceReqSuccess(CreateServiceResponse response);

    void onCreatingServiceReqFailure(CreateServiceResponse response);

}
