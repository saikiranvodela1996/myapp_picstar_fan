package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;

public interface VideoEventView extends BaseMvpView {



    void onCreatingvideoEvntSuccess(VideoMsgResponse response);

     void onCreatingvideoEvntFailure(VideoMsgResponse response);

     void onCreatingServiceReqSuccess(CreateServiceResponse response);

     void onCreatingServiceReqFailure(CreateServiceResponse response);
}
