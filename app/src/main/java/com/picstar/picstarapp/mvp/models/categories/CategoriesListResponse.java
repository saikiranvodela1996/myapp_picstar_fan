package com.picstar.picstarapp.mvp.models.categories;

import com.squareup.moshi.Json;

import java.util.List;

public class CategoriesListResponse {

    @Json(name = "status")
    private String status;
    @Json(name = "message")
    private String message;
    @Json(name = "info")
    private List<Info> info = null;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Info> getInfo() {
        return info;
    }

    public void setInfo(List<Info> info) {
        this.info = info;
    }
}
