package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdRequest;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.PendingVideoMsgsResponse;
import com.picstar.picstarapp.mvp.models.pendingVideoMsgs.VideoMsgsPendingReq;
import com.picstar.picstarapp.mvp.models.stockphotos.StockPhotosResponse;
import com.picstar.picstarapp.mvp.views.StockPhotosView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class StockPhotosPresenter extends BasePresenter<StockPhotosView> {


    public void getStockPicsOfCelebrity(String lang, String header, CelebritiesByIdRequest request) {
        Disposable disposable = PSRService.getInstance(lang, header).doGetStockPicsOfCelebrity(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<StockPhotosResponse>() {
                    @Override
                    public void onNext(StockPhotosResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onGettingStockPicsSuccess(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onGettingStockPicsFailure(response);
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
