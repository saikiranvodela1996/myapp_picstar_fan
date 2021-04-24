package com.picstar.picstarapp.mvp.models.celebrities;

import com.squareup.moshi.Json;

import java.io.Serializable;

public class ServiceDetails  implements Serializable {

@Json(name = "service_id")
private Integer serviceId;
@Json(name = "service_name")
private String serviceName;
@Json(name = "active")
private Boolean active;

public Integer getServiceId() {
return serviceId;
}

public void setServiceId(Integer serviceId) {
this.serviceId = serviceId;
}

public String getServiceName() {
return serviceName;
}

public void setServiceName(String serviceName) {
this.serviceName = serviceName;
}

public Boolean getActive() {
return active;
}

public void setActive(Boolean active) {
this.active = active;
}

}