package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.Info;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;

public interface VideoMsgsPendingView extends BaseMvpView {

    void gettingPendingorcompltdVideoMsgsSuccess(PendingVideoMsgsResponse response);

    void userBlocked(String msg);

    void gettingPendingorcompltdVideoMsgsFailure(PendingVideoMsgsResponse response);

    void onClickPayNow(Info info);

    void onClickVideo(String videoUrl);
}
