package com.picstar.picstarapp.mvp.views;


import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.history.HistoryResponse;
import com.picstar.picstarapp.mvp.models.liveeventshistory.LiveEventsHistoryResponse;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;

public interface HistoryView extends BaseMvpView {


    void onGettingHistorySuccess(LiveEventsHistoryResponse response);
    void onGettingHistoryFailure(LiveEventsHistoryResponse response);
}
