package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.Info;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;

public interface VideoMsgsPendingView extends BaseMvpView {

    void gettingPendingorcompltdVideoMsgsSuccess(PendingVideoMsgsResponse response);

    void gettingPendingorcompltdVideoMsgsFailure(PendingVideoMsgsResponse response);
    void onClickPayNow(Info info);
}
