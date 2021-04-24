package com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest;

import com.squareup.moshi.Json;

public class Info {

@Json(name = "service_request_id")
private Integer serviceRequestId;
@Json(name = "userId")
private String userId;
@Json(name = "celebrity_id")
private String celebrityId;
@Json(name = "service_request_type_id")
private Integer serviceRequestTypeId;
@Json(name = "status")
private String status;
@Json(name = "file_path")
private String filePath;
@Json(name = "event_id")
private Integer eventId;
@Json(name = "photo_id")
private Integer photoId;
@Json(name = "video_event_id")
private Integer videoEventId;
@Json(name = "amount")
private Double amount;
@Json(name = "created_at")
private String createdAt;
@Json(name = "updated_at")
private String updatedAt;
@Json(name = "celebrity_user")
private Object celebrityUser;

public Integer getServiceRequestId() {
return serviceRequestId;
}

public void setServiceRequestId(Integer serviceRequestId) {
this.serviceRequestId = serviceRequestId;
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

public Integer getServiceRequestTypeId() {
return serviceRequestTypeId;
}

public void setServiceRequestTypeId(Integer serviceRequestTypeId) {
this.serviceRequestTypeId = serviceRequestTypeId;
}

public String getStatus() {
return status;
}

public void setStatus(String status) {
this.status = status;
}

public String getFilePath() {
return filePath;
}

public void setFilePath(String filePath) {
this.filePath = filePath;
}

public Integer getEventId() {
return eventId;
}

public void setEventId(Integer eventId) {
this.eventId = eventId;
}

public Integer getPhotoId() {
return photoId;
}

public void setPhotoId(Integer photoId) {
this.photoId = photoId;
}

public Integer getVideoEventId() {
return videoEventId;
}

public void setVideoEventId(Integer videoEventId) {
this.videoEventId = videoEventId;
}

public Double getAmount() {
return amount;
}

public void setAmount(Double amount) {
this.amount = amount;
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

public Object getCelebrityUser() {
return celebrityUser;
}

public void setCelebrityUser(Object celebrityUser) {
this.celebrityUser = celebrityUser;
}

}