package com.picstar.picstarapp.mvp.models.livevideo.pending;

public class LiveVideoPendingReq {
    String userId, celebrityId, status;

    int page;

    public String getUserId() {
        return userId;
    }

    public String getCelebrityId() {
        return celebrityId;
    }

    public String getStatus() {
        return status;
    }

    public int getPage() {
        return page;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCelebrityId(String celebrityId) {
        this.celebrityId = celebrityId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
