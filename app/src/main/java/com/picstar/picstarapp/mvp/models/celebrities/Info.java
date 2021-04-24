package com.picstar.picstarapp.mvp.models.celebrities;

import com.squareup.moshi.Json;

import java.io.Serializable;
import java.util.List;

public class Info implements Serializable {

    @Json(name = "user_id")
    private String userId;
    @Json(name = "profile_pic")
    private String profilePic;
    @Json(name = "celebrity_description")
    private String celebrityDescription;
    @Json(name = "celebrity_location")
    private String celebrityLocation;
    @Json(name = "cover_pic")
    private String coverPic;
    @Json(name = "username")
    private String username;
    @Json(name = "likes")
    private int likes;
    @Json(name = "favs")
    private int favs;
    @Json(name = "photo_selfies_count")
    private int photoSelfiesCount;
    @Json(name = "video_messages_count")
    private int videoMessagesCount;
    @Json(name = "live_photo_selfies_count")
    private int livePhotoSelfiesCount;

    public int getLiveVideosCount() {
        return liveVideosCount;
    }

    public void setLiveLiveVideosCount(int liveVideosCount) {
        this.liveVideosCount = liveVideosCount;
    }

    @Json(name = "live_videos_count")
    private int liveVideosCount;





    @Json(name = "categories_of_celebrity")
    private List<String> categoriesOfCelebrity = null;
    @Json(name = "services_offering")
    private List<ServicesOffering> servicesOffering = null;

    boolean isFavourite=false;

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getCelebrityDescription() {
        return celebrityDescription;
    }

    public void setCelebrityDescription(String celebrityDescription) {
        this.celebrityDescription = celebrityDescription;
    }

    public String getCelebrityLocation() {
        return celebrityLocation;
    }

    public void setCelebrityLocation(String celebrityLocation) {
        this.celebrityLocation = celebrityLocation;
    }

    public String getCoverPic() {
        return coverPic;
    }

    public void setCoverPic(String coverPic) {
        this.coverPic = coverPic;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getFavs() {
        return favs;
    }

    public void setFavs(int favs) {
        this.favs = favs;
    }

    public int getPhotoSelfiesCount() {
        return photoSelfiesCount;
    }

    public void setPhotoSelfiesCount(int photoSelfiesCount) {
        this.photoSelfiesCount = photoSelfiesCount;
    }

    public int getVideoMessagesCount() {
        return videoMessagesCount;
    }

    public void setVideoMessagesCount(int videoMessagesCount) {
        this.videoMessagesCount = videoMessagesCount;
    }

    public int getLivePhotoSelfiesCount() {
        return livePhotoSelfiesCount;
    }

    public void setLivePhotoSelfiesCount(int livePhotoSelfiesCount) {
        this.livePhotoSelfiesCount = livePhotoSelfiesCount;
    }

    public List<String> getCategoriesOfCelebrity() {
        return categoriesOfCelebrity;
    }

    public void setCategoriesOfCelebrity(List<String> categoriesOfCelebrity) {
        this.categoriesOfCelebrity = categoriesOfCelebrity;
    }

    public List<ServicesOffering> getServicesOffering() {
        return servicesOffering;
    }

    public void setServicesOffering(List<ServicesOffering> servicesOffering) {
        this.servicesOffering = servicesOffering;
    }
}