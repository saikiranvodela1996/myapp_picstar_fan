package com.picstar.picstarapp.mvp.models.videomsgshistoryresponse;

import com.squareup.moshi.Json;

public class VideoMsgsHistoryResponse {

@Json(name = "status")
private String status;
@Json(name = "message")
private Object message;
@Json(name = "info")
private Info info;

public String getStatus() {
return status;
}

public void setStatus(String status) {
this.status = status;
}

public Object getMessage() {
return message;
}

public void setMessage(Object message) {
this.message = message;
}

public Info getInfo() {
return info;
}

public void setInfo(Info info) {
this.info = info;
}

}