package com.picstar.picstarapp.mvp.models.updateprofile;

import com.squareup.moshi.Json;

public class UpdateProfileReq {

@Json(name = "user_id")
private String userId;
@Json(name = "username")
private String username;
@Json(name = "profile_pic")
private String profilePic;
@Json(name = "dob")
private String dob;
@Json(name = "phone_number")
private String phoneNumber;
@Json(name = "gender")
private String gender;

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

public String getPhoneNumber() {
return phoneNumber;
}

public void setPhoneNumber(String phoneNumber) {
this.phoneNumber = phoneNumber;
}

public String getGender() {
return gender;
}

public void setGender(String gender) {
this.gender = gender;
}

}