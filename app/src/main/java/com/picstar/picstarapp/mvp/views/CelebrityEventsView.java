package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.RequestLiveSelfieRespnse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;

public interface CelebrityEventsView  extends BaseMvpView {
    void onGettingCelebrityEventsSuccess(CelebrityEventsResponse response);
    void userBlocked(String msg);
     void onGettingCelebrityEventsFailure(CelebrityEventsResponse response);
     void onRequestingSelfieSuccess(CreateServiceResponse requestLiveSelfieRespnse, int eventId);
    void onRequestingSelfieFailure(CreateServiceResponse requestLiveSelfieRespnse);

}
