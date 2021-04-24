package com.picstar.picstarapp.mvp.models.payment;

import com.squareup.moshi.Json;

public class PaymentReq {

@Json(name = "amount")
private String amount;
@Json(name = "currency")
private String currency;
@Json(name = "description")
private String description;
@Json(name = "source")
private String source;

public String getAmount() {
return amount;
}

public void setAmount(String amount) {
this.amount = amount;
}

public String getCurrency() {
return currency;
}

public void setCurrency(String currency) {
this.currency = currency;
}

public String getDescription() {
return description;
}

public void setDescription(String description) {
this.description = description;
}

public String getSource() {
return source;
}

public void setSource(String source) {
this.source = source;
}

}