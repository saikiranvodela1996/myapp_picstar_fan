package com.picstar.picstarapp.mvp.models.liveeventshistory;

import java.util.List;

import com.squareup.moshi.Json;

public class Info {

    @Json(name = "service_requests")
    private List<ServiceRequest> serviceRequests = null;
    @Json(name = "service_request_statuses")
    private List<String> serviceRequestStatuses = null;

    public List<ServiceRequest> getServiceRequests() {
        return serviceRequests;
    }

    public void setServiceRequests(List<ServiceRequest> serviceRequests) {
        this.serviceRequests = serviceRequests;
    }

    public List<String> getServiceRequestStatuses() {
        return serviceRequestStatuses;
    }

    public void setServiceRequestStatuses(List<String> serviceRequestStatuses) {
        this.serviceRequestStatuses = serviceRequestStatuses;
    }

}