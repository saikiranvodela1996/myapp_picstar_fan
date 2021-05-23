package com.picstar.picstarapp.mvp.views;

import com.picstar.picstarapp.base.BaseMvpView;
import com.picstar.picstarapp.mvp.models.stockphotos.StockPhotosResponse;

public interface StockPhotosView  extends BaseMvpView {
  void onGettingStockPicsSuccess(StockPhotosResponse response);
  void userBlocked(String msg);
  void onGettingStockPicsFailure(StockPhotosResponse response);
}
