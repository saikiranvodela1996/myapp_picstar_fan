package com.picstar.picstarapp.mvp.models.favourites;

import com.squareup.moshi.Json;

public class Info {

@Json(name = "id")
private Integer id;
@Json(name = "user_id")
private String userId;
@Json(name = "celebrity_id")
private String celebrityId;

public Integer getId() {
return id;
}

public void setId(Integer id) {
this.id = id;
}

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

}