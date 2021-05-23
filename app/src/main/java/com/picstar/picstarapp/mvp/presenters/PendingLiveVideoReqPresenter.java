package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.livevideo.pending.LiveVideoPendingReq;
import com.picstar.picstarapp.mvp.models.livevideo.pending.PendingLiveVideoResponse;
import com.picstar.picstarapp.mvp.views.LiveVideoPendingView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PendingLiveVideoReqPresenter extends BasePresenter<LiveVideoPendingView> {




    public void getPendingVideoMsgs(String lang,String header, LiveVideoPendingReq request) {
        Disposable disposable = PSRService.getInstance(lang,header).dogetPendingLiveVideo(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<PendingLiveVideoResponse>(){
                    @Override
                    public void onNext(PendingLiveVideoResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().gettingPendingorcompltdLiveVideoSuccess(response);
                            }
                            else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            }
                            else
                            {
                                getMvpView().gettingPendingorcompltdLiveVideoFailure(response);
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
