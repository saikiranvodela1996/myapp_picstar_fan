package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavRequest;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgRequest;
import com.picstar.picstarapp.mvp.models.videomsgs.VideoMsgResponse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.views.CelebrityEventsView;
import com.picstar.picstarapp.mvp.views.VideoEventView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class VideoMsgPresenter  extends BasePresenter<VideoEventView> {




    public void videoMsgRequest(String header, VideoMsgRequest request) {
        Disposable disposable = PSRService.getInstance(header).videoMsgRequest(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<VideoMsgResponse>(){
                    @Override
                    public void onNext(VideoMsgResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onCreatingvideoEvntSuccess(response);
                            }else
                            {
                                getMvpView().onCreatingvideoEvntFailure(response);
                            }
                        }
                    }

                    @Override
                    public void onSessionExpired() {
                        if (getMvpView() != null) {
                            getMvpView().onSessionExpired();
                        }
                    }
                    @Override
                    public void onConnectionLost() {
                        if (getMvpView() != null) {
                            getMvpView().onNoInternetConnection();
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


    public void createRequest(String header, CreateServiceReq request) {
        Disposable disposable = PSRService.getInstance(header).doCreateRequest(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CreateServiceResponse>(){
                    @Override
                    public void onNext(CreateServiceResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onCreatingServiceReqSuccess(response);
                            }else
                            {
                                getMvpView().onCreatingServiceReqFailure(response);
                            }
                        }
                    }
                    @Override
                    public void onSessionExpired() {
                        if (getMvpView() != null) {
                            getMvpView().onSessionExpired();
                        }
                    }

                    @Override
                    public void onConnectionLost() {
                        if (getMvpView() != null) {
                            getMvpView().onNoInternetConnection();
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
