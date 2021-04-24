package com.picstar.picstarapp.mvp.models.history;

import java.util.List;
import com.squareup.moshi.Json;

public class Info {

@Json(name = "service_requests")
private List<ServiceRequest> serviceRequests = null;

public List<ServiceRequest> getServiceRequests() {
return serviceRequests;
}

public void setServiceRequests(List<ServiceRequest> serviceRequests) {
this.serviceRequests = serviceRequests;
}

}