package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.login.LoginResponse;

public interface LoginView extends BaseMvpView {

    void onLoginSuccess(LoginResponse response);

    void userBlocked(LoginResponse response);

    void onLoginFailed(LoginResponse response);
}
