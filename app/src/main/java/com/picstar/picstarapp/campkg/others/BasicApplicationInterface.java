package com.picstar.picstarapp.campkg.others;


import java.util.Date;
import java.util.List;

import android.graphics.Canvas;
import android.location.Location;
import android.util.Pair;
import android.view.MotionEvent;

import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.RawImage;


public abstract class BasicApplicationInterface implements ApplicationInterface {
    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public int getCameraIdPref() {
        return 0;
    }

    @Override
    public String getFlashPref() {
        return "flash_off";
    }

    @Override
    public String getFocusPref(boolean is_video) {
        return "focus_mode_continuous_picture";
    }



    @Override
    public String getSceneModePref() {
        return CameraController.SCENE_MODE_DEFAULT;
    }

    @Override
    public String getISOPref() {
        return CameraController.ISO_DEFAULT;
    }

    @Override
    public int getExposureCompensationPref() {
        return 0;
    }

    @Override
    public Pair<Integer, Integer> getCameraResolutionPref() {
        return null;
    }

    @Override
    public int getImageQualityPref() {
        return 90;
    }

    @Override
    public boolean getFaceDetectionPref() {
        return false;
    }


    @Override
    public float getVideoCaptureRateFactor() {
        return 1.0f;
    }

    @Override
    public String getPreviewSizePref() {
        return "preference_preview_size_wysiwyg";
    }

    @Override
    public String getPreviewRotationPref() {
        return "0";
    }

    @Override
    public String getLockOrientationPref() {
        return "none";
    }

    @Override
    public boolean getTouchCapturePref() {
        return false;
    }

    @Override
    public boolean getDoubleTapCapturePref() {
        return false;
    }

    @Override
    public boolean getPausePreviewPref() {
        return false;
    }


    @Override
    public boolean getShutterSoundPref() {
        return true;
    }

    @Override
    public boolean getStartupFocusPref() {
        return true;
    }

    @Override
    public long getTimerPref() {
        return 0;
    }

    @Override
    public String getRepeatPref() {
        return "1";
    }

    @Override
    public long getRepeatIntervalPref() {
        return 0;
    }

    @Override
    public boolean getGeotaggingPref() {
        return false;
    }

    @Override
    public boolean getRequireLocationPref() {
        return false;
    }

    @Override
    public boolean getRecordAudioPref() {
        return true;
    }


    @Override
    public int getZoomPref() {
        return 0;
    }

    @Override
    public double getCalibratedLevelAngle() {
        return 0;
    }

    @Override
    public boolean canTakeNewPhoto() {
        return true;
    }

    @Override
    public boolean imageQueueWouldBlock(int n_raw, int n_jpegs) {
        return false;
    }

    @Override
    public long getExposureTimePref() {
        return CameraController.EXPOSURE_TIME_DEFAULT;
    }

    @Override
    public float getFocusDistancePref(boolean is_target_distance) {
        return 0;
    }

    @Override
    public boolean isExpoBracketingPref() {
        return false;
    }

    @Override
    public int getExpoBracketingNImagesPref() {
        return 3;
    }

    @Override
    public double getExpoBracketingStopsPref() {
        return 2.0;
    }

    @Override
    public int getFocusBracketingNImagesPref() {
        return 3;
    }

    @Override
    public boolean getFocusBracketingAddInfinityPref() {
        return false;
    }

    @Override
    public boolean isFocusBracketingPref() {
        return false;
    }

    @Override
    public boolean isCameraBurstPref() {
        return false;
    }

    @Override
    public int getBurstNImages() {
        return 5;
    }

    @Override
    public boolean getBurstForNoiseReduction() {
        return false;
    }

    @Override
    public NRModePref getNRModePref() {
        return NRModePref.NRMODE_NORMAL;
    }

    @Override
    public boolean getOptimiseAEForDROPref() {
        return false;
    }

    @Override
    public RawPref getRawPref() {
        return RawPref.RAWPREF_JPEG_ONLY;
    }

    @Override
    public int getMaxRawImages() {
        return 2;
    }

    @Override
    public boolean useCamera2FakeFlash() {
        return false;
    }

    @Override
    public boolean useCamera2FastBurst() {
        return true;
    }


    @Override
    public boolean isPreviewInBackground() {
        return false;
    }

    @Override
    public boolean allowZoom() {
        return true;
    }

    @Override
    public boolean isTestAlwaysFocus() {
        return false;
    }

    @Override
    public void cameraSetup() {

    }

    @Override
    public void touchEvent(MotionEvent event) {

    }

    @Override
    public void onFailedStartPreview() {

    }

    @Override
    public void onCameraError() {

    }

    @Override
    public void onPhotoError() {

    }


    @Override
    public void hasPausedPreview(boolean paused) {

    }

    @Override
    public void cameraInOperation(boolean in_operation, boolean is_video) {

    }

    @Override
    public void turnFrontScreenFlashOn() {

    }

    @Override
    public void cameraClosed() {

    }

    @Override
    public void timerBeep(long remaining_time) {

    }

    @Override
    public void multitouchZoom(int new_zoom) {

    }

    @Override
    public void setCameraIdPref(int cameraId) {

    }

    @Override
    public void setFlashPref(String flash_value) {

    }

    @Override
    public void setFocusPref(String focus_value, boolean is_video) {

    }


    @Override
    public void setSceneModePref(String scene_mode) {

    }

    @Override
    public void clearSceneModePref() {

    }

    @Override
    public void setISOPref(String iso) {

    }

    @Override
    public void clearISOPref() {

    }

    @Override
    public void setExposureCompensationPref(int exposure) {

    }

    @Override
    public void clearExposureCompensationPref() {

    }

    @Override
    public void setCameraResolutionPref(int width, int height) {

    }

    @Override
    public void setZoomPref(int zoom) {

    }

    @Override
    public void setExposureTimePref(long exposure_time) {

    }

    @Override
    public void clearExposureTimePref() {

    }

    @Override
    public void setFocusDistancePref(float focus_distance, boolean is_target_distance) {

    }

    @Override
    public void onDrawPreview(Canvas canvas) {

    }

    @Override
    public boolean onBurstPictureTaken(List<byte[]> images, Date current_date) {
        return false;
    }

    @Override
    public boolean onRawPictureTaken(RawImage raw_image, Date current_date) {
        return false;
    }

    @Override
    public boolean onRawBurstPictureTaken(List<RawImage> raw_images, Date current_date) {
        return false;
    }

    @Override
    public void onCaptureStarted() {

    }

    @Override
    public void onPictureCompleted() {

    }

    @Override
    public void onContinuousFocusMove(boolean start) {

    }
}

