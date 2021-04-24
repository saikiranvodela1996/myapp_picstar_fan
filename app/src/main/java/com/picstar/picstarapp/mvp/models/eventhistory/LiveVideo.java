package com.picstar.picstarapp.mvp.models.eventhistory;

import com.squareup.moshi.Json;

public class LiveVideo {

@Json(name = "live_video_id")
private Integer liveVideoId;
@Json(name = "user_id")
private String userId;
@Json(name = "celebrity_id")
private String celebrityId;
@Json(name = "live_video_name")
private String liveVideoName;
@Json(name = "live_video_desc")
private String liveVideoDesc;
@Json(name = "live_video_datetime")
private String liveVideoDatetime;
@Json(name = "live_video_status")
private Integer liveVideoStatus;

public Integer getLiveVideoId() {
return liveVideoId;
}

public void setLiveVideoId(Integer liveVideoId) {
this.liveVideoId = liveVideoId;
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

public String getLiveVideoName() {
return liveVideoName;
}

public void setLiveVideoName(String liveVideoName) {
this.liveVideoName = liveVideoName;
}

public String getLiveVideoDesc() {
return liveVideoDesc;
}

public void setLiveVideoDesc(String liveVideoDesc) {
this.liveVideoDesc = liveVideoDesc;
}

public String getLiveVideoDatetime() {
return liveVideoDatetime;
}

public void setLiveVideoDatetime(String liveVideoDatetime) {
this.liveVideoDatetime = liveVideoDatetime;
}

public Integer getLiveVideoStatus() {
return liveVideoStatus;
}

public void setLiveVideoStatus(Integer liveVideoStatus) {
this.liveVideoStatus = liveVideoStatus;
}

}