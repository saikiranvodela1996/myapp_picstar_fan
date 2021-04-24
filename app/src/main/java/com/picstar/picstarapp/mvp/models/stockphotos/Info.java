package com.picstar.picstarapp.mvp.models.stockphotos;

import com.squareup.moshi.Json;

public class Info {

@Json(name = "photoId")
private Integer photoId;
@Json(name = "user_id")
private String userId;
@Json(name = "photo_url")
private String photoUrl;
@Json(name = "photo_type")
private Integer photoType;
@Json(name = "created_at")
private String createdAt;
@Json(name = "updated_at")
private String updatedAt;

public Integer getPhotoId() {
return photoId;
}

public void setPhotoId(Integer photoId) {
this.photoId = photoId;
}

public String getUserId() {
return userId;
}

public void setUserId(String userId) {
this.userId = userId;
}

public String getPhotoUrl() {
return photoUrl;
}

public void setPhotoUrl(String photoUrl) {
this.photoUrl = photoUrl;
}

public Integer getPhotoType() {
return photoType;
}

public void setPhotoType(Integer photoType) {
this.photoType = photoType;
}

public String getCreatedAt() {
return createdAt;
}

public void setCreatedAt(String createdAt) {
this.createdAt = createdAt;
}

public String getUpdatedAt() {
return updatedAt;
}

public void setUpdatedAt(String updatedAt) {
this.updatedAt = updatedAt;
}

}