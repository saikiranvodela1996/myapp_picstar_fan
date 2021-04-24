package com.picstar.picstarapp.campkg.preview;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.CamcorderProfile;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSInvalidStateException;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.MeasureSpec;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController1;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController2;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerException;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerManager;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerManager1;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerManager2;
import com.picstar.picstarapp.campkg.cameracontroller.RawImage;
import com.picstar.picstarapp.campkg.camui.MySurfaceView;
import com.picstar.picstarapp.campkg.camui.MyTextureView;
import com.picstar.picstarapp.campkg.others.ApplicationInterface;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.ScriptC_histogram_compute;
import com.picstar.picstarapp.campkg.others.TakePhoto;
import com.picstar.picstarapp.utils.PSR_Utils;

public class Preview implements SurfaceHolder.Callback, TextureView.SurfaceTextureListener {
    private static final String TAG = "Preview";
    private final boolean using_android_l;
    private final ApplicationInterface applicationInterface;
    private final CameraSurface cameraSurface;
    private CanvasView canvasView;
    private boolean set_preview_size;
    private int preview_w, preview_h;
    private boolean set_textureview_size;
    private int textureview_w, textureview_h;
    private RenderScript rs; // lazily created, so we don't take up resources if application isn't using renderscript
    private ScriptC_histogram_compute histogramScript; // lazily create for performance
    private boolean want_preview_bitmap; // whether application has requested we generate bitmap for the preview
    private Bitmap preview_bitmap;
    private long last_preview_bitmap_time_ms; // time the last preview_bitmap was updated
    private RefreshPreviewBitmapTask refreshPreviewBitmapTask;

    private boolean want_histogram; // whether to generate a histogram, requires want_preview_bitmap==true

    public enum HistogramType {
        HISTOGRAM_TYPE_RGB,
        HISTOGRAM_TYPE_LUMINANCE,
        HISTOGRAM_TYPE_VALUE,
        HISTOGRAM_TYPE_INTENSITY,
        HISTOGRAM_TYPE_LIGHTNESS
    }

    private HistogramType histogram_type = HistogramType.HISTOGRAM_TYPE_VALUE;
    private int[] histogram;
    private long last_histogram_time_ms; // time the last histogram was updated
    private boolean want_zebra_stripes; // whether to generate zebra stripes bitmap, requires want_preview_bitmap==true
    private int zebra_stripes_threshold; // pixels with max rgb value equal to or greater than this threshold are marked with zebra stripes
    private Bitmap zebra_stripes_bitmap_buffer;
    private Bitmap zebra_stripes_bitmap;
    private boolean want_focus_peaking; // whether to generate focus peaking bitmap, requires want_preview_bitmap==true
    private Bitmap focus_peaking_bitmap_buffer;
    private Bitmap focus_peaking_bitmap;

    private final Matrix camera_to_preview_matrix = new Matrix();
    private final Matrix preview_to_camera_matrix = new Matrix();
    private double preview_targetRatio;
    private boolean app_is_paused = true;
    private boolean has_surface;
    private boolean has_aspect_ratio;
    private double aspect_ratio;
    private final CameraControllerManager camera_controller_manager;
    private CameraController camera_controller;

    enum CameraOpenState {
        CAMERAOPENSTATE_CLOSED, // have yet to attempt to open the camera (either at all, or since the camera was closed)
        CAMERAOPENSTATE_OPENING, // the camera is currently being opened (on a background thread)
        CAMERAOPENSTATE_OPENED, // either the camera is open (if camera_controller!=null) or we failed to open the camera (if camera_controller==null)
        CAMERAOPENSTATE_CLOSING // the camera is currently being closed (on a background thread)
    }

    private CameraOpenState camera_open_state = CameraOpenState.CAMERAOPENSTATE_CLOSED;
    private AsyncTask<Void, Void, CameraController> open_camera_task; // background task used for opening camera
    private CloseCameraTask close_camera_task; // background task used for closing camera
    private boolean has_permissions = true; // whether we have permissions necessary to operate the camera (camera, storage); assume true until we've been denied one of them
    private static final int PHASE_NORMAL = 0;
    private static final int PHASE_TIMER = 1;
    private static final int PHASE_TAKING_PHOTO = 2;
    private static final int PHASE_PREVIEW_PAUSED = 3; // the paused state after taking a photo
    private volatile int phase = PHASE_NORMAL; // must be volatile for test project reading the state
    private final Timer takePictureTimer = new Timer();
    private TimerTask takePictureTimerTask;
    private final Timer beepTimer = new Timer();
    private TimerTask beepTimerTask;
    private long take_photo_time;
    private int remaining_repeat_photos;
    private boolean is_preview_started;
    private OrientationEventListener orientationEventListener;
    private int current_orientation; // orientation received by onOrientationChanged
    private int current_rotation; // orientation relative to camera's orientation (used for parameters.setRotation())
    private boolean has_level_angle;
    private double natural_level_angle; // "level" angle of device, before applying any calibration and without accounting for screen orientation
    private double level_angle; // "level" angle of device, including calibration
    private double orig_level_angle; // "level" angle of device, including calibration, but without accounting for screen orientation
    private boolean has_pitch_angle;
    private double pitch_angle;
    private boolean camera_controller_supports_zoom;
    private boolean has_zoom;
    private int max_zoom_factor;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;
    private List<Integer> zoom_ratios;
    private float minimum_focus_distance;
    private boolean touch_was_multitouch;
    private float touch_orig_x;
    private float touch_orig_y;
    private List<String> supported_flash_values; // our "values" format
    private int current_flash_index = -1; // this is an index into the supported_flash_values array, or -1 if no flash modes available
    private List<String> supported_focus_values; // our "values" format
    private int current_focus_index = -1; // this is an index into the supported_focus_values array, or -1 if no focus modes available
    private int max_num_focus_areas;
    private boolean continuous_focus_move_is_started;
    private boolean is_exposure_lock_supported;
    private boolean is_exposure_locked;
    private boolean is_white_balance_lock_supported;
    private boolean is_white_balance_locked;

    private List<String> color_effects;
    private List<String> scene_modes;
    private List<String> white_balances;
    private List<String> antibanding;
    private List<String> edge_modes;
    private List<String> noise_reduction_modes; // n.b., this is for the Camera2 API setting, not for Open Camera's Noise Reduction photo mode
    private List<String> isos;
    private boolean supports_white_balance_temperature;
    private int min_temperature;
    private int max_temperature;
    private boolean supports_iso_range;
    private int min_iso;
    private int max_iso;
    private boolean supports_exposure_time;
    private long min_exposure_time;
    private long max_exposure_time;
    private List<String> exposures;
    private int min_exposure;
    private int max_exposure;
    private float exposure_step;
    private boolean supports_expo_bracketing;
    private int max_expo_bracketing_n_images;
    private boolean supports_focus_bracketing;
    private boolean supports_burst;
    private boolean supports_raw;
    private float view_angle_x;
    private float view_angle_y;

    private List<CameraController.Size> supported_preview_sizes;

    private List<CameraController.Size> sizes;
    private int current_size_index = -1; // this is an index into the sizes array, or -1 if sizes not yet set

    private boolean video_high_speed; // whether the current video mode requires high speed frame rate (note this may still be true even if is_video==false, so potentially we could switch photo/video modes without setting up the flag)
    private int ui_rotation;
    private boolean supports_face_detection;
    private boolean using_face_detection;
    private CameraController.Face[] faces_detected;
    private final AccessibilityManager accessibility_manager;
    private boolean supports_video_stabilization;
    private boolean supports_photo_video_recording;
    private boolean can_disable_shutter_sound;
    private int tonemap_max_curve_points;
    private boolean supports_tonemap_curve;
    private boolean has_focus_area;
    private int focus_screen_x;
    private int focus_screen_y;
    private long focus_complete_time = -1;
    private long focus_started_time = -1;
    private int focus_success = FOCUS_DONE;
    private static final int FOCUS_WAITING = 0;
    private static final int FOCUS_SUCCESS = 1;
    private static final int FOCUS_FAILED = 2;
    private static final int FOCUS_DONE = 3;
    private String set_flash_value_after_autofocus = "";
    private boolean take_photo_after_autofocus; // set to take a photo when the in-progress autofocus has completed; if setting, remember to call camera_controller.setCaptureFollowAutofocusHint()
    private boolean successfully_focused;
    private long successfully_focused_time = -1;
    private static final float sensor_alpha = 0.8f; // for filter
    private boolean has_gravity;
    private final float[] gravity = new float[3];
    private boolean has_geomagnetic;
    private final float[] geomagnetic = new float[3];
    private final float[] deviceRotation = new float[9];
    private final float[] cameraRotation = new float[9];
    private final float[] deviceInclination = new float[9];
    private boolean has_geo_direction;
    private final float[] geo_direction = new float[3];
    private final float[] new_geo_direction = new float[3];
    private final DecimalFormat decimal_format_1dp = new DecimalFormat("#.#");
    private final DecimalFormat decimal_format_2dp = new DecimalFormat("#.##");
    private final Handler reset_continuous_focus_handler = new Handler();
    private Runnable reset_continuous_focus_runnable;
    private boolean autofocus_in_continuous_mode;

    // for testing; must be volatile for test project reading the state
    private boolean is_test; // whether called from OpenCamera.test testing
    public volatile int count_cameraStartPreview;
    public volatile int count_cameraAutoFocus;
    public volatile int count_cameraTakePicture;
    public volatile int count_cameraContinuousFocusMoving;
    public volatile boolean test_fail_open_camera;
    public volatile boolean test_ticker_called; // set from MySurfaceView or CanvasView

    public Preview(ApplicationInterface applicationInterface, ViewGroup parent) {
        if (MyDebug.LOG) {
            Log.d(TAG, "new Preview");
        }

        this.applicationInterface = applicationInterface;

        Activity activity = (Activity) this.getContext();
        if (activity.getIntent() != null && activity.getIntent().getExtras() != null) {
            // whether called from testing
            is_test = activity.getIntent().getExtras().getBoolean("test_project");
            if (MyDebug.LOG)
                Log.d(TAG, "is_test: " + is_test);
        }

        this.using_android_l = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && applicationInterface.useCamera2();
        if (MyDebug.LOG) {
            Log.d(TAG, "using_android_l?: " + using_android_l);
        }

        boolean using_texture_view = false;
        if (using_android_l) {
            // use a TextureView for Android L - had bugs with SurfaceView not resizing properly on Nexus 7; and good to use a TextureView anyway
            // ideally we'd use a TextureView for older camera API too, but sticking with SurfaceView to avoid risk of breaking behaviour
            using_texture_view = true;
        }

        if (using_texture_view) {
            this.cameraSurface = new MyTextureView(getContext(), this);
            // a TextureView can't be used both as a camera preview, and used for drawing on, so we use a separate CanvasView
            this.canvasView = new CanvasView(getContext(), this);
            camera_controller_manager = new CameraControllerManager2(getContext());
        } else {
            this.cameraSurface = new MySurfaceView(getContext(), this);
            camera_controller_manager = new CameraControllerManager1();
        }
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener());
        gestureDetector.setOnDoubleTapListener(new DoubleTapListener());
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        accessibility_manager = (AccessibilityManager) activity.getSystemService(Activity.ACCESSIBILITY_SERVICE);

        parent.addView(cameraSurface.getView());
        if (canvasView != null) {
            parent.addView(canvasView);
        }
    }

    private Resources getResources() {
        return cameraSurface.getView().getResources();
    }

    public View getView() {
        return cameraSurface.getView();
    }

    private void calculateCameraToPreviewMatrix() {
        if (MyDebug.LOG)
            Log.d(TAG, "calculateCameraToPreviewMatrix");
        if (camera_controller == null)
            return;
        camera_to_preview_matrix.reset();
        if (!using_android_l) {
            boolean mirror = camera_controller.isFrontFacing();
            camera_to_preview_matrix.setScale(mirror ? -1 : 1, 1);
            int display_orientation = camera_controller.getDisplayOrientation();
            if (MyDebug.LOG) {
                Log.d(TAG, "orientation of display relative to camera orientaton: " + display_orientation);
            }
            camera_to_preview_matrix.postRotate(display_orientation);
        } else {
            boolean mirror = camera_controller.isFrontFacing();
            camera_to_preview_matrix.setScale(1, mirror ? -1 : 1);
            int degrees = getDisplayRotationDegrees();
            int result = (camera_controller.getCameraOrientation() - degrees + 360) % 360;
            if (MyDebug.LOG) {
                Log.d(TAG, "orientation of display relative to natural orientaton: " + degrees);
                Log.d(TAG, "orientation of display relative to camera orientaton: " + result);
            }
            camera_to_preview_matrix.postRotate(result);
        }
        camera_to_preview_matrix.postScale(cameraSurface.getView().getWidth() / 2000f, cameraSurface.getView().getHeight() / 2000f);
        camera_to_preview_matrix.postTranslate(cameraSurface.getView().getWidth() / 2f, cameraSurface.getView().getHeight() / 2f);
    }

    private void calculatePreviewToCameraMatrix() {
        if (camera_controller == null)
            return;
        calculateCameraToPreviewMatrix();
        if (!camera_to_preview_matrix.invert(preview_to_camera_matrix)) {
            if (MyDebug.LOG)
                Log.d(TAG, "calculatePreviewToCameraMatrix failed to invert matrix!?");
        }
    }

    private ArrayList<CameraController.Area> getAreas(float x, float y) {
        float[] coords = {x, y};
        calculatePreviewToCameraMatrix();
        preview_to_camera_matrix.mapPoints(coords);
        float focus_x = coords[0];
        float focus_y = coords[1];

        int focus_size = 50;
        if (MyDebug.LOG) {
            Log.d(TAG, "x, y: " + x + ", " + y);
            Log.d(TAG, "focus x, y: " + focus_x + ", " + focus_y);
        }
        Rect rect = new Rect();
        rect.left = (int) focus_x - focus_size;
        rect.right = (int) focus_x + focus_size;
        rect.top = (int) focus_y - focus_size;
        rect.bottom = (int) focus_y + focus_size;
        if (rect.left < -1000) {
            rect.left = -1000;
            rect.right = rect.left + 2 * focus_size;
        } else if (rect.right > 1000) {
            rect.right = 1000;
            rect.left = rect.right - 2 * focus_size;
        }
        if (rect.top < -1000) {
            rect.top = -1000;
            rect.bottom = rect.top + 2 * focus_size;
        } else if (rect.bottom > 1000) {
            rect.bottom = 1000;
            rect.top = rect.bottom - 2 * focus_size;
        }

        ArrayList<CameraController.Area> areas = new ArrayList<>();
        areas.add(new CameraController.Area(rect, 1000));
        return areas;
    }

    public boolean touchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        scaleGestureDetector.onTouchEvent(event);
        if (camera_controller == null) {
            return true;
        }
        applicationInterface.touchEvent(event);
        if (event.getPointerCount() != 1) {
            touch_was_multitouch = true;
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) {
            if (event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1) {
                touch_was_multitouch = false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touch_orig_x = event.getX();
                    touch_orig_y = event.getY();
                    if (MyDebug.LOG)
                        Log.d(TAG, "touch down at " + touch_orig_x + " , " + touch_orig_y);
                }
            }
            return true;
        }

        if (touch_was_multitouch) {
            return true;
        }
        if (this.isTakingPhotoOrOnTimer()) {
            return true;
        }

        {
            float x = event.getX();
            float y = event.getY();
            float diff_x = x - touch_orig_x;
            float diff_y = y - touch_orig_y;
            float dist2 = diff_x * diff_x + diff_y * diff_y;
            float scale = getResources().getDisplayMetrics().density;
            float tol = 31 * scale + 0.5f; // convert dps to pixels (about 0.5cm)
            if (MyDebug.LOG) {
                Log.d(TAG, "touched from " + touch_orig_x + " , " + touch_orig_y + " to " + x + " , " + y);
                Log.d(TAG, "dist: " + Math.sqrt(dist2));
                Log.d(TAG, "tol: " + tol);
            }
            if (dist2 > tol * tol) {
                if (MyDebug.LOG)
                    Log.d(TAG, "touch was a swipe");
                return true;
            }
        }
        startCameraPreview();
        cancelAutoFocus();

        if (camera_controller != null && !this.using_face_detection) {
            this.has_focus_area = false;
            ArrayList<CameraController.Area> areas = getAreas(event.getX(), event.getY());
            if (camera_controller.setFocusAndMeteringArea(areas)) {
                if (MyDebug.LOG)
                    Log.d(TAG, "set focus (and metering?) area");
                this.has_focus_area = true;
                this.focus_screen_x = (int) event.getX();
                this.focus_screen_y = (int) event.getY();
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "didn't set focus area in this mode, may have set metering");
                // don't set has_focus_area in this mode
            }
        }

        if (applicationInterface.getTouchCapturePref()) {
            if (MyDebug.LOG)
                Log.d(TAG, "touch to capture");
            // interpret as if user had clicked take photo/video button, except that we set the focus/metering areas
            this.takePicturePressed(false, false);
            return true;
        }

        tryAutoFocus(false, true);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (Preview.this.camera_controller != null && Preview.this.has_zoom) {
                Preview.this.scaleZoom(detector.getScaleFactor());
            }
            return true;
        }
    }

    public boolean onDoubleTap() {
        if (MyDebug.LOG)
            Log.d(TAG, "onDoubleTap()");
        if (applicationInterface.getDoubleTapCapturePref()) {
            if (MyDebug.LOG)
                Log.d(TAG, "double-tap to capture");
            // interpret as if user had clicked take photo/video button (don't need to set focus/metering, as this was done in touchEvent() for the first touch of the double-tap)
            takePicturePressed(false, false);

        }
        return true;
    }

    private class DoubleTapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (MyDebug.LOG)
                Log.d(TAG, "onDoubleTap()");
            return Preview.this.onDoubleTap();
        }
    }

    public void clearFocusAreas() {
        if (MyDebug.LOG)
            Log.d(TAG, "clearFocusAreas()");
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }
        // don't cancelAutoFocus() here, otherwise we get sluggish zoom behaviour on Camera2 API
        camera_controller.clearFocusAndMetering();
        has_focus_area = false;
        focus_success = FOCUS_DONE;
        successfully_focused = false;
    }

    public void getMeasureSpec(int[] spec, int widthSpec, int heightSpec) {
        if (MyDebug.LOG)
            Log.d(TAG, "getMeasureSpec");
        if (!this.hasAspectRatio()) {
            if (MyDebug.LOG)
                Log.d(TAG, "doesn't have aspect ratio");
            spec[0] = widthSpec;
            spec[1] = heightSpec;
            return;
        }
        double aspect_ratio = this.getAspectRatio();

        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        // Get the padding of the border background.
        int hPadding = cameraSurface.getView().getPaddingLeft() + cameraSurface.getView().getPaddingRight();
        int vPadding = cameraSurface.getView().getPaddingTop() + cameraSurface.getView().getPaddingBottom();

        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding;
        previewHeight -= vPadding;

        boolean widthLonger = previewWidth > previewHeight;
        int longSide = (widthLonger ? previewWidth : previewHeight);
        int shortSide = (widthLonger ? previewHeight : previewWidth);
        if (longSide > shortSide * aspect_ratio) {
            longSide = (int) ((double) shortSide * aspect_ratio);
        } else {
            shortSide = (int) ((double) longSide / aspect_ratio);
        }
        if (widthLonger) {
            previewWidth = longSide;
            previewHeight = shortSide;
        } else {
            previewWidth = shortSide;
            previewHeight = longSide;
        }

        // Add the padding of the border.
        previewWidth += hPadding;
        previewHeight += vPadding;

        spec[0] = MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY);
        spec[1] = MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY);
        if (MyDebug.LOG)
            Log.d(TAG, "return: " + spec[0] + " x " + spec[1]);
    }

    private void mySurfaceCreated() {
        if (MyDebug.LOG)
            Log.d(TAG, "mySurfaceCreated");
        this.has_surface = true;
        this.openCamera();
    }

    private void mySurfaceDestroyed() {
        if (MyDebug.LOG)
            Log.d(TAG, "mySurfaceDestroyed");
        this.has_surface = false;
        this.closeCamera(false, null);
    }

    private void mySurfaceChanged() {
        // surface size is now changed to match the aspect ratio of camera preview - so we shouldn't change the preview to match the surface size, so no need to restart preview here
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (MyDebug.LOG)
            Log.d(TAG, "surfaceCreated()");
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        mySurfaceCreated();
        cameraSurface.getView().setWillNotDraw(false); // see http://stackoverflow.com/questions/2687015/extended-surfaceviews-ondraw-method-never-called
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (MyDebug.LOG)
            Log.d(TAG, "surfaceDestroyed()");
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mySurfaceDestroyed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (MyDebug.LOG)
            Log.d(TAG, "surfaceChanged " + w + ", " + h);
        if (holder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        mySurfaceChanged();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture arg0, int width, int height) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSurfaceTextureAvailable()");
        this.set_textureview_size = true;
        this.textureview_w = width;
        this.textureview_h = height;
        mySurfaceCreated();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSurfaceTextureDestroyed()");
        this.set_textureview_size = false;
        this.textureview_w = 0;
        this.textureview_h = 0;
        mySurfaceDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int width, int height) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSurfaceTextureSizeChanged " + width + ", " + height);
        this.set_textureview_size = true;
        this.textureview_w = width;
        this.textureview_h = height;
        mySurfaceChanged();
        configureTransform();
        recreatePreviewBitmap();
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
        refreshPreviewBitmap();
    }

    private void configureTransform() {
        if (MyDebug.LOG)
            Log.d(TAG, "configureTransform");
        if (camera_controller == null || !this.set_preview_size || !this.set_textureview_size) {
            if (MyDebug.LOG)
                Log.d(TAG, "nothing to do");
            return;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "textureview size: " + textureview_w + ", " + textureview_h);
        int rotation = getDisplayRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, this.textureview_w, this.textureview_h);
        RectF bufferRect = new RectF(0, 0, this.preview_h, this.preview_w);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) textureview_h / preview_h,
                    (float) textureview_w / preview_w);
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        cameraSurface.setTransform(matrix);
    }

    private Context getContext() {
        return applicationInterface.getContext();
    }

    private interface CloseCameraCallback {
        void onClosed();
    }

    private class CloseCameraTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "CloseCameraTask";

        boolean reopen; // if set to true, reopen the camera once closed

        final CameraController camera_controller_local;
        final CloseCameraCallback closeCameraCallback;

        CloseCameraTask(CameraController camera_controller_local, CloseCameraCallback closeCameraCallback) {
            this.camera_controller_local = camera_controller_local;
            this.closeCameraCallback = closeCameraCallback;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            long debug_time = 0;
            if (MyDebug.LOG) {
                Log.d(TAG, "doInBackground, async task: " + this);
                debug_time = System.currentTimeMillis();
            }
            camera_controller_local.stopPreview();
            if (MyDebug.LOG) {
                Log.d(TAG, "time to stop preview: " + (System.currentTimeMillis() - debug_time));
            }
            camera_controller_local.release();
            if (MyDebug.LOG) {
                Log.d(TAG, "time to release camera controller: " + (System.currentTimeMillis() - debug_time));
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            if (MyDebug.LOG)
                Log.d(TAG, "onPostExecute, async task: " + this);
            camera_open_state = CameraOpenState.CAMERAOPENSTATE_CLOSED;
            close_camera_task = null; // just to be safe
            if (closeCameraCallback != null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "onPostExecute, calling closeCameraCallback.onClosed");
                closeCameraCallback.onClosed();
            }
            if (reopen) {
                if (MyDebug.LOG)
                    Log.d(TAG, "onPostExecute, reopen camera");
                openCamera();
            }
            if (MyDebug.LOG)
                Log.d(TAG, "onPostExecute done, async task: " + this);
        }
    }

    private void closeCamera(boolean async, final CloseCameraCallback closeCameraCallback) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "closeCamera()");
            Log.d(TAG, "async: " + async);
            debug_time = System.currentTimeMillis();
        }
        removePendingContinuousFocusReset();
        has_focus_area = false;
        focus_success = FOCUS_DONE;
        focus_started_time = -1;
        synchronized (this) {
            // synchronise for consistency (keep FindBugs happy)
            take_photo_after_autofocus = false;
            // no need to call camera_controller.setCaptureFollowAutofocusHint() as we're closing the camera
        }
        set_flash_value_after_autofocus = "";
        successfully_focused = false;
        preview_targetRatio = 0.0;
        // n.b., don't reset has_set_location, as we can remember the location when switching camera
        if (continuous_focus_move_is_started) {
            continuous_focus_move_is_started = false;
            applicationInterface.onContinuousFocusMove(false);
        }
        applicationInterface.cameraClosed();
        cancelTimer();
        cancelRepeat();
        if (camera_controller != null) {
            if (MyDebug.LOG) {
                Log.d(TAG, "close camera_controller");
            }
            if (camera_controller != null) {
                pausePreview(false);
                final CameraController camera_controller_local = camera_controller;
                camera_controller = null;
                if (async) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "close camera on background async");
                    camera_open_state = CameraOpenState.CAMERAOPENSTATE_CLOSING;
                    close_camera_task = new CloseCameraTask(camera_controller_local, closeCameraCallback);
                    close_camera_task.execute();
                } else {
                    if (MyDebug.LOG) {
                        Log.d(TAG, "closeCamera: about to release camera controller: " + (System.currentTimeMillis() - debug_time));
                    }
                    camera_controller_local.stopPreview();
                    if (MyDebug.LOG) {
                        Log.d(TAG, "time to stop preview: " + (System.currentTimeMillis() - debug_time));
                    }
                    camera_controller_local.release();
                    camera_open_state = CameraOpenState.CAMERAOPENSTATE_CLOSED;
                }
            }
        } else {
            if (MyDebug.LOG) {
                Log.d(TAG, "camera_controller isn't open");
            }
            if (closeCameraCallback != null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "calling closeCameraCallback.onClosed");
                closeCameraCallback.onClosed();
            }
        }

        if (orientationEventListener != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "free orientationEventListener");
            orientationEventListener.disable();
            orientationEventListener = null;
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "closeCamera: total time: " + (System.currentTimeMillis() - debug_time));
        }
    }

    public void cancelTimer() {
        if (MyDebug.LOG)
            Log.d(TAG, "cancelTimer()");
        if (this.isOnTimer()) {
            takePictureTimerTask.cancel();
            takePictureTimerTask = null;
            if (beepTimerTask != null) {
                beepTimerTask.cancel();
                beepTimerTask = null;
            }
            this.phase = PHASE_NORMAL;
            if (MyDebug.LOG)
                Log.d(TAG, "cancelled camera timer");
        }
    }

    public void cancelRepeat() {
        if (MyDebug.LOG)
            Log.d(TAG, "cancelRepeat()");
        remaining_repeat_photos = 0;
    }

    public void pausePreview(boolean stop_preview) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "pausePreview()");
            debug_time = System.currentTimeMillis();
        }
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }
        this.setPreviewPaused(false);
        if (stop_preview) {
            if (MyDebug.LOG) {
                Log.d(TAG, "pausePreview: about to stop preview: " + (System.currentTimeMillis() - debug_time));
            }
            camera_controller.stopPreview();
            if (MyDebug.LOG) {
                Log.d(TAG, "pausePreview: time to stop preview: " + (System.currentTimeMillis() - debug_time));
            }
        }
        this.phase = PHASE_NORMAL;
        this.is_preview_started = false;
        if (MyDebug.LOG) {
            Log.d(TAG, "pausePreview: about to call cameraInOperation: " + (System.currentTimeMillis() - debug_time));
        }
    }

    private void openCamera() {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "openCamera()");
            debug_time = System.currentTimeMillis();
        }
        if (camera_open_state == CameraOpenState.CAMERAOPENSTATE_OPENING) {
            if (MyDebug.LOG)
                Log.d(TAG, "already opening camera in background thread");
            return;
        } else if (camera_open_state == CameraOpenState.CAMERAOPENSTATE_CLOSING) {
            Log.d(TAG, "tried to open camera while camera is still closing in background thread");
            return;
        }
        // need to init everything now, in case we don't open the camera (but these may already be initialised from an earlier call - e.g., if we are now switching to another camera)
        // n.b., don't reset has_set_location, as we can remember the location when switching camera
        is_preview_started = false; // theoretically should be false anyway, but I had one RuntimeException from surfaceCreated()->openCamera()->setupCamera()->setPreviewSize() because is_preview_started was true, even though the preview couldn't have been started
        set_preview_size = false;
        preview_w = 0;
        preview_h = 0;
        has_focus_area = false;
        focus_success = FOCUS_DONE;
        focus_started_time = -1;
        synchronized (this) {
            // synchronise for consistency (keep FindBugs happy)
            take_photo_after_autofocus = false;
            // no need to call camera_controller.setCaptureFollowAutofocusHint() as we're opening the camera
        }
        set_flash_value_after_autofocus = "";
        successfully_focused = false;
        preview_targetRatio = 0.0;
        scene_modes = null;
        camera_controller_supports_zoom = false;
        has_zoom = false;
        max_zoom_factor = 0;
        minimum_focus_distance = 0.0f;
        zoom_ratios = null;
        faces_detected = null;
        supports_face_detection = false;
        using_face_detection = false;
        supports_video_stabilization = false;
        supports_photo_video_recording = false;
        can_disable_shutter_sound = false;
        tonemap_max_curve_points = 0;
        supports_tonemap_curve = false;
        color_effects = null;
        white_balances = null;
        antibanding = null;
        edge_modes = null;
        noise_reduction_modes = null;
        isos = null;
        supports_white_balance_temperature = false;
        min_temperature = 0;
        max_temperature = 0;
        supports_iso_range = false;
        min_iso = 0;
        max_iso = 0;
        supports_exposure_time = false;
        min_exposure_time = 0L;
        max_exposure_time = 0L;
        exposures = null;
        min_exposure = 0;
        max_exposure = 0;
        exposure_step = 0.0f;
        supports_expo_bracketing = false;
        max_expo_bracketing_n_images = 0;
        supports_focus_bracketing = false;
        supports_burst = false;
        supports_raw = false;
        view_angle_x = 55.0f; // set a sensible default
        view_angle_y = 43.0f; // set a sensible default
        sizes = null;
        current_size_index = -1;
        video_high_speed = false;
//        video_quality_handler.resetCurrentQuality();
        supported_flash_values = null;
        current_flash_index = -1;
        supported_focus_values = null;
        current_focus_index = -1;
        max_num_focus_areas = 0;
        applicationInterface.cameraInOperation(false, false);
        if (!this.has_surface) {
            if (MyDebug.LOG) {
                Log.d(TAG, "preview surface not yet available");
            }
            return;
        }
        if (this.app_is_paused) {
            if (MyDebug.LOG) {
                Log.d(TAG, "don't open camera as app is paused");
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // we restrict the checks to Android 6 or later just in case, see note in LocationSupplier.setupLocationListener()
            if (MyDebug.LOG)
                Log.d(TAG, "check for permissions");
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (MyDebug.LOG)
                    Log.d(TAG, "camera permission not available");
                has_permissions = false;
                applicationInterface.requestCameraPermission();
                // return for now - the application should try to reopen the camera if permission is granted
                return;
            }
            if (applicationInterface.needsStoragePermission() && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (MyDebug.LOG)
                    Log.d(TAG, "storage permission not available");
                has_permissions = false;
                applicationInterface.requestStoragePermission();
                // return for now - the application should try to reopen the camera if permission is granted
                return;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "permissions available");
        }
        // set in case this was previously set to false
        has_permissions = true;
        camera_open_state = CameraOpenState.CAMERAOPENSTATE_OPENING;
        int cameraId = applicationInterface.getCameraIdPref();
        if (cameraId < 0 || cameraId >= camera_controller_manager.getNumberOfCameras()) {
            if (MyDebug.LOG)
                Log.d(TAG, "invalid cameraId: " + cameraId);
            cameraId = 0;
            applicationInterface.setCameraIdPref(cameraId);
        }

        final boolean use_background_thread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        if (use_background_thread) {
            final int cameraId_f = cameraId;

            open_camera_task = new AsyncTask<Void, Void, CameraController>() {
                private static final String TAG = "Preview/openCamera";

                @Override
                protected CameraController doInBackground(Void... voids) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "doInBackground, async task: " + this);
                    return openCameraCore(cameraId_f);
                }

                /** The system calls this to perform work in the UI thread and delivers
                 * the result from doInBackground() */
                protected void onPostExecute(CameraController camera_controller) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "onPostExecute, async task: " + this);
                    // see note in openCameraCore() for why we set camera_controller here
                    Preview.this.camera_controller = camera_controller;
                    cameraOpened();
                    // set camera_open_state after cameraOpened, just in case a non-UI thread is listening for this - also
                    // important for test code waitUntilCameraOpened(), as test code runs on a different thread
                    camera_open_state = CameraOpenState.CAMERAOPENSTATE_OPENED;
                    open_camera_task = null; // just to be safe
                    if (MyDebug.LOG)
                        Log.d(TAG, "onPostExecute done, async task: " + this);
                }

                protected void onCancelled(CameraController camera_controller) {
                    if (MyDebug.LOG) {
                        Log.d(TAG, "onCancelled, async task: " + this);
                        Log.d(TAG, "camera_controller: " + camera_controller);
                    }
                    // this typically means the application has paused whilst we were opening camera in background - so should just
                    // dispose of the camera controller
                    if (camera_controller != null) {
                        // this is the local camera_controller, not Preview.this.camera_controller!
                        camera_controller.release();
                    }
                    camera_open_state = CameraOpenState.CAMERAOPENSTATE_OPENED; // n.b., still set OPENED state - important for test thread to know that this callback is complete
                    open_camera_task = null; // just to be safe
                    if (MyDebug.LOG)
                        Log.d(TAG, "onCancelled done, async task: " + this);
                }
            }.execute();
        } else {
            this.camera_controller = openCameraCore(cameraId);
            if (MyDebug.LOG) {
                Log.d(TAG, "openCamera: time after opening camera: " + (System.currentTimeMillis() - debug_time));
            }

            cameraOpened();
            camera_open_state = CameraOpenState.CAMERAOPENSTATE_OPENED;
        }
    }

    private CameraController openCameraCore(int cameraId) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "openCameraCore()");
            debug_time = System.currentTimeMillis();
        }
        // We pass a camera controller back to the UI thread rather than assigning to camera_controller here, because:
        // * If we set camera_controller directly, we'd need to synchronize, otherwise risk of memory barrier issues
        // * Risk of race conditions if UI thread accesses camera_controller before we have called cameraOpened().
        CameraController camera_controller_local;
        try {
            if (test_fail_open_camera) {
                throw new CameraControllerException();
            }
            CameraController.ErrorCallback cameraErrorCallback = new CameraController.ErrorCallback() {
                public void onError() {
                    if (MyDebug.LOG)
                        Log.e(TAG, "error from CameraController: camera device failed");
                    if (camera_controller != null) {
                        camera_controller = null;
                        camera_open_state = CameraOpenState.CAMERAOPENSTATE_CLOSED;
                        applicationInterface.onCameraError();
                    }
                }
            };
            if (using_android_l) {
                CameraController.ErrorCallback previewErrorCallback = new CameraController.ErrorCallback() {
                    public void onError() {
                        if (MyDebug.LOG)
                            Log.e(TAG, "error from CameraController: preview failed to start");
                        applicationInterface.onFailedStartPreview();
                    }
                };
                camera_controller_local = new CameraController2(Preview.this.getContext(), cameraId, previewErrorCallback, cameraErrorCallback);
                if (applicationInterface.useCamera2FakeFlash()) {
                    camera_controller_local.setUseCamera2FakeFlash(true);
                }
            } else
                camera_controller_local = new CameraController1(cameraId, cameraErrorCallback);
            //throw new CameraControllerException(); // uncomment to test camera not opening
        } catch (CameraControllerException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "Failed to open camera: " + e.getMessage());
            e.printStackTrace();
            camera_controller_local = null;
        }

        if (MyDebug.LOG) {
            Log.d(TAG, "openCamera: total time for openCameraCore: " + (System.currentTimeMillis() - debug_time));
        }
        return camera_controller_local;
    }


    private void cameraOpened() {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "cameraOpened()");
            debug_time = System.currentTimeMillis();
        }
        boolean take_photo = false;
        if (camera_controller != null) {
            Activity activity = (Activity) Preview.this.getContext();
            if (MyDebug.LOG)
                Log.d(TAG, "intent: " + activity.getIntent());
            if (activity.getIntent() != null && activity.getIntent().getExtras() != null) {
                take_photo = activity.getIntent().getExtras().getBoolean(TakePhoto.TAKE_PHOTO);
                activity.getIntent().removeExtra(TakePhoto.TAKE_PHOTO);
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "no intent data");
            }
            if (MyDebug.LOG)
                Log.d(TAG, "take_photo?: " + take_photo);

            setCameraDisplayOrientation();
            if (orientationEventListener == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "create orientationEventListener");
                orientationEventListener = new OrientationEventListener(activity) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        Preview.this.onOrientationChanged(orientation);
                    }
                };
                orientationEventListener.enable();
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "openCamera: time after setting orientation: " + (System.currentTimeMillis() - debug_time));
            }

            if (MyDebug.LOG)
                Log.d(TAG, "call setPreviewDisplay");
            cameraSurface.setPreviewDisplay(camera_controller);
            if (MyDebug.LOG) {
                Log.d(TAG, "openCamera: time after setting preview display: " + (System.currentTimeMillis() - debug_time));
            }

            setupCamera(take_photo);
            if (this.using_android_l) {
                configureTransform();
            }
        }

        if (MyDebug.LOG) {
            Log.d(TAG, "openCamera: total time for cameraOpened: " + (System.currentTimeMillis() - debug_time));
        }
    }
    public void retryOpenCamera() {
        if (MyDebug.LOG)
            Log.d(TAG, "retryOpenCamera()");
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "try to reopen camera");
            this.openCamera();
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "camera already open");
        }
    }

    public void reopenCamera() {
        closeCamera(true, new CloseCameraCallback() {
            @Override
            public void onClosed() {
                openCamera();
            }
        });
    }

    public void setupCamera(boolean take_photo) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            debug_time = System.currentTimeMillis();
        }
        if (camera_controller == null) {
            return;
        }
        boolean do_startup_focus = !take_photo && applicationInterface.getStartupFocusPref();
        try {
            setupCameraParameters();
        } catch (CameraControllerException e) {
            e.printStackTrace();
            applicationInterface.onCameraError();
            closeCamera(false, null);
            return;
        }
        if (do_startup_focus && using_android_l && camera_controller.supportsAutoFocus()) {
            set_flash_value_after_autofocus = "";
            String old_flash_value = camera_controller.getFlashValue();
            if (old_flash_value.length() > 0 && !old_flash_value.equals("flash_off") && !old_flash_value.equals("flash_torch")) {
                set_flash_value_after_autofocus = old_flash_value;
                camera_controller.setFlashValue("flash_off");
            }
        }
        if (this.supports_raw && applicationInterface.getRawPref() != ApplicationInterface.RawPref.RAWPREF_JPEG_ONLY) {
            camera_controller.setRaw(true, applicationInterface.getMaxRawImages());
        } else {
            camera_controller.setRaw(false, 0);
        }
        setupBurstMode();

        if (camera_controller.isBurstOrExpo()) {
            CameraController.Size current_size = getCurrentPictureSize();
            if (current_size != null && !current_size.supports_burst) {
                CameraController.Size new_size = null;
                for (int i = 0; i < sizes.size(); i++) {
                    CameraController.Size size = sizes.get(i);
                    if (size.supports_burst && size.width * size.height <= current_size.width * current_size.height) {
                        if (new_size == null || size.width * size.height > new_size.width * new_size.height) {
                            current_size_index = i;
                            new_size = size;
                        }
                    }
                }
                if (new_size == null) {
                    Log.e(TAG, "can't find burst-supporting picture size smaller than the current picture size");
                    // just find largest that supports burst
                    for (int i = 0; i < sizes.size(); i++) {
                        CameraController.Size size = sizes.get(i);
                        if (size.supports_burst) {
                            if (new_size == null || size.width * size.height > new_size.width * new_size.height) {
                                current_size_index = i;
                                new_size = size;
                            }
                        }
                    }
                    if (new_size == null) {
                        Log.e(TAG, "can't find burst-supporting picture size");
                    }
                }
            }
        }

        camera_controller.setOptimiseAEForDRO(applicationInterface.getOptimiseAEForDROPref());
        setPreviewSize(); // need to call this when we switch cameras, not just when we run for the first time
        startCameraPreview();
        if (this.has_zoom && applicationInterface.getZoomPref() != 0) {
            zoomTo(applicationInterface.getZoomPref());
        } else if (camera_controller_supports_zoom && !has_zoom) {
            camera_controller.setZoom(0);
        }
        applicationInterface.cameraSetup(); // must call this after the above take_photo code for calling switchVideo
        if (MyDebug.LOG) {
            Log.d(TAG, "setupCamera: total time after cameraSetup: " + (System.currentTimeMillis() - debug_time));
        }

        if (take_photo) {
            String focus_value = getCurrentFocusValue();
            final int delay = (focus_value != null && focus_value.equals("focus_mode_continuous_picture")) ? 1500 : 500;
            if (MyDebug.LOG)
                Log.d(TAG, "delay for take photo: " + delay);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (MyDebug.LOG)
                        Log.d(TAG, "do automatic take picture");
                    takePicture( false);
                }
            }, delay);
        }

        if (do_startup_focus) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (MyDebug.LOG)
                        Log.d(TAG, "do startup autofocus");
                    tryAutoFocus(true, false); // so we get the autofocus when starting up - we do this on a delay, as calling it immediately means the autofocus doesn't seem to work properly sometimes (at least on Galaxy Nexus)
                }
            }, 500);
        }

    }

    public void setupBurstMode() {
        if (MyDebug.LOG)
            Log.d(TAG, "setupBurstMode()");
        if (this.supports_expo_bracketing && applicationInterface.isExpoBracketingPref()) {
            camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_EXPO);
            camera_controller.setExpoBracketingNImages(applicationInterface.getExpoBracketingNImagesPref());
            camera_controller.setExpoBracketingStops(applicationInterface.getExpoBracketingStopsPref());
            // setUseExpoFastBurst called when taking a photo
        } else if (this.supports_focus_bracketing && applicationInterface.isFocusBracketingPref()) {
            camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_FOCUS);
            camera_controller.setFocusBracketingNImages(applicationInterface.getFocusBracketingNImagesPref());
            camera_controller.setFocusBracketingAddInfinity(applicationInterface.getFocusBracketingAddInfinityPref());
        } else if (this.supports_burst && applicationInterface.isCameraBurstPref()) {
            if (applicationInterface.getBurstForNoiseReduction()) {
                if (this.supports_exposure_time) { // noise reduction mode also needs manual exposure
                    ApplicationInterface.NRModePref nr_mode = applicationInterface.getNRModePref();
                    camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_NORMAL);
                    camera_controller.setBurstForNoiseReduction(true, nr_mode == ApplicationInterface.NRModePref.NRMODE_LOW_LIGHT);
                } else {
                    camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_NONE);
                }
            } else {
                camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_NORMAL);
                camera_controller.setBurstForNoiseReduction(false, false);
                camera_controller.setBurstNImages(applicationInterface.getBurstNImages());
            }
        } else {
            camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_NONE);
        }
    }

    private void setupCameraParameters() throws CameraControllerException {
        {
            String value = applicationInterface.getSceneModePref();
            CameraController.SupportedValues supported_values = camera_controller.setSceneMode(value);
            if (supported_values != null) {
                scene_modes = supported_values.values;
                applicationInterface.setSceneModePref(supported_values.selected_value);
            } else {
                applicationInterface.clearSceneModePref();
            }
            CameraController.CameraFeatures camera_features = camera_controller.getCameraFeatures();
            this.camera_controller_supports_zoom = camera_features.is_zoom_supported;
            this.has_zoom = camera_features.is_zoom_supported && applicationInterface.allowZoom();
            if (this.has_zoom) {
                this.max_zoom_factor = camera_features.max_zoom;
                this.zoom_ratios = camera_features.zoom_ratios;
            } else {
                this.max_zoom_factor = 0;
                this.zoom_ratios = null;
            }
            this.minimum_focus_distance = camera_features.minimum_focus_distance;
            this.supports_face_detection = camera_features.supports_face_detection;
            this.sizes = camera_features.picture_sizes;
            supported_flash_values = camera_features.supported_flash_values;
            supported_focus_values = camera_features.supported_focus_values;
            this.max_num_focus_areas = camera_features.max_num_focus_areas;
            this.is_exposure_lock_supported = camera_features.is_exposure_lock_supported;
            this.is_white_balance_lock_supported = camera_features.is_white_balance_lock_supported;
            this.supports_video_stabilization = camera_features.is_video_stabilization_supported;
            this.supports_photo_video_recording = camera_features.is_photo_video_recording_supported;
            this.can_disable_shutter_sound = camera_features.can_disable_shutter_sound;
            this.tonemap_max_curve_points = camera_features.tonemap_max_curve_points;
            this.supports_tonemap_curve = camera_features.supports_tonemap_curve;
            this.supports_white_balance_temperature = camera_features.supports_white_balance_temperature;
            this.min_temperature = camera_features.min_temperature;
            this.max_temperature = camera_features.max_temperature;
            this.supports_iso_range = camera_features.supports_iso_range;
            this.min_iso = camera_features.min_iso;
            this.max_iso = camera_features.max_iso;
            this.supports_exposure_time = camera_features.supports_exposure_time;
            this.min_exposure_time = camera_features.min_exposure_time;
            this.max_exposure_time = camera_features.max_exposure_time;
            this.min_exposure = camera_features.min_exposure;
            this.max_exposure = camera_features.max_exposure;
            this.exposure_step = camera_features.exposure_step;
            this.supports_expo_bracketing = camera_features.supports_expo_bracketing;
            this.max_expo_bracketing_n_images = camera_features.max_expo_bracketing_n_images;
            this.supports_focus_bracketing = camera_features.supports_focus_bracketing;
            this.supports_burst = camera_features.supports_burst;
            this.supports_raw = camera_features.supports_raw;
            this.view_angle_x = camera_features.view_angle_x;
            this.view_angle_y = camera_features.view_angle_y;
            this.supported_preview_sizes = camera_features.preview_sizes;
        }

        String value = applicationInterface.getISOPref();
        boolean is_manual_iso = false;
        if (supports_iso_range) {
            this.isos = null; // if supports_iso_range==true, caller shouldn't be using getSupportedISOs()
            if (value.equals(CameraController.ISO_DEFAULT)) {
                if (MyDebug.LOG)
                    Log.d(TAG, "setting auto iso");
                camera_controller.setManualISO(false, 0);
            } else {
                int iso = parseManualISOValue(value);
                if (iso >= 0) {
                    is_manual_iso = true;
                    if (MyDebug.LOG)
                        Log.d(TAG, "iso: " + iso);
                    camera_controller.setManualISO(true, iso);
                } else {
                    camera_controller.setManualISO(false, 0);
                    value = CameraController.ISO_DEFAULT; // so we switch the preferences back to auto mode, rather than the invalid value
                }

                applicationInterface.setISOPref(value);
            }
        } else {
            CameraController.SupportedValues supported_values = camera_controller.setISO(value);
            if (supported_values != null) {
                isos = supported_values.values;
                if (!supported_values.selected_value.equals(CameraController.ISO_DEFAULT)) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "has manual iso");
                    is_manual_iso = true;
                }
                applicationInterface.setISOPref(supported_values.selected_value);

            } else {
                applicationInterface.clearISOPref();
            }
        }

        if (is_manual_iso) {
            if (supports_exposure_time) {
                long exposure_time_value = applicationInterface.getExposureTimePref();
                if (MyDebug.LOG)
                    Log.d(TAG, "saved exposure_time: " + exposure_time_value);
                if (exposure_time_value < getMinimumExposureTime())
                    exposure_time_value = getMinimumExposureTime();
                else if (exposure_time_value > getMaximumExposureTime())
                    exposure_time_value = getMaximumExposureTime();
                camera_controller.setExposureTime(exposure_time_value);
                // now save
                applicationInterface.setExposureTimePref(exposure_time_value);
            } else {
                // delete key in case it's present (e.g., if feature no longer available due to change in OS, or switching APIs)
                applicationInterface.clearExposureTimePref();
            }

            if (this.using_android_l && supported_flash_values != null) {
                // flash modes not supported when using Camera2 and manual ISO
                // (it's unclear flash is useful - ideally we'd at least offer torch, but ISO seems to reset to 100 when flash/torch is on!)
                supported_flash_values = null;
                if (MyDebug.LOG)
                    Log.d(TAG, "flash not supported in Camera2 manual mode");
            }
        }
        {
            exposures = null;
            if (min_exposure != 0 || max_exposure != 0) {
                exposures = new ArrayList<>();
                for (int i = min_exposure; i <= max_exposure; i++) {
                    exposures.add("" + i);
                }
                if (!is_manual_iso) {
                    int exposure = applicationInterface.getExposureCompensationPref();
                    if (exposure < min_exposure || exposure > max_exposure) {
                        exposure = 0;
                        if (MyDebug.LOG)
                            Log.d(TAG, "saved exposure not supported, reset to 0");
                        if (exposure < min_exposure || exposure > max_exposure) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "zero isn't an allowed exposure?! reset to min " + min_exposure);
                            exposure = min_exposure;
                        }
                    }
                    camera_controller.setExposureCompensation(exposure);
                    applicationInterface.setExposureCompensationPref(exposure);
                }
            } else {
                applicationInterface.clearExposureCompensationPref();
            }
        }

        {
            current_size_index = -1;
            Pair<Integer, Integer> resolution = applicationInterface.getCameraResolutionPref();
            if (resolution != null) {
                int resolution_w = resolution.first;
                int resolution_h = resolution.second;
                for (int i = 0; i < sizes.size() && current_size_index == -1; i++) {
                    CameraController.Size size = sizes.get(i);
                    if (size.width == resolution_w && size.height == resolution_h) {
                        current_size_index = i;
                        if (MyDebug.LOG)
                            Log.d(TAG, "set current_size_index to: " + current_size_index);
                    }
                }
            }

            if (current_size_index == -1) {
                // set to largest
                CameraController.Size current_size = null;
                for (int i = 0; i < sizes.size(); i++) {
                    CameraController.Size size = sizes.get(i);
                    if (current_size == null || size.width * size.height > current_size.width * current_size.height) {
                        current_size_index = i;
                        current_size = size;
                    }
                }
            }
            {
                CameraController.Size current_size = getCurrentPictureSize();
                if (current_size != null) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "Current size index " + current_size_index + ": " + current_size.width + ", " + current_size.height);

                    applicationInterface.setCameraResolutionPref(current_size.width, current_size.height);
                }
            }
        }

        {
            int image_quality = applicationInterface.getImageQualityPref();
            if (MyDebug.LOG)
                Log.d(TAG, "set up jpeg quality: " + image_quality);
            camera_controller.setJpegQuality(image_quality);
        }
        {
            if (MyDebug.LOG) {
                Log.d(TAG, "set up flash");
                Log.d(TAG, "flash values: " + supported_flash_values);
            }
            current_flash_index = -1;
            if (supported_flash_values != null && supported_flash_values.size() > 1) {

                String flash_value = applicationInterface.getFlashPref();
                if (flash_value.length() > 0) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "found existing flash_value: " + flash_value);
                    if (!updateFlash(flash_value, false)) { // don't need to save, as this is the value that's already saved
                        if (MyDebug.LOG)
                            Log.d(TAG, "flash value no longer supported!");
                        updateFlash(0, true);
                    }
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "found no existing flash_value");
                    if (supported_flash_values.contains("flash_auto"))
                        updateFlash("flash_auto", true);
                    else
                        updateFlash("flash_off", true);
                }
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "flash not supported");
                supported_flash_values = null;
            }
        }

        {
            if (MyDebug.LOG)
                Log.d(TAG, "set up focus");
            current_focus_index = -1;
            if (supported_focus_values != null && supported_focus_values.size() > 1) {
                if (MyDebug.LOG)
                    Log.d(TAG, "focus values: " + supported_focus_values);

                setFocusPref(true);
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "focus not supported");
                supported_focus_values = null;
            }
        }
        {
            float focus_distance_value = applicationInterface.getFocusDistancePref(false);
            if (MyDebug.LOG)
                Log.d(TAG, "saved focus_distance: " + focus_distance_value);
            if (focus_distance_value < 0.0f)
                focus_distance_value = 0.0f;
            else if (focus_distance_value > minimum_focus_distance)
                focus_distance_value = minimum_focus_distance;
            camera_controller.setFocusDistance(focus_distance_value);
            camera_controller.setFocusBracketingSourceDistance(focus_distance_value);
            // now save
            applicationInterface.setFocusDistancePref(focus_distance_value, false);
        }
        {
            float focus_distance_value = applicationInterface.getFocusDistancePref(true);
            if (MyDebug.LOG)
                Log.d(TAG, "saved focus_bracketing_target_distance: " + focus_distance_value);
            if (focus_distance_value < 0.0f)
                focus_distance_value = 0.0f;
            else if (focus_distance_value > minimum_focus_distance)
                focus_distance_value = minimum_focus_distance;
            camera_controller.setFocusBracketingTargetDistance(focus_distance_value);
            // now save
            applicationInterface.setFocusDistancePref(focus_distance_value, true);
        }

        {
            is_exposure_locked = false;
        }

        {
            is_white_balance_locked = false;
        }

    }

    private void setPreviewSize() {
        if (MyDebug.LOG)
            Log.d(TAG, "setPreviewSize()");
        // also now sets picture size
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }
        if (is_preview_started) {
            return;
        }
        if (!using_android_l) {
            // don't do for Android L, else this means we get flash on startup autofocus if flash is on
            this.cancelAutoFocus();
        }
        // first set picture size (for photo mode, must be done now so we can set the picture size from this; for video, doesn't really matter when we set it)
        CameraController.Size new_size = null;
        new_size = getCurrentPictureSize();
        if (new_size != null) {
            camera_controller.setPictureSize(new_size.width, new_size.height);
        }
        // set optimal preview size
        if (supported_preview_sizes != null && supported_preview_sizes.size() > 0) {
            CameraController.Size best_size = getOptimalPreviewSize(supported_preview_sizes);
            camera_controller.setPreviewSize(best_size.width, best_size.height);
            this.set_preview_size = true;
            this.preview_w = best_size.width;
            this.preview_h = best_size.height;
            this.setAspectRatio(((double) best_size.width) / (double) best_size.height);
        }
    }

    private CamcorderProfile getCamcorderProfile(String quality) {
        if (MyDebug.LOG)
            Log.d(TAG, "getCamcorderProfile(): " + quality);
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return CamcorderProfile.get(0, CamcorderProfile.QUALITY_HIGH);
        }
        int cameraId = camera_controller.getCameraId();
        CamcorderProfile camcorder_profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH); // default
        try {
            String profile_string = quality;
            int index = profile_string.indexOf('_');
            if (index != -1) {
                profile_string = quality.substring(0, index);
                if (MyDebug.LOG)
                    Log.d(TAG, "    profile_string: " + profile_string);
            }
            int profile = Integer.parseInt(profile_string);
            camcorder_profile = CamcorderProfile.get(cameraId, profile);
            if (index != -1 && index + 1 < quality.length()) {
                String override_string = quality.substring(index + 1);
                if (MyDebug.LOG)
                    Log.d(TAG, "    override_string: " + override_string);
                if (override_string.charAt(0) == 'r' && override_string.length() >= 4) {
                    index = override_string.indexOf('x');
                    if (index == -1) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "override_string invalid format, can't find x");
                    } else {
                        String resolution_w_s = override_string.substring(1, index); // skip first 'r'
                        String resolution_h_s = override_string.substring(index + 1);
                        if (MyDebug.LOG) {
                            Log.d(TAG, "resolution_w_s: " + resolution_w_s);
                            Log.d(TAG, "resolution_h_s: " + resolution_h_s);
                        }
                        // copy to local variable first, so that if we fail to parse height, we don't set the width either
                        int resolution_w = Integer.parseInt(resolution_w_s);
                        int resolution_h = Integer.parseInt(resolution_h_s);
                        camcorder_profile.videoFrameWidth = resolution_w;
                        camcorder_profile.videoFrameHeight = resolution_h;
                    }
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "unknown override_string initial code, or otherwise invalid format");
                }
            }
        } catch (NumberFormatException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "failed to parse video quality: " + quality);
            e.printStackTrace();
        }
        return camcorder_profile;
    }

    private static String formatFloatToString(final float f) {
        final int i = (int) f;
        if (f == i)
            return Integer.toString(i);
        return String.format(Locale.getDefault(), "%.2f", f);
    }

    private static int greatestCommonFactor(int a, int b) {
        while (b > 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    private static String getAspectRatio(int width, int height) {
        int gcf = greatestCommonFactor(width, height);
        if (gcf > 0) {
            // had a Google Play crash due to gcf being 0!? Implies width must be zero
            width /= gcf;
            height /= gcf;
        }
        return width + ":" + height;
    }

    private static String getMPString(int width, int height) {
        float mp = (width * height) / 1000000.0f;
        return formatFloatToString(mp) + "MP";
    }

    private static String getBurstString(Resources resources, boolean supports_burst) {
        // should return empty string if supports_burst==true, as this is also used for video resolution strings
        return supports_burst ? "" : ", " + resources.getString(R.string.no_burst);
    }

    public static String getAspectRatioMPString(Resources resources, int width, int height, boolean supports_burst) {
        return "(" + getAspectRatio(width, height) + ", " + getMPString(width, height) + getBurstString(resources, supports_burst) + ")";
    }

    public double getTargetRatio() {
        return preview_targetRatio;
    }

    private double calculateTargetRatioForPreview(Point display_size) {
        double targetRatio;
        String preview_size = applicationInterface.getPreviewSizePref();
        // should always use wysiwig for video mode, otherwise we get incorrect aspect ratio shown when recording video (at least on Galaxy Nexus, e.g., at 640x480)
        // also not using wysiwyg mode with video caused corruption on Samsung cameras (tested with Samsung S3, Android 4.3, front camera, infinity focus)
        if (preview_size.equals("preference_preview_size_wysiwyg")) {
            if (MyDebug.LOG)
                Log.d(TAG, "set preview aspect ratio from photo size (wysiwyg)");
            CameraController.Size picture_size = camera_controller.getPictureSize();
            if (MyDebug.LOG)
                Log.d(TAG, "picture_size: " + picture_size.width + " x " + picture_size.height);
            targetRatio = ((double) picture_size.width) / (double) picture_size.height;
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "set preview aspect ratio from display size");
            // base target ratio from display size - means preview will fill the device's display as much as possible
            // but if the preview's aspect ratio differs from the actual photo/video size, the preview will show a cropped version of what is actually taken
            targetRatio = ((double) display_size.x) / (double) display_size.y;
        }
        this.preview_targetRatio = targetRatio;
        if (MyDebug.LOG)
            Log.d(TAG, "targetRatio: " + targetRatio);
        return targetRatio;
    }

    private static CameraController.Size getClosestSize(List<CameraController.Size> sizes, double targetRatio, CameraController.Size max_size) {
        if (MyDebug.LOG)
            Log.d(TAG, "getClosestSize()");
        CameraController.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for (CameraController.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (max_size != null) {
                if (size.width > max_size.width || size.height > max_size.height)
                    continue;
            }
            if (Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(ratio - targetRatio);
            }
        }
        return optimalSize;
    }

    public CameraController.Size getOptimalPreviewSize(List<CameraController.Size> sizes) {
        if (MyDebug.LOG)
            Log.d(TAG, "getOptimalPreviewSize()");
        final double ASPECT_TOLERANCE = 0.05;
        if (sizes == null)
            return null;
        CameraController.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        Point display_size = new Point();
        Activity activity = (Activity) this.getContext();
        {
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getSize(display_size);
            if (display_size.x < display_size.y) {
                display_size.set(display_size.y, display_size.x);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
        }
        double targetRatio = calculateTargetRatioForPreview(display_size);
        int targetHeight = Math.min(display_size.y, display_size.x);
        if (targetHeight <= 0) {
            targetHeight = display_size.y;
        }
        // Try to find the size which matches the aspect ratio, and is closest match to display height
        for (CameraController.Size size : sizes) {
            if (MyDebug.LOG)
                Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            // can't find match for aspect ratio, so find closest one
            if (MyDebug.LOG)
                Log.d(TAG, "no preview size matches the aspect ratio");
            optimalSize = getClosestSize(sizes, targetRatio, null);
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "chose optimalSize: " + optimalSize.width + " x " + optimalSize.height);
            Log.d(TAG, "optimalSize ratio: " + ((double) optimalSize.width / optimalSize.height));
        }
        return optimalSize;
    }

    private void setAspectRatio(double ratio) {
        if (ratio <= 0.0)
            throw new IllegalArgumentException();

        has_aspect_ratio = true;
        if (aspect_ratio != ratio) {
            aspect_ratio = ratio;
            if (MyDebug.LOG)
                Log.d(TAG, "new aspect ratio: " + aspect_ratio);
            cameraSurface.getView().requestLayout();
            if (canvasView != null) {
                canvasView.requestLayout();
            }
        }
    }

    private boolean hasAspectRatio() {
        return has_aspect_ratio;
    }

    private double getAspectRatio() {
        return aspect_ratio;
    }

    public int getDisplayRotation() {
        // gets the display rotation (as a Surface.ROTATION_* constant), taking into account the getRotatePreviewPreferenceKey() setting
        Activity activity = (Activity) this.getContext();
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        String rotate_preview = applicationInterface.getPreviewRotationPref();
        if (MyDebug.LOG)
            Log.d(TAG, "    rotate_preview = " + rotate_preview);
        if (rotate_preview.equals("180")) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    rotation = Surface.ROTATION_180;
                    break;
                case Surface.ROTATION_90:
                    rotation = Surface.ROTATION_270;
                    break;
                case Surface.ROTATION_180:
                    rotation = Surface.ROTATION_0;
                    break;
                case Surface.ROTATION_270:
                    rotation = Surface.ROTATION_90;
                    break;
                default:
                    break;
            }
        }

        return rotation;
    }

    private int getDisplayRotationDegrees() {
        if (MyDebug.LOG)
            Log.d(TAG, "getDisplayRotationDegrees");
        int rotation = getDisplayRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "    degrees = " + degrees);
        return degrees;
    }

    // note, if orientation is locked to landscape this is only called when setting up the activity, and will always have the same orientation
    public void setCameraDisplayOrientation() {
        if (camera_controller == null) {
            return;
        }
        if (using_android_l) {
            // need to configure the textureview
            configureTransform();
        } else {
            int degrees = getDisplayRotationDegrees();
            if (MyDebug.LOG)
                Log.d(TAG, "    degrees = " + degrees);
            // note the code to make the rotation relative to the camera sensor is done in camera_controller.setDisplayOrientation()
            camera_controller.setDisplayOrientation(degrees);
        }
    }

    // for taking photos - see http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setRotation(int)
    private void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
            return;
        if (camera_controller == null) {
            return;
        }
        orientation = (orientation + 45) / 90 * 90;
        this.current_orientation = orientation % 360;
        int new_rotation;
        int camera_orientation = camera_controller.getCameraOrientation();
        if (camera_controller.isFrontFacing()) {
            new_rotation = (camera_orientation - orientation + 360) % 360;
        } else {
            new_rotation = (camera_orientation + orientation) % 360;
        }
        if (new_rotation != current_rotation) {
            if (MyDebug.LOG) {
                Log.d(TAG, "    current_orientation is " + current_orientation);
                Log.d(TAG, "    info orientation is " + camera_orientation);
                Log.d(TAG, "    set Camera rotation from " + current_rotation + " to " + new_rotation);
            }
            this.current_rotation = new_rotation;
        }
    }

    private int getDeviceDefaultOrientation() {
        WindowManager windowManager = (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
        Configuration config = getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();
        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }

    private int getImageVideoRotation() {
        if (MyDebug.LOG)
            Log.d(TAG, "getImageVideoRotation() from current_rotation " + current_rotation);
        String lock_orientation = applicationInterface.getLockOrientationPref();
        if (lock_orientation.equals("landscape")) {
            int camera_orientation = camera_controller.getCameraOrientation();
            int device_orientation = getDeviceDefaultOrientation();
            int result;
            if (device_orientation == Configuration.ORIENTATION_PORTRAIT) {
                // should be equivalent to onOrientationChanged(270)
                if (camera_controller.isFrontFacing()) {
                    result = (camera_orientation + 90) % 360;
                } else {
                    result = (camera_orientation + 270) % 360;
                }
            } else {
                // should be equivalent to onOrientationChanged(0)
                result = camera_orientation;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "getImageVideoRotation() lock to landscape, returns " + result);
            return result;
        } else if (lock_orientation.equals("portrait")) {
            int camera_orientation = camera_controller.getCameraOrientation();
            int result;
            int device_orientation = getDeviceDefaultOrientation();
            if (device_orientation == Configuration.ORIENTATION_PORTRAIT) {
                // should be equivalent to onOrientationChanged(0)
                result = camera_orientation;
            } else {
                // should be equivalent to onOrientationChanged(90)
                if (camera_controller.isFrontFacing()) {
                    result = (camera_orientation + 270) % 360;
                } else {
                    result = (camera_orientation + 90) % 360;
                }
            }
            if (MyDebug.LOG)
                Log.d(TAG, "getImageVideoRotation() lock to portrait, returns " + result);
            return result;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "getImageVideoRotation() returns current_rotation " + current_rotation);
        return this.current_rotation;
    }

    public void draw(Canvas canvas) {
        if (this.app_is_paused) {
            return;
        }
        if (this.focus_success != FOCUS_DONE) {
            if (focus_complete_time != -1 && System.currentTimeMillis() > focus_complete_time + 1000) {
                focus_success = FOCUS_DONE;
            }
        }
        applicationInterface.onDrawPreview(canvas);
    }

    public int getScaledZoomFactor(float scale_factor) {
        if (MyDebug.LOG)
            Log.d(TAG, "getScaledZoomFactor() " + scale_factor);

        int new_zoom_factor = 0;
        if (this.camera_controller != null && this.has_zoom) {
            int zoom_factor = camera_controller.getZoom();
            float zoom_ratio = this.zoom_ratios.get(zoom_factor) / 100.0f;
            zoom_ratio *= scale_factor;

            new_zoom_factor = zoom_factor;
            if (zoom_ratio <= 1.0f) {
                new_zoom_factor = 0;
            } else if (zoom_ratio >= zoom_ratios.get(max_zoom_factor) / 100.0f) {
                new_zoom_factor = max_zoom_factor;
            } else {
                // find the closest zoom level
                if (scale_factor > 1.0f) {
                    // zooming in
                    for (int i = zoom_factor; i < zoom_ratios.size(); i++) {
                        if (zoom_ratios.get(i) / 100.0f >= zoom_ratio) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "zoom int, found new zoom by comparing " + zoom_ratios.get(i) / 100.0f + " >= " + zoom_ratio);
                            new_zoom_factor = i;
                            break;
                        }
                    }
                } else {
                    // zooming out
                    for (int i = zoom_factor; i >= 0; i--) {
                        if (zoom_ratios.get(i) / 100.0f <= zoom_ratio) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "zoom out, found new zoom by comparing " + zoom_ratios.get(i) / 100.0f + " <= " + zoom_ratio);
                            new_zoom_factor = i;
                            break;
                        }
                    }
                }
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "zoom_ratio is now " + zoom_ratio);
                Log.d(TAG, "    old zoom_factor " + zoom_factor + " ratio " + zoom_ratios.get(zoom_factor) / 100.0f);
                Log.d(TAG, "    chosen new zoom_factor " + new_zoom_factor + " ratio " + zoom_ratios.get(new_zoom_factor) / 100.0f);
            }
        }

        return new_zoom_factor;
    }

    public void scaleZoom(float scale_factor) {
        if (MyDebug.LOG)
            Log.d(TAG, "scaleZoom() " + scale_factor);
        if (this.camera_controller != null && this.has_zoom) {
            int new_zoom_factor = getScaledZoomFactor(scale_factor);
            // n.b., don't call zoomTo; this should be called indirectly by applicationInterface.multitouchZoom()
            applicationInterface.multitouchZoom(new_zoom_factor);
        }
    }

    public void zoomTo(int new_zoom_factor) {
        if (MyDebug.LOG)
            Log.d(TAG, "ZoomTo(): " + new_zoom_factor);
        if (new_zoom_factor < 0)
            new_zoom_factor = 0;
        else if (new_zoom_factor > max_zoom_factor)
            new_zoom_factor = max_zoom_factor;
        // problem where we crashed due to calling this function with null camera should be fixed now, but check again just to be safe
        if (camera_controller != null) {
            if (this.has_zoom) {
                // don't cancelAutoFocus() here, otherwise we get sluggish zoom behaviour on Camera2 API
                camera_controller.setZoom(new_zoom_factor);
                applicationInterface.setZoomPref(new_zoom_factor);
                clearFocusAreas();
            }
        }
    }

    public void setFocusDistance(float new_focus_distance, boolean is_target_distance) {
        if (MyDebug.LOG) {
            Log.d(TAG, "setFocusDistance: " + new_focus_distance);
            Log.d(TAG, "is_target_distance: " + is_target_distance);
        }
        if (camera_controller != null) {
            if (new_focus_distance < 0.0f)
                new_focus_distance = 0.0f;
            else if (new_focus_distance > minimum_focus_distance)
                new_focus_distance = minimum_focus_distance;
            boolean focus_changed = false;
            if (is_target_distance) {
                focus_changed = true;
                camera_controller.setFocusBracketingTargetDistance(new_focus_distance);
                // also set the focus distance, so the user can see what the target distance looks like
                camera_controller.setFocusDistance(new_focus_distance);
            } else if (camera_controller.setFocusDistance(new_focus_distance)) {
                focus_changed = true;
                camera_controller.setFocusBracketingSourceDistance(new_focus_distance);
            }

            if (focus_changed) {
                // now save
                applicationInterface.setFocusDistancePref(new_focus_distance, is_target_distance);
                {
                    String focus_distance_s;
                    if (new_focus_distance > 0.0f) {
                        float real_focus_distance = 1.0f / new_focus_distance;
                        focus_distance_s = decimal_format_2dp.format(real_focus_distance) + getResources().getString(R.string.metres_abbreviation);
                    } else {
                        focus_distance_s = getResources().getString(R.string.infinite);
                    }
                    int id = R.string.focus_distance;
                    if (this.supports_focus_bracketing && applicationInterface.isFocusBracketingPref())
                        id = is_target_distance ? R.string.focus_bracketing_target_distance : R.string.focus_bracketing_source_distance;
                }
            }
        }
    }

    public void setExposure(int new_exposure) {
        if (MyDebug.LOG)
            Log.d(TAG, "setExposure(): " + new_exposure);
        if (camera_controller != null && (min_exposure != 0 || max_exposure != 0)) {
            cancelAutoFocus();
            if (new_exposure < min_exposure)
                new_exposure = min_exposure;
            else if (new_exposure > max_exposure)
                new_exposure = max_exposure;
            if (camera_controller.setExposureCompensation(new_exposure)) {
                // now save
                applicationInterface.setExposureCompensationPref(new_exposure);
            }
        }
    }

    public int parseManualISOValue(String value) {
        int iso;
        try {
            if (MyDebug.LOG)
                Log.d(TAG, "setting manual iso");
            iso = Integer.parseInt(value);
            if (MyDebug.LOG)
                Log.d(TAG, "iso: " + iso);
        } catch (NumberFormatException exception) {
            if (MyDebug.LOG)
                Log.d(TAG, "iso invalid format, can't parse to int");
            iso = -1;
        }
        return iso;
    }

    public void setISO(int new_iso) {
        if (MyDebug.LOG)
            Log.d(TAG, "setISO(): " + new_iso);
        if (camera_controller != null && supports_iso_range) {
            if (new_iso < min_iso)
                new_iso = min_iso;
            else if (new_iso > max_iso)
                new_iso = max_iso;
            if (camera_controller.setISO(new_iso)) {
                // now save
                applicationInterface.setISOPref("" + new_iso);
            }
        }
    }


    public boolean canSwitchCamera() {
        if (this.phase == PHASE_TAKING_PHOTO) {
            // just to be safe - risk of cancelling the autofocus before taking a photo, or otherwise messing things up
            if (MyDebug.LOG)
                Log.d(TAG, "currently taking a photo");
            return false;
        }
        int n_cameras = camera_controller_manager.getNumberOfCameras();
        if (MyDebug.LOG)
            Log.d(TAG, "found " + n_cameras + " cameras");
        if (n_cameras == 0)
            return false;
        return true;
    }

    public void setCamera(int cameraId) {
        if (MyDebug.LOG)
            Log.d(TAG, "setCamera(): " + cameraId);
        if (cameraId < 0 || cameraId >= camera_controller_manager.getNumberOfCameras()) {
            if (MyDebug.LOG)
                Log.d(TAG, "invalid cameraId: " + cameraId);
            cameraId = 0;
        }
        if (camera_open_state == CameraOpenState.CAMERAOPENSTATE_OPENING) {
            if (MyDebug.LOG)
                Log.d(TAG, "already opening camera in background thread");
            return;
        }
        if (canSwitchCamera()) {
            final int cameraId_f = cameraId;
            closeCamera(true, new CloseCameraCallback() {
                @Override
                public void onClosed() {
                    if (MyDebug.LOG)
                        Log.d(TAG, "CloseCameraCallback.onClosed");
                    applicationInterface.setCameraIdPref(cameraId_f);
                    openCamera();
                }
            });
        }
    }

    public static int[] chooseBestPreviewFps(List<int[]> fps_ranges) {
        if (MyDebug.LOG)
            Log.d(TAG, "chooseBestPreviewFps()");

        int selected_min_fps = -1, selected_max_fps = -1;
        for (int[] fps_range : fps_ranges) {
            if (MyDebug.LOG) {
                Log.d(TAG, "    supported fps range: " + fps_range[0] + " to " + fps_range[1]);
            }
            int min_fps = fps_range[0];
            int max_fps = fps_range[1];
            if (max_fps >= 30000) {
                if (selected_min_fps == -1 || min_fps < selected_min_fps) {
                    selected_min_fps = min_fps;
                    selected_max_fps = max_fps;
                } else if (min_fps == selected_min_fps && max_fps > selected_max_fps) {
                    selected_min_fps = min_fps;
                    selected_max_fps = max_fps;
                }
            }
        }

        if (selected_min_fps != -1) {
            if (MyDebug.LOG) {
                Log.d(TAG, "    chosen fps range: " + selected_min_fps + " to " + selected_max_fps);
            }
        } else {
            // just pick the widest range; if more than one, pick the one with highest max
            int selected_diff = -1;
            for (int[] fps_range : fps_ranges) {
                int min_fps = fps_range[0];
                int max_fps = fps_range[1];
                int diff = max_fps - min_fps;
                if (selected_diff == -1 || diff > selected_diff) {
                    selected_min_fps = min_fps;
                    selected_max_fps = max_fps;
                    selected_diff = diff;
                } else if (diff == selected_diff && max_fps > selected_max_fps) {
                    selected_min_fps = min_fps;
                    selected_max_fps = max_fps;
                    selected_diff = diff;
                }
            }
            if (MyDebug.LOG)
                Log.d(TAG, "    can't find fps range 30fps or better, so picked widest range: " + selected_min_fps + " to " + selected_max_fps);
        }
        return new int[]{selected_min_fps, selected_max_fps};
    }

    private void setPreviewFps() {
        if (MyDebug.LOG)
            Log.d(TAG, "setPreviewFps()");
        List<int[]> fps_ranges = camera_controller.getSupportedPreviewFpsRange();
        if (fps_ranges == null || fps_ranges.size() == 0) {
            if (MyDebug.LOG)
                Log.d(TAG, "fps_ranges not available");
            return;
        }
        int[] selected_fps = null;
        {
            if (using_android_l) {
                if (MyDebug.LOG)
                    Log.d(TAG, "don't set preview fps for camera2 and photo");
            } else {
                selected_fps = chooseBestPreviewFps(fps_ranges);
            }
        }
        if (selected_fps != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "set preview fps range: " + Arrays.toString(selected_fps));
            camera_controller.setPreviewFpsRange(selected_fps[0], selected_fps[1]);
        } else if (using_android_l) {
            camera_controller.clearPreviewFpsRange();
        }
    }

    private void setFocusPref(boolean auto_focus) {
        if (MyDebug.LOG)
            Log.d(TAG, "setFocusPref()");
        String focus_value = applicationInterface.getFocusPref(false);
        if (focus_value.length() > 0) {
            if (MyDebug.LOG)
                Log.d(TAG, "found existing focus_value: " + focus_value);
            if (!updateFocus(focus_value, true, false, auto_focus)) { // don't need to save, as this is the value that's already saved
                if (MyDebug.LOG)
                    Log.d(TAG, "focus value no longer supported!");
                updateFocus(0, true, true, auto_focus);
            }
        } else {
            updateFocus("focus_mode_continuous_picture", true, true, auto_focus);
        }
    }

    private boolean updateFlash(String flash_value, boolean save) {
        if (MyDebug.LOG)
            Log.d(TAG, "updateFlash(): " + flash_value);
        if (supported_flash_values != null) {
            int new_flash_index = supported_flash_values.indexOf(flash_value);
            if (MyDebug.LOG)
                Log.d(TAG, "new_flash_index: " + new_flash_index);
            if (new_flash_index != -1) {
                updateFlash(new_flash_index, save);
                return true;
            }
        }
        return false;
    }

    public void cycleFlash(boolean skip_torch, boolean save) {
        if (MyDebug.LOG)
            Log.d(TAG, "cycleFlash()");
        if (supported_flash_values != null) {
            int new_flash_index = (current_flash_index + 1) % supported_flash_values.size();
            if (supported_flash_values.get(new_flash_index).equals("flash_torch")) {
                if (MyDebug.LOG)
                    Log.d(TAG, "cycle past torch");
                new_flash_index = (new_flash_index + 1) % supported_flash_values.size();
            }
            updateFlash(new_flash_index, save);
        }
    }

    private void updateFlash(int new_flash_index, boolean save) {
        if (MyDebug.LOG)
            Log.d(TAG, "updateFlash(): " + new_flash_index);
        if (supported_flash_values != null && new_flash_index != current_flash_index) {
            boolean initial = current_flash_index == -1;
            current_flash_index = new_flash_index;
            if (MyDebug.LOG)
                Log.d(TAG, "    current_flash_index is now " + current_flash_index + " (initial " + initial + ")");

            String flash_value = supported_flash_values.get(current_flash_index);
            String[] flash_values = getResources().getStringArray(R.array.flash_values);
            for (int i = 0; i < flash_values.length; i++) {
                if (flash_value.equals(flash_values[i])) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "    found entry: " + i);
                    break;
                }
            }
            this.setFlash(flash_value);
            if (save) {
                applicationInterface.setFlashPref(flash_value);
            }
        }
    }

    private void setFlash(String flash_value) {
        if (MyDebug.LOG)
            Log.d(TAG, "setFlash() " + flash_value);
        set_flash_value_after_autofocus = ""; // this overrides any previously saved setting, for during the startup autofocus
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }
        cancelAutoFocus();
        camera_controller.setFlashValue(flash_value);
    }

    // this returns the flash value indicated by the UI, rather than from the camera parameters (may be different, e.g., in startup autofocus!)
    public String getCurrentFlashValue() {
        if (this.current_flash_index == -1)
            return null;
        return this.supported_flash_values.get(current_flash_index);
    }

    private boolean supportedFocusValue(String focus_value) {
        if (MyDebug.LOG)
            Log.d(TAG, "supportedFocusValue(): " + focus_value);
        if (this.supported_focus_values != null) {
            int new_focus_index = supported_focus_values.indexOf(focus_value);
            if (MyDebug.LOG)
                Log.d(TAG, "new_focus_index: " + new_focus_index);
            return new_focus_index != -1;
        }
        return false;
    }

    private boolean updateFocus(String focus_value, boolean quiet, boolean save, boolean auto_focus) {
        if (MyDebug.LOG)
            Log.d(TAG, "updateFocus(): " + focus_value);
        if (this.supported_focus_values != null) {
            int new_focus_index = supported_focus_values.indexOf(focus_value);
            if (MyDebug.LOG)
                Log.d(TAG, "new_focus_index: " + new_focus_index);
            if (new_focus_index != -1) {
                updateFocus(new_focus_index, quiet, save, auto_focus);
                return true;
            }
        }
        return false;
    }

    private void updateFocus(int new_focus_index, boolean quiet, boolean save, boolean auto_focus) {
        if (MyDebug.LOG)
            Log.d(TAG, "updateFocus(): " + new_focus_index + " current_focus_index: " + current_focus_index);
        // updates the Focus button, and Focus camera mode
        if (this.supported_focus_values != null && new_focus_index != current_focus_index) {
            current_focus_index = new_focus_index;
            if (MyDebug.LOG)
                Log.d(TAG, "    current_focus_index is now " + current_focus_index);

            String focus_value = supported_focus_values.get(current_focus_index);
            if (MyDebug.LOG)
                Log.d(TAG, "    focus_value: " + focus_value);
            this.setFocusValue(focus_value, auto_focus);

            if (save) {
                // now save
                applicationInterface.setFocusPref(focus_value, false);
            }
        }
    }

    public String getCurrentFocusValue() {
        if (MyDebug.LOG)
            Log.d(TAG, "getCurrentFocusValue()");
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return null;
        }
        if (this.supported_focus_values != null && this.current_focus_index != -1)
            return this.supported_focus_values.get(current_focus_index);
        return null;
    }

    private void setFocusValue(String focus_value, boolean auto_focus) {
        if (MyDebug.LOG)
            Log.d(TAG, "setFocusValue() " + focus_value);
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            return;
        }
        cancelAutoFocus();
        removePendingContinuousFocusReset(); // this isn't strictly needed as the reset_continuous_focus_runnable will check the ui focus mode when it runs, but good to remove it anyway
        autofocus_in_continuous_mode = false;
        camera_controller.setFocusValue(focus_value);
        setupContinuousFocusMove();
        clearFocusAreas();
        if (auto_focus && !focus_value.equals("focus_mode_locked")) {
            tryAutoFocus(false, false);
        }
    }

    private void setupContinuousFocusMove() {
        if (MyDebug.LOG)
            Log.d(TAG, "setupContinuousFocusMove()");
        if (continuous_focus_move_is_started) {
            continuous_focus_move_is_started = false;
            applicationInterface.onContinuousFocusMove(false);
        }
        String focus_value = current_focus_index != -1 ? supported_focus_values.get(current_focus_index) : null;
        if (MyDebug.LOG)
            Log.d(TAG, "focus_value is " + focus_value);
        if (camera_controller != null && focus_value != null && focus_value.equals("focus_mode_continuous_picture")) {
            if (MyDebug.LOG)
                Log.d(TAG, "set continuous picture focus move callback");
            camera_controller.setContinuousFocusMoveCallback(new CameraController.ContinuousFocusMoveCallback() {
                @Override
                public void onContinuousFocusMove(boolean start) {
                    if (start != continuous_focus_move_is_started) { // filter out repeated calls with same start value
                        continuous_focus_move_is_started = start;
                        count_cameraContinuousFocusMoving++;
                        applicationInterface.onContinuousFocusMove(start);
                    }
                }
            });
        } else if (camera_controller != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "remove continuous picture focus move callback");
            camera_controller.setContinuousFocusMoveCallback(null);
        }
    }

    public void takePicturePressed(boolean photo_snapshot, boolean continuous_fast_burst) {
        Log.d("CAMERA_TRACKING","takePicturePressed4");
        if (camera_controller == null) {
            Log.d("CAMERA_TRACKING","null controller");

            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            this.phase = PHASE_NORMAL;
            applicationInterface.onControllerNull();
            PSR_Utils.hideProgressDialog();
            return;
        }
        Log.d("CAMERA_TRACKING","takePicturePressed5");
        if (!this.has_surface) {
            if (MyDebug.LOG)
                Log.d(TAG, "preview surface not yet available");
            this.phase = PHASE_NORMAL;
            return;
        }
        Log.d("CAMERA_TRACKING","takePicturePressed6");
        if (this.isOnTimer()) {
            cancelTimer();
            return;
        }
        Log.d("CAMERA_TRACKING","takePicturePressed7");
        if (this.phase == PHASE_TAKING_PHOTO) {
            // user requested take photo while already taking photo
            if (MyDebug.LOG)
                Log.d(TAG, "already taking a photo");
            if (remaining_repeat_photos != 0) {
                cancelRepeat();
            } else if (camera_controller.getBurstType() == CameraController.BurstType.BURSTTYPE_FOCUS && camera_controller.isCapturingBurst()) {
                camera_controller.stopFocusBracketingBurst();
            }
            return;
        }
        // check it's okay to take a photo
        Log.d("CAMERA_TRACKING","takePicturePressed8");
        if (!applicationInterface.canTakeNewPhoto()) {
            if (MyDebug.LOG)
                Log.d(TAG, "don't take another photo, queue is full");
            return;
        }
        // make sure that preview running (also needed to hide trash/share icons)
        this.startCameraPreview();

        if (photo_snapshot || continuous_fast_burst) {
            // go straight to taking a photo, ignore timer or repeat options
            takePicture( continuous_fast_burst);
            return;
        }

        long timer_delay = applicationInterface.getTimerPref();

        String repeat_mode_value = applicationInterface.getRepeatPref();
        if (repeat_mode_value.equals("unlimited")) {
            if (MyDebug.LOG)
                Log.d(TAG, "unlimited repeat");
            remaining_repeat_photos = -1;
        } else {
            int n_repeat;
            try {
                n_repeat = Integer.parseInt(repeat_mode_value);
                if (MyDebug.LOG)
                    Log.d(TAG, "n_repeat: " + n_repeat);
            } catch (NumberFormatException e) {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to parse repeat_mode value: " + repeat_mode_value);
                e.printStackTrace();
                n_repeat = 1;
            }
            remaining_repeat_photos = n_repeat - 1;
        }
        Log.d("CAMERA_TRACKING","takePicturePressed9");

        if (timer_delay == 0) {
            Log.d("CAMERA_TRACKING","takePicturePressed10");
            takePicture( continuous_fast_burst);
        } else {
            Log.d("CAMERA_TRACKING","takePicturePressed11");
            takePictureOnTimer(timer_delay, false);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "takePicturePressed exit");
    }

    private void takePictureOnTimer(final long timer_delay, boolean repeated) {
        if (MyDebug.LOG) {
            Log.d(TAG, "takePictureOnTimer");
            Log.d(TAG, "timer_delay: " + timer_delay);
        }
        this.phase = PHASE_TIMER;
        class TakePictureTimerTask extends TimerTask {
            public void run() {
                if (beepTimerTask != null) {
                    beepTimerTask.cancel();
                    beepTimerTask = null;
                }
                Activity activity = (Activity) Preview.this.getContext();
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        // we run on main thread to avoid problem of camera closing at the same time
                        // but still need to check that the camera hasn't closed or the task halted, since TimerTask.run() started
                        if (camera_controller != null && takePictureTimerTask != null)
                            takePicture(false);
                        else {
                            if (MyDebug.LOG)
                                Log.d(TAG, "takePictureTimerTask: don't take picture, as already cancelled");
                        }
                    }
                });
            }
        }
        take_photo_time = System.currentTimeMillis() + timer_delay;
        if (MyDebug.LOG)
            Log.d(TAG, "take photo at: " + take_photo_time);
        takePictureTimer.schedule(takePictureTimerTask = new TakePictureTimerTask(), timer_delay);

        class BeepTimerTask extends TimerTask {
            long remaining_time = timer_delay;

            public void run() {
                if (remaining_time > 0) { // check in case this isn't cancelled by time we take the photo
                    applicationInterface.timerBeep(remaining_time);
                }
                remaining_time -= 1000;
            }
        }
        beepTimer.schedule(beepTimerTask = new BeepTimerTask(), 0, 1000);
    }

    private void takePicture(boolean continuous_fast_burst) {
        if (MyDebug.LOG)
            Log.d(TAG, "takePicture");
        //this.thumbnail_anim = false;
        this.phase = PHASE_TAKING_PHOTO;
        synchronized (this) {
            // synchronise for consistency (keep FindBugs happy)
            take_photo_after_autofocus = false;
        }
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            this.phase = PHASE_NORMAL;
            applicationInterface.cameraInOperation(false, false);
            return;
        }
        if (!this.has_surface) {
            if (MyDebug.LOG)
                Log.d(TAG, "preview surface not yet available");
            this.phase = PHASE_NORMAL;
            applicationInterface.cameraInOperation(false, false);
            return;
        }

        boolean store_location = applicationInterface.getGeotaggingPref();
        if (store_location) {
            boolean require_location = applicationInterface.getRequireLocationPref();
            if (require_location) {
                if (applicationInterface.getLocation() != null) {
                    // fine, we have location
                } else {
                    this.phase = PHASE_NORMAL;
                    applicationInterface.cameraInOperation(false, false);
                    return;
                }
            }
        }
        takePhoto(false, continuous_fast_burst);
        if (MyDebug.LOG)
            Log.d(TAG, "takePicture exit");
    }

    private void takePhoto(boolean skip_autofocus, final boolean continuous_fast_burst) {
        if (MyDebug.LOG)
            Log.d(TAG, "takePhoto");
        if (camera_controller == null) {
            Log.e(TAG, "camera not opened in takePhoto!");
            return;
        }
        applicationInterface.cameraInOperation(true, false);
        String current_ui_focus_value = getCurrentFocusValue();
        if (MyDebug.LOG)
            Log.d(TAG, "current_ui_focus_value is " + current_ui_focus_value);

        if (autofocus_in_continuous_mode) {
            if (MyDebug.LOG)
                Log.d(TAG, "continuous mode where user touched to focus");

            boolean wait_for_focus;

            synchronized (this) {
                // as below, if an autofocus is in progress, then take photo when it's completed
                if (focus_success == FOCUS_WAITING) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "autofocus_in_continuous_mode: take photo after current focus");
                    wait_for_focus = true;
                    take_photo_after_autofocus = true;
                } else {
                    // when autofocus_in_continuous_mode==true, it means the user recently touched to focus in continuous focus mode, so don't do another focus
                    if (MyDebug.LOG)
                        Log.d(TAG, "autofocus_in_continuous_mode: no need to refocus");
                    wait_for_focus = false;
                }
            }

            // call CameraController outside the lock
            if (wait_for_focus) {
                camera_controller.setCaptureFollowAutofocusHint(true);
            } else {
                takePhotoWhenFocused(continuous_fast_burst);
            }
        } else if (camera_controller.focusIsContinuous()) {
            if (MyDebug.LOG)
                Log.d(TAG, "call autofocus for continuous focus mode");
            // we call via autoFocus(), to avoid risk of taking photo while the continuous focus is focusing - risk of blurred photo, also sometimes get bug in such situations where we end of repeatedly focusing
            // this is the case even if skip_autofocus is true (as we still can't guarantee that continuous focusing might be occurring)
            // note: if the user touches to focus in continuous mode, we camera controller may be in auto focus mode, so we should only enter this codepath if the camera_controller is in continuous focus mode
            CameraController.AutoFocusCallback autoFocusCallback = new CameraController.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "continuous mode autofocus complete: " + success);
                    takePhotoWhenFocused(continuous_fast_burst);
                }
            };
            camera_controller.autoFocus(autoFocusCallback, true);
        } else if (skip_autofocus || this.recentlyFocused()) {
            if (MyDebug.LOG) {
                if (skip_autofocus) {
                    Log.d(TAG, "skip_autofocus flag set");
                } else {
                    Log.d(TAG, "recently focused successfully, so no need to refocus");
                }
            }
            takePhotoWhenFocused(continuous_fast_burst);
        } else if (current_ui_focus_value != null && (current_ui_focus_value.equals("focus_mode_auto") || current_ui_focus_value.equals("focus_mode_macro"))) {
            boolean wait_for_focus;
            // n.b., we check focus_value rather than camera_controller.supportsAutoFocus(), as we want to discount focus_mode_locked
            synchronized (this) {
                if (focus_success == FOCUS_WAITING) {
                    // Needed to fix bug (on Nexus 6, old camera API): if flash was on, pointing at a dark scene, and we take photo when already autofocusing, the autofocus never returned so we got stuck!
                    // In general, probably a good idea to not redo a focus - just use the one that's already in progress
                    if (MyDebug.LOG)
                        Log.d(TAG, "take photo after current focus");
                    wait_for_focus = true;
                    take_photo_after_autofocus = true;
                } else {
                    wait_for_focus = false;
                    focus_success = FOCUS_DONE; // clear focus rectangle for new refocus
                }
            }

            // call CameraController outside the lock
            if (wait_for_focus) {
                camera_controller.setCaptureFollowAutofocusHint(true);
            } else {
                CameraController.AutoFocusCallback autoFocusCallback = new CameraController.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "autofocus complete: " + success);
                        ensureFlashCorrect(); // need to call this in case user takes picture before startup focus completes!
                        prepareAutoFocusPhoto();
                        takePhotoWhenFocused(continuous_fast_burst);
                    }
                };
                if (MyDebug.LOG)
                    Log.d(TAG, "start autofocus to take picture");
                camera_controller.autoFocus(autoFocusCallback, true);
                count_cameraAutoFocus++;
            }
        } else {
            takePhotoWhenFocused(continuous_fast_burst);
        }
    }

    private void prepareAutoFocusPhoto() {
        if (MyDebug.LOG)
            Log.d(TAG, "prepareAutoFocusPhoto");
        if (using_android_l) {
            String flash_value = camera_controller.getFlashValue();
            // getFlashValue() may return "" if flash not supported!
            if (flash_value.length() > 0 && (flash_value.equals("flash_auto") || flash_value.equals("flash_red_eye"))) {
                if (MyDebug.LOG)
                    Log.d(TAG, "wait for a bit...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void takePhotoWhenFocused(boolean continuous_fast_burst) {
        // should be called when auto-focused
        if (MyDebug.LOG)
            Log.d(TAG, "takePhotoWhenFocused");
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
            this.phase = PHASE_NORMAL;
            applicationInterface.cameraInOperation(false, false);
            return;
        }
        if (!this.has_surface) {
            if (MyDebug.LOG)
                Log.d(TAG, "preview surface not yet available");
            this.phase = PHASE_NORMAL;
            applicationInterface.cameraInOperation(false, false);
            return;
        }

        final String focus_value = current_focus_index != -1 ? supported_focus_values.get(current_focus_index) : null;
        if (MyDebug.LOG) {
            Log.d(TAG, "focus_value is " + focus_value);
            Log.d(TAG, "focus_success is " + focus_success);
        }

        if (focus_value != null && focus_value.equals("focus_mode_locked") && focus_success == FOCUS_WAITING) {
            cancelAutoFocus();
        }
        removePendingContinuousFocusReset(); // to avoid switching back to continuous focus mode while taking a photo - instead we'll always make sure we switch back after taking a photo
        updateParametersFromLocation(); // do this now, not before, so we don't set location parameters during focus (sometimes get RuntimeException)

        focus_success = FOCUS_DONE; // clear focus rectangle if not already done
        successfully_focused = false; // so next photo taken will require an autofocus
        if (MyDebug.LOG)
            Log.d(TAG, "remaining_repeat_photos: " + remaining_repeat_photos);

        CameraController.PictureCallback pictureCallback = new CameraController.PictureCallback() {
            private boolean success = false; // whether jpeg callback succeeded
            private boolean has_date = false;
            private Date current_date = null;

            public void onStarted() {
                if (MyDebug.LOG)
                    Log.d(TAG, "onStarted");
                applicationInterface.onCaptureStarted();
            }

            public void onCompleted() {
                if (MyDebug.LOG)
                    Log.d(TAG, "onCompleted");
                applicationInterface.onPictureCompleted();
                if (!using_android_l) {
                    is_preview_started = false; // preview automatically stopped due to taking photo on original Camera API
                }
                phase = PHASE_NORMAL; // need to set this even if remaining repeat photos, so we can restart the preview
                if (remaining_repeat_photos == -1 || remaining_repeat_photos > 0) {
                    if (!is_preview_started) {
                        startCameraPreview();
                        if (MyDebug.LOG)
                            Log.d(TAG, "repeat mode photos remaining: onPictureTaken started preview: " + remaining_repeat_photos);
                    }
                    applicationInterface.cameraInOperation(false, false);
                } else {
                    phase = PHASE_NORMAL;
                    boolean pause_preview = applicationInterface.getPausePreviewPref();
                    if (MyDebug.LOG)
                        Log.d(TAG, "pause_preview? " + pause_preview);
                    if (pause_preview && success) {
                        if (is_preview_started) {
                            if (camera_controller != null) {
                                camera_controller.stopPreview();
                            }
                            is_preview_started = false;
                        }
                        setPreviewPaused(true);
                    } else {
                        if (!is_preview_started) {
                            startCameraPreview();
                        }
                        applicationInterface.cameraInOperation(false, false);
                        if (MyDebug.LOG)
                            Log.d(TAG, "onPictureTaken started preview");
                    }
                }
                continuousFocusReset(); // in case we took a photo after user had touched to focus (causing us to switch from continuous to autofocus mode)
                if (camera_controller != null && focus_value != null && (focus_value.equals("focus_mode_continuous_picture") || focus_value.equals("focus_mode_continuous_video"))) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "cancelAutoFocus to restart continuous focusing");
                    camera_controller.cancelAutoFocus(); // needed to restart continuous focusing
                }

                if (camera_controller != null && camera_controller.getBurstType() == CameraController.BurstType.BURSTTYPE_CONTINUOUS) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "continuous burst mode ended, so revert to standard mode");
                    setupBurstMode();
                }

                if (MyDebug.LOG)
                    Log.d(TAG, "do we need to take another photo? remaining_repeat_photos: " + remaining_repeat_photos);
                if (remaining_repeat_photos == -1 || remaining_repeat_photos > 0) {
                    takeRemainingRepeatPhotos();
                }
            }

            private void initDate() {
                if (!has_date) {
                    has_date = true;
                    current_date = new Date();
                    if (MyDebug.LOG)
                        Log.d(TAG, "picture taken on date: " + current_date);
                }
            }

            public void onPictureTaken(byte[] data) {
                if (MyDebug.LOG)
                    Log.d(TAG, "onPictureTaken");
                initDate();
                if (!applicationInterface.onPictureTaken(data, current_date)) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "applicationInterface.onPictureTaken failed");
                    success = false;
                } else {
                    success = true;
                }
            }

            public void onRawPictureTaken(RawImage raw_image) {
                if (MyDebug.LOG)
                    Log.d(TAG, "onRawPictureTaken");
                initDate();
                if (!applicationInterface.onRawPictureTaken(raw_image, current_date)) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "applicationInterface.onRawPictureTaken failed");
                }
            }

            public void onBurstPictureTaken(List<byte[]> images) {
                if (MyDebug.LOG)
                    Log.d(TAG, "onBurstPictureTaken");
                initDate();

                success = true;
                if (!applicationInterface.onBurstPictureTaken(images, current_date)) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "applicationInterface.onBurstPictureTaken failed");
                    success = false;
                }
            }

            public void onRawBurstPictureTaken(List<RawImage> raw_images) {
                if (MyDebug.LOG)
                    Log.d(TAG, "onRawBurstPictureTaken");
                initDate();

                if (!applicationInterface.onRawBurstPictureTaken(raw_images, current_date)) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "applicationInterface.onRawBurstPictureTaken failed");
                }
            }

            public boolean imageQueueWouldBlock(int n_raw, int n_jpegs) {
                if (MyDebug.LOG)
                    Log.d(TAG, "imageQueueWouldBlock");
                return applicationInterface.imageQueueWouldBlock(n_raw, n_jpegs);
            }

            public void onFrontScreenTurnOn() {
                if (MyDebug.LOG)
                    Log.d(TAG, "onFrontScreenTurnOn");
                applicationInterface.turnFrontScreenFlashOn();
            }
        };
        CameraController.ErrorCallback errorCallback = new CameraController.ErrorCallback() {
            public void onError() {
                if (MyDebug.LOG)
                    Log.e(TAG, "error from takePicture");
                count_cameraTakePicture--; // cancel out the increment from after the takePicture() call
                if (MyDebug.LOG) {
                    Log.d(TAG, "count_cameraTakePicture is now: " + count_cameraTakePicture);
                }
                applicationInterface.onPhotoError();
                phase = PHASE_NORMAL;
                startCameraPreview();
                applicationInterface.cameraInOperation(false, false);
            }
        };
        {
            camera_controller.setRotation(getImageVideoRotation());

            boolean enable_sound = applicationInterface.getShutterSoundPref();
            if (MyDebug.LOG)
                Log.d(TAG, "enable_sound? " + enable_sound);
            camera_controller.enableShutterSound(enable_sound);
            if (using_android_l) {
                boolean use_camera2_fast_burst = applicationInterface.useCamera2FastBurst();
                if (MyDebug.LOG)
                    Log.d(TAG, "use_camera2_fast_burst? " + use_camera2_fast_burst);
                camera_controller.setUseExpoFastBurst(use_camera2_fast_burst);
            }
            if (continuous_fast_burst) {
                camera_controller.setBurstType(CameraController.BurstType.BURSTTYPE_CONTINUOUS);
            }

            if (MyDebug.LOG)
                Log.d(TAG, "about to call takePicture");
            camera_controller.takePicture(pictureCallback, errorCallback);
            count_cameraTakePicture++;
            if (MyDebug.LOG) {
                Log.d(TAG, "count_cameraTakePicture is now: " + count_cameraTakePicture);
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "takePhotoWhenFocused exit");
    }

    private void takeRemainingRepeatPhotos() {
        if (MyDebug.LOG)
            Log.d(TAG, "takeRemainingRepeatPhotos");
        if (remaining_repeat_photos == -1 || remaining_repeat_photos > 0) {
            if (camera_controller == null) {
                Log.e(TAG, "remaining_repeat_photos still set, but camera is closed!: " + remaining_repeat_photos);
                cancelRepeat();
            } else {
                // check it's okay to take a photo
                if (!applicationInterface.canTakeNewPhoto()) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "takeRemainingRepeatPhotos: still processing...");
                    // wait a bit then check again
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (MyDebug.LOG)
                                Log.d(TAG, "takeRemainingRepeatPhotos: check again from post delayed runnable");
                            takeRemainingRepeatPhotos();
                        }
                    }, 500);
                    return;
                }

                if (remaining_repeat_photos > 0)
                    remaining_repeat_photos--;
                if (MyDebug.LOG)
                    Log.d(TAG, "takeRemainingRepeatPhotos: remaining_repeat_photos is now: " + remaining_repeat_photos);

                long timer_delay = applicationInterface.getRepeatIntervalPref();
                if (timer_delay == 0) {
                    // we set skip_autofocus to go straight to taking a photo rather than refocusing, for speed
                    // need to manually set the phase
                    phase = PHASE_TAKING_PHOTO;
                    takePhoto(true, false);
                } else {
                    takePictureOnTimer(timer_delay, true);
                }
            }
        }
    }

    public void requestAutoFocus() {
        if (MyDebug.LOG)
            Log.d(TAG, "requestAutoFocus");
        cancelAutoFocus();
        tryAutoFocus(false, true);
    }

    private void tryAutoFocus(final boolean startup, final boolean manual) {
        // manual: whether user has requested autofocus (e.g., by touching screen, or volume focus, or hardware focus button)
        // consider whether you want to call requestAutoFocus() instead (which properly cancels any in-progress auto-focus first)
        if (MyDebug.LOG) {
            Log.d(TAG, "tryAutoFocus");
            Log.d(TAG, "startup? " + startup);
            Log.d(TAG, "manual? " + manual);
        }
        if (camera_controller == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "camera not opened!");
        } else if (!this.has_surface) {
            if (MyDebug.LOG)
                Log.d(TAG, "preview surface not yet available");
        } else if (!this.is_preview_started) {
            if (MyDebug.LOG)
                Log.d(TAG, "preview not yet started");
        } else if (this.isTakingPhotoOrOnTimer()) {
            // if taking a video, we allow manual autofocuses
            // autofocus may cause problem if there is a video corruption problem, see testTakeVideoBitrate() on Nexus 7 at 30Mbs or 50Mbs, where the startup autofocus would cause a problem here
            if (MyDebug.LOG)
                Log.d(TAG, "currently taking a photo");
        } else {
            if (manual) {
                // remove any previous request to switch back to continuous
                removePendingContinuousFocusReset();
            }
            if (manual && camera_controller.focusIsContinuous() && supportedFocusValue("focus_mode_auto")) {
                if (MyDebug.LOG)
                    Log.d(TAG, "switch from continuous to autofocus mode for touch focus");
                camera_controller.setFocusValue("focus_mode_auto"); // switch to autofocus
                autofocus_in_continuous_mode = true;
                // we switch back to continuous via a new reset_continuous_focus_runnable in autoFocusCompleted()
            }
            if (camera_controller.supportsAutoFocus()) {
                if (MyDebug.LOG)
                    Log.d(TAG, "try to start autofocus");
                if (!using_android_l) {
                    set_flash_value_after_autofocus = "";
                    String old_flash_value = camera_controller.getFlashValue();
                    // getFlashValue() may return "" if flash not supported!
                    if (startup && old_flash_value.length() > 0 && !old_flash_value.equals("flash_off") && !old_flash_value.equals("flash_torch")) {
                        set_flash_value_after_autofocus = old_flash_value;
                        camera_controller.setFlashValue("flash_off");
                    }
                    if (MyDebug.LOG)
                        Log.d(TAG, "set_flash_value_after_autofocus is now: " + set_flash_value_after_autofocus);
                }
                CameraController.AutoFocusCallback autoFocusCallback = new CameraController.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "autofocus complete: " + success);
                        autoFocusCompleted(manual, success, false);
                    }
                };

                this.focus_success = FOCUS_WAITING;
                if (MyDebug.LOG)
                    Log.d(TAG, "set focus_success to " + focus_success);
                this.focus_complete_time = -1;
                this.successfully_focused = false;
                camera_controller.autoFocus(autoFocusCallback, false);
                count_cameraAutoFocus++;
                this.focus_started_time = System.currentTimeMillis();
                if (MyDebug.LOG)
                    Log.d(TAG, "autofocus started, count now: " + count_cameraAutoFocus);
            } else if (has_focus_area) {
                // do this so we get the focus box, for focus modes that support focus area, but don't support autofocus
                focus_success = FOCUS_SUCCESS;
                focus_complete_time = System.currentTimeMillis();
                // n.b., don't set focus_started_time as that may be used for application to show autofocus animation
            }
        }
    }


    private void removePendingContinuousFocusReset() {
        if (MyDebug.LOG)
            Log.d(TAG, "removePendingContinuousFocusReset");
        if (reset_continuous_focus_runnable != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "remove pending reset_continuous_focus_runnable");
            reset_continuous_focus_handler.removeCallbacks(reset_continuous_focus_runnable);
            reset_continuous_focus_runnable = null;
        }
    }
    private void continuousFocusReset() {
        if (MyDebug.LOG)
            Log.d(TAG, "switch back to continuous focus after autofocus?");
        if (camera_controller != null && autofocus_in_continuous_mode) {
            autofocus_in_continuous_mode = false;
            // check again
            String current_ui_focus_value = getCurrentFocusValue();
            if (current_ui_focus_value != null && !camera_controller.getFocusValue().equals(current_ui_focus_value) && camera_controller.getFocusValue().equals("focus_mode_auto")) {
                camera_controller.cancelAutoFocus();
                camera_controller.setFocusValue(current_ui_focus_value);
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "no need to switch back to continuous focus after autofocus, mode already changed");
            }
        }
    }

    private void cancelAutoFocus() {
        if (MyDebug.LOG)
            Log.d(TAG, "cancelAutoFocus");
        if (camera_controller != null) {
            camera_controller.cancelAutoFocus();
            autoFocusCompleted(false, false, true);
        }
    }

    private void ensureFlashCorrect() {
        // ensures flash is in correct mode, in case where we had to turn flash temporarily off for startup autofocus
        if (set_flash_value_after_autofocus.length() > 0 && camera_controller != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "set flash back to: " + set_flash_value_after_autofocus);
            camera_controller.setFlashValue(set_flash_value_after_autofocus);
            set_flash_value_after_autofocus = "";
        }
    }

    private void autoFocusCompleted(boolean manual, boolean success, boolean cancelled) {
        if (MyDebug.LOG) {
            Log.d(TAG, "autoFocusCompleted");
            Log.d(TAG, "    manual? " + manual);
            Log.d(TAG, "    success? " + success);
            Log.d(TAG, "    cancelled? " + cancelled);
        }
        if (cancelled) {
            focus_success = FOCUS_DONE;
        } else {
            focus_success = success ? FOCUS_SUCCESS : FOCUS_FAILED;
            focus_complete_time = System.currentTimeMillis();
        }
        if (manual && !cancelled && (success || applicationInterface.isTestAlwaysFocus())) {
            successfully_focused = true;
            successfully_focused_time = focus_complete_time;
        }
        if (manual && camera_controller != null && autofocus_in_continuous_mode) {
            String current_ui_focus_value = getCurrentFocusValue();
            if (MyDebug.LOG)
                Log.d(TAG, "current_ui_focus_value: " + current_ui_focus_value);
            if (current_ui_focus_value != null && !camera_controller.getFocusValue().equals(current_ui_focus_value) && camera_controller.getFocusValue().equals("focus_mode_auto")) {
                reset_continuous_focus_runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (MyDebug.LOG)
                            Log.d(TAG, "reset_continuous_focus_runnable running...");
                        reset_continuous_focus_runnable = null;
                        continuousFocusReset();
                    }
                };
                reset_continuous_focus_handler.postDelayed(reset_continuous_focus_runnable, 3000);
            }
        }
        ensureFlashCorrect();
        if (this.using_face_detection && !cancelled) {
            // On some devices such as mtk6589, face detection does not resume as written in documentation so we have
            // to cancelfocus when focus is finished
            if (camera_controller != null) {
                camera_controller.cancelAutoFocus();
            }
        }

        boolean local_take_photo_after_autofocus = false;
        synchronized (this) {
            local_take_photo_after_autofocus = take_photo_after_autofocus;
            take_photo_after_autofocus = false;
        }
        // call CameraController outside the lock
        if (local_take_photo_after_autofocus) {
            if (MyDebug.LOG)
                Log.d(TAG, "take_photo_after_autofocus is set");
            prepareAutoFocusPhoto();
            takePhotoWhenFocused(false);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "autoFocusCompleted exit");
    }

    public void startCameraPreview() {
        if (camera_controller != null && !this.isTakingPhotoOrOnTimer() && !is_preview_started) {
            if (MyDebug.LOG)
                Log.d(TAG, "starting the camera preview");
            {
                camera_controller.setRecordingHint(false);
            }
            setPreviewFps();
            try {
                camera_controller.startPreview();
                count_cameraStartPreview++;
            } catch (CameraControllerException e) {
                if (MyDebug.LOG)
                    Log.d(TAG, "CameraControllerException trying to startPreview");
                e.printStackTrace();
                applicationInterface.onFailedStartPreview();
                return;
            }
            this.is_preview_started = true;
            if (this.using_face_detection) {
                if (MyDebug.LOG)
                    Log.d(TAG, "start face detection");
                camera_controller.startFaceDetection();
                faces_detected = null;
            }
        }
        this.setPreviewPaused(false);
        this.setupContinuousFocusMove();
    }

    private void setPreviewPaused(boolean paused) {
        if (MyDebug.LOG)
            Log.d(TAG, "setPreviewPaused: " + paused);
        applicationInterface.hasPausedPreview(paused);
        if (paused) {
            this.phase = PHASE_PREVIEW_PAUSED;
        } else {
            this.phase = PHASE_NORMAL;
            applicationInterface.cameraInOperation(false, false);
        }
    }

    public void onAccelerometerSensorChanged(SensorEvent event) {
        this.has_gravity = true;
        for (int i = 0; i < 3; i++) {
            //this.gravity[i] = event.values[i];
            this.gravity[i] = sensor_alpha * this.gravity[i] + (1.0f - sensor_alpha) * event.values[i];
        }
        calculateGeoDirection();

        double x = gravity[0];
        double y = gravity[1];
        double z = gravity[2];
        double mag = Math.sqrt(x * x + y * y + z * z);
		/*if( MyDebug.LOG )
			Log.d(TAG, "xyz: " + x + ", " + y + ", " + z);*/

        this.has_pitch_angle = false;
        if (mag > 1.0e-8) {
            this.has_pitch_angle = true;
            this.pitch_angle = Math.asin(-z / mag) * 180.0 / Math.PI;
			/*if( MyDebug.LOG )
				Log.d(TAG, "pitch: " + pitch_angle);*/

            this.has_level_angle = true;
            this.natural_level_angle = Math.atan2(-x, y) * 180.0 / Math.PI;
            if (this.natural_level_angle < -0.0) {
                this.natural_level_angle += 360.0;
            }

            updateLevelAngles();
        } else {
            Log.e(TAG, "accel sensor has zero mag: " + mag);
            this.has_level_angle = false;
        }

    }

    public void updateLevelAngles() {
        if (has_level_angle) {
            this.level_angle = this.natural_level_angle;
            double calibrated_level_angle = applicationInterface.getCalibratedLevelAngle();
            this.level_angle -= calibrated_level_angle;
            this.orig_level_angle = this.level_angle;
            this.level_angle -= (float) this.current_orientation;
            if (this.level_angle < -180.0) {
                this.level_angle += 360.0;
            } else if (this.level_angle > 180.0) {
                this.level_angle -= 360.0;
            }
        }
    }

    public boolean hasLevelAngle() {
        return this.has_level_angle;
    }

    public boolean hasLevelAngleStable() {
        if (!is_test && has_pitch_angle && Math.abs(pitch_angle) > 70.0) {
            // note that if is_test, we always set the level angle - since the device typically lies face down when running tests...
            return false;
        }
        return this.has_level_angle;
    }

    public double getLevelAngle() {
        return this.level_angle;
    }

    public double getOrigLevelAngle() {
        return this.orig_level_angle;
    }

    public boolean hasPitchAngle() {
        return this.has_pitch_angle;
    }

    public double getPitchAngle() {
        return this.pitch_angle;
    }

    public void onMagneticSensorChanged(SensorEvent event) {
        this.has_geomagnetic = true;
        for (int i = 0; i < 3; i++) {
            //this.geomagnetic[i] = event.values[i];
            this.geomagnetic[i] = sensor_alpha * this.geomagnetic[i] + (1.0f - sensor_alpha) * event.values[i];
        }
        calculateGeoDirection();
    }

    private void calculateGeoDirection() {
        if (!this.has_gravity || !this.has_geomagnetic) {
            return;
        }
        if (!SensorManager.getRotationMatrix(this.deviceRotation, this.deviceInclination, this.gravity, this.geomagnetic)) {
            return;
        }
        SensorManager.remapCoordinateSystem(this.deviceRotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, this.cameraRotation);
        boolean has_old_geo_direction = has_geo_direction;
        this.has_geo_direction = true;
        SensorManager.getOrientation(cameraRotation, new_geo_direction);
        for (int i = 0; i < 3; i++) {
            float old_compass = (float) Math.toDegrees(geo_direction[i]);
            float new_compass = (float) Math.toDegrees(new_geo_direction[i]);
            if (has_old_geo_direction) {
                old_compass = lowPassFilter(old_compass, new_compass, 0.1f, 10.0f);
            } else {
                old_compass = new_compass;
            }
            geo_direction[i] = (float) Math.toRadians(old_compass);
        }
		/*if( MyDebug.LOG ) {
			Log.d(TAG, "new_geo_direction: " + (new_geo_direction[0]*180/Math.PI) + ", " + (new_geo_direction[1]*180/Math.PI) + ", " + (new_geo_direction[2]*180/Math.PI));
			Log.d(TAG, "geo_direction: " + (geo_direction[0]*180/Math.PI) + ", " + (geo_direction[1]*180/Math.PI) + ", " + (geo_direction[2]*180/Math.PI));
		}*/
    }

    private float lowPassFilter(float old_value, float new_value, float smooth, float threshold) {
        float diff = Math.abs(new_value - old_value);
        if (diff < 180.0f) {
            if (diff > threshold) {
                old_value = new_value;
            } else {
                old_value = old_value + smooth * (new_value - old_value);
            }
        } else {
            if (360.0f - diff > threshold) {
                old_value = new_value;
            } else {
                if (old_value > new_value) {
                    old_value = (old_value + smooth * ((360 + new_value - old_value) % 360) + 360) % 360;
                } else {
                    old_value = (old_value - smooth * ((360 - new_value + old_value) % 360) + 360) % 360;
                }
            }
        }
        return old_value;
    }

    public boolean hasGeoDirection() {
        return has_geo_direction;
    }

    public double getGeoDirection() {
        return geo_direction[0];
    }

    public boolean supportsFaceDetection() {
        // don't log this, as we call from DrawPreview!
        return supports_face_detection;
    }

    public boolean supportsVideoStabilization() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsVideoStabilization");
        return supports_video_stabilization;
    }

    public boolean supportsPhotoVideoRecording() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsPhotoVideoRecording");
        return supports_photo_video_recording && !video_high_speed;
    }

    public boolean canDisableShutterSound() {
        if (MyDebug.LOG)
            Log.d(TAG, "canDisableShutterSound");
        return can_disable_shutter_sound;
    }

    public int getTonemapMaxCurvePoints() {
        if (MyDebug.LOG)
            Log.d(TAG, "getTonemapMaxCurvePoints");
        return tonemap_max_curve_points;
    }

    public boolean supportsTonemapCurve() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsTonemapCurve");
        return supports_tonemap_curve;
    }

    public List<String> getSupportedColorEffects() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedColorEffects");
        return this.color_effects;
    }

    public List<String> getSupportedSceneModes() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedSceneModes");
        return this.scene_modes;
    }

    public List<String> getSupportedWhiteBalances() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedWhiteBalances");
        return this.white_balances;
    }

    public List<String> getSupportedAntiBanding() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedAntiBanding");
        return this.antibanding;
    }

    public List<String> getSupportedEdgeModes() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedEdgeModes");
        return this.edge_modes;
    }

    public List<String> getSupportedNoiseReductionModes() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedNoiseReductionModes");
        return this.noise_reduction_modes;
    }

    public String getISOKey() {
        if (MyDebug.LOG)
            Log.d(TAG, "getISOKey");
        return camera_controller == null ? "" : camera_controller.getISOKey();
    }

    /**
     * Whether manual white balance temperatures can be specified via setWhiteBalanceTemperature().
     */
    public boolean supportsWhiteBalanceTemperature() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsWhiteBalanceTemperature");
        return this.supports_white_balance_temperature;
    }

    /**
     * Minimum allowed white balance temperature.
     */
    public int getMinimumWhiteBalanceTemperature() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMinimumWhiteBalanceTemperature");
        return this.min_temperature;
    }


    public int getMaximumWhiteBalanceTemperature() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMaximumWhiteBalanceTemperature");
        return this.max_temperature;
    }


    public boolean supportsISORange() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsISORange");
        return this.supports_iso_range;
    }

    public List<String> getSupportedISOs() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedISOs");
        return this.isos;
    }

    public int getMinimumISO() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMinimumISO");
        return this.min_iso;
    }

    public int getMaximumISO() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMaximumISO");
        return this.max_iso;
    }

    public boolean supportsExposureTime() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsExposureTime");
        return this.supports_exposure_time;
    }

    public long getMinimumExposureTime() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMinimumExposureTime: " + min_exposure_time);
        return this.min_exposure_time;
    }

    public long getMaximumExposureTime() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMaximumExposureTime: " + max_exposure_time);
        long max = max_exposure_time;
        if (applicationInterface.isExpoBracketingPref() || applicationInterface.isFocusBracketingPref() || applicationInterface.isCameraBurstPref()) {
            // doesn't make sense to allow long exposure times in these modes
            if (applicationInterface.getBurstForNoiseReduction())
                max = Math.min(max_exposure_time, 1000000000L * 2); // limit to 2s
            else
                max = Math.min(max_exposure_time, 1000000000L / 2); // limit to 0.5s
        }
        if (MyDebug.LOG)
            Log.d(TAG, "max: " + max);
        return max;
    }

    public boolean supportsExposures() {
        if (MyDebug.LOG)
            Log.d(TAG, "supportsExposures");
        return this.exposures != null;
    }

    public int getMinimumExposure() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMinimumExposure");
        return this.min_exposure;
    }

    public int getMaximumExposure() {
        if (MyDebug.LOG)
            Log.d(TAG, "getMaximumExposure");
        return this.max_exposure;
    }



    public boolean supportsExpoBracketing() {
        return this.supports_expo_bracketing;
    }

    public int maxExpoBracketingNImages() {
        if (MyDebug.LOG)
            Log.d(TAG, "maxExpoBracketingNImages");
        return this.max_expo_bracketing_n_images;
    }

    public boolean supportsFocusBracketing() {
        return this.supports_focus_bracketing;
    }

    public boolean supportsBurst() {
        return this.supports_burst;
    }

    public boolean supportsRaw() {
        return this.supports_raw;
    }

    public float getViewAngleX(boolean for_preview) {
        if (MyDebug.LOG)
            Log.d(TAG, "getViewAngleX: " + for_preview);
        CameraController.Size size = for_preview ? this.getCurrentPreviewSize() : this.getCurrentPictureSize();
        if (size == null) {
            Log.e(TAG, "can't find view angle x size");
            return this.view_angle_x;
        }
        float view_aspect_ratio = view_angle_x / view_angle_y;
        float actual_aspect_ratio = ((float) size.width) / (float) size.height;
        if (Math.abs(actual_aspect_ratio - view_aspect_ratio) < 1.0e-5f) {
            return this.view_angle_x;
        } else if (actual_aspect_ratio > view_aspect_ratio) {
            return this.view_angle_x;
        } else {
            float aspect_ratio_scale = actual_aspect_ratio / view_aspect_ratio;
            //float actual_view_angle_x = view_angle_x*aspect_ratio_scale;
            float actual_view_angle_x = (float) Math.toDegrees(2.0 * Math.atan(aspect_ratio_scale * Math.tan(Math.toRadians(view_angle_x) / 2.0)));
			/*if( MyDebug.LOG )
				Log.d(TAG, "actual_view_angle_x: " + actual_view_angle_x);*/
            return actual_view_angle_x;
        }
    }


    public float getViewAngleY(boolean for_preview) {
        if (MyDebug.LOG)
            Log.d(TAG, "getViewAngleY: " + for_preview);
        CameraController.Size size = for_preview ? this.getCurrentPreviewSize() : this.getCurrentPictureSize();
        if (size == null) {
            Log.e(TAG, "can't find view angle y size");
            return this.view_angle_y;
        }
        float view_aspect_ratio = view_angle_x / view_angle_y;
        float actual_aspect_ratio = ((float) size.width) / (float) size.height;
        if (Math.abs(actual_aspect_ratio - view_aspect_ratio) < 1.0e-5f) {
            return this.view_angle_y;
        } else if (actual_aspect_ratio > view_aspect_ratio) {
            float aspect_ratio_scale = view_aspect_ratio / actual_aspect_ratio;
            float actual_view_angle_y = (float) Math.toDegrees(2.0 * Math.atan(aspect_ratio_scale * Math.tan(Math.toRadians(view_angle_y) / 2.0)));
            return actual_view_angle_y;
        } else {
            return this.view_angle_y;
        }
    }

    public List<CameraController.Size> getSupportedPreviewSizes() {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedPreviewSizes");
        return this.supported_preview_sizes;
    }

    public CameraController.Size getCurrentPreviewSize() {
        return new CameraController.Size(preview_w, preview_h);
    }

    public double getCurrentPreviewAspectRatio() {
        return ((double) preview_w) / (double) preview_h;
    }

    public List<CameraController.Size> getSupportedPictureSizes(boolean check_burst) {
        if (MyDebug.LOG)
            Log.d(TAG, "getSupportedPictureSizes");
        if (check_burst && camera_controller != null && camera_controller.isBurstOrExpo()) {
            if (MyDebug.LOG)
                Log.d(TAG, "need to filter picture sizes for a burst mode");
            List<CameraController.Size> filtered_sizes = new ArrayList<>();
            for (CameraController.Size size : sizes) {
                if (size.supports_burst) {
                    filtered_sizes.add(size);
                }
            }
            return filtered_sizes;
        }
        return this.sizes;
    }


    public CameraController.Size getCurrentPictureSize() {
        if (current_size_index == -1 || sizes == null)
            return null;
        return sizes.get(current_size_index);
    }

    public List<String> getSupportedFlashValues() {
        return supported_flash_values;
    }

    public List<String> getSupportedFocusValues() {
        return supported_focus_values;
    }

    public int getCameraId() {
        if (camera_controller == null)
            return 0;
        return camera_controller.getCameraId();
    }

    public String getCameraAPI() {
        if (camera_controller == null)
            return "None";
        return camera_controller.getAPI();
    }

    public void onResume() {
        if (MyDebug.LOG)
            Log.d(TAG, "onResume");
        recreatePreviewBitmap();
        this.app_is_paused = false;
        cameraSurface.onResume();
        if (canvasView != null)
            canvasView.onResume();

        if (camera_open_state == CameraOpenState.CAMERAOPENSTATE_CLOSING) {
            // when pausing, we close the camera on a background thread - so if this is still happening when we resume,
            // we won't be able to open the camera, so need to open camera when it's closed
            if (MyDebug.LOG)
                Log.d(TAG, "camera still closing");
            if (close_camera_task != null) { // just to be safe
                close_camera_task.reopen = true;
            } else {
                Log.e(TAG, "onResume: state is CAMERAOPENSTATE_CLOSING, but close_camera_task is null");
            }
        } else {
            this.openCamera();
        }
    }

    public void onPause() {
        if (MyDebug.LOG)
            Log.d(TAG, "onPause");
        this.app_is_paused = true;
        if (camera_open_state == CameraOpenState.CAMERAOPENSTATE_OPENING) {
            if (MyDebug.LOG)
                Log.d(TAG, "cancel open_camera_task");
            if (open_camera_task != null) { // just to be safe
                this.open_camera_task.cancel(true);
            } else {
                Log.e(TAG, "onPause: state is CAMERAOPENSTATE_OPENING, but open_camera_task is null");
            }
        }
        //final boolean use_background_thread = false;
        final boolean use_background_thread = true;
        this.closeCamera(use_background_thread, null);
        cameraSurface.onPause();
        if (canvasView != null)
            canvasView.onPause();
        freePreviewBitmap();
    }

    public void onDestroy() {
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy");

        if (refreshPreviewBitmapTaskIsRunning()) {
            // if we're being destroyed, better to wait until completion rather than just cancelling
            try {
                refreshPreviewBitmapTask.get(); // forces thread to complete
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "exception while waiting for background_task to finish");
                e.printStackTrace();
            }
        }
        freePreviewBitmap(); // in case onDestroy() called directly without onPause()

        if (rs != null) {
            try {
                rs.destroy(); // on Android M onwards this is a NOP - instead we call RenderScript.releaseAllContexts(); in MainActivity.onDestroy()
            } catch (RSInvalidStateException e) {
                e.printStackTrace();
            }
            rs = null;
        }

        if (camera_open_state == CameraOpenState.CAMERAOPENSTATE_CLOSING) {
            // If the camera is currently closing on a background thread, then wait until the camera has closed to be safe
            if (MyDebug.LOG) {
                Log.d(TAG, "wait for close_camera_task");
            }
            if (close_camera_task != null) { // just to be safe
                long time_s = System.currentTimeMillis();
                try {
                    close_camera_task.get(3000, TimeUnit.MILLISECONDS); // set timeout to avoid ANR (camera resource should be freed by the OS when destroyed anyway)
                } catch (ExecutionException | InterruptedException | TimeoutException e) {
                    Log.e(TAG, "exception while waiting for close_camera_task to finish");
                    e.printStackTrace();
                }
                if (MyDebug.LOG) {
                    Log.d(TAG, "done waiting for close_camera_task");
                    Log.d(TAG, "### time after waiting for close_camera_task: " + (System.currentTimeMillis() - time_s));
                }
            } else {
                Log.e(TAG, "onResume: state is CAMERAOPENSTATE_CLOSING, but close_camera_task is null");
            }
        }
    }

    public void onSaveInstanceState(Bundle state) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSaveInstanceState");
    }

    public void setUIRotation(int ui_rotation) {
        if (MyDebug.LOG)
            Log.d(TAG, "setUIRotation");
        this.ui_rotation = ui_rotation;
    }

    public int getUIRotation() {
        return this.ui_rotation;
    }

    private void updateParametersFromLocation() {
        if (MyDebug.LOG)
            Log.d(TAG, "updateParametersFromLocation");
        if (camera_controller != null) {
            boolean store_location = applicationInterface.getGeotaggingPref();
            if (store_location && applicationInterface.getLocation() != null) {
                Location location = applicationInterface.getLocation();
                if (MyDebug.LOG) {
                    Log.d(TAG, "updating parameters from location...");
                    Log.d(TAG, "lat " + location.getLatitude() + " long " + location.getLongitude() + " accuracy " + location.getAccuracy() + " timestamp " + location.getTime());
                }
                camera_controller.setLocationInfo(location);
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "removing location data from parameters...");
                camera_controller.removeLocationInfo();
            }
        }
    }

    public void enablePreviewBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "enablePreviewBitmap");
        if (cameraSurface instanceof TextureView) {
            want_preview_bitmap = true;
            recreatePreviewBitmap();
        }
    }

    public void disablePreviewBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "disablePreviewBitmap");
        freePreviewBitmap();
        want_preview_bitmap = false;
        histogramScript = null; // to help garbage collection
    }

    public boolean isPreviewBitmapEnabled() {
        return this.want_preview_bitmap;
    }


    public boolean refreshPreviewBitmapTaskIsRunning() {
        return refreshPreviewBitmapTask != null;
    }

    private void recycleBitmapForPreviewTask(final Bitmap bitmap) {
        if (MyDebug.LOG)
            Log.d(TAG, "recycleBitmapForPreviewTask");
        if (!refreshPreviewBitmapTaskIsRunning()) {
            if (MyDebug.LOG)
                Log.d(TAG, "refreshPreviewBitmapTask not running, can recycle bitmap");
            bitmap.recycle();
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "refreshPreviewBitmapTask still running, wait before recycle bitmap");
            final Handler handler = new Handler();
            final long recycle_delay = 500;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!refreshPreviewBitmapTaskIsRunning()) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "refreshPreviewBitmapTask not running now, can recycle bitmap");
                        bitmap.recycle();
                    } else {
                        if (MyDebug.LOG)
                            Log.d(TAG, "refreshPreviewBitmapTask still running, wait again before recycle bitmap");
                        handler.postDelayed(this, recycle_delay);
                    }
                }
            }, recycle_delay);
        }
    }

    private void freePreviewBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "freePreviewBitmap");
        cancelRefreshPreviewBitmap();
        histogram = null;
        if (preview_bitmap != null) {
            recycleBitmapForPreviewTask(preview_bitmap);
            preview_bitmap = null;
        }
        freeZebraStripesBitmap();
        freeFocusPeakingBitmap();
    }

    private void recreatePreviewBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "recreatePreviewBitmap");
        freePreviewBitmap();

        if (want_preview_bitmap) {
            final int downscale = 4;
            int bitmap_width = textureview_w / downscale;
            int bitmap_height = textureview_h / downscale;
            int rotation = getDisplayRotationDegrees();
            if (rotation == 90 || rotation == 270) {
                int dummy = bitmap_width;
                bitmap_width = bitmap_height;
                bitmap_height = dummy;
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "bitmap_width: " + bitmap_width);
                Log.d(TAG, "bitmap_height: " + bitmap_height);
                Log.d(TAG, "rotation: " + rotation);
            }
            try {
                preview_bitmap = Bitmap.createBitmap(bitmap_width, bitmap_height, Bitmap.Config.ARGB_8888);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed to create preview_bitmap");
                e.printStackTrace();
            }
            createZebraStripesBitmap();
            createFocusPeakingBitmap();
        }
    }

    private void freeZebraStripesBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "freeZebraStripesBitmap");
        if (zebra_stripes_bitmap_buffer != null) {
            recycleBitmapForPreviewTask(zebra_stripes_bitmap_buffer);
            zebra_stripes_bitmap_buffer = null;
        }
        if (zebra_stripes_bitmap != null) {
            zebra_stripes_bitmap.recycle();
            zebra_stripes_bitmap = null;
        }
    }

    private void createZebraStripesBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "createZebraStripesBitmap");
        if (want_zebra_stripes && preview_bitmap != null) {
            try {
                zebra_stripes_bitmap_buffer = Bitmap.createBitmap(preview_bitmap.getWidth(), preview_bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed to create zebra_stripes_bitmap_buffer");
                e.printStackTrace();
            }
        }
    }

    private void freeFocusPeakingBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "freeFocusPeakingBitmap");
        if (focus_peaking_bitmap_buffer != null) {
            recycleBitmapForPreviewTask(focus_peaking_bitmap_buffer);
            focus_peaking_bitmap_buffer = null;
        }
        if (focus_peaking_bitmap != null) {
            focus_peaking_bitmap.recycle();
            focus_peaking_bitmap = null;
        }
    }

    private void createFocusPeakingBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "createFocusPeakingBitmap");
        if (want_focus_peaking & preview_bitmap != null) {
            try {
                focus_peaking_bitmap_buffer = Bitmap.createBitmap(preview_bitmap.getWidth(), preview_bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed to create focus_peaking_bitmap_buffer");
                e.printStackTrace();
            }
        }
    }

    public void enableHistogram(HistogramType histogram_type) {
        this.want_histogram = true;
        this.histogram_type = histogram_type;
    }

    public void disableHistogram() {
        this.want_histogram = false;
    }

    public void enableZebraStripes(int zebra_stripes_threshold) {
        this.want_zebra_stripes = true;
        this.zebra_stripes_threshold = zebra_stripes_threshold;
        if (this.zebra_stripes_bitmap_buffer == null) {
            createZebraStripesBitmap();
        }
    }

    public void disableZebraStripes() {
        if (this.want_zebra_stripes) {
            this.want_zebra_stripes = false;
            freeZebraStripesBitmap();
        }
    }

    public Bitmap getZebraStripesBitmap() {
        return this.zebra_stripes_bitmap;
    }

    public void enableFocusPeaking() {
        this.want_focus_peaking = true;
        if (this.focus_peaking_bitmap_buffer == null) {
            createFocusPeakingBitmap();
        }
    }

    public void disableFocusPeaking() {
        if (this.want_focus_peaking) {
            this.want_focus_peaking = false;
            freeFocusPeakingBitmap();
        }
    }

    public Bitmap getFocusPeakingBitmap() {
        return this.focus_peaking_bitmap;
    }

    private static class RefreshPreviewBitmapTaskResult {
        int[] new_histogram;
        Bitmap new_zebra_stripes_bitmap;
        Bitmap new_focus_peaking_bitmap;
    }

    private static class RefreshPreviewBitmapTask extends AsyncTask<Void, Void, RefreshPreviewBitmapTaskResult> {
        private static final String TAG = "RefreshPreviewBmTask";
        private final WeakReference<Preview> previewReference;
        private final WeakReference<ScriptC_histogram_compute> histogramScriptReference;
        private final WeakReference<Bitmap> preview_bitmapReference;
        private final WeakReference<Bitmap> zebra_stripes_bitmap_bufferReference;
        private final WeakReference<Bitmap> focus_peaking_bitmap_bufferReference;
        private final boolean update_histogram;

        RefreshPreviewBitmapTask(Preview preview, boolean update_histogram) {
            this.previewReference = new WeakReference<>(preview);
            this.preview_bitmapReference = new WeakReference<>(preview.preview_bitmap);
            this.zebra_stripes_bitmap_bufferReference = new WeakReference<>(preview.zebra_stripes_bitmap_buffer);
            this.focus_peaking_bitmap_bufferReference = new WeakReference<>(preview.focus_peaking_bitmap_buffer);
            this.update_histogram = update_histogram;

            if (preview.rs == null) {
                // create on the UI thread rather than doInBackground(), to avoid threading issues
                if (MyDebug.LOG)
                    Log.d(TAG, "create renderscript object");
                preview.rs = RenderScript.create(preview.getContext());
            }
            if (preview.histogramScript == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "create histogramScript");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    preview.histogramScript = new ScriptC_histogram_compute(preview.rs);
                }
            }
            this.histogramScriptReference = new WeakReference<>(preview.histogramScript);
        }

        private static int[] computeHistogram(Allocation allocation_in, RenderScript rs, ScriptC_histogram_compute histogramScript, HistogramType histogram_type) {

            int[] new_histogram;

            if (histogram_type == HistogramType.HISTOGRAM_TYPE_RGB) {
                if (MyDebug.LOG)
                    Log.d(TAG, "rgb histogram");
                Allocation histogramAllocationR = Allocation.createSized(rs, Element.I32(rs), 256);
                Allocation histogramAllocationG = Allocation.createSized(rs, Element.I32(rs), 256);
                Allocation histogramAllocationB = Allocation.createSized(rs, Element.I32(rs), 256);

                if (MyDebug.LOG)
                    Log.d(TAG, "bind histogram allocations");
                histogramScript.bind_histogram_r(histogramAllocationR);
                histogramScript.bind_histogram_g(histogramAllocationG);
                histogramScript.bind_histogram_b(histogramAllocationB);
                histogramScript.invoke_init_histogram_rgb();
                histogramScript.forEach_histogram_compute_rgb(allocation_in);

                new_histogram = new int[256 * 3];
                int c = 0;
                int[] temp = new int[256];

                histogramAllocationR.copyTo(temp);
                for (int i = 0; i < 256; i++)
                    new_histogram[c++] = temp[i];

                histogramAllocationG.copyTo(temp);
                for (int i = 0; i < 256; i++)
                    new_histogram[c++] = temp[i];

                histogramAllocationB.copyTo(temp);
                for (int i = 0; i < 256; i++)
                    new_histogram[c++] = temp[i];
                histogramAllocationR.destroy();
                histogramAllocationG.destroy();
                histogramAllocationB.destroy();
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "single channel histogram");
                Allocation histogramAllocation = Allocation.createSized(rs, Element.I32(rs), 256);

                if (MyDebug.LOG)
                    Log.d(TAG, "bind histogram allocation");
                histogramScript.bind_histogram(histogramAllocation);
                histogramScript.invoke_init_histogram();
                switch (histogram_type) {
                    case HISTOGRAM_TYPE_LUMINANCE:
                        histogramScript.forEach_histogram_compute_by_luminance(allocation_in);
                        break;
                    case HISTOGRAM_TYPE_VALUE:
                        histogramScript.forEach_histogram_compute_by_value(allocation_in);
                        break;
                    case HISTOGRAM_TYPE_INTENSITY:
                        histogramScript.forEach_histogram_compute_by_intensity(allocation_in);
                        break;
                    case HISTOGRAM_TYPE_LIGHTNESS:
                        histogramScript.forEach_histogram_compute_by_lightness(allocation_in);
                        break;
                }

                new_histogram = new int[256];
                histogramAllocation.copyTo(new_histogram);
                histogramAllocation.destroy();
            }
            return new_histogram;
        }

        @SuppressLint("WrongThread")
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        protected RefreshPreviewBitmapTaskResult doInBackground(Void... voids) {
            long debug_time = 0;
            if (MyDebug.LOG) {
                Log.d(TAG, "doInBackground, async task: " + this);
                debug_time = System.currentTimeMillis();
            }

            Preview preview = previewReference.get();
            if (preview == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "preview is null");
                return null;
            }
            ScriptC_histogram_compute histogramScript = histogramScriptReference.get();
            if (histogramScript == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "histogramScript is null");
                return null;
            }
            Bitmap preview_bitmap = preview_bitmapReference.get();
            if (preview_bitmap == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "preview_bitmap is null");
                return null;
            }
            Bitmap zebra_stripes_bitmap_buffer = zebra_stripes_bitmap_bufferReference.get();
            Bitmap focus_peaking_bitmap_buffer = focus_peaking_bitmap_bufferReference.get();
            Activity activity = (Activity) preview.getContext();
            if (activity == null || activity.isFinishing()) {
                if (MyDebug.LOG)
                    Log.d(TAG, "activity is null or finishing");
                return null;
            }

            RefreshPreviewBitmapTaskResult result = new RefreshPreviewBitmapTaskResult();

            try {
                if (MyDebug.LOG)
                    Log.d(TAG, "time before getBitmap: " + (System.currentTimeMillis() - debug_time));
                TextureView textureView = (TextureView) preview.cameraSurface;
                textureView.getBitmap(preview_bitmap);
                if (MyDebug.LOG)
                    Log.d(TAG, "time after getBitmap: " + (System.currentTimeMillis() - debug_time));

                Allocation allocation_in = Allocation.createFromBitmap(preview.rs, preview_bitmap);
                if (MyDebug.LOG)
                    Log.d(TAG, "time after createFromBitmap: " + (System.currentTimeMillis() - debug_time));

                if (update_histogram) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "generate histogram");

                    if (MyDebug.LOG)
                        Log.d(TAG, "time before computeHistogram: " + (System.currentTimeMillis() - debug_time));
                    result.new_histogram = computeHistogram(allocation_in, preview.rs, histogramScript, preview.histogram_type);
                    if (MyDebug.LOG)
                        Log.d(TAG, "time after computeHistogram: " + (System.currentTimeMillis() - debug_time));
                }

                if (preview.want_zebra_stripes && zebra_stripes_bitmap_buffer != null) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "generate zebra stripes bitmap");
                    Allocation output_allocation = Allocation.createFromBitmap(preview.rs, zebra_stripes_bitmap_buffer);

                    histogramScript.set_zebra_stripes_threshold(preview.zebra_stripes_threshold);
                    histogramScript.set_zebra_stripes_width(zebra_stripes_bitmap_buffer.getWidth() / 20);

                    if (MyDebug.LOG)
                        Log.d(TAG, "time before histogramScript generate_zebra_stripes: " + (System.currentTimeMillis() - debug_time));
                    histogramScript.forEach_generate_zebra_stripes(allocation_in, output_allocation);
                    if (MyDebug.LOG)
                        Log.d(TAG, "time after histogramScript generate_zebra_stripes: " + (System.currentTimeMillis() - debug_time));

                    output_allocation.copyTo(zebra_stripes_bitmap_buffer);
                    output_allocation.destroy();

                    int rotation_degrees = preview.getDisplayRotationDegrees();
                    if (MyDebug.LOG)
                        Log.d(TAG, "time before creating new_zebra_stripes_bitmap: " + (System.currentTimeMillis() - debug_time));
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-rotation_degrees);
                    result.new_zebra_stripes_bitmap = Bitmap.createBitmap(zebra_stripes_bitmap_buffer, 0, 0,
                            zebra_stripes_bitmap_buffer.getWidth(), zebra_stripes_bitmap_buffer.getHeight(), matrix, false);

                    if (MyDebug.LOG)
                        Log.d(TAG, "time after creating new_zebra_stripes_bitmap: " + (System.currentTimeMillis() - debug_time));
                }

                if (preview.want_focus_peaking && focus_peaking_bitmap_buffer != null) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "generate focus peaking bitmap");
                    Allocation output_allocation = Allocation.createFromBitmap(preview.rs, focus_peaking_bitmap_buffer);

                    histogramScript.set_bitmap(allocation_in);

                    if (MyDebug.LOG)
                        Log.d(TAG, "time before histogramScript generate_focus_peaking: " + (System.currentTimeMillis() - debug_time));
                    histogramScript.forEach_generate_focus_peaking(allocation_in, output_allocation);
                    if (MyDebug.LOG)
                        Log.d(TAG, "time after histogramScript generate_focus_peaking: " + (System.currentTimeMillis() - debug_time));
                    Allocation filtered_allocation = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        filtered_allocation = Allocation.createTyped(preview.rs, Type.createXY(preview.rs, Element.RGBA_8888(preview.rs), focus_peaking_bitmap_buffer.getWidth(), focus_peaking_bitmap_buffer.getHeight()));
                    }
                    histogramScript.set_bitmap(output_allocation);
                    if (MyDebug.LOG)
                        Log.d(TAG, "time before histogramScript generate_focus_peaking_filtered: " + (System.currentTimeMillis() - debug_time));
                    histogramScript.forEach_generate_focus_peaking_filtered(output_allocation, filtered_allocation);
                    if (MyDebug.LOG)
                        Log.d(TAG, "time after histogramScript generate_focus_peaking_filtered: " + (System.currentTimeMillis() - debug_time));
                    output_allocation.destroy();
                    output_allocation = filtered_allocation;

                    output_allocation.copyTo(focus_peaking_bitmap_buffer);
                    output_allocation.destroy();

                    int rotation_degrees = preview.getDisplayRotationDegrees();
                    if (MyDebug.LOG)
                        Log.d(TAG, "time before creating new_focus_peaking_bitmap: " + (System.currentTimeMillis() - debug_time));
                    Matrix matrix = new Matrix();
                    matrix.postRotate(-rotation_degrees);
                    result.new_focus_peaking_bitmap = Bitmap.createBitmap(focus_peaking_bitmap_buffer, 0, 0,
                            focus_peaking_bitmap_buffer.getWidth(), focus_peaking_bitmap_buffer.getHeight(), matrix, false);
                    if (MyDebug.LOG)
                        Log.d(TAG, "time after creating new_focus_peaking_bitmap: " + (System.currentTimeMillis() - debug_time));
                }

                allocation_in.destroy();
            } catch (IllegalStateException e) {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to getBitmap");
                e.printStackTrace();
            } catch (RSInvalidStateException e) {
                if (MyDebug.LOG)
                    Log.e(TAG, "renderscript failure");
                e.printStackTrace();
            }

            if (MyDebug.LOG) {
                Log.d(TAG, "time taken: " + (System.currentTimeMillis() - debug_time));
            }
            return result;
        }

        @Override
        protected void onPostExecute(RefreshPreviewBitmapTaskResult result) {
            if (MyDebug.LOG)
                Log.d(TAG, "onPostExecute, async task: " + this);

            Preview preview = previewReference.get();
            if (preview == null) {
                return;
            }
            Activity activity = (Activity) preview.getContext();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (result == null) {
                return;
            }

            if (result.new_histogram != null)
                preview.histogram = result.new_histogram;

            if (preview.zebra_stripes_bitmap != null) {
                preview.zebra_stripes_bitmap.recycle();
            }
            preview.zebra_stripes_bitmap = result.new_zebra_stripes_bitmap;

            if (preview.focus_peaking_bitmap != null) {
                preview.focus_peaking_bitmap.recycle();
            }
            preview.focus_peaking_bitmap = result.new_focus_peaking_bitmap;

            preview.refreshPreviewBitmapTask = null;

            if (MyDebug.LOG)
                Log.d(TAG, "onPostExecute done, async task: " + this);
        }

        @Override
        protected void onCancelled() {
            if (MyDebug.LOG)
                Log.d(TAG, "onCancelled, async task: " + this);
            Preview preview = previewReference.get();
            if (preview == null) {
                return;
            }
            preview.refreshPreviewBitmapTask = null;
        }
    }

    private void refreshPreviewBitmap() {
        final int refresh_histogram_rate_ms = 200;
        final long refresh_time = (want_zebra_stripes || want_focus_peaking) ? 40 : refresh_histogram_rate_ms;
        long time_now = System.currentTimeMillis();
        if (want_preview_bitmap && preview_bitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                !app_is_paused && !applicationInterface.isPreviewInBackground() &&
                !refreshPreviewBitmapTaskIsRunning() && time_now > last_preview_bitmap_time_ms + refresh_time) {
            if (MyDebug.LOG)
                Log.d(TAG, "refreshPreviewBitmap");
            // even if we're running the background task at a faster rate (due to zebra stripes etc), we still update the histogram
            // at the standard rate
            boolean update_histogram = want_histogram && time_now > last_histogram_time_ms + refresh_histogram_rate_ms;
            this.last_preview_bitmap_time_ms = time_now;
            if (update_histogram) {
                this.last_histogram_time_ms = time_now;
            }
            refreshPreviewBitmapTask = new RefreshPreviewBitmapTask(this, update_histogram);
            refreshPreviewBitmapTask.execute();
        }
    }

    private void cancelRefreshPreviewBitmap() {
        if (MyDebug.LOG)
            Log.d(TAG, "cancelRefreshPreviewBitmap");
        if (refreshPreviewBitmapTaskIsRunning()) {
            refreshPreviewBitmapTask.cancel(true);
            // we don't set refreshPreviewBitmapTask to null - this will be done by the task itself when it completes;
            // and we want to know when the task is no longer running (e.g., for freePreviewBitmap()).
        }
    }



    public long getFrameRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return 16;
        return this.isTakingPhoto() ? 500 : 100;
    }

    public boolean isTakingPhoto() {
        return this.phase == PHASE_TAKING_PHOTO;
    }

    public boolean usingCamera2API() {
        return this.using_android_l;
    }

    public CameraController getCameraController() {
        return this.camera_controller;
    }

    public CameraControllerManager getCameraControllerManager() {
        return this.camera_controller_manager;
    }

    public boolean supportsFlash() {
        return this.supported_flash_values != null;
    }

    public boolean supportsExposureLock() {
        return this.is_exposure_lock_supported;
    }

    public boolean supportsWhiteBalanceLock() {
        return this.is_white_balance_lock_supported;
    }

    public boolean supportsZoom() {
        return this.has_zoom;
    }

    public boolean hasFocusArea() {
        return this.has_focus_area;
    }

    public Pair<Integer, Integer> getFocusPos() {
        return new Pair<>(focus_screen_x, focus_screen_y);
    }

    public boolean isTakingPhotoOrOnTimer() {
        return this.phase == PHASE_TAKING_PHOTO || this.phase == PHASE_TIMER;
    }

    public boolean isOnTimer() {
        return this.phase == PHASE_TIMER;
    }

    public boolean isPreviewPaused() {
        return this.phase == PHASE_PREVIEW_PAUSED;
    }

    public boolean isFocusWaiting() {
        return focus_success == FOCUS_WAITING;
    }

    public boolean isFocusRecentSuccess() {
        return focus_success == FOCUS_SUCCESS;
    }

    public long timeSinceStartedAutoFocus() {
        if (focus_started_time != -1)
            return System.currentTimeMillis() - focus_started_time;
        return 0;
    }

    public boolean isFocusRecentFailure() {
        return focus_success == FOCUS_FAILED;
    }

    private boolean recentlyFocused() {
        return this.successfully_focused && System.currentTimeMillis() < this.successfully_focused_time + 5000;
    }

    public CameraController.Face[] getFacesDetected() {
        return this.faces_detected;
    }


    public float getZoomRatio() {
        if (zoom_ratios == null)
            return 1.0f;
        int zoom_factor = camera_controller.getZoom();
        return this.zoom_ratios.get(zoom_factor) / 100.0f;
    }
}
