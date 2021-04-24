package com.picstar.picstarapp.mvp.models.recommCelebrtys;

import com.squareup.moshi.Json;

import java.util.List;

public class RecommCelebrityResponse {

@Json(name = "status")
private String status;
@Json(name = "message")
private Object message;
@Json(name = "info")
private List<Info> info = null;

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

public List<Info> getInfo() {
return info;
}

public void setInfo(List<Info> info) {
this.info = info;
}

}