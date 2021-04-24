package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.videomsgshistoryresponse.VideoMsgsHistoryResponse;

public interface VideoMsgsHistoryView  extends BaseMvpView {
    void onGettingHistorySuccess(VideoMsgsHistoryResponse response);
    void onGettingHistoryFailure(VideoMsgsHistoryResponse response);
}
