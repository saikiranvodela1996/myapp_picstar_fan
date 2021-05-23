package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileResponse;

public interface UpdateUserProfileView extends BaseMvpView {


    void onUpdatingSuccess(UpdateProfileResponse response);

    void userBlocked(String msg);

    void onUpdatingFailure(UpdateProfileResponse response);
}
