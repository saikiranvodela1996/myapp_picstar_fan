package com.picstar.picstarapp.mvp.models.videomsgs.createservicerequest;

import com.squareup.moshi.Json;

public class CreateServiceReq {

    @Json(name = "user_id")
    private String userId;
    @Json(name = "celebrity_id")
    private String celebrityId;
    @Json(name = "service_request_type_id")
    private int serviceRequestTypeId;
    @Json(name = "file_path")
    private String filePath;
    @Json(name = "event_id")
    private Integer eventId;
    @Json(name = "photo_id")
    private Integer photoId;
    @Json(name = "video_event_id")
    private Integer videoEventId;
    @Json(name = "payment_info")
    private String paymentInfo;

    public String getPaymentInfo() {
        return paymentInfo;
    }

    public void setPaymentInfo(String paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    public Integer getServiceRequestId() {
        return serviceRequestId;
    }

    public void setServiceRequestId(int serviceRequestId) {
        this.serviceRequestId = serviceRequestId;
    }

    @Json(name = "service_request_id")
    private int serviceRequestId;



    public Integer getLiveVideoId() {
        return liveVideoId;
    }

    public void setLiveVideoId(Integer liveVideoId) {
        this.liveVideoId = liveVideoId;
    }

    @Json(name = "live_video_id")
    private Integer liveVideoId;

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

    public int getServiceRequestTypeId() {
        return serviceRequestTypeId;
    }

    public void setServiceRequestTypeId(int serviceRequestTypeId) {
        this.serviceRequestTypeId = serviceRequestTypeId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Integer photoId) {
        this.photoId = photoId;
    }

    public Integer getVideoEventId() {
        return videoEventId;
    }

    public void setVideoEventId(Integer videoEventId) {
        this.videoEventId = videoEventId;
    }

}