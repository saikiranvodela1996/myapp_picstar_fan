package com.picstar.picstarapp.campkg.camui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.activities.PhotoSelfieCameraActivity;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.others.GyroSensor;
import com.picstar.picstarapp.campkg.others.MyApplicationInterface;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PreferenceKeys;
import com.picstar.picstarapp.campkg.preview.Preview;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DrawPreview {
    private static final String TAG = "DrawPreview";

    private final PhotoSelfieCameraActivity main_activity;
    private final MyApplicationInterface applicationInterface;
    private final SharedPreferences sharedPreferences;
    private boolean has_settings;
    private MyApplicationInterface.PhotoMode photoMode;
    private int angle_highlight_color_pref;
    private boolean take_photo_border_pref;
    private boolean preview_size_wysiwyg_pref;
    private boolean show_angle_line_pref;
    private boolean show_pitch_lines_pref;
    private boolean show_geo_direction_lines_pref;
    private boolean immersive_mode_everything_pref;
    private String preference_grid_pref;
    private String ghost_image_pref;
    private String ghost_selected_image_pref = "";
    private Bitmap ghost_selected_image_bitmap;
    private boolean want_histogram;
    private Preview.HistogramType histogram_type;
    private boolean want_zebra_stripes;
    private int zebra_stripes_threshold;
    private boolean want_focus_peaking;
    private int focus_peaking_color_pref;
    private final Paint p = new Paint();
    private final RectF draw_rect = new RectF();
    private final float scale;
    private final float stroke_width; // stroke_width used for various UI elements
    private final static double close_level_angle = 1.0f;
    private Bitmap location_bitmap;
    private Bitmap location_off_bitmap;
    private Bitmap raw_jpeg_bitmap;
    private Bitmap raw_only_bitmap;
    private Bitmap auto_stabilise_bitmap;
    private Bitmap dro_bitmap;
    private Bitmap hdr_bitmap;
    private Bitmap panorama_bitmap;
    private Bitmap expo_bitmap;
    private Bitmap focus_bracket_bitmap;
    private Bitmap burst_bitmap;
    private Bitmap nr_bitmap;
    private Bitmap photostamp_bitmap;
    private Bitmap flash_bitmap;
    private Bitmap face_detection_bitmap;
    private Bitmap audio_disabled_bitmap;
    private Bitmap high_speed_fps_bitmap;
    private Bitmap slow_motion_bitmap;
    private Bitmap time_lapse_bitmap;
    private Bitmap rotate_left_bitmap;
    private Bitmap rotate_right_bitmap;
    private final Rect icon_dest = new Rect();
    private final Path path = new Path();
    private Bitmap last_thumbnail; // thumbnail of last picture taken
    private volatile boolean thumbnail_anim; // whether we are displaying the thumbnail animation; must be volatile for test project reading the state
    private long thumbnail_anim_start_ms = -1; // time that the thumbnail animation started
    private final RectF thumbnail_anim_src_rect = new RectF();
    private final RectF thumbnail_anim_dst_rect = new RectF();
    private final Matrix thumbnail_anim_matrix = new Matrix();
    private boolean last_thumbnail_is_video; // whether thumbnail is for video
    private boolean show_last_image; // whether to show the last image as part of "pause preview"
    private final RectF last_image_src_rect = new RectF();
    private final RectF last_image_dst_rect = new RectF();
    private final Matrix last_image_matrix = new Matrix();
    private boolean allow_ghost_last_image; // whether to allow ghosting the last image
    private boolean taking_picture; // true iff camera is in process of capturing a picture (including any necessary prior steps such as autofocus, flash/precapture)
    private boolean front_screen_flash; // true iff the front screen display should maximise to simulate flash
    private boolean continuous_focus_moving;
    private long continuous_focus_moving_ms;
    private boolean enable_gyro_target_spot;
    private final List<float[]> gyro_directions = new ArrayList<>();
    private final float[] transformed_gyro_direction = new float[3];
    private final float[] gyro_direction_up = new float[3];
    private final float[] transformed_gyro_direction_up = new float[3];
    private float view_angle_x_preview;
    private float view_angle_y_preview;
    private long last_view_angles_time;

    public DrawPreview(PhotoSelfieCameraActivity main_activity, MyApplicationInterface applicationInterface) {
        if (MyDebug.LOG)
            Log.d(TAG, "DrawPreview");
        this.main_activity = main_activity;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
        this.applicationInterface = applicationInterface;
        p.setAntiAlias(true);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setStrokeCap(Paint.Cap.ROUND);
        scale = getContext().getResources().getDisplayMetrics().density;
        this.stroke_width = (1.0f * scale + 0.5f); // convert dps to pixels
        p.setStrokeWidth(this.stroke_width);

        location_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        location_off_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        raw_jpeg_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        raw_only_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        auto_stabilise_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        dro_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        hdr_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        panorama_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        expo_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        focus_bracket_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        burst_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        nr_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        photostamp_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        flash_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        face_detection_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        audio_disabled_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        high_speed_fps_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        slow_motion_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        time_lapse_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        rotate_left_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
        rotate_right_bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher_background);
    }

    public void onDestroy() {
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy");
        // clean up just in case
        if (location_bitmap != null) {
            location_bitmap.recycle();
            location_bitmap = null;
        }
        if (location_off_bitmap != null) {
            location_off_bitmap.recycle();
            location_off_bitmap = null;
        }
        if (raw_jpeg_bitmap != null) {
            raw_jpeg_bitmap.recycle();
            raw_jpeg_bitmap = null;
        }
        if (raw_only_bitmap != null) {
            raw_only_bitmap.recycle();
            raw_only_bitmap = null;
        }
        if (auto_stabilise_bitmap != null) {
            auto_stabilise_bitmap.recycle();
            auto_stabilise_bitmap = null;
        }
        if (dro_bitmap != null) {
            dro_bitmap.recycle();
            dro_bitmap = null;
        }
        if (hdr_bitmap != null) {
            hdr_bitmap.recycle();
            hdr_bitmap = null;
        }
        if (panorama_bitmap != null) {
            panorama_bitmap.recycle();
            panorama_bitmap = null;
        }
        if (expo_bitmap != null) {
            expo_bitmap.recycle();
            expo_bitmap = null;
        }
        if (focus_bracket_bitmap != null) {
            focus_bracket_bitmap.recycle();
            focus_bracket_bitmap = null;
        }
        if (burst_bitmap != null) {
            burst_bitmap.recycle();
            burst_bitmap = null;
        }
        if (nr_bitmap != null) {
            nr_bitmap.recycle();
            nr_bitmap = null;
        }
        if (photostamp_bitmap != null) {
            photostamp_bitmap.recycle();
            photostamp_bitmap = null;
        }
        if (flash_bitmap != null) {
            flash_bitmap.recycle();
            flash_bitmap = null;
        }
        if (face_detection_bitmap != null) {
            face_detection_bitmap.recycle();
            face_detection_bitmap = null;
        }
        if (audio_disabled_bitmap != null) {
            audio_disabled_bitmap.recycle();
            audio_disabled_bitmap = null;
        }
        if (high_speed_fps_bitmap != null) {
            high_speed_fps_bitmap.recycle();
            high_speed_fps_bitmap = null;
        }
        if (slow_motion_bitmap != null) {
            slow_motion_bitmap.recycle();
            slow_motion_bitmap = null;
        }
        if (time_lapse_bitmap != null) {
            time_lapse_bitmap.recycle();
            time_lapse_bitmap = null;
        }
        if (rotate_left_bitmap != null) {
            rotate_left_bitmap.recycle();
            rotate_left_bitmap = null;
        }
        if (rotate_right_bitmap != null) {
            rotate_right_bitmap.recycle();
            rotate_right_bitmap = null;
        }

        if (ghost_selected_image_bitmap != null) {
            ghost_selected_image_bitmap.recycle();
            ghost_selected_image_bitmap = null;
        }
        ghost_selected_image_pref = "";
    }

    private Context getContext() {
        return main_activity;
    }

    /**
     * Sets a current thumbnail for a photo or video just taken. Used for thumbnail animation,
     * and when ghosting the last image.
     */
    public void updateThumbnail(Bitmap thumbnail, boolean is_video, boolean want_thumbnail_animation) {
        if (MyDebug.LOG)
            Log.d(TAG, "updateThumbnail");
        if (want_thumbnail_animation && applicationInterface.getThumbnailAnimationPref()) {
            if (MyDebug.LOG)
                Log.d(TAG, "thumbnail_anim started");
            thumbnail_anim = true;
            thumbnail_anim_start_ms = System.currentTimeMillis();
        }
        Bitmap old_thumbnail = this.last_thumbnail;
        this.last_thumbnail = thumbnail;
        this.last_thumbnail_is_video = is_video;
        this.allow_ghost_last_image = true;
        if (old_thumbnail != null) {
            // only recycle after we've set the new thumbnail
            old_thumbnail.recycle();
        }
    }

    public boolean hasThumbnailAnimation() {
        return this.thumbnail_anim;
    }

    /**
     * Displays the thumbnail as a fullscreen image (used for pause preview option).
     */
    public void showLastImage() {
        if (MyDebug.LOG)
            Log.d(TAG, "showLastImage");
        this.show_last_image = true;
    }

    public void clearLastImage() {
        if (MyDebug.LOG)
            Log.d(TAG, "clearLastImage");
        this.show_last_image = false;
    }

    public void clearGhostImage() {
        if (MyDebug.LOG)
            Log.d(TAG, "clearGhostImage");
        this.allow_ghost_last_image = false;
    }

    public void cameraInOperation(boolean in_operation) {
        if (in_operation) {
            taking_picture = true;
        } else {
            taking_picture = false;
            front_screen_flash = false;
        }
    }

    public void turnFrontScreenFlashOn() {
        if (MyDebug.LOG)
            Log.d(TAG, "turnFrontScreenFlashOn");
        front_screen_flash = true;
    }

    public void onContinuousFocusMove(boolean start) {
        if (MyDebug.LOG)
            Log.d(TAG, "onContinuousFocusMove: " + start);
        if (start) {
            if (!continuous_focus_moving) { // don't restart the animation if already in motion
                continuous_focus_moving = true;
                continuous_focus_moving_ms = System.currentTimeMillis();
            }
        }
    }

    public void clearContinuousFocusMove() {
        if (MyDebug.LOG)
            Log.d(TAG, "clearContinuousFocusMove");
        if (continuous_focus_moving) {
            continuous_focus_moving = false;
            continuous_focus_moving_ms = 0;
        }
    }

    public void setGyroDirectionMarker(float x, float y, float z) {
        enable_gyro_target_spot = true;
        this.gyro_directions.clear();
        addGyroDirectionMarker(x, y, z);
        gyro_direction_up[0] = 0.f;
        gyro_direction_up[1] = 1.f;
        gyro_direction_up[2] = 0.f;
    }

    public void addGyroDirectionMarker(float x, float y, float z) {
        float[] vector = new float[]{x, y, z};
        this.gyro_directions.add(vector);
    }

    public void clearGyroDirectionMarker() {
        enable_gyro_target_spot = false;
    }

    public void updateSettings() {
        if (MyDebug.LOG)
            Log.d(TAG, "updateSettings");

        photoMode = applicationInterface.getPhotoMode();
        if (MyDebug.LOG)
            Log.d(TAG, "photoMode: " + photoMode);

        String angle_highlight_color = sharedPreferences.getString(PreferenceKeys.ShowAngleHighlightColorPreferenceKey, "#14e715");
        angle_highlight_color_pref = Color.parseColor(angle_highlight_color);
        take_photo_border_pref = sharedPreferences.getBoolean(PreferenceKeys.TakePhotoBorderPreferenceKey, true);
        preview_size_wysiwyg_pref = sharedPreferences.getString(PreferenceKeys.PreviewSizePreferenceKey, "preference_preview_size_wysiwyg").equals("preference_preview_size_wysiwyg");
        show_angle_line_pref = sharedPreferences.getBoolean(PreferenceKeys.ShowAngleLinePreferenceKey, false);
        show_pitch_lines_pref = sharedPreferences.getBoolean(PreferenceKeys.ShowPitchLinesPreferenceKey, false);
        show_geo_direction_lines_pref = sharedPreferences.getBoolean(PreferenceKeys.ShowGeoDirectionLinesPreferenceKey, false);
        String immersive_mode = sharedPreferences.getString(PreferenceKeys.ImmersiveModePreferenceKey, "immersive_mode_low_profile");
        immersive_mode_everything_pref = immersive_mode.equals("immersive_mode_everything");
        preference_grid_pref = sharedPreferences.getString(PreferenceKeys.ShowGridPreferenceKey, "preference_grid_none");

        ghost_image_pref = sharedPreferences.getString(PreferenceKeys.GhostImagePreferenceKey, "preference_ghost_image_off");
        if (ghost_image_pref.equals("preference_ghost_image_selected")) {
            String new_ghost_selected_image_pref = sharedPreferences.getString(PreferenceKeys.GhostSelectedImageSAFPreferenceKey, "");
            if (MyDebug.LOG)
                Log.d(TAG, "new_ghost_selected_image_pref: " + new_ghost_selected_image_pref);

            KeyguardManager keyguard_manager = (KeyguardManager) main_activity.getSystemService(Context.KEYGUARD_SERVICE);
            boolean is_locked = keyguard_manager != null && keyguard_manager.inKeyguardRestrictedInputMode();
            if (MyDebug.LOG)
                Log.d(TAG, "is_locked?: " + is_locked);

            if (is_locked) {
                if (ghost_selected_image_bitmap != null) {
                    ghost_selected_image_bitmap.recycle();
                    ghost_selected_image_bitmap = null;
                    ghost_selected_image_pref = ""; // so we'll load the bitmap again when unlocked
                }
            } else if (!new_ghost_selected_image_pref.equals(ghost_selected_image_pref)) {
                if (MyDebug.LOG)
                    Log.d(TAG, "ghost_selected_image_pref has changed");
                ghost_selected_image_pref = new_ghost_selected_image_pref;
                if (ghost_selected_image_bitmap != null) {
                    ghost_selected_image_bitmap.recycle();
                    ghost_selected_image_bitmap = null;
                }
                Uri uri = Uri.parse(ghost_selected_image_pref);
                try {
                    File file = main_activity.getStorageUtils().getFileFromDocumentUriSAF(uri, false);
                    ghost_selected_image_bitmap = loadBitmap(uri, file);
                } catch (IOException e) {
                    Log.e(TAG, "failed to load ghost_selected_image uri: " + uri);
                    e.printStackTrace();
                    ghost_selected_image_bitmap = null;
                    // don't set ghost_selected_image_pref to null, as we don't want to repeatedly try loading the invalid uri
                }
            }
        } else {
            if (ghost_selected_image_bitmap != null) {
                ghost_selected_image_bitmap.recycle();
                ghost_selected_image_bitmap = null;
            }
            ghost_selected_image_pref = "";
        }

        String histogram_pref = sharedPreferences.getString(PreferenceKeys.HistogramPreferenceKey, "preference_histogram_off");
        want_histogram = !histogram_pref.equals("preference_histogram_off") && main_activity.supportsPreviewBitmaps();
        histogram_type = Preview.HistogramType.HISTOGRAM_TYPE_VALUE;
        if (want_histogram) {
            switch (histogram_pref) {
                case "preference_histogram_rgb":
                    histogram_type = Preview.HistogramType.HISTOGRAM_TYPE_RGB;
                    break;
                case "preference_histogram_luminance":
                    histogram_type = Preview.HistogramType.HISTOGRAM_TYPE_LUMINANCE;
                    break;
                case "preference_histogram_value":
                    histogram_type = Preview.HistogramType.HISTOGRAM_TYPE_VALUE;
                    break;
                case "preference_histogram_intensity":
                    histogram_type = Preview.HistogramType.HISTOGRAM_TYPE_INTENSITY;
                    break;
                case "preference_histogram_lightness":
                    histogram_type = Preview.HistogramType.HISTOGRAM_TYPE_LIGHTNESS;
                    break;
            }
        }

        String zebra_stripes_value = sharedPreferences.getString(PreferenceKeys.ZebraStripesPreferenceKey, "0");
        try {
            zebra_stripes_threshold = Integer.parseInt(zebra_stripes_value);
        } catch (NumberFormatException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "failed to parse zebra_stripes_value: " + zebra_stripes_value);
            e.printStackTrace();
            zebra_stripes_threshold = 0;
        }
        want_zebra_stripes = zebra_stripes_threshold != 0 & main_activity.supportsPreviewBitmaps();

        String focus_peaking_pref = sharedPreferences.getString(PreferenceKeys.FocusPeakingPreferenceKey, "preference_focus_peaking_off");
        want_focus_peaking = !focus_peaking_pref.equals("preference_focus_peaking_off") && main_activity.supportsPreviewBitmaps();
        String focus_peaking_color = sharedPreferences.getString(PreferenceKeys.FocusPeakingColorPreferenceKey, "#ffffff");
        focus_peaking_color_pref = Color.parseColor(focus_peaking_color);

        last_view_angles_time = 0; // force view angles to be recomputed

        has_settings = true;
    }

    private void updateCachedViewAngles(long time_ms) {
        if (last_view_angles_time == 0 || time_ms > last_view_angles_time + 10000) {
            if (MyDebug.LOG)
                Log.d(TAG, "update cached view angles");
            Preview preview = main_activity.getPreview();
            view_angle_x_preview = preview.getViewAngleX(true);
            view_angle_y_preview = preview.getViewAngleY(true);
            last_view_angles_time = time_ms;
        }
    }
    private Bitmap loadBitmap(Uri uri, File file) throws IOException {
        if (MyDebug.LOG)
            Log.d(TAG, "loadBitmap: " + uri);
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(main_activity.getContentResolver(), uri);
        } catch (Exception e) {
            // Although Media.getBitmap() is documented as only throwing FileNotFoundException, IOException
            // (with the former being a subset of IOException anyway), I've had SecurityException from
            // Google Play - best to catch everything just in case.
            Log.e(TAG, "MediaStore.Images.Media.getBitmap exception");
            e.printStackTrace();
            throw new IOException();
        }
        if (bitmap == null) {
            // just in case!
            Log.e(TAG, "MediaStore.Images.Media.getBitmap returned null");
            throw new IOException();
        }

        // now need to take exif orientation into account, as some devices or camera apps store the orientation in the exif tag,
        // which getBitmap() doesn't account for
        ExifInterface exif = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // better to use the Uri from Android 7, so this works when images are shared to Vibrance
            try (InputStream inputStream = main_activity.getContentResolver().openInputStream(uri)) {
                exif = new ExifInterface(inputStream);
            }
        } else {
            if (file != null) {
                exif = new ExifInterface(file.getAbsolutePath());
            }
        }
        if (exif != null) {
            int exif_orientation_s = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            boolean needs_tf = false;
            int exif_orientation = 0;
            // see http://jpegclub.org/exif_orientation.html
            // and http://stackoverflow.com/questions/20478765/how-to-get-the-correct-orientation-of-the-image-selected-from-the-default-image
            if (exif_orientation_s == ExifInterface.ORIENTATION_UNDEFINED || exif_orientation_s == ExifInterface.ORIENTATION_NORMAL) {
                // leave unchanged
            } else if (exif_orientation_s == ExifInterface.ORIENTATION_ROTATE_180) {
                needs_tf = true;
                exif_orientation = 180;
            } else if (exif_orientation_s == ExifInterface.ORIENTATION_ROTATE_90) {
                needs_tf = true;
                exif_orientation = 90;
            } else if (exif_orientation_s == ExifInterface.ORIENTATION_ROTATE_270) {
                needs_tf = true;
                exif_orientation = 270;
            } else {
                // just leave unchanged for now
                if (MyDebug.LOG)
                    Log.e(TAG, "    unsupported exif orientation: " + exif_orientation_s);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "    exif orientation: " + exif_orientation);

            if (needs_tf) {
                if (MyDebug.LOG)
                    Log.d(TAG, "    need to rotate bitmap due to exif orientation tag");
                Matrix m = new Matrix();
                m.setRotate(exif_orientation, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
                Bitmap rotated_bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotated_bitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated_bitmap;
                }
            }
        }

        return bitmap;
    }

    private void drawGrids(Canvas canvas) {
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        if (camera_controller == null) {
            return;
        }

        p.setStrokeWidth(stroke_width);

        switch (preference_grid_pref) {
            case "preference_grid_3x3":
                p.setColor(Color.WHITE);
                canvas.drawLine(canvas.getWidth() / 3.0f, 0.0f, canvas.getWidth() / 3.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(2.0f * canvas.getWidth() / 3.0f, 0.0f, 2.0f * canvas.getWidth() / 3.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(0.0f, canvas.getHeight() / 3.0f, canvas.getWidth() - 1.0f, canvas.getHeight() / 3.0f, p);
                canvas.drawLine(0.0f, 2.0f * canvas.getHeight() / 3.0f, canvas.getWidth() - 1.0f, 2.0f * canvas.getHeight() / 3.0f, p);
                break;
            case "preference_grid_phi_3x3":
                p.setColor(Color.WHITE);
                canvas.drawLine(canvas.getWidth() / 2.618f, 0.0f, canvas.getWidth() / 2.618f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(1.618f * canvas.getWidth() / 2.618f, 0.0f, 1.618f * canvas.getWidth() / 2.618f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(0.0f, canvas.getHeight() / 2.618f, canvas.getWidth() - 1.0f, canvas.getHeight() / 2.618f, p);
                canvas.drawLine(0.0f, 1.618f * canvas.getHeight() / 2.618f, canvas.getWidth() - 1.0f, 1.618f * canvas.getHeight() / 2.618f, p);
                break;
            case "preference_grid_4x2":
                p.setColor(Color.GRAY);
                canvas.drawLine(canvas.getWidth() / 4.0f, 0.0f, canvas.getWidth() / 4.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(canvas.getWidth() / 2.0f, 0.0f, canvas.getWidth() / 2.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(3.0f * canvas.getWidth() / 4.0f, 0.0f, 3.0f * canvas.getWidth() / 4.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(0.0f, canvas.getHeight() / 2.0f, canvas.getWidth() - 1.0f, canvas.getHeight() / 2.0f, p);
                p.setColor(Color.WHITE);
                int crosshairs_radius = (int) (20 * scale + 0.5f); // convert dps to pixels

                canvas.drawLine(canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f - crosshairs_radius, canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f + crosshairs_radius, p);
                canvas.drawLine(canvas.getWidth() / 2.0f - crosshairs_radius, canvas.getHeight() / 2.0f, canvas.getWidth() / 2.0f + crosshairs_radius, canvas.getHeight() / 2.0f, p);
                break;
            case "preference_grid_crosshair":
                p.setColor(Color.WHITE);
                canvas.drawLine(canvas.getWidth() / 2.0f, 0.0f, canvas.getWidth() / 2.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(0.0f, canvas.getHeight() / 2.0f, canvas.getWidth() - 1.0f, canvas.getHeight() / 2.0f, p);
                break;
            case "preference_grid_golden_spiral_right":
            case "preference_grid_golden_spiral_left":
            case "preference_grid_golden_spiral_upside_down_right":
            case "preference_grid_golden_spiral_upside_down_left":
                canvas.save();
                switch (preference_grid_pref) {
                    case "preference_grid_golden_spiral_left":
                        canvas.scale(-1.0f, 1.0f, canvas.getWidth() * 0.5f, canvas.getHeight() * 0.5f);
                        break;
                    case "preference_grid_golden_spiral_right":
                        // no transformation needed
                        break;
                    case "preference_grid_golden_spiral_upside_down_left":
                        canvas.rotate(180.0f, canvas.getWidth() * 0.5f, canvas.getHeight() * 0.5f);
                        break;
                    case "preference_grid_golden_spiral_upside_down_right":
                        canvas.scale(1.0f, -1.0f, canvas.getWidth() * 0.5f, canvas.getHeight() * 0.5f);
                        break;
                }
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(stroke_width);
                int fibb = 34;
                int fibb_n = 21;
                int left = 0, top = 0;
                int full_width = canvas.getWidth();
                int full_height = canvas.getHeight();
                int width = (int) (full_width * ((double) fibb_n) / (double) (fibb));
                int height = full_height;

                for (int count = 0; count < 2; count++) {
                    canvas.save();
                    draw_rect.set(left, top, left + width, top + height);
                    canvas.clipRect(draw_rect);
                    canvas.drawRect(draw_rect, p);
                    draw_rect.set(left, top, left + 2 * width, top + 2 * height);
                    canvas.drawOval(draw_rect, p);
                    canvas.restore();

                    int old_fibb = fibb;
                    fibb = fibb_n;
                    fibb_n = old_fibb - fibb;

                    left += width;
                    full_width = full_width - width;
                    width = full_width;
                    height = (int) (height * ((double) fibb_n) / (double) (fibb));

                    canvas.save();
                    draw_rect.set(left, top, left + width, top + height);
                    canvas.clipRect(draw_rect);
                    canvas.drawRect(draw_rect, p);
                    draw_rect.set(left - width, top, left + width, top + 2 * height);
                    canvas.drawOval(draw_rect, p);
                    canvas.restore();

                    old_fibb = fibb;
                    fibb = fibb_n;
                    fibb_n = old_fibb - fibb;

                    top += height;
                    full_height = full_height - height;
                    height = full_height;
                    width = (int) (width * ((double) fibb_n) / (double) (fibb));
                    left += full_width - width;

                    canvas.save();
                    draw_rect.set(left, top, left + width, top + height);
                    canvas.clipRect(draw_rect);
                    canvas.drawRect(draw_rect, p);
                    draw_rect.set(left - width, top - height, left + width, top + height);
                    canvas.drawOval(draw_rect, p);
                    canvas.restore();

                    old_fibb = fibb;
                    fibb = fibb_n;
                    fibb_n = old_fibb - fibb;

                    full_width = full_width - width;
                    width = full_width;
                    left -= width;
                    height = (int) (height * ((double) fibb_n) / (double) (fibb));
                    top += full_height - height;

                    canvas.save();
                    draw_rect.set(left, top, left + width, top + height);
                    canvas.clipRect(draw_rect);
                    canvas.drawRect(draw_rect, p);
                    draw_rect.set(left, top - height, left + 2 * width, top + height);
                    canvas.drawOval(draw_rect, p);
                    canvas.restore();

                    old_fibb = fibb;
                    fibb = fibb_n;
                    fibb_n = old_fibb - fibb;

                    full_height = full_height - height;
                    height = full_height;
                    top -= height;
                    width = (int) (width * ((double) fibb_n) / (double) (fibb));
                }

                canvas.restore();
                p.setStyle(Paint.Style.FILL); // reset

                break;
            case "preference_grid_golden_triangle_1":
            case "preference_grid_golden_triangle_2":
                p.setColor(Color.WHITE);
                double theta = Math.atan2(canvas.getWidth(), canvas.getHeight());
                double dist = canvas.getHeight() * Math.cos(theta);
                float dist_x = (float) (dist * Math.sin(theta));
                float dist_y = (float) (dist * Math.cos(theta));
                if (preference_grid_pref.equals("preference_grid_golden_triangle_1")) {
                    canvas.drawLine(0.0f, canvas.getHeight() - 1.0f, canvas.getWidth() - 1.0f, 0.0f, p);
                    canvas.drawLine(0.0f, 0.0f, dist_x, canvas.getHeight() - dist_y, p);
                    canvas.drawLine(canvas.getWidth() - 1.0f - dist_x, dist_y - 1.0f, canvas.getWidth() - 1.0f, canvas.getHeight() - 1.0f, p);
                } else {
                    canvas.drawLine(0.0f, 0.0f, canvas.getWidth() - 1.0f, canvas.getHeight() - 1.0f, p);
                    canvas.drawLine(canvas.getWidth() - 1.0f, 0.0f, canvas.getWidth() - 1.0f - dist_x, canvas.getHeight() - dist_y, p);
                    canvas.drawLine(dist_x, dist_y - 1.0f, 0.0f, canvas.getHeight() - 1.0f, p);
                }
                break;
            case "preference_grid_diagonals":
                p.setColor(Color.WHITE);
                canvas.drawLine(0.0f, 0.0f, canvas.getHeight() - 1.0f, canvas.getHeight() - 1.0f, p);
                canvas.drawLine(canvas.getHeight() - 1.0f, 0.0f, 0.0f, canvas.getHeight() - 1.0f, p);
                int diff = canvas.getWidth() - canvas.getHeight();
                if (diff > 0) {
                    canvas.drawLine(diff, 0.0f, diff + canvas.getHeight() - 1.0f, canvas.getHeight() - 1.0f, p);
                    canvas.drawLine(diff + canvas.getHeight() - 1.0f, 0.0f, diff, canvas.getHeight() - 1.0f, p);
                }
                break;
        }
    }

    private void drawCropGuides(Canvas canvas) {
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        if (preview_size_wysiwyg_pref) {
            String preference_crop_guide = sharedPreferences.getString(PreferenceKeys.ShowCropGuidePreferenceKey, "crop_guide_none");
            if (camera_controller != null && preview.getTargetRatio() > 0.0 && !preference_crop_guide.equals("crop_guide_none")) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(stroke_width);
                p.setColor(Color.rgb(255, 235, 59)); // Yellow 500
                double crop_ratio = -1.0;
                switch (preference_crop_guide) {
                    case "crop_guide_1":
                        crop_ratio = 1.0;
                        break;
                    case "crop_guide_1.25":
                        crop_ratio = 1.25;
                        break;
                    case "crop_guide_1.33":
                        crop_ratio = 1.33333333;
                        break;
                    case "crop_guide_1.4":
                        crop_ratio = 1.4;
                        break;
                    case "crop_guide_1.5":
                        crop_ratio = 1.5;
                        break;
                    case "crop_guide_1.78":
                        crop_ratio = 1.77777778;
                        break;
                    case "crop_guide_1.85":
                        crop_ratio = 1.85;
                        break;
                    case "crop_guide_2":
                        crop_ratio = 2.0;
                        break;
                    case "crop_guide_2.33":
                        crop_ratio = 2.33333333;
                        break;
                    case "crop_guide_2.35":
                        crop_ratio = 2.35006120; // actually 1920:817
                        break;
                    case "crop_guide_2.4":
                        crop_ratio = 2.4;
                        break;
                }
                if (crop_ratio > 0.0 && Math.abs(preview.getCurrentPreviewAspectRatio() - crop_ratio) > 1.0e-5) {
                    int left = 1, top = 1, right = canvas.getWidth() - 1, bottom = canvas.getHeight() - 1;
                    if (crop_ratio > preview.getTargetRatio()) {
                        // crop ratio is wider, so we have to crop top/bottom
                        double new_hheight = ((double) canvas.getWidth()) / (2.0f * crop_ratio);
                        top = (canvas.getHeight() / 2 - (int) new_hheight);
                        bottom = (canvas.getHeight() / 2 + (int) new_hheight);
                    } else {
                        // crop ratio is taller, so we have to crop left/right
                        double new_hwidth = (((double) canvas.getHeight()) * crop_ratio) / 2.0f;
                        left = (canvas.getWidth() / 2 - (int) new_hwidth);
                        right = (canvas.getWidth() / 2 + (int) new_hwidth);
                    }
                    canvas.drawRect(left, top, right, bottom, p);
                }
                p.setStyle(Paint.Style.FILL); // reset
            }
        }
    }

    private void drawHistogramChannel(Canvas canvas, int[] histogram_channel, int max) {
        path.reset();
        path.moveTo(icon_dest.left, icon_dest.bottom);
        for (int c = 0; c < histogram_channel.length; c++) {
            double c_alpha = c / (double) histogram_channel.length;
            int x = (int) (c_alpha * icon_dest.width());
            int h = (histogram_channel[c] * icon_dest.height()) / max;
            path.lineTo(icon_dest.left + x, icon_dest.bottom - h);
        }
        path.lineTo(icon_dest.right, icon_dest.bottom);
        path.close();
        canvas.drawPath(path, p);
    }


    private void drawAngleLines(Canvas canvas, long time_ms) {
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        boolean has_level_angle = preview.hasLevelAngle();
        boolean actual_show_angle_line_pref;
        if (photoMode == MyApplicationInterface.PhotoMode.Panorama) {
            // in panorama mode, we should the level iff we aren't taking the panorama photos
            actual_show_angle_line_pref = !main_activity.getApplicationInterface().getGyroSensor().isRecording();
        } else
            actual_show_angle_line_pref = show_angle_line_pref;
        if (camera_controller != null && !preview.isPreviewPaused() && has_level_angle && (actual_show_angle_line_pref || show_pitch_lines_pref || show_geo_direction_lines_pref)) {
            int ui_rotation = preview.getUIRotation();
            double level_angle = preview.getLevelAngle();
            boolean has_pitch_angle = preview.hasPitchAngle();
            double pitch_angle = preview.getPitchAngle();
            boolean has_geo_direction = preview.hasGeoDirection();
            double geo_direction = preview.getGeoDirection();
            // n.b., must draw this without the standard canvas rotation
            int radius_dps = (ui_rotation == 90 || ui_rotation == 270) ? 60 : 80;
            int radius = (int) (radius_dps * scale + 0.5f); // convert dps to pixels
            double angle = -preview.getOrigLevelAngle();
            // see http://android-developers.blogspot.co.uk/2010/09/one-screen-turn-deserves-another.html
            int rotation = main_activity.getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_90:
                case Surface.ROTATION_270:
                    angle -= 90.0;
                    break;
                case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                default:
                    break;
            }
            int cx = canvas.getWidth() / 2;
            int cy = canvas.getHeight() / 2;

            boolean is_level = false;
            if (has_level_angle && Math.abs(level_angle) <= close_level_angle) { // n.b., use level_angle, not angle or orig_level_angle
                is_level = true;
            }

            if (is_level) {
                radius = (int) (radius * 1.2);
            }

            canvas.save();
            canvas.rotate((float) angle, cx, cy);

            final int line_alpha = 160;
            float hthickness = (0.5f * scale + 0.5f); // convert dps to pixels
            p.setStyle(Paint.Style.FILL);
            if (actual_show_angle_line_pref && preview.hasLevelAngleStable()) {
                // only show the angle line if level angle "stable" (i.e., not pointing near vertically up or down)
                // draw outline
                p.setColor(Color.BLACK);
                p.setAlpha(64);
                // can't use drawRoundRect(left, top, right, bottom, ...) as that requires API 21
                draw_rect.set(cx - radius - hthickness, cy - 2 * hthickness, cx + radius + hthickness, cy + 2 * hthickness);
                canvas.drawRoundRect(draw_rect, 2 * hthickness, 2 * hthickness, p);
                // draw the vertical crossbar
                draw_rect.set(cx - 2 * hthickness, cy - radius / 2.0f - hthickness, cx + 2 * hthickness, cy + radius / 2.0f + hthickness);
                canvas.drawRoundRect(draw_rect, hthickness, hthickness, p);
                // draw inner portion
                if (is_level) {
                    p.setColor(angle_highlight_color_pref);
                } else {
                    p.setColor(Color.WHITE);
                }
                p.setAlpha(line_alpha);
                draw_rect.set(cx - radius, cy - hthickness, cx + radius, cy + hthickness);
                canvas.drawRoundRect(draw_rect, hthickness, hthickness, p);
                draw_rect.set(cx - hthickness, cy - radius / 2.0f, cx + hthickness, cy + radius / 2.0f);
                canvas.drawRoundRect(draw_rect, hthickness, hthickness, p);

                if (is_level) {
                    p.setColor(Color.BLACK);
                    p.setAlpha(64);
                    draw_rect.set(cx - radius - hthickness, cy - 7 * hthickness, cx + radius + hthickness, cy - 3 * hthickness);
                    canvas.drawRoundRect(draw_rect, 2 * hthickness, 2 * hthickness, p);
                    p.setColor(angle_highlight_color_pref);
                    p.setAlpha(line_alpha);
                    draw_rect.set(cx - radius, cy - 6 * hthickness, cx + radius, cy - 4 * hthickness);
                    canvas.drawRoundRect(draw_rect, hthickness, hthickness, p);
                }
            }
            updateCachedViewAngles(time_ms); // ensure view_angle_x_preview, view_angle_y_preview are computed and up to date
            float camera_angle_x = this.view_angle_x_preview;
            float camera_angle_y = this.view_angle_y_preview;
            float angle_scale_x = (float) (canvas.getWidth() / (2.0 * Math.tan(Math.toRadians((camera_angle_x / 2.0)))));
            float angle_scale_y = (float) (canvas.getHeight() / (2.0 * Math.tan(Math.toRadians((camera_angle_y / 2.0)))));
            float angle_scale = (float) Math.sqrt(angle_scale_x * angle_scale_x + angle_scale_y * angle_scale_y);
            angle_scale *= preview.getZoomRatio();
            if (has_pitch_angle && show_pitch_lines_pref) {
                int pitch_radius_dps = (ui_rotation == 90 || ui_rotation == 270) ? 100 : 80;
                int pitch_radius = (int) (pitch_radius_dps * scale + 0.5f); // convert dps to pixels
                int angle_step = 10;
                if (preview.getZoomRatio() >= 2.0f)
                    angle_step = 5;
                for (int latitude_angle = -90; latitude_angle <= 90; latitude_angle += angle_step) {
                    double this_angle = pitch_angle - latitude_angle;
                    if (Math.abs(this_angle) < 90.0) {
                        float pitch_distance = angle_scale * (float) Math.tan(Math.toRadians(this_angle)); // angle_scale is already in pixels rather than dps
                        p.setColor(Color.BLACK);
                        p.setAlpha(64);
                        draw_rect.set(cx - pitch_radius - hthickness, cy + pitch_distance - 2 * hthickness, cx + pitch_radius + hthickness, cy + pitch_distance + 2 * hthickness);
                        canvas.drawRoundRect(draw_rect, 2 * hthickness, 2 * hthickness, p);
                        p.setColor(Color.WHITE);
                        p.setTextAlign(Paint.Align.LEFT);
                        if (latitude_angle == 0 && Math.abs(pitch_angle) < 1.0) {
                            p.setAlpha(255);
                        } else if (latitude_angle == 90 && Math.abs(pitch_angle - 90) < 3.0) {
                            p.setAlpha(255);
                        } else if (latitude_angle == -90 && Math.abs(pitch_angle + 90) < 3.0) {
                            p.setAlpha(255);
                        } else {
                            p.setAlpha(line_alpha);
                        }
                        draw_rect.set(cx - pitch_radius, cy + pitch_distance - hthickness, cx + pitch_radius, cy + pitch_distance + hthickness);
                        canvas.drawRoundRect(draw_rect, hthickness, hthickness, p);
                        // draw pitch angle indicator
                        applicationInterface.drawTextWithBackground(canvas, p, "" + latitude_angle + "\u00B0", p.getColor(), Color.BLACK, (int) (cx + pitch_radius + 4 * hthickness), (int) (cy + pitch_distance - 2 * hthickness), MyApplicationInterface.Alignment.ALIGNMENT_CENTRE);
                    }
                }
            }
            if (has_geo_direction && has_pitch_angle && show_geo_direction_lines_pref) {
                int geo_radius_dps = (ui_rotation == 90 || ui_rotation == 270) ? 80 : 100;
                int geo_radius = (int) (geo_radius_dps * scale + 0.5f); // convert dps to pixels
                float geo_angle = (float) Math.toDegrees(geo_direction);
                int angle_step = 10;
                if (preview.getZoomRatio() >= 2.0f)
                    angle_step = 5;
                for (int longitude_angle = 0; longitude_angle < 360; longitude_angle += angle_step) {
                    double this_angle = longitude_angle - geo_angle;
                    while (this_angle >= 360.0)
                        this_angle -= 360.0;
                    while (this_angle < -360.0)
                        this_angle += 360.0;
                    if (this_angle > 180.0)
                        this_angle = -(360.0 - this_angle);
                    if (Math.abs(this_angle) < 90.0) {
                        float geo_distance = angle_scale * (float) Math.tan(Math.toRadians(this_angle)); // angle_scale is already in pixels rather than dps
                        p.setColor(Color.BLACK);
                        p.setAlpha(64);
                        draw_rect.set(cx + geo_distance - 2 * hthickness, cy - geo_radius - hthickness, cx + geo_distance + 2 * hthickness, cy + geo_radius + hthickness);
                        canvas.drawRoundRect(draw_rect, 2 * hthickness, 2 * hthickness, p);
                        p.setColor(Color.WHITE);
                        p.setTextAlign(Paint.Align.CENTER);
                        p.setAlpha(line_alpha);
                        draw_rect.set(cx + geo_distance - hthickness, cy - geo_radius, cx + geo_distance + hthickness, cy + geo_radius);
                        canvas.drawRoundRect(draw_rect, hthickness, hthickness, p);
                        applicationInterface.drawTextWithBackground(canvas, p, "" + longitude_angle + "\u00B0", p.getColor(), Color.BLACK, (int) (cx + geo_distance), (int) (cy - geo_radius - 4 * hthickness), MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM);
                    }
                }
            }

            p.setAlpha(255);
            p.setStyle(Paint.Style.FILL); // reset

            canvas.restore();
        }
    }


    private void doFocusAnimation(Canvas canvas, long time_ms) {
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        if (camera_controller != null && continuous_focus_moving && !taking_picture) {
            long dt = time_ms - continuous_focus_moving_ms;
            final long length = 1000;
            if (dt <= length) {
                float frac = ((float) dt) / (float) length;
                float pos_x = canvas.getWidth() / 2.0f;
                float pos_y = canvas.getHeight() / 2.0f;
                float min_radius = (40 * scale + 0.5f); // convert dps to pixels
                float max_radius = (60 * scale + 0.5f); // convert dps to pixels
                float radius;
                if (frac < 0.5f) {
                    float alpha = frac * 2.0f;
                    radius = (1.0f - alpha) * min_radius + alpha * max_radius;
                } else {
                    float alpha = (frac - 0.5f) * 2.0f;
                    radius = (1.0f - alpha) * max_radius + alpha * min_radius;
                }
				/*if( MyDebug.LOG ) {
					Log.d(TAG, "dt: " + dt);
					Log.d(TAG, "radius: " + radius);
				}*/
                p.setColor(Color.WHITE);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(stroke_width);
                canvas.drawCircle(pos_x, pos_y, radius, p);
                p.setStyle(Paint.Style.FILL); // reset
            } else {
                clearContinuousFocusMove();
            }
        }

        if (preview.isFocusWaiting() || preview.isFocusRecentSuccess() || preview.isFocusRecentFailure()) {
            long time_since_focus_started = preview.timeSinceStartedAutoFocus();
            float min_radius = (40 * scale + 0.5f); // convert dps to pixels
            float max_radius = (45 * scale + 0.5f); // convert dps to pixels
            float radius = min_radius;
            if (time_since_focus_started > 0) {
                final long length = 500;
                float frac = ((float) time_since_focus_started) / (float) length;
                if (frac > 1.0f)
                    frac = 1.0f;
                if (frac < 0.5f) {
                    float alpha = frac * 2.0f;
                    radius = (1.0f - alpha) * min_radius + alpha * max_radius;
                } else {
                    float alpha = (frac - 0.5f) * 2.0f;
                    radius = (1.0f - alpha) * max_radius + alpha * min_radius;
                }
            }
            int size = (int) radius;

            if (preview.isFocusRecentSuccess())
                p.setColor(Color.rgb(20, 231, 21)); // Green A400
            else if (preview.isFocusRecentFailure())
                p.setColor(Color.rgb(244, 67, 54)); // Red 500
            else
                p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke_width);
            int pos_x;
            int pos_y;
            if (preview.hasFocusArea()) {
                Pair<Integer, Integer> focus_pos = preview.getFocusPos();
                pos_x = focus_pos.first;
                pos_y = focus_pos.second;
            } else {
                pos_x = canvas.getWidth() / 2;
                pos_y = canvas.getHeight() / 2;
            }
            float frac = 0.5f;
            // horizontal strokes
            canvas.drawLine(pos_x - size, pos_y - size, pos_x - frac * size, pos_y - size, p);
            canvas.drawLine(pos_x + frac * size, pos_y - size, pos_x + size, pos_y - size, p);
            canvas.drawLine(pos_x - size, pos_y + size, pos_x - frac * size, pos_y + size, p);
            canvas.drawLine(pos_x + frac * size, pos_y + size, pos_x + size, pos_y + size, p);
            // vertical strokes
            canvas.drawLine(pos_x - size, pos_y - size, pos_x - size, pos_y - frac * size, p);
            canvas.drawLine(pos_x - size, pos_y + frac * size, pos_x - size, pos_y + size, p);
            canvas.drawLine(pos_x + size, pos_y - size, pos_x + size, pos_y - frac * size, p);
            canvas.drawLine(pos_x + size, pos_y + frac * size, pos_x + size, pos_y + size, p);
            p.setStyle(Paint.Style.FILL); // reset
        }
    }

    public void onDrawPreview(Canvas canvas) {
        if (!has_settings) {
            if (MyDebug.LOG)
                Log.d(TAG, "onDrawPreview: need to update settings");
            updateSettings();
        }
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        int ui_rotation = preview.getUIRotation();

        final long time_ms = System.currentTimeMillis();

        // set up preview bitmaps (histogram etc)
        boolean want_preview_bitmap = want_histogram || want_zebra_stripes || want_focus_peaking;
        if (want_preview_bitmap != preview.isPreviewBitmapEnabled()) {
            if (want_preview_bitmap) {
                preview.enablePreviewBitmap();
            } else
                preview.disablePreviewBitmap();
        }
        if (want_preview_bitmap) {
            if (want_histogram)
                preview.enableHistogram(histogram_type);
            else
                preview.disableHistogram();

            if (want_zebra_stripes)
                preview.enableZebraStripes(zebra_stripes_threshold);
            else
                preview.disableZebraStripes();

            if (want_focus_peaking)
                preview.enableFocusPeaking();
            else
                preview.disableFocusPeaking();
        }

        // see documentation for CameraController.shouldCoverPreview()
        if (preview.usingCamera2API() && (camera_controller == null || camera_controller.shouldCoverPreview())) {
            p.setColor(Color.BLACK);
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
        }

        if (camera_controller != null && front_screen_flash) {
            p.setColor(Color.WHITE);
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
        } else if ("flash_frontscreen_torch".equals(preview.getCurrentFlashValue())) { // getCurrentFlashValue() may return null
            p.setColor(Color.WHITE);
            p.setAlpha(200); // set alpha so user can still see some of the preview
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
            p.setAlpha(255);
        }

        if (main_activity.getMainUI().inImmersiveMode()) {
            if (immersive_mode_everything_pref) {
                return;
            }
        }

        if (camera_controller != null && taking_picture && !front_screen_flash && take_photo_border_pref) {
            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke_width);
            float this_stroke_width = (5.0f * scale + 0.5f); // convert dps to pixels
            p.setStrokeWidth(this_stroke_width);
            canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
            p.setStyle(Paint.Style.FILL); // reset
            p.setStrokeWidth(stroke_width); // reset
        }
        drawGrids(canvas);

        drawCropGuides(canvas);

        if (last_thumbnail != null && !last_thumbnail_is_video && camera_controller != null && (show_last_image || (allow_ghost_last_image && !front_screen_flash && ghost_image_pref.equals("preference_ghost_image_last")))) {
            if (show_last_image) {
                p.setColor(Color.rgb(0, 0, 0)); // in case image doesn't cover the canvas (due to different aspect ratios)
                canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p); // in case
            }
            setLastImageMatrix(canvas, last_thumbnail, ui_rotation, !show_last_image);
            if (!show_last_image)
                p.setAlpha(127);
            canvas.drawBitmap(last_thumbnail, last_image_matrix, p);
            if (!show_last_image)
                p.setAlpha(255);
        } else if (camera_controller != null && !front_screen_flash && ghost_selected_image_bitmap != null) {
            setLastImageMatrix(canvas, ghost_selected_image_bitmap, ui_rotation, true);
            p.setAlpha(127);
            canvas.drawBitmap(ghost_selected_image_bitmap, last_image_matrix, p);
            p.setAlpha(255);
        }

        if (preview.isPreviewBitmapEnabled()) {
            // draw additional real-time effects

            // draw zebra stripes
            Bitmap zebra_stripes_bitmap = preview.getZebraStripesBitmap();
            if (zebra_stripes_bitmap != null) {
                setLastImageMatrix(canvas, zebra_stripes_bitmap, 0, false);
                p.setAlpha(255);
                canvas.drawBitmap(zebra_stripes_bitmap, last_image_matrix, p);
            }

            // draw focus peaking
            Bitmap focus_peaking_bitmap = preview.getFocusPeakingBitmap();
            if (focus_peaking_bitmap != null) {
                setLastImageMatrix(canvas, focus_peaking_bitmap, 0, false);
                p.setAlpha(127);
                if (focus_peaking_color_pref != Color.WHITE) {
                    p.setColorFilter(new PorterDuffColorFilter(focus_peaking_color_pref, PorterDuff.Mode.SRC_IN));
                }
                canvas.drawBitmap(focus_peaking_bitmap, last_image_matrix, p);
                if (focus_peaking_color_pref != Color.WHITE) {
                    p.setColorFilter(null);
                }
                p.setAlpha(255);
            }
        }

        //doThumbnailAnimation(canvas, time_ms);
        drawAngleLines(canvas, time_ms);

        doFocusAnimation(canvas, time_ms);

        CameraController.Face[] faces_detected = preview.getFacesDetected();
        if (faces_detected != null) {
            p.setColor(Color.rgb(255, 235, 59)); // Yellow 500
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke_width);
            for (CameraController.Face face : faces_detected) {
                if (face.score >= 50) {
                    canvas.drawRect(face.rect, p);
                }
            }
            p.setStyle(Paint.Style.FILL); // reset
        }

        if (enable_gyro_target_spot && camera_controller != null) {
            GyroSensor gyroSensor = main_activity.getApplicationInterface().getGyroSensor();
            if (gyroSensor.isRecording()) {
                for (float[] gyro_direction : gyro_directions) {
                    gyroSensor.getRelativeInverseVector(transformed_gyro_direction, gyro_direction);
                    gyroSensor.getRelativeInverseVector(transformed_gyro_direction_up, gyro_direction_up);
                    float angle_x = -(float) Math.asin(transformed_gyro_direction[1]);
                    float angle_y = -(float) Math.asin(transformed_gyro_direction[0]);
                    if (Math.abs(angle_x) < 0.5f * Math.PI && Math.abs(angle_y) < 0.5f * Math.PI) {
                        updateCachedViewAngles(time_ms); // ensure view_angle_x_preview, view_angle_y_preview are computed and up to date
                        float camera_angle_x = this.view_angle_x_preview;
                        float camera_angle_y = this.view_angle_y_preview;
                        float angle_scale_x = (float) (canvas.getWidth() / (2.0 * Math.tan(Math.toRadians((camera_angle_x / 2.0)))));
                        float angle_scale_y = (float) (canvas.getHeight() / (2.0 * Math.tan(Math.toRadians((camera_angle_y / 2.0)))));
                        angle_scale_x *= preview.getZoomRatio();
                        angle_scale_y *= preview.getZoomRatio();
                        float distance_x = angle_scale_x * (float) Math.tan(angle_x); // angle_scale is already in pixels rather than dps
                        float distance_y = angle_scale_y * (float) Math.tan(angle_y); // angle_scale is already in pixels rather than dps
                        p.setColor(Color.WHITE);
                        drawGyroSpot(canvas, 0.0f, 0.0f, -1.0f, 0.0f, 48, true); // draw spot for the centre of the screen, to help the user orient the device
                        p.setColor(Color.BLUE);
                        float dir_x = -transformed_gyro_direction_up[1];
                        float dir_y = -transformed_gyro_direction_up[0];
                        drawGyroSpot(canvas, distance_x, distance_y, dir_x, dir_y, 45, false);
                    }

                    if (gyroSensor.isUpright() != 0 && Math.abs(angle_x) <= 20.0f * 0.0174532925199f) {
                        canvas.save();
                        canvas.rotate(ui_rotation, canvas.getWidth() / 2.0f, canvas.getHeight() / 2.0f);
                        final int icon_size = (int) (64 * scale + 0.5f); // convert dps to pixels
                        final int cy_offset = (int) (80 * scale + 0.5f); // convert dps to pixels
                        int cx = canvas.getWidth() / 2, cy = canvas.getHeight() / 2 - cy_offset;
                        icon_dest.set(cx - icon_size / 2, cy - icon_size / 2, cx + icon_size / 2, cy + icon_size / 2);
                        canvas.drawBitmap(gyroSensor.isUpright() > 0 ? rotate_left_bitmap : rotate_right_bitmap, null, icon_dest, p);
                        canvas.restore();
                    }
                }
            }
        }
    }

    private void setLastImageMatrix(Canvas canvas, Bitmap bitmap, int this_ui_rotation, boolean flip_front) {
        Preview preview = main_activity.getPreview();
        CameraController camera_controller = preview.getCameraController();
        last_image_src_rect.left = 0;
        last_image_src_rect.top = 0;
        last_image_src_rect.right = bitmap.getWidth();
        last_image_src_rect.bottom = bitmap.getHeight();
        if (this_ui_rotation == 90 || this_ui_rotation == 270) {
            last_image_src_rect.right = bitmap.getHeight();
            last_image_src_rect.bottom = bitmap.getWidth();
        }
        last_image_dst_rect.left = 0;
        last_image_dst_rect.top = 0;
        last_image_dst_rect.right = canvas.getWidth();
        last_image_dst_rect.bottom = canvas.getHeight();
        last_image_matrix.setRectToRect(last_image_src_rect, last_image_dst_rect, Matrix.ScaleToFit.CENTER); // use CENTER to preserve aspect ratio
        if (this_ui_rotation == 90 || this_ui_rotation == 270) {
            float diff = bitmap.getHeight() - bitmap.getWidth();
            last_image_matrix.preTranslate(diff / 2.0f, -diff / 2.0f);
        }
        last_image_matrix.preRotate(this_ui_rotation, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
        if (flip_front) {
            boolean is_front_facing = camera_controller != null && camera_controller.isFrontFacing();
            if (is_front_facing && !sharedPreferences.getString(PreferenceKeys.FrontCameraMirrorKey, "preference_front_camera_mirror_no").equals("preference_front_camera_mirror_photo")) {
                last_image_matrix.preScale(-1.0f, 1.0f, bitmap.getWidth() / 2.0f, 0.0f);
            }
        }
    }

    private void drawGyroSpot(Canvas canvas, float distance_x, float distance_y, float dir_x, float dir_y, int radius_dp, boolean outline) {
        if (outline) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke_width);
            p.setAlpha(255);
        } else {
            p.setAlpha(127);
        }
        float radius = (radius_dp * scale + 0.5f);
        float cx = canvas.getWidth() / 2.0f + distance_x;
        float cy = canvas.getHeight() / 2.0f + distance_y;
        canvas.drawCircle(cx, cy, radius, p);
        p.setAlpha(255);
        p.setStyle(Paint.Style.FILL);
    }

}
