package com.picstar.picstarapp.mvp.models.liveeventshistory;

import com.squareup.moshi.Json;

public class LiveEvent {

@Json(name = "event_id")
private Integer eventId;
@Json(name = "user_id")
private String userId;
@Json(name = "event_name")
private String eventName;
@Json(name = "event_desc")
private String eventDesc;
@Json(name = "event_location")
private String eventLocation;
@Json(name = "event_date")
private String eventDate;
@Json(name = "event_type")
private String eventType;
@Json(name = "created_requests")
private Integer createdRequests;

public Integer getEventId() {
return eventId;
}

public void setEventId(Integer eventId) {
this.eventId = eventId;
}

public String getUserId() {
return userId;
}

public void setUserId(String userId) {
this.userId = userId;
}

public String getEventName() {
return eventName;
}

public void setEventName(String eventName) {
this.eventName = eventName;
}

public String getEventDesc() {
return eventDesc;
}

public void setEventDesc(String eventDesc) {
this.eventDesc = eventDesc;
}

public String getEventLocation() {
return eventLocation;
}

public void setEventLocation(String eventLocation) {
this.eventLocation = eventLocation;
}

public String getEventDate() {
return eventDate;
}

public void setEventDate(String eventDate) {
this.eventDate = eventDate;
}

public String getEventType() {
return eventType;
}

public void setEventType(String eventType) {
this.eventType = eventType;
}

public Integer getCreatedRequests() {
return createdRequests;
}

public void setCreatedRequests(Integer createdRequests) {
this.createdRequests = createdRequests;
}

}