package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.history.HistoryResponse;
import com.picstar.picstarapp.mvp.models.videomsgshistoryresponse.VideoMsgsHistoryResponse;
import com.picstar.picstarapp.mvp.views.VideoMsgsHistoryView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class VideoMsgHistoryPresenter extends BasePresenter<VideoMsgsHistoryView> {


    public void getVideoMsgsHistory(String header, String  userID,int pageNo,int serviceReqId,String status ) {
        Disposable disposable = PSRService.getInstance(header).doGetVideoMsgHistory(userID,pageNo,serviceReqId,status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<VideoMsgsHistoryResponse>(){
                    @Override
                    public void onNext(VideoMsgsHistoryResponse response ) {
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
