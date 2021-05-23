package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.categories.CategoriesListResponse;
import com.picstar.picstarapp.mvp.models.celebrities.CelebritiesByIdResponse;
import com.picstar.picstarapp.mvp.models.favourites.AddtoFavResponse;
import com.picstar.picstarapp.mvp.models.recommCelebrtys.RecommCelebrityResponse;

public interface CategoriesView extends BaseMvpView {

    void onGettingList(CategoriesListResponse response);




    void userBlocked(String  response);
    void onFailure(CategoriesListResponse response);

    void onGettingCelebritiesList(CelebritiesByIdResponse response);

    void onGettingCelebritiesFailure(CelebritiesByIdResponse response);

    void onAddingToFavsSuccess(AddtoFavResponse response, String celebrityID);

    void onAddingToFavsFailure(AddtoFavResponse response);

    void onGettingRecommCelebritiesList(CelebritiesByIdResponse recommCelebrityResponse);

    void onGettimgRecommCelebritiesFailure(CelebritiesByIdResponse recommCelebrityResponse);


    void onGettingSearchListSuccess(CelebritiesByIdResponse response);

    void onGettingSearchListFailure(CelebritiesByIdResponse response);


}
