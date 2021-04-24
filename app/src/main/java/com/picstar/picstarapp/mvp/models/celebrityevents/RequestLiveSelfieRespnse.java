package com.picstar.picstarapp.mvp.models.celebrityevents;

import com.squareup.moshi.Json;

public class RequestLiveSelfieRespnse {

@Json(name = "status")
private String status;
@Json(name = "message")
private String message;
@Json(name = "liveselfieinfo")
private Liveselfieinfo liveselfieinfo;

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

public Liveselfieinfo getLiveselfieinfo() {
return liveselfieinfo;
}

public void setLiveselfieinfo(Liveselfieinfo liveselfieinfo) {
this.liveselfieinfo = liveselfieinfo;
}

}