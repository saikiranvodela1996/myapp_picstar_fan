package com.picstar.picstarapp.mvp.models.favourites;

import com.squareup.moshi.Json;

public class AddtoFavRequest {

    @Json(name = "user_id")
    private String userId;
    @Json(name = "celebrity_id")
    private String celebrityId;
    @Json(name = "fav_status")
    private boolean favStatus;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCelebrityId() {
        return celebrityId;
    }

    public void setCelebrityId(String celebrityId) {
        this.celebrityId = celebrityId;
    }

    public boolean getFavStatus() {
        return favStatus;
    }

    public void setFavStatus(boolean favStatus) {
        this.favStatus = favStatus;
    }

}