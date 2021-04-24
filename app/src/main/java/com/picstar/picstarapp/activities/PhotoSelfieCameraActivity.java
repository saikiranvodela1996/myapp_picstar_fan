package com.picstar.picstarapp.activities;


import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.renderscript.RenderScript;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.bumptech.glide.Glide;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerManager2;
import com.picstar.picstarapp.campkg.camui.MainUI;
import com.picstar.picstarapp.campkg.others.MagneticSensor;
import com.picstar.picstarapp.campkg.others.MyApplicationInterface;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PermissionHandler;
import com.picstar.picstarapp.campkg.others.PreferenceKeys;
import com.picstar.picstarapp.campkg.others.SoundPoolManager;
import com.picstar.picstarapp.campkg.others.SpeechControl;
import com.picstar.picstarapp.campkg.others.StorageUtils;
import com.picstar.picstarapp.campkg.others.TextFormatter;
import com.picstar.picstarapp.campkg.preview.Preview;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PhotoSelfieCameraActivity extends Activity implements View.OnTouchListener{

    private static final String TAG = "MainActivity";
    private static int activity_count = 0;
    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private PermissionHandler permissionHandler;
    private MainUI mainUI;
    private MyApplicationInterface applicationInterface;
    private TextFormatter textFormatter;
    private SoundPoolManager soundPoolManager;
    private MagneticSensor magneticSensor;
    private SpeechControl speechControl;
    private Preview preview;
    private OrientationEventListener orientationEventListener;
    private int large_heap_memory;
    private boolean supports_auto_stabilise;
    private boolean supports_force_video_4k;
    private boolean supports_camera2;
    private boolean camera_in_background; // whether the camera is covered by a fragment/dialog (such as settings or folder picker)
    private final Map<Integer, Bitmap> preloaded_bitmap_resources = new Hashtable<>();
    private ValueAnimator gallery_save_anim;
    private boolean last_continuous_fast_burst; // whether the last photo operation was a continuous_fast_burst
    private TextToSpeech textToSpeech;
    private boolean textToSpeechSuccess;
    public boolean is_test; // whether called from OpenCamera.test testing
    public volatile boolean test_low_memory;
    public volatile boolean test_have_angle;
    public volatile float test_angle;
    public volatile String test_last_saved_image;
    public static boolean test_force_supports_camera2; // okay to be static, as this is set for an entire test suite
    private final String CHANNEL_ID = "notifii_camera_channel";
    private final int image_saving_notification_id = 1;
    private int requestCode;
    private boolean isCaptureClickable = true;
    private static View view = null;
    private boolean is_multi_cam;
    private List<Integer> back_camera_ids;
    private List<Integer> front_camera_ids;
    private List<Integer> other_camera_ids;
    // these matrices will be used to move and zoom image
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private Matrix imageMatrix = new Matrix();
    // we can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    // remember some things for zooming
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float d = 0f;
    private float newRot = 0f;
    private float[] lastEvent = null;
    private Bitmap bmap;
    private float dx; // postTranslate X distance
    private float dy; // postTranslate Y distance
    private float[] matrixValues = new float[9];
    private float[] imagematrixValues = new float[9];
    float matrixX = 0; // X coordinate of matrix inside the ImageView
    float matrixY = 0; // Y coordinate of matrix inside the ImageView
    float width = 0; // width of drawable
    float height = 0; // height of drawable
    private float MAX_ZOOM = 1;
    private float MIN_ZOOM = 0.7f;
    private int actualHeight = 0;
    private int actualWidth = 0;



    private String photoSelfieCost;
    private boolean isCamFlippable=true;

    public String getCelebrityId() {
        return celebrityId;
    }

    public int getPhotoId() {
        return photoId;
    }

    private String celebrityId;
    private int photoId;

    @BindView(R.id.transparentLayer)
    View middleLayer;
    @BindView(R.id.captureLayout)
    FrameLayout captureLayout;
    private Bitmap bitmap;
    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    @BindView(R.id.title_Tv)
    TextView toolbarTitle;
    @BindView(R.id.search_option_btn)
    ImageView searchOption;
    @BindView(R.id.imageView)
    ImageView editableView;
    @BindView(R.id.frm)
    FrameLayout frameLayout;

    public void setCaptureClickable(boolean captureClickable) {
        isCaptureClickable = captureClickable;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activity_count++;
        super.onCreate(savedInstanceState);
        isCaptureClickable = true;
//        if (getIntent().hasExtra(NTF_Constants.Extras_Keys.KEY_PHOT_POS))
//            requestCode = getIntent().getIntExtra(NTF_Constants.Extras_Keys.KEY_PHOT_POS, LogPackageInFragment.CLICK_PHOTO_REQUEST_CODE_1);
        requestCode = 11;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // don't show orientation animations
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
            getWindow().setAttributes(layout);
        }

        setContentView(R.layout.activity_camera_preview2);
        ButterKnife.bind(this);
        //changing status bar color.....
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolBar_bgcolor, this.getTheme()));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolBar_bgcolor));
        }
        //preventing from taking screenshot in this screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        if (getIntent() != null&& getIntent().getExtras() != null) {
            String path = getIntent().getStringExtra("IMAGEPATH");
            celebrityId = getIntent().getStringExtra(PSRConstants.CELEBRITYID);
            photoId = getIntent().getIntExtra(PSRConstants.CELEBRITYPHOTOID, 0);
            photoSelfieCost=getIntent().getStringExtra(PSRConstants.PHOTOSELFIECOST);

            bitmap = PSR_Utils.getBitmap(path);

            Glide.with(this)
                    .load(bitmap)
                    .dontTransform()
                    .into(editableView);
        }
        view = findViewById(R.id.preview);


        leftSideMenu.setImageResource(R.drawable.ic_back);
        toolbarTitle.setText(getString(R.string.photoSelfie_txt));

//        setPreview();
        previewSettings();

        if (getIntent() != null && getIntent().getExtras() != null) {
            // whether called from testing
            is_test = getIntent().getExtras().getBoolean("test_project");
            if (MyDebug.LOG)
                Log.d(TAG, "is_test: " + is_test);
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        large_heap_memory = activityManager.getLargeMemoryClass();
        if (large_heap_memory >= 128) {
            supports_auto_stabilise = true;
        }
        if (activityManager.getMemoryClass() >= 128 || activityManager.getLargeMemoryClass() >= 512) {
            supports_force_video_4k = true;
        }
        permissionHandler = new PermissionHandler(this);
        mainUI = new MainUI(this);
        applicationInterface = new MyApplicationInterface(this, savedInstanceState);
        textFormatter = new TextFormatter(this);
        soundPoolManager = new SoundPoolManager(this);
        magneticSensor = new MagneticSensor(this);
        speechControl = new SpeechControl(this);
        initCamera2Support();
        setWindowFlagsForCamera();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        magneticSensor.initSensor(mSensorManager);
        preview = new Preview(applicationInterface, ((ViewGroup) this.findViewById(R.id.preview)));
        View takePhotoButton = findViewById(R.id.take_photo);
        takePhotoButton.setVisibility(View.INVISIBLE);
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                PhotoSelfieCameraActivity.this.mainUI.onOrientationChanged(orientation);
            }
        };
        takePhotoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return longClickedTakePhoto();
            }
        });
        takePhotoButton.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "takePhotoButton ACTION_UP");
                    takePhotoButtonLongClickCancelled();
                    if (MyDebug.LOG)
                        Log.d(TAG, "takePhotoButton ACTION_UP done");
                }
                return false;
            }
        });

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if (!usingKitKatImmersiveMode())
                            return;
                        if (MyDebug.LOG)
                            Log.d(TAG, "onSystemUiVisibilityChange: " + visibility);
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            mainUI.setImmersiveMode(false);
                            setImmersiveTimer();
                        } else {
                            mainUI.setImmersiveMode(true);
                        }
                    }
                });

        // show "about" dialog for first time use; also set some per-device defaults
        boolean has_done_first_time = sharedPreferences.contains(PreferenceKeys.FirstTimePreferenceKey);
        if (MyDebug.LOG)
            Log.d(TAG, "has_done_first_time: " + has_done_first_time);
        if (!has_done_first_time) {
            setDeviceDefaults();
        }
        if (!has_done_first_time) {
            setFirstTimeFlag();
        }

        {
            int version_code = -1;
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                version_code = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                if (MyDebug.LOG)
                    Log.d(TAG, "NameNotFoundException exception trying to get version number");
                e.printStackTrace();
            }
            if (version_code != -1) {
                int latest_version = sharedPreferences.getInt(PreferenceKeys.LatestVersionPreferenceKey, 0);
                if (MyDebug.LOG) {
                    Log.d(TAG, "version_code: " + version_code);
                    Log.d(TAG, "latest_version: " + latest_version);
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PreferenceKeys.LatestVersionPreferenceKey, version_code);
                editor.apply();
            }
        }

        textToSpeechSuccess = false;
        // run in separate thread so as to not delay startup time
        new Thread(new Runnable() {
            public void run() {
                textToSpeech = new TextToSpeech(PhotoSelfieCameraActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "TextToSpeech initialised");
                        if (status == TextToSpeech.SUCCESS) {
                            textToSpeechSuccess = true;
                            if (MyDebug.LOG)
                                Log.d(TAG, "TextToSpeech succeeded");
                        } else {
                            if (MyDebug.LOG)
                                Log.d(TAG, "TextToSpeech failed");
                        }
                    }
                });
            }
        }).start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Open Camera Image Saving";
            String description = "Notification channel for processing and saving images in the background";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        editableView.setOnTouchListener(this);

    }


    public String getPhotoSelfieCost() {
        return photoSelfieCost;
    }
    //with rotation
    /*public boolean onTouch(View v, MotionEvent event) {
        // handle touch events here
        editableView = (ImageView) v;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.d("SCALE_XY_MOTION", "1");
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d("SCALE_XY_MOTION", "2");
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                Log.d("SCALE_XY_MOTION", "3");
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    Log.d("SCALE_XY_MOTION", "DRAG");
                    matrix.set(savedMatrix);
                    matrix.getValues(matrixValues);
                    matrixX = matrixValues[2];
                    matrixY = matrixValues[5];
                    width = matrixValues[0] * (((ImageView) editableView).getDrawable()
                            .getIntrinsicWidth());
                    height = matrixValues[4] * (((ImageView) editableView).getDrawable()
                            .getIntrinsicHeight());

                    dx = event.getX() - start.x;
                    dy = event.getY() - start.y;

                    float angle = getRotatedAngle(editableView.getImageMatrix());
                    angle = angle % 360;
                    if (angle < 180) {
                        int horizontalAdditionalDistance = getHorizontalAdditionalDistanceBelow180(angle, height);
                        int verticalAdditionalDistance = getVerticalAdditionalDistanceBelow180(angle, width);
                        //if image will go oustside top bound
                        if (matrixY + dy < 0) {
                            dy = -matrixY;
                        }
                        //if image will go outside right bound
                        if (matrixX + dx + width > editableView.getWidth()) {
                            dx = editableView.getWidth() - matrixX - width;
                        }
                        //if image will go outside left bound
                        if (matrixX + dx - horizontalAdditionalDistance < 0) {
                            dx = -matrixX + horizontalAdditionalDistance;
                        }
                        //if image will go outside bottom bound
                        if (matrixY + dy + height + verticalAdditionalDistance > editableView.getHeight()) {
                            dy = editableView.getHeight() - matrixY - height - verticalAdditionalDistance;
                        }
                    } else {
                        angle = 360 - angle;
                        int horizontalAdditionalDistance = getHorizontalAdditionalDistanceBelow180((angle), height);
                        int verticalAdditionalDistance = getVerticalAdditionalDistanceBelow180((angle), width);

                        //if image will go oustside top bound
                        if (matrixY + dy - verticalAdditionalDistance < 0) {
                            dy = -matrixY + verticalAdditionalDistance;
                        }
                        //if image will go outside bottom bound
                        if (matrixY + dy + height > editableView.getHeight()) {
                            dy = editableView.getHeight() - matrixY - height;
                        }
                        //if image will go outside left bound
                        if (matrixX + dx < 0) {
                            dx = -matrixX;
                        }
                        //if image will go outside right bound
                        if (matrixX + dx + width + horizontalAdditionalDistance > editableView.getWidth()) {
                            dx = editableView.getWidth() - matrixX - width - horizontalAdditionalDistance;
                        }
                    }
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    Log.d("SCALE_XY_MOTION", "ZOOM");
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        float scale = (newDist / oldDist);
                        editableView.getImageMatrix().getValues(imagematrixValues);
                        float width1 = imagematrixValues[0] * (((ImageView) editableView).getDrawable()
                                .getIntrinsicWidth());
                        float height1 = imagematrixValues[4] * (((ImageView) editableView).getDrawable()
                                .getIntrinsicHeight());
                        if ((actualHeight > height1 && actualWidth > width1) || scale < 1) {
                            if (2 * height1 > actualHeight || scale > 1) {
                                matrix.set(savedMatrix);
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                        }
                        Log.d("SCALE_X_Y::", "" + scale);
                    }
                    //todo - uncomment this to enable rotation
                    if (lastEvent != null && event.getPointerCount() == 2 || event.getPointerCount() == 3) {
                        newRot = rotation(event);
                        float r = newRot - d;
                        float[] values = new float[9];
                        matrix.getValues(values);
                        float tx = values[2];
                        float ty = values[5];
                        float sx = values[0];
                        float xc = (view.getWidth() / 2) * sx;
                        float yc = (view.getHeight() / 2) * sx;
                        matrix.postRotate(r, tx + xc, ty + yc);
                    }
                }
                break;
        }

        editableView.setImageMatrix(matrix);

        bmap = Bitmap.createBitmap(editableView.getWidth(), editableView.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmap);
        editableView.draw(canvas);

        //fin.setImageBitmap(bmap);
        return true;
    }*/

    //without rotation
    public boolean onTouch(View v, MotionEvent event) {
        // handle touch events here
        editableView = (ImageView) v;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.d("SCALE_XY_MOTION", "1");
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.d("SCALE_XY_MOTION", "2");
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                Log.d("SCALE_XY_MOTION", "3");
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    Log.d("SCALE_XY_MOTION", "DRAG");
                    matrix.set(savedMatrix);
                    matrix.getValues(matrixValues);
                    matrixX = matrixValues[2];
                    matrixY = matrixValues[5];
                    width = matrixValues[0] * (((ImageView) editableView).getDrawable()
                            .getIntrinsicWidth());
                    height = matrixValues[4] * (((ImageView) editableView).getDrawable()
                            .getIntrinsicHeight());

                    dx = event.getX() - start.x;
                    dy = event.getY() - start.y;
                    //if image will go oustside top bound
                    if (matrixY + dy < 0) {
                        dy = -matrixY;
                    }
                    //if image will go outside right bound
                    if (matrixX + dx + width > editableView.getWidth()) {
                        dx = editableView.getWidth() - matrixX - width;
                    }
                    //if image will go outside left bound
                    if (matrixX + dx < 0) {
                        dx = -matrixX;
                    }
                    //if image will go outside bottom bound
                    if (matrixY + dy + height > editableView.getHeight()) {
                        dy = editableView.getHeight() - matrixY - height;
                    }
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    Log.d("SCALE_XY_MOTION", "ZOOM");
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        float scale = (newDist / oldDist);
                        editableView.getImageMatrix().getValues(imagematrixValues);
                        float width1 = imagematrixValues[0] * (((ImageView) editableView).getDrawable()
                                .getIntrinsicWidth());
                        float height1 = imagematrixValues[4] * (((ImageView) editableView).getDrawable()
                                .getIntrinsicHeight());
                        if ((actualHeight > height1 && actualWidth > width1) || scale < 1) {
                            if (2 * height1 > actualHeight || scale > 1) {
                                matrix.set(savedMatrix);
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                        }
                        Log.d("SCALE_X_Y::", "" + scale);
                    }
                }
                break;
        }

        editableView.setImageMatrix(matrix);

        bmap = Bitmap.createBitmap(editableView.getWidth(), editableView.getHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bmap);
        editableView.draw(canvas);

        //fin.setImageBitmap(bmap);
        return true;
    }

    private void setPreview() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, width);
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
    }

    /**
     * Determine the space between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        float s = x * x + y * y;
        return (float) Math.sqrt(s);
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Calculate the degree to be rotated by.
     *
     * @param event
     * @return Degrees
     */
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private int getRotatedAngle(Matrix mMatrixx) {
        float[] v = new float[9];
        mMatrixx.getValues(v);
        float rAngle = -1 * Math.round(Math.atan2(v[Matrix.MSKEW_X], v[Matrix.MSCALE_X]) * (180 / Math.PI));
        if (rAngle < 0) {
            rAngle = 360 + rAngle;
        }
        return Math.abs((int) rAngle);
    }


    private int getHorizontalAdditionalDistanceBelow180(double angle, double diagonalLength) {
        double reqLength = (Math.sin(Math.toRadians(angle)) * diagonalLength);
        return (int) Math.abs(reqLength);
    }

    private int getVerticalAdditionalDistanceBelow180(double angle, double diagonalLength) {
        double reqLength = (Math.sin(Math.toRadians(angle)) * diagonalLength);
        return (int) Math.abs(reqLength);
    }

    @OnClick(R.id.frontcam_btn) void  onClickFrontCam(View view)
    {
        if(isCamFlippable) {
            isFrontCamEnabled = !isFrontCamEnabled;
            if (this.preview.canSwitchCamera()) {
                int cameraId = getNextCameraId();
                userSwitchToCamera(cameraId);
            }
        }
    }

    private void userSwitchToCamera(int cameraId) {
        if( MyDebug.LOG )
            Log.d(TAG, "userSwitchToCamera: " + cameraId);
        // prevent slowdown if user repeatedly clicks:
        applicationInterface.reset(true);
        this.preview.setCamera(cameraId);
        // no need to call mainUI.setSwitchCameraContentDescription - this will be called from Preview.cameraSetup when the
        // new camera is opened
    }

    public boolean isMultiCamEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return is_multi_cam && sharedPreferences.getBoolean(PreferenceKeys.MultiCamButtonPreferenceKey, true);
    }
    private int getActualCameraId() {
        if( preview.getCameraController() == null )
            return applicationInterface.getCameraIdPref();
        else
            return preview.getCameraId();
    }

    public int getNextCameraId() {
        if (MyDebug.LOG)
            Log.d(TAG, "getNextCameraId");
        int cameraId = getActualCameraId();
        if (MyDebug.LOG)
            Log.d(TAG, "current cameraId: " + cameraId);
        if (this.preview.canSwitchCamera()) {
            if (isMultiCamEnabled()) {
                // don't use preview.getCameraController(), as it may be null if user quickly switches between cameras
                switch (preview.getCameraControllerManager().getFacing(cameraId)) {
                    case FACING_BACK:
                        if (front_camera_ids.size() > 0)
                            cameraId = front_camera_ids.get(0);
                        else if (other_camera_ids.size() > 0)
                            cameraId = other_camera_ids.get(0);
                        break;
                    case FACING_FRONT:
                        if (other_camera_ids.size() > 0)
                            cameraId = other_camera_ids.get(0);
                        else if (back_camera_ids.size() > 0)
                            cameraId = back_camera_ids.get(0);
                        break;
                    default:
                        if (back_camera_ids.size() > 0)
                            cameraId = back_camera_ids.get(0);
                        else if (front_camera_ids.size() > 0)
                            cameraId = front_camera_ids.get(0);
                        break;
                }
            } else {
                int n_cameras = preview.getCameraControllerManager().getNumberOfCameras();
                cameraId = (cameraId + 1) % n_cameras;
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "next cameraId: " + cameraId);
        return cameraId;
    }


    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        finish();
    }

    private void previewSettings() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, width);
        middleLayer.setLayoutParams(layoutParams);
        FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(width, height - width);
        layoutParams2.gravity = Gravity.BOTTOM;
        captureLayout.setLayoutParams(layoutParams2);
        FrameLayout.LayoutParams layoutParams4 = new FrameLayout.LayoutParams(width, width);
        editableView.setLayoutParams(layoutParams4);


    }


    /* private void cleanBitmapMemory(Bitmap bitmap) {
         imagePreview.setImageResource(android.R.color.transparent);
         cropImageView.setImageResource(android.R.color.transparent);
         if (bitmap != null && !bitmap.isRecycled()) {
             bitmap = null;
             System.gc();
         }
     }*/
    public ImageView getCelebrityImgv() {
        return editableView;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setDeviceDefaults() {
        if (MyDebug.LOG)
            Log.d(TAG, "setDeviceDefaults");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean is_samsung = Build.MANUFACTURER.toLowerCase(Locale.US).contains("samsung");
        boolean is_oneplus = Build.MANUFACTURER.toLowerCase(Locale.US).contains("oneplus");
        if (is_samsung || is_oneplus) {
            if (MyDebug.LOG)
                Log.d(TAG, "set fake flash for camera2");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(PreferenceKeys.Camera2FakeFlashPreferenceKey, true);
            editor.apply();
        }
    }

    private void initCamera2Support() {
        if (MyDebug.LOG)
            Log.d(TAG, "initCamera2Support");
        supports_camera2 = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CameraControllerManager2 manager2 = new CameraControllerManager2(this);
            supports_camera2 = false;
            int n_cameras = manager2.getNumberOfCameras();
            if (n_cameras == 0) {
                if (MyDebug.LOG)
                    Log.d(TAG, "Camera2 reports 0 cameras");
                supports_camera2 = false;
            }
            for (int i = 0; i < n_cameras && !supports_camera2; i++) {
                if (manager2.allowCamera2Support(i)) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "camera " + i + " has at least limited support for Camera2 API");
                    supports_camera2 = true;
                }
            }
        }

        //test_force_supports_camera2 = true; // test
        if (test_force_supports_camera2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (MyDebug.LOG)
                    Log.d(TAG, "forcing supports_camera2");
                supports_camera2 = true;
            }
        }

        if (MyDebug.LOG)
            Log.d(TAG, "supports_camera2? " + supports_camera2);

        // handle the switch from a boolean preference_use_camera2 to String preference_camera_api
        // that occurred in v1.48
        if (supports_camera2) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (!sharedPreferences.contains(PreferenceKeys.CameraAPIPreferenceKey) // doesn't have the new key set yet
                    && sharedPreferences.contains("preference_use_camera2") // has the old key set
                    && sharedPreferences.getBoolean("preference_use_camera2", false) // and camera2 was enabled
            ) {
                if (MyDebug.LOG)
                    Log.d(TAG, "transfer legacy camera2 boolean preference to new api option");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PreferenceKeys.CameraAPIPreferenceKey, "preference_camera_api_camera2");
                editor.remove("preference_use_camera2"); // remove the old key, just in case
                editor.apply();
            }
        }
    }


    @Override
    protected void onDestroy() {
        if (MyDebug.LOG) {
            Log.d(TAG, "onDestroy");
            Log.d(TAG, "size of preloaded_bitmap_resources: " + preloaded_bitmap_resources.size());
        }
        activity_count--;
        cancelImageSavingNotification();

        waitUntilImageQueueEmpty();

        preview.onDestroy();
        if (applicationInterface != null) {
            applicationInterface.onDestroy();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity_count == 0) {
            if (MyDebug.LOG)
                Log.d(TAG, "release renderscript contexts");
            RenderScript.releaseAllContexts();
        }
        for (Map.Entry<Integer, Bitmap> entry : preloaded_bitmap_resources.entrySet()) {
            if (MyDebug.LOG)
                Log.d(TAG, "recycle: " + entry.getKey());
            entry.getValue().recycle();
        }
        preloaded_bitmap_resources.clear();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy done");
    }

    private void setFirstTimeFlag() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PreferenceKeys.FirstTimePreferenceKey, true);
        editor.apply();
    }

    private static String getOnlineHelpUrl(String append) {
        return "https://opencamera.sourceforge.io/" + append;
    }

    public void audioTrigger() {
        if (MyDebug.LOG)
            Log.d(TAG, "ignore audio trigger due to popup open");
        if (popupIsOpen()) {
            if (MyDebug.LOG)
                Log.d(TAG, "ignore audio trigger due to popup open");
        } else if (camera_in_background) {
            if (MyDebug.LOG)
                Log.d(TAG, "ignore audio trigger due to camera in background");
        } else if (preview.isTakingPhotoOrOnTimer()) {
            if (MyDebug.LOG)
                Log.d(TAG, "ignore audio trigger due to already taking photo or on timer");
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "schedule take picture due to loud noise");
            //takePicture();
            this.runOnUiThread(new Runnable() {
                public void run() {
                    if (MyDebug.LOG)
                        Log.d(TAG, "taking picture due to audio trigger");
                    takePicture(false);
                }
            });
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (MyDebug.LOG)
            Log.d(TAG, "onKeyDown: " + keyCode);
        boolean handled = mainUI.onKeyDown(keyCode, event);
        if (handled)
            return true;
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (MyDebug.LOG)
            Log.d(TAG, "onKeyUp: " + keyCode);
        mainUI.onKeyUp(keyCode, event);
        return super.onKeyUp(keyCode, event);
    }

    private final SensorEventListener accelerometerListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            preview.onAccelerometerSensorChanged(event);
        }
    };

    private final BroadcastReceiver cameraReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MyDebug.LOG)
                Log.d(TAG, "cameraReceiver.onReceive");
            PhotoSelfieCameraActivity.this.takePicture(false);
        }
    };

    @Override
    protected void onResume() {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "onResume");
            debug_time = System.currentTimeMillis();
        }
        super.onResume();
        cancelImageSavingNotification();
        getWindow().getDecorView().getRootView().setBackgroundColor(Color.BLACK);
        mSensorManager.registerListener(accelerometerListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        magneticSensor.registerMagneticListener(mSensorManager);
        orientationEventListener.enable();
        registerReceiver(cameraReceiver, new IntentFilter("com.miband2.action.CAMERA"));
        speechControl.initSpeechRecognizer();
        initLocation();
        initGyroSensors();
        soundPoolManager.initSound();
        soundPoolManager.loadSound(R.raw.mybeep);
        soundPoolManager.loadSound(R.raw.mybeep_hi);
        mainUI.layoutUI();
        applicationInterface.reset(); // should be called before opening the camera in preview.onResume()
        preview.onResume();
        if (MyDebug.LOG) {
            Log.d(TAG, "onResume: total time to resume: " + (System.currentTimeMillis() - debug_time));
        }
        frameLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = frameLayout.getMeasuredWidth();
                int height = frameLayout.getMeasuredHeight();
                actualHeight = height;
                actualWidth = width;
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!this.camera_in_background && hasFocus) {
            initImmersiveMode();
        }
    }

    @Override
    protected void onPause() {
        super.onPause(); // docs say to call this before freeing other things
        mSensorManager.unregisterListener(accelerometerListener);
        magneticSensor.unregisterMagneticListener(mSensorManager);
        orientationEventListener.disable();
        try {
            unregisterReceiver(cameraReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        freeAudioListener(false);
        speechControl.stopSpeechRecognizer();
        applicationInterface.stopPanorama(true); // in practice not needed as we should stop panorama when camera is closed, but good to do it explicitly here, before disabling the gyro sensors
        applicationInterface.getGyroSensor().disableSensors();
        soundPoolManager.releaseSound();
        applicationInterface.clearLastImages(); // this should happen when pausing the preview, but call explicitly just to be safe
        applicationInterface.getDrawPreview().clearGhostImage();
        preview.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        preview.setCameraDisplayOrientation();
        super.onConfigurationChanged(newConfig);
    }

    public void waitUntilImageQueueEmpty() {
        if (MyDebug.LOG)
            Log.d(TAG, "waitUntilImageQueueEmpty");
        applicationInterface.getImageSaver().waitUntilDone();
    }

    private boolean longClickedTakePhoto() {
        if (supportsFastBurst()) {
            CameraController.Size current_size = preview.getCurrentPictureSize();
            if (current_size != null && current_size.supports_burst) {
                MyApplicationInterface.PhotoMode photo_mode = applicationInterface.getPhotoMode();
                if (photo_mode == MyApplicationInterface.PhotoMode.Standard &&
                        applicationInterface.isRawOnly(photo_mode)) {
                } else if (photo_mode == MyApplicationInterface.PhotoMode.Standard ||
                        photo_mode == MyApplicationInterface.PhotoMode.FastBurst) {
                    this.takePicturePressed(false, true);
                    return true;
                }
            }
        }
        // return false, so a regular click will still be triggered when the user releases the touch
        return false;
    }

    public void visibleDoneBtn(boolean isVisible)
    {

    }

    public void clickedTakePhoto(View view) {

        if (!PSR_Utils.handleDoubleClick(this))
            return;

        if (!isCaptureClickable)
            return;
        isCamFlippable=false;
        PSR_Utils.showProgressDialog(this);
        this.takePicture(false);
        findViewById(R.id.button_capture_cancel).setClickable(false);
    }

    public boolean popupIsOpen() {
        return mainUI.popupIsOpen();
    }

    public void closePopup() {
        mainUI.closePopup();
    }

    private final PreferencesListener preferencesListener = new PreferencesListener();

    public void onControllerNull() {
        isCaptureClickable = true;
        isCamFlippable=true;
        PSR_Utils.hideProgressDialog();
    }

    class PreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String TAG = "PreferencesListener";


        void startListening() {
            if (MyDebug.LOG)
                Log.d(TAG, "startListening");

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PhotoSelfieCameraActivity.this);
            // n.b., registerOnSharedPreferenceChangeListener warns that we must keep a reference to the listener (which
            // is this class) as long as we want to listen for changes, otherwise the listener may be garbage collected!
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (MyDebug.LOG)
                Log.d(TAG, "onSharedPreferenceChanged: " + key);


            switch (key) {
                // we whitelist preferences where we're sure that we don't need to call updateForSettings() if they've changed
                case "preference_timer":
                case "preference_burst_mode":
                case "preference_burst_interval":
                    //case "preference_ghost_image": // don't whitelist this, as may need to reload ghost image (at fullscreen resolution) if "last" is enabled
                case "preference_touch_capture":
                case "preference_pause_preview":
                case "preference_shutter_sound":
                case "preference_timer_beep":
                case "preference_timer_speak":
                case "preference_volume_keys":
                case "preference_audio_noise_control_sensitivity":
                case "preference_using_saf":
                case "preference_save_photo_prefix":
                case "preference_save_video_prefix":
                case "preference_save_zulu_time":
                case "preference_show_when_locked":
                case "preference_startup_focus":
                case "preference_show_zoom":
                case "preference_show_angle":
                case "preference_show_angle_line":
                case "preference_show_pitch_lines":
                case "preference_angle_highlight_color":
                    //case "preference_show_geo_direction": // don't whitelist these, as if enabled we need to call checkMagneticAccuracy()
                    //case "preference_show_geo_direction_lines": // as above
                case "preference_show_battery":
                case "preference_show_time":
                case "preference_free_memory":
                case "preference_show_iso":
                case "preference_grid":
                case "preference_crop_guide":
                case "preference_show_toasts":
                case "preference_thumbnail_animation":
                case "preference_take_photo_border":
                case "preference_keep_display_on":
                case "preference_max_brightness":
                    //case "preference_hdr_save_expo": // we need to update if this is changed, as it affects whether we request RAW or not in HDR mode when RAW is enabled
                case "preference_front_camera_mirror":
                case "preference_stamp":
                case "preference_stamp_dateformat":
                case "preference_stamp_timeformat":
                case "preference_stamp_gpsformat":
                case "preference_textstamp":
                case "preference_stamp_fontsize":
                case "preference_stamp_font_color":
                case "preference_stamp_style":
                case "preference_background_photo_saving":
                case "preference_record_audio":
                case "preference_record_audio_src":
                case "preference_record_audio_channels":
                case "preference_lock_video":
                case "preference_video_subtitle":
                case "preference_require_location":
                    if (MyDebug.LOG)
                        Log.d(TAG, "this change doesn't require update");
                    break;
                case PreferenceKeys.WaterType:
                    break;
                default:
                    if (MyDebug.LOG)
                        Log.d(TAG, "this change does require update");
                    break;
            }
        }

    }

    public void openSettings() {
        closePopup();
        preview.cancelTimer(); // best to cancel any timer, in case we take a photo while settings window is open, or when changing settings
        preview.cancelRepeat(); // similarly cancel the auto-repeat mode!
        applicationInterface.stopPanorama(true); // important to stop panorama recording, as we might end up as we'll be changing camera parameters when the settings window closes
        stopAudioListeners();

        Bundle bundle = new Bundle();
        bundle.putInt("cameraId", this.preview.getCameraId());
        bundle.putInt("nCameras", preview.getCameraControllerManager().getNumberOfCameras());
        bundle.putString("camera_api", this.preview.getCameraAPI());
        bundle.putBoolean("using_android_l", this.preview.usingCamera2API());
        bundle.putBoolean("supports_auto_stabilise", this.supports_auto_stabilise);
        bundle.putBoolean("supports_flash", this.preview.supportsFlash());
        bundle.putBoolean("supports_force_video_4k", this.supports_force_video_4k);
        bundle.putBoolean("supports_camera2", this.supports_camera2);
        bundle.putBoolean("supports_face_detection", this.preview.supportsFaceDetection());
        bundle.putBoolean("supports_raw", this.preview.supportsRaw());
        bundle.putBoolean("supports_burst_raw", this.supportsBurstRaw());
        bundle.putBoolean("supports_hdr", this.supportsHDR());
        bundle.putBoolean("supports_nr", this.supportsNoiseReduction());
        bundle.putBoolean("supports_panorama", this.supportsPanorama());
        bundle.putBoolean("supports_expo_bracketing", this.supportsExpoBracketing());
        bundle.putBoolean("supports_preview_bitmaps", this.supportsPreviewBitmaps());
        bundle.putInt("max_expo_bracketing_n_images", this.maxExpoBracketingNImages());
        bundle.putBoolean("supports_exposure_compensation", this.preview.supportsExposures());
        bundle.putInt("exposure_compensation_min", this.preview.getMinimumExposure());
        bundle.putInt("exposure_compensation_max", this.preview.getMaximumExposure());
        bundle.putBoolean("supports_iso_range", this.preview.supportsISORange());
        bundle.putInt("iso_range_min", this.preview.getMinimumISO());
        bundle.putInt("iso_range_max", this.preview.getMaximumISO());
        bundle.putBoolean("supports_exposure_time", this.preview.supportsExposureTime());
        bundle.putBoolean("supports_exposure_lock", this.preview.supportsExposureLock());
        bundle.putBoolean("supports_white_balance_lock", this.preview.supportsWhiteBalanceLock());
        bundle.putLong("exposure_time_min", this.preview.getMinimumExposureTime());
        bundle.putLong("exposure_time_max", this.preview.getMaximumExposureTime());
        bundle.putBoolean("supports_white_balance_temperature", this.preview.supportsWhiteBalanceTemperature());
        bundle.putInt("white_balance_temperature_min", this.preview.getMinimumWhiteBalanceTemperature());
        bundle.putInt("white_balance_temperature_max", this.preview.getMaximumWhiteBalanceTemperature());
        bundle.putBoolean("supports_video_stabilization", this.preview.supportsVideoStabilization());
        bundle.putBoolean("can_disable_shutter_sound", this.preview.canDisableShutterSound());
        bundle.putInt("tonemap_max_curve_points", this.preview.getTonemapMaxCurvePoints());
        bundle.putBoolean("supports_tonemap_curve", this.preview.supportsTonemapCurve());
        bundle.putBoolean("supports_photo_video_recording", this.preview.supportsPhotoVideoRecording());
        bundle.putFloat("camera_view_angle_x", preview.getViewAngleX(false));
        bundle.putFloat("camera_view_angle_y", preview.getViewAngleY(false));

        putBundleExtra(bundle, "color_effects", this.preview.getSupportedColorEffects());
        putBundleExtra(bundle, "scene_modes", this.preview.getSupportedSceneModes());
        putBundleExtra(bundle, "white_balances", this.preview.getSupportedWhiteBalances());
        putBundleExtra(bundle, "isos", this.preview.getSupportedISOs());
        bundle.putInt("magnetic_accuracy", magneticSensor.getMagneticAccuracy());
        bundle.putString("iso_key", this.preview.getISOKey());
        if (this.preview.getCameraController() != null) {
            bundle.putString("parameters_string", preview.getCameraController().getParametersString());
        }
        List<String> antibanding = this.preview.getSupportedAntiBanding();
        putBundleExtra(bundle, "antibanding", antibanding);
        if (antibanding != null) {
            String[] entries_arr = new String[antibanding.size()];
            int i = 0;
            for (String value : antibanding) {
                entries_arr[i] = getMainUI().getEntryForAntiBanding(value);
                i++;
            }
            bundle.putStringArray("antibanding_entries", entries_arr);
        }
        List<String> edge_modes = this.preview.getSupportedEdgeModes();
        putBundleExtra(bundle, "edge_modes", edge_modes);
        if (edge_modes != null) {
            String[] entries_arr = new String[edge_modes.size()];
            int i = 0;
            for (String value : edge_modes) {
                entries_arr[i] = getMainUI().getEntryForNoiseReductionMode(value);
                i++;
            }
            bundle.putStringArray("edge_modes_entries", entries_arr);
        }
        List<String> noise_reduction_modes = this.preview.getSupportedNoiseReductionModes();
        putBundleExtra(bundle, "noise_reduction_modes", noise_reduction_modes);
        if (noise_reduction_modes != null) {
            String[] entries_arr = new String[noise_reduction_modes.size()];
            int i = 0;
            for (String value : noise_reduction_modes) {
                entries_arr[i] = getMainUI().getEntryForNoiseReductionMode(value);
                i++;
            }
            bundle.putStringArray("noise_reduction_modes_entries", entries_arr);
        }

        List<CameraController.Size> preview_sizes = this.preview.getSupportedPreviewSizes();
        if (preview_sizes != null) {
            int[] widths = new int[preview_sizes.size()];
            int[] heights = new int[preview_sizes.size()];
            int i = 0;
            for (CameraController.Size size : preview_sizes) {
                widths[i] = size.width;
                heights[i] = size.height;
                i++;
            }
            bundle.putIntArray("preview_widths", widths);
            bundle.putIntArray("preview_heights", heights);
        }
        bundle.putInt("preview_width", preview.getCurrentPreviewSize().width);
        bundle.putInt("preview_height", preview.getCurrentPreviewSize().height);

        List<CameraController.Size> sizes = this.preview.getSupportedPictureSizes(false);
        if (sizes != null) {
            int[] widths = new int[sizes.size()];
            int[] heights = new int[sizes.size()];
            boolean[] supports_burst = new boolean[sizes.size()];
            int i = 0;
            for (CameraController.Size size : sizes) {
                widths[i] = size.width;
                heights[i] = size.height;
                supports_burst[i] = size.supports_burst;
                i++;
            }
            bundle.putIntArray("resolution_widths", widths);
            bundle.putIntArray("resolution_heights", heights);
            bundle.putBooleanArray("resolution_supports_burst", supports_burst);
        }
        if (preview.getCurrentPictureSize() != null) {
            bundle.putInt("resolution_width", preview.getCurrentPictureSize().width);
            bundle.putInt("resolution_height", preview.getCurrentPictureSize().height);
        }


        putBundleExtra(bundle, "flash_values", this.preview.getSupportedFlashValues());
        putBundleExtra(bundle, "focus_values", this.preview.getSupportedFocusValues());

        preferencesListener.startListening();

        setWindowFlagsForSettings();
    }

    @Override
    public void onBackPressed() {
        if (MyDebug.LOG)
            Log.d(TAG, "onBackPressed");
        else if (preview != null && preview.isPreviewPaused()) {
            if (MyDebug.LOG)
                Log.d(TAG, "preview was paused, so unpause it");
            preview.startCameraPreview();
            return;
        } else {
            if (popupIsOpen()) {
                closePopup();
                return;
            }
        }
        super.onBackPressed();
    }

    public boolean usingKitKatImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String immersive_mode = sharedPreferences.getString(PreferenceKeys.ImmersiveModePreferenceKey, "immersive_mode_low_profile");
            if (immersive_mode.equals("immersive_mode_gui") || immersive_mode.equals("immersive_mode_everything"))
                return true;
        }
        return false;
    }

    private Handler immersive_timer_handler = null;
    private Runnable immersive_timer_runnable = null;

    private void setImmersiveTimer() {
        if (immersive_timer_handler != null && immersive_timer_runnable != null) {
            immersive_timer_handler.removeCallbacks(immersive_timer_runnable);
        }
        immersive_timer_handler = new Handler();
        immersive_timer_handler.postDelayed(immersive_timer_runnable = new Runnable() {
            @Override
            public void run() {
                if (MyDebug.LOG)
                    Log.d(TAG, "setImmersiveTimer: run");
                if (!camera_in_background && !popupIsOpen() && usingKitKatImmersiveMode())
                    setImmersiveMode(true);
            }
        }, 5000);
    }

    public void initImmersiveMode() {
        if (!usingKitKatImmersiveMode()) {
            setImmersiveMode(true);
        } else {
            // don't start in immersive mode, only after a timer
            setImmersiveTimer();
        }
    }

    public void setImmersiveMode(boolean on) {
        if (MyDebug.LOG)
            Log.d(TAG, "setImmersiveMode: " + on);
        // n.b., preview.setImmersiveMode() is called from onSystemUiVisibilityChange()
        if (on) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && usingKitKatImmersiveMode()) {
                if (applicationInterface.getPhotoMode() == MyApplicationInterface.PhotoMode.Panorama) {
                    // don't allow the kitkat-style immersive mode for panorama mode (problem that in "full" immersive mode, the gyro spot can't be seen - we could fix this, but simplest to just disallow)
                    getWindow().getDecorView().setSystemUiVisibility(0);
                } else {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
                }
            } else {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                String immersive_mode = sharedPreferences.getString(PreferenceKeys.ImmersiveModePreferenceKey, "immersive_mode_low_profile");
                if (immersive_mode.equals("immersive_mode_low_profile"))
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                else
                    getWindow().getDecorView().setSystemUiVisibility(0);
            }
        } else
            getWindow().getDecorView().setSystemUiVisibility(0);
    }

    public void setBrightnessForCamera(boolean force_max) {
        if (MyDebug.LOG)
            Log.d(TAG, "setBrightnessForCamera");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final WindowManager.LayoutParams layout = getWindow().getAttributes();
        if (force_max || sharedPreferences.getBoolean(PreferenceKeys.getMaxBrightnessPreferenceKey(), true)) {
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        } else {
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        }

        this.runOnUiThread(new Runnable() {
            public void run() {
                getWindow().setAttributes(layout);
            }
        });
    }

    public void setWindowFlagsForCamera() {
        if (MyDebug.LOG)
            Log.d(TAG, "setWindowFlagsForCamera");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preview != null) {
            preview.setCameraDisplayOrientation();
        }
        if (preview != null && mainUI != null) {
            mainUI.layoutUI();
        }
        if (sharedPreferences.getBoolean(PreferenceKeys.getKeepDisplayOnPreferenceKey(), true)) {
            if (MyDebug.LOG)
                Log.d(TAG, "do keep screen on");
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "don't keep screen on");
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        if (sharedPreferences.getBoolean(PreferenceKeys.getShowWhenLockedPreferenceKey(), true)) {
            if (MyDebug.LOG)
                Log.d(TAG, "do show when locked");
            // keep Open Camera on top of screen-lock (will still need to unlock when going to gallery or settings)
            showWhenLocked(true);
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "don't show when locked");
            showWhenLocked(false);
        }

        setBrightnessForCamera(false);

        initImmersiveMode();
        camera_in_background = false;

        magneticSensor.clearDialog(); // if the magnetic accuracy was opened, it must have been closed now
    }

    private void setWindowFlagsForSettings() {
        setWindowFlagsForSettings(true);
    }

    public void setWindowFlagsForSettings(boolean set_lock_protect) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (set_lock_protect) {
            // settings should still be protected by screen lock
            showWhenLocked(false);
        }
        {
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(layout);
        }

        setImmersiveMode(false);
        camera_in_background = true;
    }

    private void showWhenLocked(boolean show) {
        {
            if (show) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
        }
    }

    public void showAlert(final AlertDialog alert) {
        if (MyDebug.LOG)
            Log.d(TAG, "showAlert");
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                alert.show();
            }
        }, 20);
        // note that 1ms usually fixes the problem, but not always; 10ms seems fine, have set 20ms
        // just in case
    }

    public void savingImage(final boolean started) {
       /* if( MyDebug.LOG )
            Log.d(TAG, "savingImage: " + started);

        this.runOnUiThread(new Runnable() {
            public void run() {
                final ImageButton galleryButton = findViewById(R.id.gallery);
                if( started ) {
                    //galleryButton.setColorFilter(0x80ffffff, PorterDuff.Mode.MULTIPLY);
                    if( gallery_save_anim == null ) {
                        gallery_save_anim = ValueAnimator.ofInt(Color.argb(200, 255, 255, 255), Color.argb(63, 255, 255, 255));
                        gallery_save_anim.setEvaluator(new ArgbEvaluator());
                        gallery_save_anim.setRepeatCount(ValueAnimator.INFINITE);
                        gallery_save_anim.setRepeatMode(ValueAnimator.REVERSE);
                        gallery_save_anim.setDuration(500);
                    }
                    gallery_save_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            galleryButton.setColorFilter((Integer)animation.getAnimatedValue(), PorterDuff.Mode.MULTIPLY);
                        }
                    });
                    gallery_save_anim.start();
                }
                else
                if( gallery_save_anim != null ) {
                    gallery_save_anim.cancel();
                }
                galleryButton.setColorFilter(null);
            }
        });*/
    }

    public void imageQueueChanged() {

        if (applicationInterface.getImageSaver().getNImagesToSave() == 0) {
            cancelImageSavingNotification();
        }
    }

    private void cancelImageSavingNotification() {
        if (MyDebug.LOG)
            Log.d(TAG, "cancelImageSavingNotification");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.cancel(image_saving_notification_id);
        }
    }

    public void clickedGallery(View view) {
        if (MyDebug.LOG)
            Log.d(TAG, "clickedGallery");
        openGallery();
    }

    private void openGallery() {
        if (MyDebug.LOG)
            Log.d(TAG, "openGallery");
        Uri uri = applicationInterface.getStorageUtils().getLastMediaScanned();
    /*    try {
            SingleTon.getInstance().setCapturedImageUri(getApplicationInterface().getStorageUtils().getLastMediaScanned());
            startActivity(new Intent(this, ImageEditingActivity.class));
            finish();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        boolean is_raw = false; // note that getLastMediaScanned() will never return RAW images, as we only record JPEGs
        if (uri == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "go to latest media");
            StorageUtils.Media media = applicationInterface.getStorageUtils().getLatestMedia();
            if (media != null) {
                uri = media.uri;
                is_raw = media.path != null && media.path.toLowerCase(Locale.US).endsWith(".dng");
            }
        }

        if (uri != null) {
            // check uri exists
            if (MyDebug.LOG) {
                Log.d(TAG, "found most recent uri: " + uri);
                Log.d(TAG, "is_raw: " + is_raw);
            }
            try {
                ContentResolver cr = getContentResolver();
                ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
                if (pfd == null) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "uri no longer exists (1): " + uri);
                    uri = null;
                    is_raw = false;
                } else {
                    pfd.close();
                }
            } catch (IOException e) {
                if (MyDebug.LOG)
                    Log.d(TAG, "uri no longer exists (2): " + uri);
                uri = null;
                is_raw = false;
            }
        }
        if (uri == null) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            is_raw = false;
        }
        if (!is_test) {
            if (MyDebug.LOG)
                Log.d(TAG, "launch uri:" + uri);
            final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
            boolean done = false;
            if (!is_raw) {
                if (MyDebug.LOG)
                    Log.d(TAG, "try REVIEW_ACTION");
                try {
                    Intent intent = new Intent(REVIEW_ACTION, uri);
                    this.startActivity(intent);
                    done = true;
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (!done) {
                if (MyDebug.LOG)
                    Log.d(TAG, "try ACTION_VIEW");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    try {
                        this.startActivity(intent);
                    } catch (SecurityException e2) {
                        // have received this crash from Google Play - don't display a toast, simply do nothing
                        Log.e(TAG, "SecurityException from ACTION_VIEW startActivity");
                        e2.printStackTrace();
                    }
                } else {
                }
            }
        }
    }


    static private void putBundleExtra(Bundle bundle, String key, List<String> values) {
        if (values != null) {
            String[] values_arr = new String[values.size()];
            int i = 0;
            for (String value : values) {
                values_arr[i] = value;
                i++;
            }
            bundle.putStringArray(key, values_arr);
        }
    }

    public void takePicture(boolean photo_snapshot) {
        isCaptureClickable = false;

        if (applicationInterface.getPhotoMode() == MyApplicationInterface.PhotoMode.Panorama) {
            if (preview.isTakingPhoto()) {
                if (MyDebug.LOG)
                    Log.d(TAG, "ignore whilst taking panorama photo");
            } else if (applicationInterface.getGyroSensor().isRecording()) {
                if (MyDebug.LOG)
                    Log.d(TAG, "panorama complete");
                applicationInterface.finishPanorama();
                return;
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "start panorama");
                applicationInterface.startPanorama();
            }
        }

        this.takePicturePressed(photo_snapshot, false);
    }

    public boolean lastContinuousFastBurst() {
        return this.last_continuous_fast_burst;
    }

    public void takePicturePressed(boolean photo_snapshot, boolean continuous_fast_burst) {
        if (MyDebug.LOG)
            Log.d(TAG, "takePicturePressed");

        closePopup();

        this.last_continuous_fast_burst = continuous_fast_burst;
        this.preview.takePicturePressed(photo_snapshot, continuous_fast_burst);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (MyDebug.LOG)
            Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(state);
        if (this.preview != null) {
            preview.onSaveInstanceState(state);
        }
        if (this.applicationInterface != null) {
            applicationInterface.onSaveInstanceState(state);
        }
    }


    public void cameraSetup() {
        if (this.supportsForceVideo4K() && preview.usingCamera2API()) {
            this.disableForceVideo4K();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        {
            View takePhotoButton = findViewById(R.id.take_photo);
            if (sharedPreferences.getBoolean(PreferenceKeys.ShowTakePhotoPreferenceKey, true)) {
                if (!mainUI.inImmersiveMode()) {
                    takePhotoButton.setVisibility(View.VISIBLE);
                }
            } else {
                takePhotoButton.setVisibility(View.INVISIBLE);
            }
        }
        mainUI.setTakePhotoIcon();
    }

    public boolean supportsAutoStabilise() {
        if (applicationInterface.isRawOnly())
            return false; // if not saving JPEGs, no point having auto-stabilise mode, as it won't affect the RAW images
        if (applicationInterface.getPhotoMode() == MyApplicationInterface.PhotoMode.Panorama)
            return false; // not supported in panorama mode
        return this.supports_auto_stabilise;
    }

    public boolean supportsDRO() {
        if (applicationInterface.isRawOnly(MyApplicationInterface.PhotoMode.DRO))
            return false; // if not saving JPEGs, no point having DRO mode, as it won't affect the RAW images
        // require at least Android 5, for the Renderscript support in HDRProcessor
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }

    public boolean supportsHDR() {
        // we also require the device have sufficient memory to do the processing
        // also require at least Android 5, for the Renderscript support in HDRProcessor
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && large_heap_memory >= 128 && preview.supportsExpoBracketing());
    }

    public boolean supportsExpoBracketing() {
        if (applicationInterface.isImageCaptureIntent())
            return false; // don't support expo bracketing mode if called from image capture intent
        return preview.supportsExpoBracketing();
    }

    public boolean supportsFocusBracketing() {
        if (applicationInterface.isImageCaptureIntent())
            return false; // don't support focus bracketing mode if called from image capture intent
        return preview.supportsFocusBracketing();
    }

    public boolean supportsPanorama() {
        if (applicationInterface.isImageCaptureIntent())
            return false;
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && large_heap_memory >= 256 && applicationInterface.getGyroSensor().hasSensors());
    }

    public boolean supportsFastBurst() {
        if (applicationInterface.isImageCaptureIntent())
            return false; // don't support burst mode if called from image capture intent
        return (preview.usingCamera2API() && large_heap_memory >= 512 && preview.supportsBurst());
    }

    public boolean supportsNoiseReduction() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && preview.usingCamera2API() && large_heap_memory >= 512 && preview.supportsBurst() && preview.supportsExposureTime());
    }

    public boolean supportsBurstRaw() {
        return (large_heap_memory >= 512);
    }

    public boolean supportsPreviewBitmaps() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && preview.getView() instanceof TextureView && large_heap_memory >= 128;
    }

    private int maxExpoBracketingNImages() {
        return preview.maxExpoBracketingNImages();
    }

    public boolean supportsForceVideo4K() {
        return this.supports_force_video_4k;
    }

    public boolean supportsCamera2() {
        return this.supports_camera2;
    }

    private void disableForceVideo4K() {
        this.supports_force_video_4k = false;
    }

    public Preview getPreview() {
        return this.preview;
    }

    public boolean isCameraInBackground() {
        return this.camera_in_background;
    }

    public PermissionHandler getPermissionHandler() {
        return permissionHandler;
    }

    public MainUI getMainUI() {
        return this.mainUI;
    }


    public MyApplicationInterface getApplicationInterface() {
        return this.applicationInterface;
    }

    public TextFormatter getTextFormatter() {
        return this.textFormatter;
    }

    public SoundPoolManager getSoundPoolManager() {
        return this.soundPoolManager;
    }

    public StorageUtils getStorageUtils() {
        return this.applicationInterface.getStorageUtils();
    }
    public boolean isFrontCamEnabled() {
        return isFrontCamEnabled;
    }

    private boolean isFrontCamEnabled = false;



    private void freeAudioListener(boolean wait_until_done) {
        if (MyDebug.LOG)
            Log.d(TAG, "freeAudioListener");
    }


    public void stopAudioListeners() {
        freeAudioListener(true);
        if (speechControl.hasSpeechRecognition()) {
            // no need to free the speech recognizer, just stop it
            speechControl.stopListening();
        }
    }

    public void initLocation() {
    }

    private void initGyroSensors() {
        if (MyDebug.LOG)
            Log.d(TAG, "initGyroSensors");
        if (applicationInterface.getPhotoMode() == MyApplicationInterface.PhotoMode.Panorama) {
            applicationInterface.getGyroSensor().enableSensors();
        } else {
            applicationInterface.getGyroSensor().disableSensors();
        }
    }

    public void speak(String text) {
        if (textToSpeech != null && textToSpeechSuccess) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (MyDebug.LOG)
            Log.d(TAG, "onRequestPermissionsResult: requestCode " + requestCode);
        permissionHandler.onRequestPermissionsResult(requestCode, grantResults);
    }

    public void takePhotoButtonLongClickCancelled() {
        if (MyDebug.LOG)
            Log.d(TAG, "takePhotoButtonLongClickCancelled");
        if (preview.getCameraController() != null && preview.getCameraController().isContinuousBurstInProgress()) {
            preview.getCameraController().stopContinuousBurst();
        }
    }



    public static View getViewNow() {
        return view;
    }
}

