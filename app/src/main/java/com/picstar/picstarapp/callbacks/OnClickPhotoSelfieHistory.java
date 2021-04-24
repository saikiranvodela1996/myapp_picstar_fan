package com.picstar.picstarapp.callbacks;

import com.picstar.picstarapp.mvp.models.eventhistory.Info;

public interface OnClickPhotoSelfieHistory {
    void onClickPhotoSelfie(String imagePath, boolean isCameFromCompletedHistory);

    void onClickPaynow(Info info);
}
