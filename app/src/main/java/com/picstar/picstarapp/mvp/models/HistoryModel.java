package com.picstar.picstarapp.mvp.models;

import java.util.ArrayList;

public class HistoryModel {
    String name,date,location,hystoryType,price,status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHystoryType() {
        return hystoryType;
    }

    public void setHystoryType(String hystoryType) {
        this.hystoryType = hystoryType;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static ArrayList<HistoryModel> getHistory(){
        ArrayList<HistoryModel> list = new ArrayList<>();
        for (int i=0;i<10;i++){
            HistoryModel model= new HistoryModel();
            model.setDate("29/01.2021, 11:30AM");
            model.setHystoryType("Live Selfie");
            model.setLocation("Los Angeles, California, USA");
            model.setName("Lenardo Dicaprio");
            model.setPrice("5");
            model.setStatus("Confirm");
            list.add(model);
        }
        return list;
    }
}
