package com.picstar.picstarapp.mvp.models.videomsgshistoryresponse;

import com.squareup.moshi.Json;

public class VideoEvent {

@Json(name = "video_event_id")
private Integer videoEventId;
@Json(name = "user_id")
private String userId;
@Json(name = "celebrity_id")
private String celebrityId;
@Json(name = "video_event_name")
private String videoEventName;
@Json(name = "video_event_desc")
private String videoEventDesc;
@Json(name = "video_event_date")
private String videoEventDate;
@Json(name = "video_event_status")
private Integer videoEventStatus;

public Integer getVideoEventId() {
return videoEventId;
}

public void setVideoEventId(Integer videoEventId) {
this.videoEventId = videoEventId;
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

public String getVideoEventName() {
return videoEventName;
}

public void setVideoEventName(String videoEventName) {
this.videoEventName = videoEventName;
}

public String getVideoEventDesc() {
return videoEventDesc;
}

public void setVideoEventDesc(String videoEventDesc) {
this.videoEventDesc = videoEventDesc;
}

public String getVideoEventDate() {
return videoEventDate;
}

public void setVideoEventDate(String videoEventDate) {
this.videoEventDate = videoEventDate;
}

public Integer getVideoEventStatus() {
return videoEventStatus;
}

public void setVideoEventStatus(Integer videoEventStatus) {
this.videoEventStatus = videoEventStatus;
}

}