package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsRequest;
import com.picstar.picstarapp.mvp.models.celebrityevents.CelebrityEventsResponse;
import com.picstar.picstarapp.mvp.models.history.HistoryResponse;
import com.picstar.picstarapp.mvp.models.liveeventshistory.LiveEventsHistoryResponse;
import com.picstar.picstarapp.mvp.models.pendingliveselfieresponse.LiveSelfiePendingResponse;
import com.picstar.picstarapp.mvp.views.HistoryView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HistoryPresenter extends BasePresenter<HistoryView> {



    public void getHistory(String header, String  userID,int pageNo,int serviceReqId,String status ) {
        Disposable disposable = PSRService.getInstance(header).doGetHistory(userID,pageNo,serviceReqId,status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<LiveEventsHistoryResponse>(){
                    @Override
                    public void onNext(LiveEventsHistoryResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onGettingHistorySuccess(response);
                            }else
                            {
                                getMvpView().onGettingHistoryFailure(response);
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
