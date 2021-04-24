package com.picstar.picstarapp.mvp.models.videomsgs;

import com.squareup.moshi.Json;

public class VideoMsgRequest {

@Json(name = "video_event_id")
private String videoEventId;
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

public String getVideoEventId() {
return videoEventId;
}

public void setVideoEventId(String videoEventId) {
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

}