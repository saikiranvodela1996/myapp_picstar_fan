package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdRequest;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsRequest;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsResponse;
import com.picstar.picstarapp.mvp.models.celebrityevents.RequestLiveSelfieRespnse;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.views.CelebrityEventsView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CelebrityEventsPresenter extends BasePresenter<CelebrityEventsView> {


    public void getCelebrityEvents(String header, CelebrityEventsRequest request) {
        Disposable disposable = PSRService.getInstance(header).doGetEvents(request.getPage(),request.getUser_id(),request.getCelebrity_id())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CelebrityEventsResponse>(){
                    @Override
                    public void onNext(CelebrityEventsResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onGettingCelebrityEventsSuccess(response);
                            }else
                            {
                                getMvpView().onGettingCelebrityEventsFailure(response);
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







    public void requestForLiveSelfie(String header, CreateServiceReq request, int eventId) {
        Disposable disposable = PSRService.getInstance(header).doRequestForLiveSelfie(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CreateServiceResponse>(){
                    @Override
                    public void onNext(CreateServiceResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onRequestingSelfieSuccess(response, eventId);
                            }else
                            {
                                getMvpView().onRequestingSelfieFailure(response);
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
