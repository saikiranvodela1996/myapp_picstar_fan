package com.picstar.picstarapp.mvp.models.celebrities;

import com.squareup.moshi.Json;

public class CelebritiesByIdRequest {

    @Json(name = "page")
    private Integer page;
    @Json(name = "per_page")
    private Integer perPage;
    @Json(name = "category_id")
    private Integer categoryId;


    @Json(name = "user_id")
    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

}