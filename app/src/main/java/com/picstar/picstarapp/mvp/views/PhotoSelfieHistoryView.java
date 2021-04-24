package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.eventhistory.PendingHistoryResponse;
import com.picstar.picstarapp.mvp.models.history.HistoryResponse;

public interface PhotoSelfieHistoryView extends BaseMvpView {

    void onGettingHistorySuccess(PendingHistoryResponse re);
    void onGettingHistoryFailure(PendingHistoryResponse response);
}
