package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;

import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.Info;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;

public interface PendingLiveSelfieView  extends BaseMvpView {


    void onGettingPendingLiveSelfies(LiveSelfiePendingResponse response);
    void userBlocked(String msg);
    void onGettingPendingLiveSelfiesFailure(LiveSelfiePendingResponse response);

    void onClickPhotoSelfie(String imagePath, boolean isCameFromCompletedHistory);

    void onClickPaynow(Info info);
}
