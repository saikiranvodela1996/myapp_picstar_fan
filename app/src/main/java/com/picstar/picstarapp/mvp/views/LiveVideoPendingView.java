package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.livevideo.pending.PendingLiveVideoResponse;
import com.picstar.picstarapp.mvp.models.livevideo.pending.Info;

public interface LiveVideoPendingView extends BaseMvpView {

    void gettingPendingorcompltdLiveVideoSuccess(PendingLiveVideoResponse response);

    void gettingPendingorcompltdLiveVideoFailure(PendingLiveVideoResponse response);
    void onClickPaynow(Info info);
}
