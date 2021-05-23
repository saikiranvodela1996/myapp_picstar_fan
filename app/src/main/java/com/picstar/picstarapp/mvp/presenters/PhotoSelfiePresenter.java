package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.views.PhotoSelfieView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PhotoSelfiePresenter extends BasePresenter<PhotoSelfieView> {

    public void uploadPhotoSelfie(String lang, String header, CreateServiceReq request) {
        Disposable disposable = PSRService.getInstance(lang, header).createPaymentServReq(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CreateServiceResponse>() {
                    @Override
                    public void onNext(CreateServiceResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onCreatingServiceReqSuccess(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onCreatingServiceReqFailure(response);
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
                    public void onError(Throwable t) {
                        if (getMvpView() != null) {
                            getMvpView().onError(t);
                        }
                    }
                });


        compositeSubscription.add(disposable);

    }


}
