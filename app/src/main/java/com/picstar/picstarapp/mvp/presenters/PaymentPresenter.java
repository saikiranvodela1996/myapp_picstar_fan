package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceReq;
import com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest.CreateServiceResponse;
import com.picstar.picstarapp.mvp.views.PaymentView;
import com.picstar.picstarapp.network.CustomDisposableObserver;
import com.picstar.picstarapp.network.PSRService;

import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class PaymentPresenter extends BasePresenter<PaymentView> {

    public void doCallStripeChargesApi(String header,String amount,String currency,String descripn,String token ) {
        Disposable disposable = PSRService.getInstance(header).callStripeChargesApi(amount, currency, descripn, token)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<ResponseBody>(){
                    @Override
                    public void onNext(ResponseBody response ) {
                        if (getMvpView() != null) {
                            String res = null;
                            try {
                                res = response.string();
                                getMvpView().onGettingChargesResponseSuccess(res);
                            } catch (IOException e) {
                                e.printStackTrace();
                                getMvpView().onGettingChargesResponseFailure();
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










    public void doCreatePaymentServReq(String header, CreateServiceReq request) {
        Disposable disposable = PSRService.getInstance(header).createPaymentServReq(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CreateServiceResponse>(){
                    @Override
                    public void onNext(CreateServiceResponse response ) {
                        if (getMvpView() != null) {
                            if( response.getStatus().equals("SUCCESS"))
                            {
                                getMvpView().onCreatingPaymentServReqSuccess(response);
                            }else
                            {
                                getMvpView().onCreatingPaymentServReqFailure(response);
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
