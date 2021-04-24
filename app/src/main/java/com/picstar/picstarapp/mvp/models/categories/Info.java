package com.picstar.picstarapp.mvp.models.categories;

import com.squareup.moshi.Json;

public class Info {

@Json(name = "categoryId")
private Integer categoryId;
@Json(name = "name")
private String name;

public Integer getCategoryId() {
return categoryId;
}

public void setCategoryId(Integer categoryId) {
this.categoryId = categoryId;
}

public String getName() {
return name;
}

public void setName(String name) {
this.name = name;
}

}