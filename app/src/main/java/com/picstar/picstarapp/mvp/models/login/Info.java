package com.picstar.picstarapp.mvp.models.login;

import com.squareup.moshi.Json;

public class Info {

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
    private String gender;
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
    @Json(name = "phone_number")
    private Object phoneNumber;
    @Json(name = "dob")
    private String dob;
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
    @Json(name = "categoryId")
    private int category_Id;
    @Json(name = "name")
    private String name;

    @com.squareup.moshi.Json(name = "privacy_policy_url")
    private String privacyPolicyUrl;
    @com.squareup.moshi.Json(name = "contactus_phone_num")
    private String contactUsPhoneNum;
    @com.squareup.moshi.Json(name = "contactus_email")
    private String contactUsEmail;
    @com.squareup.moshi.Json(name = "contactus_address")
    private String contactUsAddress;
    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }



    public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public String getContactUsPhoneNum() {
        return contactUsPhoneNum;
    }

    public void setContactUsPhoneNum(String contactUsPhoneNum) {
        this.contactUsPhoneNum = contactUsPhoneNum;
    }

    public String getContactUsEmail() {
        return contactUsEmail;
    }

    public void setContactUsEmail(String contactUsEmail) {
        this.contactUsEmail = contactUsEmail;
    }

    public String getContactUsAddress() {
        return contactUsAddress;
    }

    public void setContactUsAddress(String contactUsAddress) {
        this.contactUsAddress = contactUsAddress;
    }



    public Integer getCategory_Id() {
        return categoryId;
    }

    public void setCategory_Id(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
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

    public Object getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Object phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String  dob) {
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

}