package com.picstar.picstarapp.mvp.presenters;

import com.chachapps.initialclasses.mvp.presenter.BasePresenter;
import com.picstar.picstarapp.mvp.models.categories.CategoriesListResponse;


import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdRequest;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdResponse;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavRequest;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavResponse;
import com.picstar.picstarapp.mvp.models.recommCelebrtys.RecommCelebrityResponse;
import com.picstar.picstarapp.mvp.views.CategoriesView;
import com.picstar.picstarapp.network.CustomDisposableObserver;

import com.picstar.picstarapp.network.PSRService;


import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class CategoriesPresenter extends BasePresenter<CategoriesView> {


    public void getCategoriesList(String lang, String header, String userID) {
        Disposable disposable = PSRService.getInstance(lang, header).doGetCategoriesList(userID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CategoriesListResponse>() {
                    @Override
                    public void onNext(CategoriesListResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onGettingList(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage());
                            } else {
                                getMvpView().onFailure(response);
                            }
                        }
                    }

                    //USER_BLOCKED
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


    public void getCelebritiesById(String lang, String header, CelebritiesByIdRequest request) {
        Disposable disposable = PSRService.getInstance(lang, header).doGetCelebritiesById(request.getPage(), request.getCategoryId(), request.getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CelebritiesByIdResponse>() {
                    @Override
                    public void onNext(CelebritiesByIdResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onGettingCelebritiesList(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onGettingCelebritiesFailure(response);
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

    public void getRecommCelebrities(String lang, String header, CelebritiesByIdRequest recommendRequest) {
        Disposable disposable = PSRService.getInstance(lang, header).doGetRecomCelebritiesById(recommendRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CelebritiesByIdResponse>() {
                    @Override
                    public void onNext(CelebritiesByIdResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onGettingRecommCelebritiesList(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onGettimgRecommCelebritiesFailure(response);
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
/*
    public void getMyFavCelebrities(String lang, String header, CelebritiesByIdRequest request) {
        Disposable disposable = PSRService.getInstance(lang, header).doGetMyFav(request.getPage(), request.getUserId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CelebritiesByIdResponse>() {
                    @Override
                    public void onNext(CelebritiesByIdResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onGettingCelebritiesList(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onGettingCelebritiesFailure(response);
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

    }*/

    public void addToMyFavs(String lang, String header, AddtoFavRequest request, String celebrityID) {
        Disposable disposable = PSRService.getInstance(lang, header).doAddToMyFavs(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<AddtoFavResponse>() {
                    @Override
                    public void onNext(AddtoFavResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onAddingToFavsSuccess(response, celebrityID);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onAddingToFavsFailure(response);
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

    public void searchForCelebrity(String lang, String header, int categoryId, int page, String searchString, String userId) {
        Disposable disposable = PSRService.getInstance(lang, header).doSearchForCelebrity(categoryId, page, searchString, userId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new CustomDisposableObserver<CelebritiesByIdResponse>() {
                    @Override
                    public void onNext(CelebritiesByIdResponse response) {
                        if (getMvpView() != null) {
                            if (response.getStatus().equals("SUCCESS")) {
                                getMvpView().onGettingSearchListSuccess(response);
                            } else if (response.getStatus().equals("USER_BLOCKED")) {
                                getMvpView().userBlocked(response.getMessage().toString());
                            } else {
                                getMvpView().onGettingSearchListFailure(response);
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

    public void cancelRequests() {
        compositeSubscription.clear();
    }

}
