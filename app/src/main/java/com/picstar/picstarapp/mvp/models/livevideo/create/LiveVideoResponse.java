package com.picstar.picstarapp.mvp.models.livevideo.create;

import com.squareup.moshi.Json;

public class LiveVideoResponse {

@Json(name = "status")
private String status;
@Json(name = "message")
private String message;
@Json(name = "info")
private Info info;

public String getStatus() {
return status;
}

public void setStatus(String status) {
this.status = status;
}

public String getMessage() {
return message;
}

public void setMessage(String message) {
this.message = message;
}

public Info getInfo() {
return info;
}

public void setInfo(Info info) {
this.info = info;
}

}