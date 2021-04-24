package com.picstar.picstarapp.mvp.models.recommCelebrtys;

import com.squareup.moshi.Json;

public class Info {

@Json(name = "name")
private String name;
@Json(name = "profile_pic")
private String profilePic;
@Json(name = "category")
private String category;

public String getName() {
return name;
}

public void setName(String name) {
this.name = name;
}

public String getProfilePic() {
return profilePic;
}

public void setProfilePic(String profilePic) {
this.profilePic = profilePic;
}

public String getCategory() {
return category;
}

public void setCategory(String category) {
this.category = category;
}

}