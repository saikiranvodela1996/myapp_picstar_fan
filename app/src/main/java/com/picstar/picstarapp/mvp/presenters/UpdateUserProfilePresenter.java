package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.liveeventshistory.LiveEventsHistoryResponse;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileReq;
import com.picstar.picstarapp.mvp.models.updateprofile.UpdateProfileResponse;
import com.picstar.picstarapp.mvp.views.UpdateUserProfileView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UpdateUserProfilePresenter extends BasePresenter<UpdateUserProfileView> {


    public void updateUserProfile(String lang, String header, UpdateProfileReq req) {
        Disposable disposable = PSRService.getInstance(lang, header).doUpdateUserProfile(req)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<UpdateProfileResponse>() {
                    @Override
                    public void onNext(UpdateProfileResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onUpdatingSuccess(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onUpdatingFailure(response);
                            }
                        }
                    }


                    @Override
                    public void onConnectionLost() {
                        if (getMvpView() != null) {
                            getMvpView().onNoInternetConnection();
                        }
                    }

                    @Override
                    public void onSessionExpired() {
                        if (getMvpView() != null) {
                            getMvpView().onSessionExpired();
                        }
                    }

                    @Override
                    public void onServerError() {
                        if (getMvpView() != null) {
                            getMvpView().onServerError();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (getMvpView() != null) {
                            getMvpView().onError(t);
                        }
                    }
                });


        compositeSubscription.add(disposable);

    }


}
