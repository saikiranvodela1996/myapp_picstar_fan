package com.picstar.picstarapp.mvp.models.celebrityevents;

public class CelebrityEventsRequest {


    String user_id,celebrity_id;

    public String getCelebrity_id() {
        return celebrity_id;
    }

    public void setCelebrity_id(String celebrity_id) {
        this.celebrity_id = celebrity_id;
    }

    int page;

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
