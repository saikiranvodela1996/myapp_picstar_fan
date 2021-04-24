package com.picstar.picstarapp.campkg.cameracontroller;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.graphics.Rect;
import android.location.Location;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;

/** CameraController is an abstract class that wraps up the access/control to
 *  the Android camera, so that the rest of the application doesn't have to
 *  deal directly with the Android camera API. It also allows us to support
 *  more than one camera API through the same API (this is used to support both
 *  the original camera API, and Android 5's Camera2 API).
 *  The class is fairly low level wrapper about the APIs - there is some
 *  additional logical/workarounds where such things are API-specific, but
 *  otherwise the calling application still controls the behaviour of the
 *  camera.
 */
public abstract class CameraController {
    private final int cameraId;
    public static final String SCENE_MODE_DEFAULT = "auto"; // chosen to match Camera.Parameters.SCENE_MODE_AUTO, but we also use compatible values for Camera2 API
    public static final String ANTIBANDING_DEFAULT = "auto"; // chosen to match Camera.Parameters.ANTIBANDING_AUTO, but we also use compatible values for Camera2 API
    public static final String NOISE_REDUCTION_MODE_DEFAULT = "default";
    public static final String ISO_DEFAULT = "auto";
    public static final long EXPOSURE_TIME_DEFAULT = 1000000000L/30; // note, responsibility of callers to check that this is within the valid min/max range
    public static final int N_IMAGES_NR_DARK = 8;
    public static final int N_IMAGES_NR_DARK_LOW_LIGHT = 15;
    volatile int count_camera_parameters_exception;
    public volatile int count_precapture_timeout;
    public volatile boolean test_wait_capture_result; // whether to test delayed capture result in Camera2 API
    public volatile boolean test_release_during_photo; // for Camera2 API, will force takePictureAfterPrecapture() to call release() on UI thread
    public volatile int test_capture_results; // for Camera2 API, how many capture requests completed with RequestTagType.CAPTURE
    public volatile int test_fake_flash_focus; // for Camera2 API, records torch turning on for fake flash during autofocus
    public volatile int test_fake_flash_precapture; // for Camera2 API, records torch turning on for fake flash during precapture
    public volatile int test_fake_flash_photo; // for Camera2 API, records torch turning on for fake flash for photo capture
    public volatile int test_af_state_null_focus; // for Camera2 API, records af_state being null even when we've requested autofocus
    public static String rotatedAngle = "-1";

    public static class CameraFeatures {
        public boolean is_zoom_supported;
        public int max_zoom;
        public List<Integer> zoom_ratios;
        public boolean supports_face_detection;
        public List<Size> picture_sizes;
        public List<Size> video_sizes;
        public List<Size> video_sizes_high_speed; // may be null if high speed not supported
        public List<Size> preview_sizes;
        public List<String> supported_flash_values;
        public List<String> supported_focus_values;
        public int max_num_focus_areas;
        public float minimum_focus_distance;
        public boolean is_exposure_lock_supported;
        public boolean is_white_balance_lock_supported;
        public boolean is_video_stabilization_supported;
        public boolean is_photo_video_recording_supported;
        public boolean supports_white_balance_temperature;
        public int min_temperature;
        public int max_temperature;
        public boolean supports_iso_range;
        public int min_iso;
        public int max_iso;
        public boolean supports_exposure_time;
        public long min_exposure_time;
        public long max_exposure_time;
        public int min_exposure;
        public int max_exposure;
        public float exposure_step;
        public boolean can_disable_shutter_sound;
        public int tonemap_max_curve_points;
        public boolean supports_tonemap_curve;
        public boolean supports_expo_bracketing; // whether setBurstTye(BURSTTYPE_EXPO) can be used
        public int max_expo_bracketing_n_images;
        public boolean supports_focus_bracketing; // whether setBurstTye(BURSTTYPE_FOCUS) can be used
        public boolean supports_burst; // whether setBurstTye(BURSTTYPE_NORMAL) can be used
        public boolean supports_raw;
        public float view_angle_x; // horizontal angle of view in degrees (when unzoomed)
        public float view_angle_y; // vertical angle of view in degrees (when unzoomed)

    }

    public enum Facing {
        FACING_BACK,
        FACING_FRONT,
        FACING_EXTERNAL,
        FACING_UNKNOWN // returned if the Camera API returned an error or an unknown type
    }

    // Android docs and FindBugs recommend that Comparators also be Serializable
    public static class RangeSorter implements Comparator<int[]>, Serializable {
        private static final long serialVersionUID = 5802214721073728212L;
        @Override
        public int compare(int[] o1, int[] o2) {
            if (o1[0] == o2[0]) return o1[1] - o2[1];
            return o1[0] - o2[0];
        }
    }

    public static class SizeSorter implements Comparator<Size>, Serializable {
        private static final long serialVersionUID = 5802214721073718212L;

        @Override
        public int compare(final Size a, final Size b) {
            return b.width * b.height - a.width * a.height;
        }
    }

    public static class Size {
        public final int width;
        public final int height;
        public boolean supports_burst; // for photo
        final List<int[]> fps_ranges; // for video
        public final boolean high_speed; // for video

        Size(int width, int height, List<int[]> fps_ranges, boolean high_speed) {
            this.width = width;
            this.height = height;
            this.supports_burst = true;
            this.fps_ranges = fps_ranges;
            this.high_speed = high_speed;
            Collections.sort(this.fps_ranges, new RangeSorter());
        }

        public Size(int width, int height) {
            this(width, height, new ArrayList<int[]>(), false);
        }

        @Override
        public boolean equals(Object o) {
            if( !(o instanceof Size) )
                return false;
            Size that = (Size)o;
            return this.width == that.width && this.height == that.height;
        }

        @Override
        public int hashCode() {
            return width*41 + height;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            for (int[] f : this.fps_ranges) {
                s.append(" [").append(f[0]).append("-").append(f[1]).append("]");
            }
            return this.width + "x" + this.height + " " + s + (this.high_speed ? "-hs" : "");
        }
    }

    public static class Area {
        final Rect rect;
        final int weight;

        public Area(Rect rect, int weight) {
            this.rect = rect;
            this.weight = weight;
        }
    }

    public interface FaceDetectionListener {
        void onFaceDetection(Face[] faces);
    }

    public interface PictureCallback {
        void onStarted(); // called immediately before we start capturing the picture
        void onCompleted(); // called after all relevant on*PictureTaken() callbacks have been called and returned
        void onPictureTaken(byte[] data);
        /** Only called if RAW is requested.
         *  Caller should call raw_image.close() when done with the image.
         */
        void onRawPictureTaken(RawImage raw_image);
        /** Only called if burst is requested.
         */
        void onBurstPictureTaken(List<byte[]> images);
        /** Only called if burst is requested.
         */
        void onRawBurstPictureTaken(List<RawImage> raw_images);
        boolean imageQueueWouldBlock(int n_raw, int n_jpegs);
        void onFrontScreenTurnOn();
    }

    /** Interface to define callback for autofocus completing. This callback may be called on the UI thread (CameraController1)
     *  or a background thread (CameraController2).
     */
    public interface AutoFocusCallback {
        void onAutoFocus(boolean success);
    }
    public interface ContinuousFocusMoveCallback {
        void onContinuousFocusMove(boolean start);
    }
    public interface ErrorCallback {
        void onError();
    }
    public static class Face {
        public final int score;
        public final Rect rect;

        Face(int score, Rect rect) {
            this.score = score;
            this.rect = rect;
        }
    }
    public static class SupportedValues {
        public final List<String> values;
        public final String selected_value;
        SupportedValues(List<String> values, String selected_value) {
            this.values = values;
            this.selected_value = selected_value;
        }
    }
    public abstract void release();
    public abstract void onError(); // triggers error mechanism - should only be called externally for testing purposes
    CameraController(int cameraId) {
        this.cameraId = cameraId;
    }
    public abstract String getAPI();
    public abstract CameraFeatures getCameraFeatures() throws CameraControllerException;
    public int getCameraId() {
        return cameraId;
    }
    public boolean shouldCoverPreview() {
        return false;
    }
    public abstract SupportedValues setSceneMode(String value);
    public abstract boolean setWhiteBalanceTemperature(int temperature);
    public abstract SupportedValues setISO(String value);
    public abstract void setManualISO(boolean manual_iso, int iso);
    public abstract boolean setISO(int iso);
    public abstract String getISOKey();
    public abstract int getISO();
    public abstract boolean setExposureTime(long exposure_time);
    public abstract Size getPictureSize();
    public abstract void setPictureSize(int width, int height);
    public abstract void setPreviewSize(int width, int height);

    public enum BurstType {
        BURSTTYPE_NONE, // no burst
        BURSTTYPE_EXPO, // enable expo bracketing mode
        BURSTTYPE_FOCUS, // enable focus bracketing mode;
        BURSTTYPE_NORMAL, // take a regular burst
        BURSTTYPE_CONTINUOUS // as BURSTTYPE_NORMAL, but bursts will fire continually until stopContinuousBurst() is called.
    }
    public abstract void setBurstType(BurstType new_burst_type);
    public abstract BurstType getBurstType();
    public abstract void setBurstNImages(int burst_requested_n_images);
    public abstract void setBurstForNoiseReduction(boolean burst_for_noise_reduction, boolean noise_reduction_low_light);
    public abstract boolean isContinuousBurstInProgress();
    public abstract void stopContinuousBurst();
    public abstract void stopFocusBracketingBurst();
    public abstract void setExpoBracketingNImages(int n_images);
    public abstract void setExpoBracketingStops(double stops);
    public abstract void setUseExpoFastBurst(boolean use_expo_fast_burst);
    public abstract boolean isBurstOrExpo();
    public abstract boolean isCapturingBurst();
    public abstract int getNBurstTaken();
    public abstract int getBurstTotal();
    public abstract void setOptimiseAEForDRO(boolean optimise_ae_for_dro);
    public abstract void setRaw(boolean want_raw, int max_raw_images);
    public void setUseCamera2FakeFlash(boolean use_fake_precapture) {}
    public abstract void setJpegQuality(int quality);
    public abstract int getZoom();
    public abstract void setZoom(int value);
    public abstract int getExposureCompensation();
    public abstract boolean setExposureCompensation(int new_exposure);
    public abstract void setPreviewFpsRange(int min, int max);
    public abstract void clearPreviewFpsRange();
    public abstract List<int []> getSupportedPreviewFpsRange(); // result depends on setting of setVideoHighSpeed()
    public abstract void setFocusValue(String focus_value);
    public abstract String getFocusValue();
    public abstract boolean setFocusDistance(float focus_distance);
    public abstract void setFocusBracketingNImages(int n_images);
    public abstract void setFocusBracketingAddInfinity(boolean focus_bracketing_add_infinity);
    public abstract void setFocusBracketingSourceDistance(float focus_bracketing_source_distance);
    public abstract void setFocusBracketingTargetDistance(float focus_bracketing_target_distance);
    public abstract void setFlashValue(String flash_value);
    public abstract String getFlashValue();
    public abstract void setRecordingHint(boolean hint);
    public abstract void setRotation(int rotation);
    public abstract void setLocationInfo(Location location);
    public abstract void removeLocationInfo();
    public abstract void enableShutterSound(boolean enabled);
    public abstract boolean setFocusAndMeteringArea(List<Area> areas);
    public abstract void clearFocusAndMetering();
    public abstract boolean supportsAutoFocus();
    public abstract boolean focusIsContinuous();
    public abstract void setPreviewDisplay(SurfaceHolder holder) throws CameraControllerException;
    public abstract void setPreviewTexture(TextureView texture) throws CameraControllerException;
    public abstract void startPreview() throws CameraControllerException;
    public abstract void stopPreview();
    public abstract boolean startFaceDetection();
    public abstract void autoFocus(final AutoFocusCallback cb, boolean capture_follows_autofocus_hint);
    public abstract void setCaptureFollowAutofocusHint(boolean capture_follows_autofocus_hint);
    public abstract void cancelAutoFocus();
    public abstract void setContinuousFocusMoveCallback(ContinuousFocusMoveCallback cb);
    public abstract void takePicture(final PictureCallback picture, final ErrorCallback error);
    public abstract void setDisplayOrientation(int degrees);
    public abstract int getDisplayOrientation();
    public abstract int getCameraOrientation();
    public abstract boolean isFrontFacing();
    public abstract void unlock();
    public abstract String getParametersString();
    public boolean captureResultHasIso() {
        return false;
    }
    public int captureResultIso() {
        return 0;
    }
    public boolean captureResultHasExposureTime() {
        return false;
    }
    public long captureResultExposureTime() {
        return 0;
    }
    SupportedValues checkModeIsSupported(List<String> values, String value, String default_value) {
        if( values != null && values.size() > 1 ) { // n.b., if there is only 1 supported value, we also return null, as no point offering the choice to the user (there are some devices, e.g., Samsung, that only have a scene mode of "auto")
            if( !values.contains(value) ) {
                if( values.contains(default_value) )
                    value = default_value;
                else
                    value = values.get(0);
            }
            return new SupportedValues(values, value);
        }
        return null;
    }

}

