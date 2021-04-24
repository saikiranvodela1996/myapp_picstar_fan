package com.picstar.picstarapp.campkg.others;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.location.Location;
import android.util.Pair;
import android.view.MotionEvent;

import com.picstar.picstarapp.campkg.cameracontroller.RawImage;


public interface ApplicationInterface {
    Context getContext(); // get the application context
    boolean useCamera2(); // should Android 5's Camera 2 API be used?
    Location getLocation(); // get current location - null if not available (or you don't care about geotagging)
    int getCameraIdPref(); // camera to use, from 0 to getCameraControllerManager().getNumberOfCameras()
    String getFlashPref(); // flash_off, flash_auto, flash_on, flash_torch, flash_red_eye
    String getFocusPref(boolean is_video); // focus_mode_auto, focus_mode_infinity, focus_mode_macro, focus_mode_locked, focus_mode_fixed, focus_mode_manual2, focus_mode_edof, focus_mode_continuous_picture, focus_mode_continuous_video
    String getSceneModePref(); // "auto" for default (strings correspond to Android's scene mode constants in android.hardware.Camera.Parameters)
    String getISOPref(); // "auto" for auto-ISO, otherwise a numerical value; see documentation for Preview.supportsISORange().
    int getExposureCompensationPref(); // 0 for default
    Pair<Integer, Integer> getCameraResolutionPref(); // return null to let Preview choose size
    int getImageQualityPref(); // jpeg quality for taking photos; "90" is a recommended default
    boolean getFaceDetectionPref(); // whether to use face detection mode
    float getVideoCaptureRateFactor(); // return 1.0f for standard operation, less than 1.0 for slow motion, more than 1.0 for timelapse; consider using a higher fps for slow motion, see getVideoFPSPref()
    String getPreviewSizePref(); // "preference_preview_size_wysiwyg" is recommended (preview matches aspect ratio of photo resolution as close as possible), but can also be "preference_preview_size_display" to maximise the preview size
    String getPreviewRotationPref(); // return "0" for default; use "180" to rotate the preview 180 degrees
    String getLockOrientationPref(); // return "none" for default; use "portrait" or "landscape" to lock photos/videos to that orientation
    boolean getTouchCapturePref(); // whether to enable touch to capture
    boolean getDoubleTapCapturePref(); // whether to enable double-tap to capture
    boolean getPausePreviewPref(); // whether to pause the preview after taking a photo
    boolean getShutterSoundPref(); // whether to play sound when taking photo
    boolean getStartupFocusPref(); // whether to do autofocus on startup
    long getTimerPref(); // time in ms for timer (so 0 for off)
    String getRepeatPref(); // return number of times to repeat photo in a row (as a string), so "1" for default; return "unlimited" for unlimited
    long getRepeatIntervalPref(); // time in ms between repeat
    boolean getGeotaggingPref(); // whether to geotag photos
    boolean getRequireLocationPref(); // if getGeotaggingPref() returns true, and this method returns true, then phot/video will only be taken if location data is available
    boolean getRecordAudioPref(); // whether to record audio when recording video
    int getZoomPref(); // index into Preview.getSupportedZoomRatios() array (each entry is the zoom factor, scaled by 100; array is sorted from min to max zoom)
    double getCalibratedLevelAngle(); // set to non-zero to calibrate the accelerometer used for the level angles
    boolean canTakeNewPhoto(); // whether taking new photos is allowed (e.g., can return false if queue for processing images would become full)
    boolean imageQueueWouldBlock(int n_raw, int n_jpegs); // called during some burst operations, whether we can allow taking the supplied number of extra photos
    // Camera2 only modes:
    long getExposureTimePref(); // only called if getISOPref() is not "default"
    float getFocusDistancePref(boolean is_target_distance);
    boolean isExpoBracketingPref(); // whether to enable burst photos with expo bracketing
    int getExpoBracketingNImagesPref(); // how many images to take for exposure bracketing
    double getExpoBracketingStopsPref(); // stops per image for exposure bracketing
    int getFocusBracketingNImagesPref(); // how many images to take for focus bracketing
    boolean getFocusBracketingAddInfinityPref(); // whether to include an additional image at infinite focus distance, for focus bracketing
    boolean isFocusBracketingPref(); // whether to enable burst photos with focus bracketing
    boolean isCameraBurstPref(); // whether to shoot the camera in burst mode (n.b., not the same as the "auto-repeat" mode)
    int getBurstNImages(); // only relevant if isCameraBurstPref() returns true; see CameraController doc for setBurstNImages().
    boolean getBurstForNoiseReduction(); // only relevant if isCameraBurstPref() returns true; see CameraController doc for setBurstForNoiseReduction().
    enum NRModePref {
        NRMODE_NORMAL,
        NRMODE_LOW_LIGHT
    }
    NRModePref getNRModePref(); // only relevant if getBurstForNoiseReduction() returns true
    boolean getOptimiseAEForDROPref(); // see CameraController doc for setOptimiseAEForDRO().
    enum RawPref {
        RAWPREF_JPEG_ONLY, // JPEG only
        RAWPREF_JPEG_DNG // JPEG and RAW (DNG)
    }
    RawPref getRawPref(); // whether to enable RAW photos
    int getMaxRawImages(); // see documentation of CameraController.setRaw(), corresponds to max_raw_images
    boolean useCamera2FakeFlash(); // whether to enable CameraController.setUseCamera2FakeFlash() for Camera2 API
    boolean useCamera2FastBurst(); // whether to enable Camera2's captureBurst() for faster taking of expo-bracketing photos (generally should be true, but some devices have problems with captureBurst())
    boolean isPreviewInBackground(); // if true, then Preview can disable real-time effects (e.g., computing histogram)
    boolean allowZoom(); // if false, don't allow zoom functionality even if the device supports it - Preview.supportsZoom() will also return false; if true, allow zoom if the device supports it
    boolean isTestAlwaysFocus(); // if true, pretend autofocus always successful
    void cameraSetup(); // called when the camera is (re-)set up - should update UI elements/parameters that depend on camera settings
    void touchEvent(MotionEvent event);
    void onFailedStartPreview(); // called if failed to start camera preview
    void onCameraError(); // called if the camera closes due to serious error.
    void onPhotoError(); // callback for failing to take a photo
    void hasPausedPreview(boolean paused); // called when the preview is paused or unpaused (due to getPausePreviewPref())
    void cameraInOperation(boolean in_operation, boolean is_video); // called when the camera starts/stops being operation (taking photos or recording video, including if preview is paused after taking a photo), use to disable GUI elements during camera operation
    void turnFrontScreenFlashOn(); // called when front-screen "flash" required (for modes flash_frontscreen_auto, flash_frontscreen_on); the application should light up the screen, until cameraInOperation(false) is called
    void cameraClosed();
    void timerBeep(long remaining_time); // n.b., called once per second on timer countdown - so application can beep, or do whatever it likes
    void multitouchZoom(int new_zoom); // indicates that the zoom has changed due to multitouch gesture on preview
    void setCameraIdPref(int cameraId);
    void setFlashPref(String flash_value);
    void setFocusPref(String focus_value, boolean is_video);
    void setSceneModePref(String scene_mode);
    void clearSceneModePref();
    void setISOPref(String iso);
    void clearISOPref();
    void setExposureCompensationPref(int exposure);
    void clearExposureCompensationPref();
    void setCameraResolutionPref(int width, int height);
    void setZoomPref(int zoom);
    void requestCameraPermission(); // for Android 6+: called when trying to open camera, but CAMERA permission not available
    boolean needsStoragePermission(); // return true if the preview should call requestStoragePermission() if WRITE_EXTERNAL_STORAGE not available (i.e., if the application needs storage permission, e.g., to save photos)
    void requestStoragePermission(); // for Android 6+: called when trying to open camera, but WRITE_EXTERNAL_STORAGE permission not available
    void setExposureTimePref(long exposure_time);
    void clearExposureTimePref();
    void onControllerNull();
    void setFocusDistancePref(float focus_distance, boolean is_target_distance);
    // callbacks
    void onDrawPreview(Canvas canvas);
    boolean onPictureTaken(byte [] data, Date current_date);
    boolean onBurstPictureTaken(List<byte []> images, Date current_date);
    boolean onRawPictureTaken(RawImage raw_image, Date current_date);
    boolean onRawBurstPictureTaken(List<RawImage> raw_images, Date current_date);
    void onCaptureStarted(); // called immediately before we start capturing the picture
    void onPictureCompleted(); // called after all picture callbacks have been called and returned
    void onContinuousFocusMove(boolean start); // called when focusing starts/stop in continuous picture mode (in photo mode only)
}

