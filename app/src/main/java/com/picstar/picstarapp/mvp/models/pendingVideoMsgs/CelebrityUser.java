package com.picstar.picstarapp.mvp.models.pendingVideoMsgs;

import com.squareup.moshi.Json;

public class CelebrityUser {

@Json(name = "userId")
private String userId;
@Json(name = "role_id")
private Integer roleId;
@Json(name = "email")
private String email;
@Json(name = "username")
private String username;
@Json(name = "first_name")
private String firstName;
@Json(name = "last_name")
private String lastName;
@Json(name = "gender")
private Object gender;
@Json(name = "fcm_reg_token")
private String fcmRegToken;
@Json(name = "device_type")
private String deviceType;
@Json(name = "device_id")
private String deviceId;
@Json(name = "login_status")
private Integer loginStatus;
@Json(name = "profile_pic")
private String profilePic;
@Json(name = "cover_pic")
private String coverPic;
@Json(name = "user_location")
private String userLocation;
@Json(name = "user_desc")
private String userDesc;
@Json(name = "phone_number")
private Object phoneNumber;
@Json(name = "dob")
private Object dob;
@Json(name = "created_at")
private String createdAt;
@Json(name = "updated_at")
private String updatedAt;
@Json(name = "login_type")
private Integer loginType;
@Json(name = "is_deleted")
private Boolean isDeleted;
@Json(name = "category_id")
private Integer categoryId;
@Json(name = "likes_count")
private Integer likesCount;
@Json(name = "favs_count")
private Integer favsCount;

public String getUserId() {
return userId;
}

public void setUserId(String userId) {
this.userId = userId;
}

public Integer getRoleId() {
return roleId;
}

public void setRoleId(Integer roleId) {
this.roleId = roleId;
}

public String getEmail() {
return email;
}

public void setEmail(String email) {
this.email = email;
}

public String getUsername() {
return username;
}

public void setUsername(String username) {
this.username = username;
}

public String getFirstName() {
return firstName;
}

public void setFirstName(String firstName) {
this.firstName = firstName;
}

public String getLastName() {
return lastName;
}

public void setLastName(String lastName) {
this.lastName = lastName;
}

public Object getGender() {
return gender;
}

public void setGender(Object gender) {
this.gender = gender;
}

public String getFcmRegToken() {
return fcmRegToken;
}

public void setFcmRegToken(String fcmRegToken) {
this.fcmRegToken = fcmRegToken;
}

public String getDeviceType() {
return deviceType;
}

public void setDeviceType(String deviceType) {
this.deviceType = deviceType;
}

public String getDeviceId() {
return deviceId;
}

public void setDeviceId(String deviceId) {
this.deviceId = deviceId;
}

public Integer getLoginStatus() {
return loginStatus;
}

public void setLoginStatus(Integer loginStatus) {
this.loginStatus = loginStatus;
}

public String getProfilePic() {
return profilePic;
}

public void setProfilePic(String profilePic) {
this.profilePic = profilePic;
}

public String getCoverPic() {
return coverPic;
}

public void setCoverPic(String coverPic) {
this.coverPic = coverPic;
}

public String getUserLocation() {
return userLocation;
}

public void setUserLocation(String userLocation) {
this.userLocation = userLocation;
}

public String getUserDesc() {
return userDesc;
}

public void setUserDesc(String userDesc) {
this.userDesc = userDesc;
}

public Object getPhoneNumber() {
return phoneNumber;
}

public void setPhoneNumber(Object phoneNumber) {
this.phoneNumber = phoneNumber;
}

public Object getDob() {
return dob;
}

public void setDob(Object dob) {
this.dob = dob;
}

public String getCreatedAt() {
return createdAt;
}

public void setCreatedAt(String createdAt) {
this.createdAt = createdAt;
}

public String getUpdatedAt() {
return updatedAt;
}

public void setUpdatedAt(String updatedAt) {
this.updatedAt = updatedAt;
}

public Integer getLoginType() {
return loginType;
}

public void setLoginType(Integer loginType) {
this.loginType = loginType;
}

public Boolean getIsDeleted() {
return isDeleted;
}

public void setIsDeleted(Boolean isDeleted) {
this.isDeleted = isDeleted;
}

public Integer getCategoryId() {
return categoryId;
}

public void setCategoryId(Integer categoryId) {
this.categoryId = categoryId;
}

public Integer getLikesCount() {
return likesCount;
}

public void setLikesCount(Integer likesCount) {
this.likesCount = likesCount;
}

public Integer getFavsCount() {
return favsCount;
}

public void setFavsCount(Integer favsCount) {
this.favsCount = favsCount;
}

}