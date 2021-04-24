package com.picstar.picstarapp.base;

public interface BaseMvpView extends com.chachapps.initialclasses.mvp.view.BaseMvpView {
    void onSessionExpired();
    void onServerError();
}
