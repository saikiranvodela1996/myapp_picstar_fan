package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.VideoMsgsPendingReq;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;
import com.picstar.picstarapp.mvp.views.PendingLiveSelfieView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PendingLiveSelfiePresenter extends BasePresenter<PendingLiveSelfieView> {





    public void getPendingLiveSelfieReqs(String header, VideoMsgsPendingReq request) {
        Disposable disposable = PSRService.getInstance(header).dogetPendingLiveSelfieReqs(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<LiveSelfiePendingResponse>(){
                    @Override
                    public void onNext(LiveSelfiePendingResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onGettingPendingLiveSelfies(response);
                            }else
                            {
                                getMvpView().onGettingPendingLiveSelfiesFailure(response);
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
