package com.picstar.picstarapp.mvp.models.login;
import com.squareup.moshi.Json;

public class LoginRequest {

    @Json(name = "user_id")
    private String userId;
    @Json(name = "username")
    private String username;
    @Json(name = "email")
    private String email;
    @Json(name = "fcm_reg_token")
    private String fcmRegToken;
    @Json(name = "device_id")
    private String deviceId;
    @Json(name = "device_type")
    private String deviceType;
    @Json(name = "login_type")
    private Integer loginType;
    @Json(name = "first_name")
    private String firstName;
    @Json(name = "last_name")
    private String lastName;
    @Json(name = "profile_pic")
    private String profilePic;
    @Json(name = "dob")
    private String dob;

    public String getAuth0Username() {
        return auth0Username;
    }

    public void setAuth0Username(String auth0Username) {
        this.auth0Username = auth0Username;
    }

    @Json(name = "auth0_username")
    private String auth0Username;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFcmRegToken() {
        return fcmRegToken;
    }

    public void setFcmRegToken(String fcmRegToken) {
        this.fcmRegToken = fcmRegToken;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Integer getLoginType() {
        return loginType;
    }

    public void setLoginType(Integer loginType) {
        this.loginType = loginType;
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

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

}
