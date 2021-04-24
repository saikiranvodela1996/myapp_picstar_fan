package com.picstar.picstarapp.callbacks;

public interface  OnClickCelebrity
    {
        void onClickCelebrity(com.picstar.picstarapp.mvp.models.celebrities.Info info);
        void onClickHeart(com.picstar.picstarapp.mvp.models.celebrities.Info info);
        void onClickRecommendCelebrity(com.picstar.picstarapp.mvp.models.celebrities.Info info);
        void onClickLogout();
    }