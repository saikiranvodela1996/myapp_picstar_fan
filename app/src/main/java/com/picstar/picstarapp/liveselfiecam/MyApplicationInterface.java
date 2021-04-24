package com.picstar.picstarapp.liveselfiecam;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;

import com.picstar.picstarapp.R;

import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.RawImage;

import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PreferenceKeys;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Our implementation of ApplicationInterface, see there for details.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyApplicationInterface extends BasicApplicationInterface {
    private static final String TAG = "MyApplicationInterface";
    private final static float aperture_default = -1.0f;
    private float aperture = aperture_default;

    // note, okay to change the order of enums in future versions, as getPhotoMode() does not rely on the order for the saved photo mode
    public enum PhotoMode {
        Standard,
        DRO, // single image "fake" HDR
        HDR, // HDR created from multiple (expo bracketing) images
        ExpoBracketing, // take multiple expo bracketed images, without combining to a single image
        FocusBracketing, // take multiple focus bracketed images, without combining to a single image
        FastBurst,
        NoiseReduction,
        Panorama
    }

    private final LiveSelfieCameraActivity main_activity;
    private final GyroSensor gyroSensor;
    private final StorageUtils storageUtils;
    private final DrawPreview drawPreview;
    private final ImageSaver imageSaver;

    private final static float panorama_pics_per_screen = 3.33333f;
    private int n_capture_images = 0; // how many calls to onPictureTaken() since the last call to onCaptureStarted()
    private int n_capture_images_raw = 0; // how many calls to onRawPictureTaken() since the last call to onCaptureStarted()
    private int n_panorama_pics = 0;
    public final static int max_panorama_pics_c = 10; // if we increase this, review against memory requirements under MainActivity.supportsPanorama()
    private boolean panorama_pic_accepted; // whether the last panorama picture was accepted, or else needs to be retaken
    private boolean panorama_dir_left_to_right = true; // direction of panorama (set after we've captured two images)

    private final Rect text_bounds = new Rect();
    private boolean used_front_screen_flash ;

    private final SharedPreferences sharedPreferences;



    private static class LastImage {
        final boolean share; // one of the images in the list should have share set to true, to indicate which image to share
        final String name;
        Uri uri;

        LastImage(Uri uri, boolean share) {
            this.name = null;
            this.uri = uri;
            this.share = share;
        }

        LastImage(String filename, boolean share) {
            this.name = filename;
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ) {
                this.uri = null;
            }
            else {
                this.uri = Uri.parse("file://" + this.name);
            }
            this.share = share;
        }
    }
    private final List<LastImage> last_images = new ArrayList<>();
    private final static int cameraId_default = 0;
    private boolean has_set_cameraId;
    private int cameraId = cameraId_default;
    private final static String nr_mode_default = "preference_nr_mode_normal";
    private String nr_mode = nr_mode_default;
    private int zoom_factor; // don't save zoom, as doing so tends to confuse users; other camera applications don't seem to save zoom when pause/resuming

    public MyApplicationInterface(LiveSelfieCameraActivity main_activity, Bundle savedInstanceState) {
        long debug_time = 0;
        if( MyDebug.LOG ) {
            Log.d(TAG, "MyApplicationInterface");
            debug_time = System.currentTimeMillis();
        }
        this.main_activity = main_activity;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
        if( MyDebug.LOG )
            Log.d(TAG, "MyApplicationInterface: time after creating location supplier: " + (System.currentTimeMillis() - debug_time));
        this.gyroSensor = new GyroSensor(main_activity);
        this.storageUtils = new StorageUtils(main_activity, this);
        if( MyDebug.LOG )
            Log.d(TAG, "MyApplicationInterface: time after creating storage utils: " + (System.currentTimeMillis() - debug_time));
        this.drawPreview = new DrawPreview(main_activity, this);

        this.imageSaver = new ImageSaver(main_activity);
        this.imageSaver.start();

        this.reset();
        if( savedInstanceState != null ) {
            // load the things we saved in onSaveInstanceState().
            if( MyDebug.LOG )
                Log.d(TAG, "read from savedInstanceState");
            has_set_cameraId = true;
            cameraId = savedInstanceState.getInt("cameraId", cameraId_default);
            nr_mode = savedInstanceState.getString("nr_mode", nr_mode_default);
        }

        if( MyDebug.LOG )
            Log.d(TAG, "MyApplicationInterface: total time to create MyApplicationInterface: " + (System.currentTimeMillis() - debug_time));
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public void onControllerNull(){
        main_activity.onControllerNull();
    }

    public void onSaveInstanceState(Bundle state) {
        if( MyDebug.LOG )
            Log.d(TAG, "onSaveInstanceState");
        if( MyDebug.LOG )
            Log.d(TAG, "save cameraId: " + cameraId);
        state.putInt("cameraId", cameraId);
        if( MyDebug.LOG )
            Log.d(TAG, "save nr_mode: " + nr_mode);
        state.putString("nr_mode", nr_mode);
    }

    public void onDestroy() {
        if( MyDebug.LOG )
            Log.d(TAG, "onDestroy");
        if( drawPreview != null ) {
            drawPreview.onDestroy();
        }
        if( imageSaver != null ) {
            imageSaver.onDestroy();
        }
    }




    public GyroSensor getGyroSensor() {
        return gyroSensor;
    }

    public StorageUtils getStorageUtils() {
        return storageUtils;
    }

    public ImageSaver getImageSaver() {
        return imageSaver;
    }

    public DrawPreview getDrawPreview() {
        return drawPreview;
    }

    @Override
    public Context getContext() {
        return main_activity;
    }

    @Override
    public boolean useCamera2() {
        if( main_activity.supportsCamera2() ) {
            String camera_api = sharedPreferences.getString(PreferenceKeys.CameraAPIPreferenceKey, PreferenceKeys.CameraAPIPreferenceDefault);
            if( "preference_camera_api_camera2".equals(camera_api) ) {
                return true;
            }
        }
        return false;
    }


    @Override
    public int getCameraIdPref() {
        return cameraId;
    }

    @Override
    public String getFlashPref() {
        return sharedPreferences.getString(PreferenceKeys.getFlashPreferenceKey(cameraId), "");
    }

    @Override
    public String getFocusPref(boolean is_video) {
        if( getPhotoMode() == PhotoMode.FocusBracketing) {
            // alway run in manual focus mode for focus bracketing
            return "focus_mode_manual2";
        }
        return sharedPreferences.getString(PreferenceKeys.getFocusPreferenceKey(cameraId, is_video), "");
    }

    @Override
    public String getSceneModePref() {
        return sharedPreferences.getString(PreferenceKeys.SceneModePreferenceKey, CameraController.SCENE_MODE_DEFAULT);
    }

    @Override
    public String getISOPref() {
        return sharedPreferences.getString(PreferenceKeys.ISOPreferenceKey, CameraController.ISO_DEFAULT);
    }

    @Override
    public int getExposureCompensationPref() {
        String value = sharedPreferences.getString(PreferenceKeys.ExposurePreferenceKey, "0");
        if( MyDebug.LOG )
            Log.d(TAG, "saved exposure value: " + value);
        int exposure = 0;
        try {
            exposure = Integer.parseInt(value);
            if( MyDebug.LOG )
                Log.d(TAG, "exposure: " + exposure);
        }
        catch(NumberFormatException exception) {
            if( MyDebug.LOG )
                Log.d(TAG, "exposure invalid format, can't parse to int");
        }
        return exposure;
    }

    public static CameraController.Size choosePanoramaResolution(List<CameraController.Size> sizes) {
        final int max_width_c = 2080;
        boolean found = false;
        CameraController.Size best_size = null;
        // find largest width <= max_width_c with aspect ratio 4:3
        for(CameraController.Size size : sizes) {
            if( size.width <= max_width_c ) {
                double aspect_ratio = ((double)size.width) / (double)size.height;
                if( Math.abs(aspect_ratio - 4.0/3.0) < 1.0e-5 ) {
                    if( !found || size.width > best_size.width ) {
                        found = true;
                        best_size = size;
                    }
                }
            }
        }
        if( found ) {
            return best_size;
        }
        for(CameraController.Size size : sizes) {
            if( size.width <= max_width_c ) {
                if( !found || size.width > best_size.width ) {
                    found = true;
                    best_size = size;
                }
            }
        }
        if( found ) {
            return best_size;
        }
        for(CameraController.Size size : sizes) {
            if( !found || size.width < best_size.width ) {
                found = true;
                best_size = size;
            }
        }
        return best_size;
    }

    @Override
    public Pair<Integer, Integer> getCameraResolutionPref() {
        if( getPhotoMode() == PhotoMode.Panorama ) {
            CameraController.Size best_size = choosePanoramaResolution(main_activity.getPreview().getSupportedPictureSizes(false));
            return new Pair<>(best_size.width, best_size.height);
        }
        String resolution_value = sharedPreferences.getString(PreferenceKeys.getResolutionPreferenceKey(cameraId), "");
        if( MyDebug.LOG )
            Log.d(TAG, "resolution_value: " + resolution_value);
        if( resolution_value.length() > 0 ) {
            // parse the saved size, and make sure it is still valid
            int index = resolution_value.indexOf(' ');
            if( index == -1 ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "resolution_value invalid format, can't find space");
            }
            else {
                String resolution_w_s = resolution_value.substring(0, index);
                String resolution_h_s = resolution_value.substring(index+1);
                if( MyDebug.LOG ) {
                    Log.d(TAG, "resolution_w_s: " + resolution_w_s);
                    Log.d(TAG, "resolution_h_s: " + resolution_h_s);
                }
                try {
                    int resolution_w = Integer.parseInt(resolution_w_s);
                    if( MyDebug.LOG )
                        Log.d(TAG, "resolution_w: " + resolution_w);
                    int resolution_h = Integer.parseInt(resolution_h_s);
                    if( MyDebug.LOG )
                        Log.d(TAG, "resolution_h: " + resolution_h);
                    return new Pair<>(resolution_w, resolution_h);
                }
                catch(NumberFormatException exception) {
                    if( MyDebug.LOG )
                        Log.d(TAG, "resolution_value invalid format, can't parse w or h to int");
                }
            }
        }
        return null;
    }
    private int getSaveImageQualityPref() {
        if( MyDebug.LOG )
            Log.d(TAG, "getSaveImageQualityPref");
        String image_quality_s = sharedPreferences.getString(PreferenceKeys.QualityPreferenceKey, "90");
        int image_quality;
        try {
            image_quality = Integer.parseInt(image_quality_s);
        }
        catch(NumberFormatException exception) {
            if( MyDebug.LOG )
                Log.e(TAG, "image_quality_s invalid format: " + image_quality_s);
            image_quality = 90;
        }
        if( isRawOnly() ) {
            // if raw only mode, we can set a lower quality for the JPEG, as it isn't going to be saved - only used for
            // the thumbnail and pause preview option
            if( MyDebug.LOG )
                Log.d(TAG, "set lower quality for raw_only mode");
            image_quality = Math.min(image_quality, 70);
        }
        return image_quality;
    }

    @Override
    public int getImageQualityPref() {
        if( MyDebug.LOG )
            Log.d(TAG, "getImageQualityPref");
        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.DRO )
            return 100;
        else if( photo_mode == PhotoMode.HDR )
            return 100;
        else if( photo_mode == PhotoMode.NoiseReduction )
            return 100;

        if( getImageFormatPref() != ImageSaver.Request.ImageFormat.STD )
            return 100;

        return getSaveImageQualityPref();
    }

    @Override
    public boolean getFaceDetectionPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.FaceDetectionPreferenceKey, false);
    }

    @Override
    public String getPreviewSizePref() {
        return sharedPreferences.getString(PreferenceKeys.PreviewSizePreferenceKey, "preference_preview_size_wysiwyg");
    }

    @Override
    public String getPreviewRotationPref() {
        return sharedPreferences.getString(PreferenceKeys.getRotatePreviewPreferenceKey(), "0");
    }

    @Override
    public String getLockOrientationPref() {
        if( getPhotoMode() == PhotoMode.Panorama )
            return "portrait"; // for now panorama only supports portrait
        return sharedPreferences.getString(PreferenceKeys.getLockOrientationPreferenceKey(), "none");
    }

    @Override
    public boolean getTouchCapturePref() {
        String value = sharedPreferences.getString(PreferenceKeys.TouchCapturePreferenceKey, "none");
        return value.equals("single");
    }

    @Override
    public boolean getDoubleTapCapturePref() {
        String value = sharedPreferences.getString(PreferenceKeys.TouchCapturePreferenceKey, "none");
        return value.equals("double");
    }

    @Override
    public boolean getPausePreviewPref() {
        if( main_activity.lastContinuousFastBurst() ) {
            return false;
        }
        else if( getPhotoMode() == PhotoMode.Panorama ) {
            // don't pause preview when taking photos for panorama mode
            return false;
        }
        return sharedPreferences.getBoolean(PreferenceKeys.PausePreviewPreferenceKey, false);
    }

    public boolean getThumbnailAnimationPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.ThumbnailAnimationPreferenceKey, true);
    }

    @Override
    public boolean getShutterSoundPref() {
        if( getPhotoMode() == PhotoMode.Panorama )
            return false;
        return sharedPreferences.getBoolean(PreferenceKeys.getShutterSoundPreferenceKey(), true);
    }

    @Override
    public boolean getStartupFocusPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.getStartupFocusPreferenceKey(), true);
    }

    @Override
    public long getTimerPref() {
        if( getPhotoMode() == PhotoMode.Panorama )
            return 0; // don't support timer with panorama
        String timer_value = sharedPreferences.getString(PreferenceKeys.getTimerPreferenceKey(), "0");
        long timer_delay;
        try {
            timer_delay = (long)Integer.parseInt(timer_value) * 1000;
        }
        catch(NumberFormatException e) {
            if( MyDebug.LOG )
                Log.e(TAG, "failed to parse preference_timer value: " + timer_value);
            e.printStackTrace();
            timer_delay = 0;
        }
        return timer_delay;
    }

    @Override
    public String getRepeatPref() {
        if( getPhotoMode() == PhotoMode.Panorama )
            return "1"; // don't support repeat with panorama
        return sharedPreferences.getString(PreferenceKeys.getRepeatModePreferenceKey(), "1");
    }

    @Override
    public long getRepeatIntervalPref() {
        String timer_value = sharedPreferences.getString(PreferenceKeys.getRepeatIntervalPreferenceKey(), "0");
        long timer_delay;
        try {
            float timer_delay_s = Float.parseFloat(timer_value);
            if( MyDebug.LOG )
                Log.d(TAG, "timer_delay_s: " + timer_delay_s);
            timer_delay = (long)(timer_delay_s * 1000);
        }
        catch(NumberFormatException e) {
            if( MyDebug.LOG )
                Log.e(TAG, "failed to parse repeat interval value: " + timer_value);
            e.printStackTrace();
            timer_delay = 0;
        }
        return timer_delay;
    }

    @Override
    public boolean getGeotaggingPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.LocationPreferenceKey, false);
    }

    @Override
    public boolean getRequireLocationPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.RequireLocationPreferenceKey, false);
    }

    boolean getGeodirectionPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.GPSDirectionPreferenceKey, false);
    }

    @Override
    public boolean getRecordAudioPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.getRecordAudioPreferenceKey(), true);
    }



    public boolean getAutoStabilisePref() {
        boolean auto_stabilise = sharedPreferences.getBoolean(PreferenceKeys.AutoStabilisePreferenceKey, false);
        return auto_stabilise && main_activity.supportsAutoStabilise();
    }

    public String getStampPref() {
        return sharedPreferences.getString(PreferenceKeys.StampPreferenceKey, "preference_stamp_no");
    }

    private String getStampDateFormatPref() {
        return sharedPreferences.getString(PreferenceKeys.StampDateFormatPreferenceKey, "preference_stamp_dateformat_default");
    }

    private String getStampTimeFormatPref() {
        return sharedPreferences.getString(PreferenceKeys.StampTimeFormatPreferenceKey, "preference_stamp_timeformat_default");
    }

    private String getStampGPSFormatPref() {
        return sharedPreferences.getString(PreferenceKeys.StampGPSFormatPreferenceKey, "preference_stamp_gpsformat_default");
    }

    private String getStampGeoAddressPref() {
        return sharedPreferences.getString(PreferenceKeys.StampGeoAddressPreferenceKey, "preference_stamp_geo_address_no");
    }

    private String getUnitsDistancePref() {
        return sharedPreferences.getString(PreferenceKeys.UnitsDistancePreferenceKey, "preference_units_distance_m");
    }

    public String getTextStampPref() {
        return sharedPreferences.getString(PreferenceKeys.TextStampPreferenceKey, "");
    }

    private int getTextStampFontSizePref() {
        int font_size = 12;
        String value = sharedPreferences.getString(PreferenceKeys.StampFontSizePreferenceKey, "12");
        if( MyDebug.LOG )
            Log.d(TAG, "saved font size: " + value);
        try {
            font_size = Integer.parseInt(value);
            if( MyDebug.LOG )
                Log.d(TAG, "font_size: " + font_size);
        }
        catch(NumberFormatException exception) {
            if( MyDebug.LOG )
                Log.d(TAG, "font size invalid format, can't parse to int");
        }
        return font_size;
    }

    @Override
    public int getZoomPref() {
        if( MyDebug.LOG )
            Log.d(TAG, "getZoomPref: " + zoom_factor);
        return zoom_factor;
    }

    @Override
    public double getCalibratedLevelAngle() {
        return sharedPreferences.getFloat(PreferenceKeys.CalibratedLevelAnglePreferenceKey, 0.0f);
    }

    @Override
    public boolean canTakeNewPhoto() {
        if( MyDebug.LOG )
            Log.d(TAG, "canTakeNewPhoto");

        int n_raw, n_jpegs;
        {
            n_jpegs = 1; // default

            if( main_activity.getPreview().supportsExpoBracketing() && this.isExpoBracketingPref() ) {
                n_jpegs = this.getExpoBracketingNImagesPref();
            }
            else if( main_activity.getPreview().supportsFocusBracketing() && this.isFocusBracketingPref() ) {
                n_jpegs = 1;
            }
            else if( main_activity.getPreview().supportsBurst() && this.isCameraBurstPref() ) {
                if( this.getBurstForNoiseReduction() ) {
                    if( this.getNRModePref() == NRModePref.NRMODE_LOW_LIGHT ) {
                        n_jpegs = CameraController.N_IMAGES_NR_DARK_LOW_LIGHT;
                    }
                    else {
                        n_jpegs = CameraController.N_IMAGES_NR_DARK;
                    }
                }
                else {
                    n_jpegs = this.getBurstNImages();
                }
            }

            if( main_activity.getPreview().supportsRaw() && this.getRawPref() == RawPref.RAWPREF_JPEG_DNG ) {
                // note, even in RAW only mode, the CameraController will still take JPEG+RAW (we still need to JPEG to
                // generate a bitmap from for thumbnail and pause preview option), so this still generates a request in
                // the ImageSaver
                n_raw = n_jpegs;
            }
            else {
                n_raw = 0;
            }
        }

        int photo_cost = imageSaver.computePhotoCost(n_raw, n_jpegs);
        if( imageSaver.queueWouldBlock(photo_cost) ) {
            if( MyDebug.LOG )
                Log.d(TAG, "canTakeNewPhoto: no, as queue would block");
            return false;
        }

        // even if the queue isn't full, we may apply additional limits
        int n_images_to_save = imageSaver.getNImagesToSave();
        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.FastBurst || photo_mode == PhotoMode.Panorama ) {
            // only allow one fast burst at a time, so require queue to be empty
            if( n_images_to_save > 0 ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "canTakeNewPhoto: no, as too many for fast burst");
                return false;
            }
        }
        if( photo_mode == PhotoMode.NoiseReduction ) {
            // allow a max of 2 photos in memory when at max of 8 images
            if( n_images_to_save >= 2*photo_cost ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "canTakeNewPhoto: no, as too many for nr");
                return false;
            }
        }
        if( n_jpegs > 1 ) {
            // if in any other kind of burst mode (e.g., expo burst, HDR), allow a max of 3 photos in memory
            if( n_images_to_save >= 3*photo_cost ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "canTakeNewPhoto: no, as too many for burst");
                return false;
            }
        }
        if( n_raw > 0 ) {
            // if RAW mode, allow a max of 3 photos
            if( n_images_to_save >= 3*photo_cost ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "canTakeNewPhoto: no, as too many for raw");
                return false;
            }
        }
        // otherwise, still have a max limit of 5 photos
        if( n_images_to_save >= 5*photo_cost ) {
            if( main_activity.supportsNoiseReduction() && n_images_to_save <= 8 ) {
                // if we take a photo in NR mode, then switch to std mode, it doesn't make sense to suddenly block!
                // so need to at least allow a new photo, if the number of photos is less than 1 NR photo
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "canTakeNewPhoto: no, as too many for regular");
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean imageQueueWouldBlock(int n_raw, int n_jpegs) {
        if( MyDebug.LOG )
            Log.d(TAG, "imageQueueWouldBlock");
        return imageSaver.queueWouldBlock(n_raw, n_jpegs);
    }

    @Override
    public long getExposureTimePref() {
        return sharedPreferences.getLong(PreferenceKeys.ExposureTimePreferenceKey, CameraController.EXPOSURE_TIME_DEFAULT);
    }

    @Override
    public float getFocusDistancePref(boolean is_target_distance) {
        return sharedPreferences.getFloat(is_target_distance ? PreferenceKeys.FocusBracketingTargetDistancePreferenceKey : PreferenceKeys.FocusDistancePreferenceKey, 0.0f);
    }

    @Override
    public boolean isExpoBracketingPref() {
        PhotoMode photo_mode = getPhotoMode();
        return photo_mode == PhotoMode.HDR || photo_mode == PhotoMode.ExpoBracketing;
    }

    @Override
    public boolean isFocusBracketingPref() {
        PhotoMode photo_mode = getPhotoMode();
        return photo_mode == PhotoMode.FocusBracketing;
    }

    @Override
    public boolean isCameraBurstPref() {
        PhotoMode photo_mode = getPhotoMode();
        return photo_mode == PhotoMode.FastBurst || photo_mode == PhotoMode.NoiseReduction;
    }

    @Override
    public int getBurstNImages() {
        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.FastBurst ) {
            String n_images_value = sharedPreferences.getString(PreferenceKeys.FastBurstNImagesPreferenceKey, "5");
            int n_images;
            try {
                n_images = Integer.parseInt(n_images_value);
            }
            catch(NumberFormatException e) {
                if( MyDebug.LOG )
                    Log.e(TAG, "failed to parse FastBurstNImagesPreferenceKey value: " + n_images_value);
                e.printStackTrace();
                n_images = 5;
            }
            return n_images;
        }
        return 1;
    }

    @Override
    public boolean getBurstForNoiseReduction() {
        PhotoMode photo_mode = getPhotoMode();
        return photo_mode == PhotoMode.NoiseReduction;
    }

    @Override
    public NRModePref getNRModePref() {
		/*if( MyDebug.LOG )
			Log.d(TAG, "nr_mode: " + nr_mode);*/
        switch( nr_mode ) {
            case "preference_nr_mode_low_light":
                return NRModePref.NRMODE_LOW_LIGHT;
        }
        return NRModePref.NRMODE_NORMAL;
    }

    @Override
    public int getExpoBracketingNImagesPref() {
        if( MyDebug.LOG )
            Log.d(TAG, "getExpoBracketingNImagesPref");
        int n_images;
        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.HDR ) {
            // always set 3 images for HDR
            n_images = 3;
        }
        else {
            String n_images_s = sharedPreferences.getString(PreferenceKeys.ExpoBracketingNImagesPreferenceKey, "3");
            try {
                n_images = Integer.parseInt(n_images_s);
            }
            catch(NumberFormatException exception) {
                if( MyDebug.LOG )
                    Log.e(TAG, "n_images_s invalid format: " + n_images_s);
                n_images = 3;
            }
        }
        if( MyDebug.LOG )
            Log.d(TAG, "n_images = " + n_images);
        return n_images;
    }

    @Override
    public double getExpoBracketingStopsPref() {
        if( MyDebug.LOG )
            Log.d(TAG, "getExpoBracketingStopsPref");
        double n_stops;
        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.HDR ) {
            // always set 2 stops for HDR
            n_stops = 2.0;
        }
        else {
            String n_stops_s = sharedPreferences.getString(PreferenceKeys.ExpoBracketingStopsPreferenceKey, "2");
            try {
                n_stops = Double.parseDouble(n_stops_s);
            }
            catch(NumberFormatException exception) {
                if( MyDebug.LOG )
                    Log.e(TAG, "n_stops_s invalid format: " + n_stops_s);
                n_stops = 2.0;
            }
        }
        if( MyDebug.LOG )
            Log.d(TAG, "n_stops = " + n_stops);
        return n_stops;
    }

    @Override
    public int getFocusBracketingNImagesPref() {
        if( MyDebug.LOG )
            Log.d(TAG, "getFocusBracketingNImagesPref");
        int n_images;
        String n_images_s = sharedPreferences.getString(PreferenceKeys.FocusBracketingNImagesPreferenceKey, "3");
        try {
            n_images = Integer.parseInt(n_images_s);
        }
        catch(NumberFormatException exception) {
            if( MyDebug.LOG )
                Log.e(TAG, "n_images_s invalid format: " + n_images_s);
            n_images = 3;
        }
        if( MyDebug.LOG )
            Log.d(TAG, "n_images = " + n_images);
        return n_images;
    }

    @Override
    public boolean getFocusBracketingAddInfinityPref() {
        return sharedPreferences.getBoolean(PreferenceKeys.FocusBracketingAddInfinityPreferenceKey, false);
    }

    public PhotoMode getPhotoMode() {
        String photo_mode_pref = sharedPreferences.getString(PreferenceKeys.PhotoModePreferenceKey, "preference_photo_mode_std");
        boolean dro = photo_mode_pref.equals("preference_photo_mode_dro");
        if( dro && main_activity.supportsDRO() )
            return PhotoMode.DRO;
        boolean hdr = photo_mode_pref.equals("preference_photo_mode_hdr");
        if( hdr && main_activity.supportsHDR() )
            return PhotoMode.HDR;
        boolean expo_bracketing = photo_mode_pref.equals("preference_photo_mode_expo_bracketing");
        if( expo_bracketing && main_activity.supportsExpoBracketing() )
            return PhotoMode.ExpoBracketing;
        boolean focus_bracketing = photo_mode_pref.equals("preference_photo_mode_focus_bracketing");
        if( focus_bracketing && main_activity.supportsFocusBracketing() )
            return PhotoMode.FocusBracketing;
        boolean fast_burst = photo_mode_pref.equals("preference_photo_mode_fast_burst");
        if( fast_burst && main_activity.supportsFastBurst() )
            return PhotoMode.FastBurst;
        boolean noise_reduction = photo_mode_pref.equals("preference_photo_mode_noise_reduction");
        if( noise_reduction && main_activity.supportsNoiseReduction() )
            return PhotoMode.NoiseReduction;
        boolean panorama = photo_mode_pref.equals("preference_photo_mode_panorama");
        if( panorama && main_activity.supportsPanorama() )
            return PhotoMode.Panorama;
        return PhotoMode.Standard;
    }

    @Override
    public boolean getOptimiseAEForDROPref() {
        PhotoMode photo_mode = getPhotoMode();
        return( photo_mode == PhotoMode.DRO );
    }

    private ImageSaver.Request.ImageFormat getImageFormatPref() {
        switch( sharedPreferences.getString(PreferenceKeys.ImageFormatPreferenceKey, "preference_image_format_jpeg") ) {
            case "preference_image_format_webp":
                return ImageSaver.Request.ImageFormat.WEBP;
            case "preference_image_format_png":
                return ImageSaver.Request.ImageFormat.PNG;
            default:
                return ImageSaver.Request.ImageFormat.STD;
        }
    }

    public boolean isRawAllowed(PhotoMode photo_mode) {
        if( isImageCaptureIntent() )
            return false;
        if( photo_mode == PhotoMode.Standard || photo_mode == PhotoMode.DRO ) {
            return true;
        }
        else if( photo_mode == PhotoMode.ExpoBracketing ) {
            return sharedPreferences.getBoolean(PreferenceKeys.AllowRawForExpoBracketingPreferenceKey, true) &&
                    main_activity.supportsBurstRaw();
        }
        else if( photo_mode == PhotoMode.HDR ) {
            // for HDR, RAW is only relevant if we're going to be saving the base expo images (otherwise there's nothing to save)
            return sharedPreferences.getBoolean(PreferenceKeys.HDRSaveExpoPreferenceKey, false) &&
                    sharedPreferences.getBoolean(PreferenceKeys.AllowRawForExpoBracketingPreferenceKey, true) &&
                    main_activity.supportsBurstRaw();
        }
        else if( photo_mode == PhotoMode.FocusBracketing ) {
            return sharedPreferences.getBoolean(PreferenceKeys.AllowRawForFocusBracketingPreferenceKey, true) &&
                    main_activity.supportsBurstRaw();
        }
        return false;
    }

    /** Return whether to capture JPEG, or RAW+JPEG.
     *  Note even if in RAW only mode, we still capture RAW+JPEG - the JPEG is needed for things like
     *  getting the bitmap for the thumbnail and pause preview option; we simply don't do any post-
     *  processing or saving on the JPEG.
     */
    @Override
    public RawPref getRawPref() {
        PhotoMode photo_mode = getPhotoMode();
        if( isRawAllowed(photo_mode) ) {
            switch( sharedPreferences.getString(PreferenceKeys.RawPreferenceKey, "preference_raw_no") ) {
                case "preference_raw_yes":
                case "preference_raw_only":
                    return RawPref.RAWPREF_JPEG_DNG;
            }
        }
        return RawPref.RAWPREF_JPEG_ONLY;
    }

    /** Whether RAW only mode is enabled.
     */
    public boolean isRawOnly() {
        PhotoMode photo_mode = getPhotoMode();
        return isRawOnly(photo_mode);
    }

    public boolean isRawOnly(PhotoMode photo_mode) {
        if( isRawAllowed(photo_mode) ) {
            switch( sharedPreferences.getString(PreferenceKeys.RawPreferenceKey, "preference_raw_no") ) {
                case "preference_raw_only":
                    return true;
            }
        }
        return false;
    }

    @Override
    public int getMaxRawImages() {
        return imageSaver.getMaxDNG();
    }

    @Override
    public boolean useCamera2FakeFlash() {
        return sharedPreferences.getBoolean(PreferenceKeys.Camera2FakeFlashPreferenceKey, false);
    }

    @Override
    public boolean useCamera2FastBurst() {
        return sharedPreferences.getBoolean(PreferenceKeys.Camera2FastBurstPreferenceKey, true);
    }

    @Override
    public boolean isPreviewInBackground() {
        return main_activity.isCameraInBackground();
    }

    @Override
    public boolean allowZoom() {
        if( getPhotoMode() == PhotoMode.Panorama ) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isTestAlwaysFocus() {
        if( MyDebug.LOG ) {
            Log.d(TAG, "isTestAlwaysFocus: " + main_activity.is_test);
        }
        return main_activity.is_test;
    }

    @Override
    public void cameraSetup() {
        main_activity.cameraSetup();
        drawPreview.clearContinuousFocusMove();
        drawPreview.updateSettings();
    }

    @Override
    public void onContinuousFocusMove(boolean start) {
        if( MyDebug.LOG )
            Log.d(TAG, "onContinuousFocusMove: " + start);
        drawPreview.onContinuousFocusMove(start);
    }

    public void startPanorama() {
        if( MyDebug.LOG )
            Log.d(TAG, "startPanorama");
        gyroSensor.startRecording();
        n_panorama_pics = 0;
        panorama_pic_accepted = false;
        panorama_dir_left_to_right = true;

        main_activity.getMainUI().setTakePhotoIcon();
    }

    /** Ends panorama and submits the panoramic images to be processed.
     */
    public void finishPanorama() {
        if( MyDebug.LOG )
            Log.d(TAG, "finishPanorama");

        imageSaver.getImageBatchRequest().panorama_dir_left_to_right = this.panorama_dir_left_to_right;

        stopPanorama(false);

        boolean image_capture_intent = isImageCaptureIntent();
        boolean do_in_background = saveInBackground(image_capture_intent);
        imageSaver.finishImageBatch(do_in_background);
    }

    public void stopPanorama(boolean is_cancelled) {
        if( MyDebug.LOG )
            Log.d(TAG, "stopPanorama");
        if( !gyroSensor.isRecording() ) {
            if( MyDebug.LOG )
                Log.d(TAG, "...nothing to stop");
            return;
        }
        gyroSensor.stopRecording();
        clearPanoramaPoint();
        if( is_cancelled ) {
            imageSaver.flushImageBatch();
        }
        main_activity.getMainUI().setTakePhotoIcon();
        main_activity.getMainUI().showGUI(); // refresh UI icons now that we've stopped panorama
    }

    private void setNextPanoramaPoint(boolean repeat) {
        if( MyDebug.LOG )
            Log.d(TAG, "setNextPanoramaPoint");
        float camera_angle_y = main_activity.getPreview().getViewAngleY(false);
        if( !repeat )
            n_panorama_pics++;
        if( MyDebug.LOG )
            Log.d(TAG, "n_panorama_pics is now: " + n_panorama_pics);
        if( n_panorama_pics == max_panorama_pics_c ) {
            if( MyDebug.LOG )
                Log.d(TAG, "reached max panorama limit");
            finishPanorama();
            return;
        }
        float angle = (float) Math.toRadians(camera_angle_y) * n_panorama_pics;
        if( n_panorama_pics > 1 && !panorama_dir_left_to_right ) {
            angle = - angle; // for right-to-left
        }
        float x = (float) Math.sin(angle / panorama_pics_per_screen);
        float z = (float) -Math.cos(angle / panorama_pics_per_screen);
        setNextPanoramaPoint(x, 0.0f, z);

        if( n_panorama_pics == 1 ) {
            // also set target for right-to-left
            angle = - angle;
            x = (float) Math.sin(angle / panorama_pics_per_screen);
            z = (float) -Math.cos(angle / panorama_pics_per_screen);
            gyroSensor.addTarget(x, 0.0f, z);
            drawPreview.addGyroDirectionMarker(x, 0.0f, z);
        }
    }

    private void setNextPanoramaPoint(float x, float y, float z) {
        if( MyDebug.LOG )
            Log.d(TAG, "setNextPanoramaPoint : " + x + " , " + y + " , " + z);

        final float target_angle = 1.0f * 0.01745329252f;
        final float upright_angle_tol = 2.0f * 0.017452406437f;
        final float too_far_angle = 45.0f * 0.01745329252f;
        gyroSensor.setTarget(x, y, z, target_angle, upright_angle_tol, too_far_angle, new GyroSensor.TargetCallback() {
            @Override
            public void onAchieved(int indx) {
                if( MyDebug.LOG ) {
                    Log.d(TAG, "TargetCallback.onAchieved: " + indx);
                    Log.d(TAG, "    n_panorama_pics: " + n_panorama_pics);
                }
                gyroSensor.disableTargetCallback();
                if( n_panorama_pics == 1 ) {
                    panorama_dir_left_to_right = indx == 0;
                    if( MyDebug.LOG )
                        Log.d(TAG, "set panorama_dir_left_to_right to " + panorama_dir_left_to_right);
                }
                main_activity.takePicturePressed(false, false);
            }

            @Override
            public void onTooFar() {
                if( MyDebug.LOG )
                    Log.d(TAG, "TargetCallback.onTooFar");

                if( !main_activity.is_test ) {
                    MyApplicationInterface.this.stopPanorama(true);
                }
            }

        });
        drawPreview.setGyroDirectionMarker(x, y, z);
    }

    private void clearPanoramaPoint() {
        if( MyDebug.LOG )
            Log.d(TAG, "clearPanoramaPoint");
        gyroSensor.clearTarget();
        drawPreview.clearGyroDirectionMarker();
    }

    static float getPanoramaPicsPerScreen() {
        return panorama_pics_per_screen;
    }

    @Override
    public void touchEvent(MotionEvent event) {
        main_activity.getMainUI().closePopup();
        if( main_activity.usingKitKatImmersiveMode() ) {
            main_activity.setImmersiveMode(false);
        }
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
        if( MyDebug.LOG )
            Log.d(TAG, "hasPausedPreview: " + paused);
    }

    @Override
    public void cameraInOperation(boolean in_operation, boolean is_video) {
        if( MyDebug.LOG )
            Log.d(TAG, "cameraInOperation: " + in_operation);
        if( !in_operation && used_front_screen_flash ) {
            main_activity.setBrightnessForCamera(false); // ensure screen brightness matches user preference, after using front screen flash
            used_front_screen_flash = false;
        }
        drawPreview.cameraInOperation(in_operation);
        main_activity.getMainUI().showGUI(!in_operation, is_video);
    }

    @Override
    public void turnFrontScreenFlashOn() {
        if( MyDebug.LOG )
            Log.d(TAG, "turnFrontScreenFlashOn");
        used_front_screen_flash = true;
        main_activity.setBrightnessForCamera(true); // ensure we have max screen brightness, even if user preference not set for max brightness
        drawPreview.turnFrontScreenFlashOn();
    }

    @Override
    public void onCaptureStarted() {
        if( MyDebug.LOG )
            Log.d(TAG, "onCaptureStarted");
        n_capture_images = 0;
        n_capture_images_raw = 0;
    }

    @Override
    public void onPictureCompleted() {
        if( MyDebug.LOG )
            Log.d(TAG, "onPictureCompleted");

        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.NoiseReduction ) {
            boolean image_capture_intent = isImageCaptureIntent();
            boolean do_in_background = saveInBackground(image_capture_intent);
            imageSaver.finishImageBatch(do_in_background);
        }
        else if( photo_mode == PhotoMode.Panorama && gyroSensor.isRecording() ) {
            if( panorama_pic_accepted ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "set next panorama point");
                this.setNextPanoramaPoint(false);
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "panorama pic wasn't accepted");
                this.setNextPanoramaPoint(true);
            }
        }
        else if( photo_mode == PhotoMode.FocusBracketing ) {
            if( MyDebug.LOG )
                Log.d(TAG, "focus bracketing completed");
            if( getShutterSoundPref() ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "play completion sound");
                MediaPlayer player = MediaPlayer.create(getContext(), Settings.System.DEFAULT_NOTIFICATION_URI);
                if( player != null ) {
                    player.start();
                }
            }
        }
        drawPreview.cameraInOperation(false);
    }

    @Override
    public void cameraClosed() {
        if( MyDebug.LOG )
            Log.d(TAG, "cameraClosed");
        this.stopPanorama(true);
        drawPreview.clearContinuousFocusMove();
    }

    void updateThumbnail(Bitmap thumbnail, boolean is_video) {
        if( MyDebug.LOG )
            Log.d(TAG, "updateThumbnail");
//        main_activity.updateGalleryIcon(thumbnail);
        drawPreview.updateThumbnail(thumbnail, is_video, true);
        if( !is_video && this.getPausePreviewPref() ) {
            drawPreview.showLastImage();
        }
    }

    @Override
    public void timerBeep(long remaining_time) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "timerBeep()");
            Log.d(TAG, "remaining_time: " + remaining_time);
        }
        if( sharedPreferences.getBoolean(PreferenceKeys.getTimerBeepPreferenceKey(), true) ) {
            if( MyDebug.LOG )
                Log.d(TAG, "play beep!");
            boolean is_last = remaining_time <= 1000;
            main_activity.getSoundPoolManager().playSound(is_last ? R.raw.mybeep_hi : R.raw.mybeep);
        }
        if( sharedPreferences.getBoolean(PreferenceKeys.getTimerSpeakPreferenceKey(), false) ) {
            if( MyDebug.LOG )
                Log.d(TAG, "speak countdown!");
            int remaining_time_s = (int)(remaining_time/1000);
            if( remaining_time_s <= 60 )
                main_activity.speak("" + remaining_time_s);
        }
    }

    @Override
    public void multitouchZoom(int new_zoom) {
    }


    @Override
    public void setCameraIdPref(int cameraId) {
        this.has_set_cameraId = true;
        this.cameraId = cameraId;
    }

    @Override
    public void setFlashPref(String flash_value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getFlashPreferenceKey(cameraId), flash_value);
        editor.apply();
    }

    @Override
    public void setFocusPref(String focus_value, boolean is_video) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getFocusPreferenceKey(cameraId, is_video), focus_value);
        editor.apply();
    }


    @Override
    public void setSceneModePref(String scene_mode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.SceneModePreferenceKey, scene_mode);
        editor.apply();
    }

    @Override
    public void clearSceneModePref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.SceneModePreferenceKey);
        editor.apply();
    }

    @Override
    public void setISOPref(String iso) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.ISOPreferenceKey, iso);
        editor.apply();
    }

    @Override
    public void clearISOPref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.ISOPreferenceKey);
        editor.apply();
    }

    @Override
    public void setExposureCompensationPref(int exposure) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.ExposurePreferenceKey, "" + exposure);
        editor.apply();
    }

    @Override
    public void clearExposureCompensationPref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.ExposurePreferenceKey);
        editor.apply();
    }

    @Override
    public void setCameraResolutionPref(int width, int height) {
        if( getPhotoMode() == PhotoMode.Panorama ) {
            // in Panorama mode we'll have set a different resolution to the user setting, so don't want that to then be saved!
            return;
        }
        String resolution_value = width + " " + height;
        if( MyDebug.LOG ) {
            Log.d(TAG, "save new resolution_value: " + resolution_value);
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceKeys.getResolutionPreferenceKey(cameraId), resolution_value);
        editor.apply();
    }

    @Override
    public void setZoomPref(int zoom) {
        if( MyDebug.LOG )
            Log.d(TAG, "setZoomPref: " + zoom);
        this.zoom_factor = zoom;
    }

    @Override
    public void requestCameraPermission() {
        if( MyDebug.LOG )
            Log.d(TAG, "requestCameraPermission");
        main_activity.getPermissionHandler().requestCameraPermission();
    }

    @Override
    public boolean needsStoragePermission() {
        if( MyDebug.LOG )
            Log.d(TAG, "needsStoragePermission");
        return true;
    }

    @Override
    public void requestStoragePermission() {
        if( MyDebug.LOG )
            Log.d(TAG, "requestStoragePermission");
        main_activity.getPermissionHandler().requestStoragePermission();
    }

    @Override
    public void setExposureTimePref(long exposure_time) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PreferenceKeys.ExposureTimePreferenceKey, exposure_time);
        editor.apply();
    }

    @Override
    public void clearExposureTimePref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PreferenceKeys.ExposureTimePreferenceKey);
        editor.apply();
    }

    @Override
    public void setFocusDistancePref(float focus_distance, boolean is_target_distance) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(is_target_distance ? PreferenceKeys.FocusBracketingTargetDistancePreferenceKey : PreferenceKeys.FocusDistancePreferenceKey, focus_distance);
        editor.apply();
    }

    private int getStampFontColor() {
        String color = sharedPreferences.getString(PreferenceKeys.StampFontColorPreferenceKey, "#ffffff");
        return Color.parseColor(color);
    }

    public void reset() {
        if( MyDebug.LOG )
            Log.d(TAG, "reset");
        this.zoom_factor = 0;
    }

    public void reset(boolean switched_camera) {
        if( MyDebug.LOG )
            Log.d(TAG, "reset");
        if( switched_camera ) {
            // aperture is reset when switching camera, but not when application is paused or switching between photo/video etc
            this.aperture = aperture_default;
        }
        this.zoom_factor = 0;
    }

    @Override
    public void onDrawPreview(Canvas canvas) {
        if( !main_activity.isCameraInBackground() ) {
            drawPreview.onDrawPreview(canvas);
        }
    }

    public enum Alignment {
        ALIGNMENT_TOP,
        ALIGNMENT_CENTRE,
        ALIGNMENT_BOTTOM
    }

    public enum Shadow {
        SHADOW_NONE,
        SHADOW_OUTLINE,
        SHADOW_BACKGROUND
    }

    public int drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, Alignment alignment_y) {
        return drawTextWithBackground(canvas, paint, text, foreground, background, location_x, location_y, alignment_y, null, Shadow.SHADOW_OUTLINE);
    }

    public int drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, Alignment alignment_y, String ybounds_text, Shadow shadow) {
        return drawTextWithBackground(canvas, paint, text, foreground, background, location_x, location_y, alignment_y, null, shadow, null);
    }

    public int drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y, Alignment alignment_y, String ybounds_text, Shadow shadow, Rect bounds) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        paint.setAlpha(64);
        if( bounds != null ) {
            text_bounds.set(bounds);
        }
        else {
            int alt_height = 0;
            if( ybounds_text != null ) {
                paint.getTextBounds(ybounds_text, 0, ybounds_text.length(), text_bounds);
                alt_height = text_bounds.bottom - text_bounds.top;
            }
            paint.getTextBounds(text, 0, text.length(), text_bounds);
            if( ybounds_text != null ) {
                text_bounds.bottom = text_bounds.top + alt_height;
            }
        }
        final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels
        if( paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER ) {
            float width = paint.measureText(text); // n.b., need to use measureText rather than getTextBounds here
            if( paint.getTextAlign() == Paint.Align.CENTER )
                width /= 2.0f;
            text_bounds.left -= width;
            text_bounds.right -= width;
        }
        text_bounds.left += location_x - padding;
        text_bounds.right += location_x + padding;
        int top_y_diff = - text_bounds.top + padding - 1;
        if( alignment_y == Alignment.ALIGNMENT_TOP ) {
            int height = text_bounds.bottom - text_bounds.top + 2*padding;
            text_bounds.top = location_y - 1;
            text_bounds.bottom = text_bounds.top + height;
            location_y += top_y_diff;
        }
        else if( alignment_y == Alignment.ALIGNMENT_CENTRE ) {
            int height = text_bounds.bottom - text_bounds.top + 2*padding;
            //int y_diff = - text_bounds.top + padding - 1;
            text_bounds.top = (int)(0.5 * ( (location_y - 1) + (text_bounds.top + location_y - padding) )); // average of ALIGNMENT_TOP and ALIGNMENT_BOTTOM
            text_bounds.bottom = text_bounds.top + height;
            location_y += (int)(0.5*top_y_diff); // average of ALIGNMENT_TOP and ALIGNMENT_BOTTOM
        }
        else {
            text_bounds.top += location_y - padding;
            text_bounds.bottom += location_y + padding;
        }
        if( shadow == Shadow.SHADOW_BACKGROUND ) {
            paint.setColor(background);
            paint.setAlpha(64);
            canvas.drawRect(text_bounds, paint);
            paint.setAlpha(255);
        }
        paint.setColor(foreground);
        canvas.drawText(text, location_x, location_y, paint);
        if( shadow == Shadow.SHADOW_OUTLINE ) {
            paint.setColor(background);
            paint.setStyle(Paint.Style.STROKE);
            float current_stroke_width = paint.getStrokeWidth();
            paint.setStrokeWidth(1);
            canvas.drawText(text, location_x, location_y, paint);
            paint.setStyle(Paint.Style.FILL); // set back to default
            paint.setStrokeWidth(current_stroke_width); // reset
        }
        return text_bounds.bottom - text_bounds.top;
    }

    private boolean saveInBackground(boolean image_capture_intent) {
        boolean do_in_background = true;
        if( image_capture_intent )
            do_in_background = false;
        else if( getPausePreviewPref() )
            do_in_background = false;
        return do_in_background;
    }

    public boolean isImageCaptureIntent() {
        boolean image_capture_intent = false;
        String action = main_activity.getIntent().getAction();
        if( MediaStore.ACTION_IMAGE_CAPTURE.equals(action) || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action) ) {
            if( MyDebug.LOG )
                Log.d(TAG, "from image capture intent");
            image_capture_intent = true;
        }
        return image_capture_intent;
    }

    private boolean forceSuffix(PhotoMode photo_mode) {
        // focus bracketing and fast burst shots come is as separate requests, so we need to make sure we get the filename suffixes right
        return photo_mode == PhotoMode.FocusBracketing || photo_mode == PhotoMode.FastBurst ||
                (
                        main_activity.getPreview().getCameraController() != null &&
                                main_activity.getPreview().getCameraController().isCapturingBurst()
                );
    }

    private boolean saveImage(boolean save_expo, List<byte []> images, Date current_date) {
        if( MyDebug.LOG )
            Log.d(TAG, "saveImage");

        System.gc();

        boolean image_capture_intent = isImageCaptureIntent();
        Uri image_capture_intent_uri = null;
        if( image_capture_intent ) {
            if( MyDebug.LOG )
                Log.d(TAG, "from image capture intent");
            Bundle myExtras = main_activity.getIntent().getExtras();
            if( myExtras != null ) {
                image_capture_intent_uri = myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
                if( MyDebug.LOG )
                    Log.d(TAG, "save to: " + image_capture_intent_uri);
            }
        }

        boolean using_camera2 = main_activity.getPreview().usingCamera2API();
        ImageSaver.Request.ImageFormat image_format = getImageFormatPref();
        int image_quality = getSaveImageQualityPref();
        if( MyDebug.LOG )
            Log.d(TAG, "image_quality: " + image_quality);
        boolean do_auto_stabilise = getAutoStabilisePref() && main_activity.getPreview().hasLevelAngleStable();
        double level_angle = do_auto_stabilise ? main_activity.getPreview().getLevelAngle() : 0.0;
        if( do_auto_stabilise && main_activity.test_have_angle )
            level_angle = main_activity.test_angle;
        if( do_auto_stabilise && main_activity.test_low_memory )
            level_angle = 45.0;
        // I have received crashes where camera_controller was null - could perhaps happen if this thread was running just as the camera is closing?
        boolean is_front_facing = main_activity.getPreview().getCameraController() != null && main_activity.getPreview().getCameraController().isFrontFacing();
        boolean mirror = is_front_facing && sharedPreferences.getString(PreferenceKeys.FrontCameraMirrorKey, "preference_front_camera_mirror_no").equals("preference_front_camera_mirror_photo");
        String preference_stamp = this.getStampPref();
        String preference_textstamp = this.getTextStampPref();
        int font_size = getTextStampFontSizePref();
        int color = getStampFontColor();
        String pref_style = sharedPreferences.getString(PreferenceKeys.StampStyleKey, "preference_stamp_style_shadowed");
        String preference_stamp_dateformat = this.getStampDateFormatPref();
        String preference_stamp_timeformat = this.getStampTimeFormatPref();
        String preference_stamp_gpsformat = this.getStampGPSFormatPref();
        String preference_stamp_geo_address = this.getStampGeoAddressPref();
        String preference_units_distance = this.getUnitsDistancePref();
        boolean panorama_crop = sharedPreferences.getString(PreferenceKeys.PanoramaCropPreferenceKey, "preference_panorama_crop_on").equals("preference_panorama_crop_on");
        boolean store_location = getGeotaggingPref() && getLocation() != null;
        Location location = store_location ? getLocation() : null;
        boolean store_geo_direction = main_activity.getPreview().hasGeoDirection() && getGeodirectionPref();
        double geo_direction = store_geo_direction ? main_activity.getPreview().getGeoDirection() : 0.0;
        String custom_tag_artist = sharedPreferences.getString(PreferenceKeys.ExifArtistPreferenceKey, "");
        String custom_tag_copyright = sharedPreferences.getString(PreferenceKeys.ExifCopyrightPreferenceKey, "");
        String preference_hdr_contrast_enhancement = sharedPreferences.getString(PreferenceKeys.HDRContrastEnhancementPreferenceKey, "preference_hdr_contrast_enhancement_smart");

        int iso = 800; // default value if we can't get ISO
        long exposure_time = 1000000000L/30; // default value if we can't get shutter speed
        float zoom_factor = 1.0f;
        if( main_activity.getPreview().getCameraController() != null ) {
            if( main_activity.getPreview().getCameraController().captureResultHasIso() ) {
                iso = main_activity.getPreview().getCameraController().captureResultIso();
                if( MyDebug.LOG )
                    Log.d(TAG, "iso: " + iso);
            }
            if( main_activity.getPreview().getCameraController().captureResultHasExposureTime() ) {
                exposure_time = main_activity.getPreview().getCameraController().captureResultExposureTime();
                if( MyDebug.LOG )
                    Log.d(TAG, "exposure_time: " + exposure_time);
            }

            zoom_factor = main_activity.getPreview().getZoomRatio();
        }

        boolean has_thumbnail_animation = getThumbnailAnimationPref();

        boolean do_in_background = saveInBackground(image_capture_intent);

        String ghost_image_pref = sharedPreferences.getString(PreferenceKeys.GhostImagePreferenceKey, "preference_ghost_image_off");

        int sample_factor = 1;
        if( !this.getPausePreviewPref() && !ghost_image_pref.equals("preference_ghost_image_last") ) {
            sample_factor *= 4;
            if( !has_thumbnail_animation ) {
                // can use even lower resolution if we don't have the thumbnail animation
                sample_factor *= 4;
            }
        }
        if( MyDebug.LOG )
            Log.d(TAG, "sample_factor: " + sample_factor);

        boolean success;
        PhotoMode photo_mode = getPhotoMode();
        if( !main_activity.is_test && photo_mode == PhotoMode.Panorama && gyroSensor.isRecording() && gyroSensor.hasTarget() && !gyroSensor.isTargetAchieved() ) {
            if( MyDebug.LOG )
                Log.d(TAG, "ignore panorama image as target no longer achieved!");
            // n.b., gyroSensor.hasTarget() will be false if this is the first picture in the panorama series
            panorama_pic_accepted = false;
            success = true; // still treat as success
        }
        else if( photo_mode == PhotoMode.NoiseReduction || photo_mode == PhotoMode.Panorama ) {
            boolean first_image = false;
            if( photo_mode == PhotoMode.Panorama ) {
                panorama_pic_accepted = true;
                first_image = n_panorama_pics == 0;
            }
            else
                first_image = n_capture_images == 1;
            if( first_image ) {
                ImageSaver.Request.SaveBase save_base = ImageSaver.Request.SaveBase.SAVEBASE_NONE;
                if( photo_mode == PhotoMode.NoiseReduction ) {
                    String save_base_preference = sharedPreferences.getString(PreferenceKeys.NRSaveExpoPreferenceKey, "preference_nr_save_no");
                    switch( save_base_preference ) {
                        case "preference_nr_save_single":
                            save_base = ImageSaver.Request.SaveBase.SAVEBASE_FIRST;
                            break;
                        case "preference_nr_save_all":
                            save_base = ImageSaver.Request.SaveBase.SAVEBASE_ALL;
                            break;
                    }
                }
                else if( photo_mode == PhotoMode.Panorama ) {
                    String save_base_preference = sharedPreferences.getString(PreferenceKeys.PanoramaSaveExpoPreferenceKey, "preference_panorama_save_no");
                    switch( save_base_preference ) {
                        case "preference_panorama_save_all":
                            save_base = ImageSaver.Request.SaveBase.SAVEBASE_ALL;
                            break;
                        case "preference_panorama_save_all_plus_debug":
                            save_base = ImageSaver.Request.SaveBase.SAVEBASE_ALL_PLUS_DEBUG;
                            break;
                    }
                }

                imageSaver.startImageBatch(true,
                        photo_mode == PhotoMode.NoiseReduction ? ImageSaver.Request.ProcessType.AVERAGE : ImageSaver.Request.ProcessType.PANORAMA,
                        save_base,
                        image_capture_intent, image_capture_intent_uri,
                        using_camera2,
                        image_format, image_quality,
                        do_auto_stabilise, level_angle, photo_mode == PhotoMode.Panorama,
                        is_front_facing,
                        mirror,
                        current_date,
                        iso,
                        exposure_time,
                        zoom_factor,
                        preference_stamp, preference_textstamp, font_size, color, pref_style, preference_stamp_dateformat, preference_stamp_timeformat, preference_stamp_gpsformat, preference_stamp_geo_address, preference_units_distance,
                        panorama_crop,
                        store_location, location, store_geo_direction, geo_direction,
                        custom_tag_artist, custom_tag_copyright,
                        sample_factor);

                if( photo_mode == PhotoMode.Panorama ) {
                    imageSaver.getImageBatchRequest().camera_view_angle_x = main_activity.getPreview().getViewAngleX(false);
                    imageSaver.getImageBatchRequest().camera_view_angle_y = main_activity.getPreview().getViewAngleY(false);
                }
            }

            float [] gyro_rotation_matrix = null;
            if( photo_mode == PhotoMode.Panorama ) {
                gyro_rotation_matrix = new float[9];
                this.gyroSensor.getRotationMatrix(gyro_rotation_matrix);
            }

            imageSaver.addImageBatch(images.get(0), gyro_rotation_matrix);
            success = true;
        }
        else {
            boolean is_hdr = photo_mode == PhotoMode.DRO || photo_mode == PhotoMode.HDR;
            boolean force_suffix = forceSuffix(photo_mode);
            success = imageSaver.saveImageJpeg(do_in_background, is_hdr,
                    force_suffix,
                    force_suffix ? (n_capture_images-1) : 0,
                    save_expo, images,
                    image_capture_intent, image_capture_intent_uri,
                    using_camera2,
                    image_format, image_quality,
                    do_auto_stabilise, level_angle,
                    is_front_facing,
                    mirror,
                    current_date,
                    preference_hdr_contrast_enhancement,
                    iso,
                    exposure_time,
                    zoom_factor,
                    preference_stamp, preference_textstamp, font_size, color, pref_style, preference_stamp_dateformat, preference_stamp_timeformat, preference_stamp_gpsformat, preference_stamp_geo_address, preference_units_distance,
                    false, // panorama doesn't use this codepath
                    store_location, location, store_geo_direction, geo_direction,
                    custom_tag_artist, custom_tag_copyright,
                    sample_factor);
        }

        if( MyDebug.LOG )
            Log.d(TAG, "saveImage complete, success: " + success);

        return success;
    }

    @Override
    public boolean onPictureTaken(byte [] data, Date current_date) {
        if( MyDebug.LOG )
            Log.d(TAG, "onPictureTaken");

        n_capture_images++;
        if( MyDebug.LOG )
            Log.d(TAG, "n_capture_images is now " + n_capture_images);

        List<byte []> images = new ArrayList<>();
        images.add(data);

        boolean success = saveImage(false, images, current_date);

        if( MyDebug.LOG )
            Log.d(TAG, "onPictureTaken complete, success: " + success);

        return success;
    }

    @Override
    public boolean onBurstPictureTaken(List<byte []> images, Date current_date) {
        if( MyDebug.LOG )
            Log.d(TAG, "onBurstPictureTaken: received " + images.size() + " images");

        boolean success;
        PhotoMode photo_mode = getPhotoMode();
        if( photo_mode == PhotoMode.HDR ) {
            if( MyDebug.LOG )
                Log.d(TAG, "HDR mode");
            boolean save_expo = sharedPreferences.getBoolean(PreferenceKeys.HDRSaveExpoPreferenceKey, false);
            if( MyDebug.LOG )
                Log.d(TAG, "save_expo: " + save_expo);

            success = saveImage(save_expo, images, current_date);
        }
        else {
            if( MyDebug.LOG ) {
                Log.d(TAG, "exposure/focus bracketing mode mode");
                if( photo_mode != PhotoMode.ExpoBracketing && photo_mode != PhotoMode.FocusBracketing )
                    Log.e(TAG, "onBurstPictureTaken called with unexpected photo mode?!: " + photo_mode);
            }

            success = saveImage(true, images, current_date);
        }
        return success;
    }

    @Override
    public boolean onRawPictureTaken(RawImage raw_image, Date current_date) {
        if( MyDebug.LOG )
            Log.d(TAG, "onRawPictureTaken");
        System.gc();

        n_capture_images_raw++;
        if( MyDebug.LOG )
            Log.d(TAG, "n_capture_images_raw is now " + n_capture_images_raw);

        boolean do_in_background = saveInBackground(false);

        PhotoMode photo_mode = getPhotoMode();
        boolean force_suffix = forceSuffix(photo_mode);
        // N.B., n_capture_images_raw will be 1 for first image, not 0, so subtract 1 so we start off from _0.
        // (It wouldn't be a huge problem if we did start from _1, but it would be inconsistent with the naming
        // of images where images.size() > 1 (e.g., expo bracketing mode) where we also start from _0.)
        int suffix_offset = force_suffix ? (n_capture_images_raw-1) : 0;
        boolean success = imageSaver.saveImageRaw(do_in_background, force_suffix, suffix_offset, raw_image, current_date);

        if( MyDebug.LOG )
            Log.d(TAG, "onRawPictureTaken complete");
        return success;
    }

    @Override
    public boolean onRawBurstPictureTaken(List<RawImage> raw_images, Date current_date) {
        if( MyDebug.LOG )
            Log.d(TAG, "onRawBurstPictureTaken");
        System.gc();

        boolean do_in_background = saveInBackground(false);

        // currently we don't ever do post processing with RAW burst images, so just save them all
        boolean success = true;
        for(int i=0;i<raw_images.size() && success;i++) {
            success = imageSaver.saveImageRaw(do_in_background, true, i, raw_images.get(i), current_date);
        }

        if( MyDebug.LOG )
            Log.d(TAG, "onRawBurstPictureTaken complete");
        return success;
    }

    void addLastImage(File file, boolean share) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "addLastImage: " + file);
            Log.d(TAG, "share?: " + share);
        }
        LastImage last_image = new LastImage(file.getAbsolutePath(), share);
        last_images.add(last_image);
    }

    void addLastImageSAF(Uri uri, boolean share) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "addLastImageSAF: " + uri);
            Log.d(TAG, "share?: " + share);
        }
        LastImage last_image = new LastImage(uri, share);
        last_images.add(last_image);
    }

    public void clearLastImages() {
        if( MyDebug.LOG )
            Log.d(TAG, "clearLastImages");
        last_images.clear();
        drawPreview.clearLastImage();
    }

    void scannedFile(File file, Uri uri) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "scannedFile");
            Log.d(TAG, "file: " + file);
            Log.d(TAG, "uri: " + uri);
        }
        // see note under LastImage constructor for why we need to update the Uris
        for(int i=0;i<last_images.size();i++) {
            LastImage last_image = last_images.get(i);
            if( MyDebug.LOG )
                Log.d(TAG, "compare to last_image: " + last_image.name);
            if( last_image.uri == null && last_image.name != null && last_image.name.equals(file.getAbsolutePath()) ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "updated last_image : " + i);
                last_image.uri = uri;
            }
        }
    }

}

