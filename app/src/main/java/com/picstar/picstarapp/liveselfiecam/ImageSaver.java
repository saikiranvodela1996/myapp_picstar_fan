package com.picstar.picstarapp.liveselfiecam;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Xml;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import com.picstar.picstarapp.activities.BluredImageActivity;

import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.RawImage;

import com.picstar.picstarapp.campkg.others.HDRProcessor;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PanoramaProcessor;

import com.picstar.picstarapp.campkg.others.TextFormatter;
import com.picstar.picstarapp.utils.PSRConstants;
import com.picstar.picstarapp.utils.PSR_Utils;

import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ImageSaver extends Thread {
    private static final String TAG = "ImageSaver";
    private static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
    private static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
    private static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    private static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    private final Paint p = new Paint();
    private final LiveSelfieCameraActivity main_activity;
    private final HDRProcessor hdrProcessor;
    private final PanoramaProcessor panoramaProcessor;
    private int n_images_to_save = 0;
    private int n_real_images_to_save = 0;
    private final int queue_capacity;
    private final BlockingQueue<Request> queue;
    private final static int queue_cost_jpeg_c = 1; // also covers WEBP
    private final static int queue_cost_dng_c = 6;
    public static volatile boolean test_small_queue_size; // needs to be static, as it needs to be set before activity is created to take effect
    public volatile boolean test_slow_saving;
    public volatile boolean test_queue_blocked;

    static class Request {
        enum Type {
            JPEG, // also covers WEBP
            RAW,
            DUMMY
        }

        final Type type;

        enum ProcessType {
            NORMAL,
            HDR,
            AVERAGE,
            PANORAMA
        }

        final ProcessType process_type; // for type==JPEG
        final boolean force_suffix; // affects filename suffixes for saving jpeg_images: if true, filenames will always be appended with a suffix like _0, even if there's only 1 image in jpeg_images
        final int suffix_offset; // affects filename suffixes for saving jpeg_images, when force_suffix is true or there are multiple images in jpeg_images: the suffixes will be offset by this number

        enum SaveBase {
            SAVEBASE_NONE,
            SAVEBASE_FIRST,
            SAVEBASE_ALL,
            SAVEBASE_ALL_PLUS_DEBUG // for PANORAMA
        }

        final SaveBase save_base; // whether to save the base images, for process_type HDR, AVERAGE or PANORAMA
        final List<byte[]> jpeg_images;
        final RawImage raw_image; // for raw
        final boolean image_capture_intent;
        final Uri image_capture_intent_uri;
        final boolean using_camera2;

        enum ImageFormat {
            STD, // leave unchanged from the standard JPEG format
            WEBP,
            PNG
        }

        ImageFormat image_format;
        int image_quality;
        boolean do_auto_stabilise;
        final double level_angle;
        final List<float[]> gyro_rotation_matrix; // used for panorama (one 3x3 matrix per jpeg_images entry), otherwise can be null
        boolean panorama_dir_left_to_right; // used for panorama
        float camera_view_angle_x; // used for panorama
        float camera_view_angle_y; // used for panorama
        final boolean is_front_facing;
        boolean mirror;
        final Date current_date;
        final String preference_hdr_contrast_enhancement; // for HDR
        final int iso; // not applicable for RAW image
        final long exposure_time; // not applicable for RAW image
        final float zoom_factor; // not applicable for RAW image
        String preference_stamp;
        String preference_textstamp;
        final int font_size;
        final int color;
        final String pref_style;
        final String preference_stamp_dateformat;
        final String preference_stamp_timeformat;
        final String preference_stamp_gpsformat;
        final String preference_stamp_geo_address;
        final String preference_units_distance;
        final boolean panorama_crop; // used for panorama
        final boolean store_location;
        final Location location;
        final boolean store_geo_direction;
        final double geo_direction;
        final String custom_tag_artist;
        final String custom_tag_copyright;
        final int sample_factor; // sampling factor for thumbnail, higher means lower quality

        Request(Type type,
                ProcessType process_type,
                boolean force_suffix,
                int suffix_offset,
                SaveBase save_base,
                List<byte[]> jpeg_images,
                RawImage raw_image,
                boolean image_capture_intent, Uri image_capture_intent_uri,
                boolean using_camera2,
                ImageFormat image_format, int image_quality,
                boolean do_auto_stabilise, double level_angle, List<float[]> gyro_rotation_matrix,
                boolean is_front_facing,
                boolean mirror,
                Date current_date,
                String preference_hdr_contrast_enhancement,
                int iso,
                long exposure_time,
                float zoom_factor,
                String preference_stamp, String preference_textstamp, int font_size, int color, String pref_style, String preference_stamp_dateformat, String preference_stamp_timeformat, String preference_stamp_gpsformat, String preference_stamp_geo_address, String preference_units_distance,
                boolean panorama_crop,
                boolean store_location, Location location, boolean store_geo_direction, double geo_direction,
                String custom_tag_artist,
                String custom_tag_copyright,
                int sample_factor) {
            this.type = type;
            this.process_type = process_type;
            this.force_suffix = force_suffix;
            this.suffix_offset = suffix_offset;
            this.save_base = save_base;
            this.jpeg_images = jpeg_images;
            this.raw_image = raw_image;
            this.image_capture_intent = image_capture_intent;
            this.image_capture_intent_uri = image_capture_intent_uri;
            this.using_camera2 = using_camera2;
            this.image_format = image_format;
            this.image_quality = image_quality;
            this.do_auto_stabilise = do_auto_stabilise;
            this.level_angle = level_angle;
            this.gyro_rotation_matrix = gyro_rotation_matrix;
            this.is_front_facing = is_front_facing;
            this.mirror = mirror;
            this.current_date = current_date;
            this.preference_hdr_contrast_enhancement = preference_hdr_contrast_enhancement;
            this.iso = iso;
            this.exposure_time = exposure_time;
            this.zoom_factor = zoom_factor;
            this.preference_stamp = preference_stamp;
            this.preference_textstamp = preference_textstamp;
            this.font_size = font_size;
            this.color = color;
            this.pref_style = pref_style;
            this.preference_stamp_dateformat = preference_stamp_dateformat;
            this.preference_stamp_timeformat = preference_stamp_timeformat;
            this.preference_stamp_gpsformat = preference_stamp_gpsformat;
            this.preference_stamp_geo_address = preference_stamp_geo_address;
            this.preference_units_distance = preference_units_distance;
            this.panorama_crop = panorama_crop;
            this.store_location = store_location;
            this.location = location;
            this.store_geo_direction = store_geo_direction;
            this.geo_direction = geo_direction;
            this.custom_tag_artist = custom_tag_artist;
            this.custom_tag_copyright = custom_tag_copyright;
            this.sample_factor = sample_factor;
        }

        Request copy() {
            return new Request(this.type,
                    this.process_type,
                    this.force_suffix,
                    this.suffix_offset,
                    this.save_base,
                    this.jpeg_images,
                    this.raw_image,
                    this.image_capture_intent, this.image_capture_intent_uri,
                    this.using_camera2,
                    this.image_format, this.image_quality,
                    this.do_auto_stabilise, this.level_angle, this.gyro_rotation_matrix,
                    this.is_front_facing,
                    this.mirror,
                    this.current_date,
                    this.preference_hdr_contrast_enhancement,
                    this.iso,
                    this.exposure_time,
                    this.zoom_factor,
                    this.preference_stamp, this.preference_textstamp, this.font_size, this.color, this.pref_style, this.preference_stamp_dateformat, this.preference_stamp_timeformat, this.preference_stamp_gpsformat, this.preference_stamp_geo_address, this.preference_units_distance,
                    this.panorama_crop, this.store_location, this.location, this.store_geo_direction, this.geo_direction,
                    this.custom_tag_artist,
                    this.custom_tag_copyright,
                    this.sample_factor);
        }
    }

    ImageSaver(LiveSelfieCameraActivity main_activity) {
        if (MyDebug.LOG)
            Log.d(TAG, "ImageSaver");
        this.main_activity = main_activity;

        ActivityManager activityManager = (ActivityManager) main_activity.getSystemService(Activity.ACTIVITY_SERVICE);
        this.queue_capacity = computeQueueSize(activityManager.getLargeMemoryClass());
        this.queue = new ArrayBlockingQueue<>(queue_capacity); // since we remove from the queue and then process in the saver thread, in practice the number of background photos - including the one being processed - is one more than the length of this queue

        this.hdrProcessor = new HDRProcessor(main_activity, main_activity.is_test);
        this.panoramaProcessor = new PanoramaProcessor(main_activity, hdrProcessor);

        p.setAntiAlias(true);
    }

    public static int computeQueueSize(int large_heap_memory) {
        if (MyDebug.LOG)
            Log.d(TAG, "large max memory = " + large_heap_memory + "MB");
        int max_queue_size;
        if (MyDebug.LOG)
            Log.d(TAG, "test_small_queue_size?: " + test_small_queue_size);
        if (test_small_queue_size) {
            large_heap_memory = 0;
        }

        if (large_heap_memory >= 512) {
            max_queue_size = 34;
        } else if (large_heap_memory >= 256) {
            max_queue_size = 12;
        } else if (large_heap_memory >= 128) {
            max_queue_size = 8;
        } else {
            max_queue_size = 6;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "max_queue_size = " + max_queue_size);
        return max_queue_size;
    }

    public static int computeRequestCost(boolean is_raw, int n_images) {
        if (MyDebug.LOG) {
            Log.d(TAG, "computeRequestCost");
            Log.d(TAG, "is_raw: " + is_raw);
            Log.d(TAG, "n_images: " + n_images);
        }
        int cost;
        if (is_raw)
            cost = n_images * queue_cost_dng_c;
        else {
            cost = n_images * queue_cost_jpeg_c;
        }
        return cost;
    }

    int computePhotoCost(int n_raw, int n_jpegs) {
        if (MyDebug.LOG) {
            Log.d(TAG, "computePhotoCost");
            Log.d(TAG, "n_raw: " + n_raw);
            Log.d(TAG, "n_jpegs: " + n_jpegs);
        }
        int cost = 0;
        if (n_raw > 0)
            cost += computeRequestCost(true, n_raw);
        if (n_jpegs > 0)
            cost += computeRequestCost(false, n_jpegs);
        if (MyDebug.LOG)
            Log.d(TAG, "cost: " + cost);
        return cost;
    }

    /**
     * Whether taking an extra photo would overflow the queue, resulting in the UI hanging.
     *
     * @param n_raw   The number of JPEGs that will be taken.
     * @param n_jpegs The number of JPEGs that will be taken.
     */
    boolean queueWouldBlock(int n_raw, int n_jpegs) {
        int photo_cost = this.computePhotoCost(n_raw, n_jpegs);
        return this.queueWouldBlock(photo_cost);
    }

    /**
     * Whether taking an extra photo would overflow the queue, resulting in the UI hanging.
     *
     * @param photo_cost The result returned by computePhotoCost().
     */
    synchronized boolean queueWouldBlock(int photo_cost) {
        if (MyDebug.LOG) {
            Log.d(TAG, "queueWouldBlock");
            Log.d(TAG, "photo_cost: " + photo_cost);
            Log.d(TAG, "n_images_to_save: " + n_images_to_save);
            Log.d(TAG, "queue_capacity: " + queue_capacity);
        }
        // we add one to queue, to account for the image currently being processed; n_images_to_save includes an image
        // currently being processed
        if (n_images_to_save == 0) {
            // In theory, we should never have the extra_cost large enough to block the queue even when no images are being
            // saved - but we have this just in case. This means taking the photo will likely block the UI, but we don't want
            // to disallow ever taking photos!
            if (MyDebug.LOG)
                Log.d(TAG, "queue is empty");
            return false;
        } else if (n_images_to_save + photo_cost > queue_capacity + 1) {
            if (MyDebug.LOG)
                Log.d(TAG, "queue would block");
            return true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "queue would not block");
        return false;
    }

    /**
     * Returns the maximum number of DNG images that might be held by the image saver queue at once, before blocking.
     */
    int getMaxDNG() {
        int max_dng = (queue_capacity + 1) / queue_cost_dng_c;
        max_dng++; // increase by 1, as the user can still take one extra photo if the queue is exactly full
        if (MyDebug.LOG)
            Log.d(TAG, "max_dng = " + max_dng);
        return max_dng;
    }

    public synchronized int getNImagesToSave() {
        return n_images_to_save;
    }

    void onDestroy() {
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy");
        if (panoramaProcessor != null) {
            panoramaProcessor.onDestroy();
        }
        if (hdrProcessor != null) {
            hdrProcessor.onDestroy();
        }
    }

   /* @Override
    public void run() {
        main_activity.runOnUiThread(new Runnable() {
            public void run() {
                main_activity.imageQueueChanged();
                try {
                    if (SingleTon.getInstance().getCapturedimage() != null) {
                        main_activity.clickedGallery(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }*/

    @Override
    public void run() {
        if (MyDebug.LOG)
            Log.d(TAG, "starting ImageSaver thread...");
        while (true) {
            try {
                if (MyDebug.LOG)
                    Log.d(TAG, "ImageSaver thread reading from queue, size: " + queue.size());
                Request request = queue.take(); // if empty, take() blocks until non-empty
                // Only decrement n_images_to_save after we've actually saved the image! Otherwise waitUntilDone() will return
                // even though we still have a last image to be saved.
                if (MyDebug.LOG)
                    Log.d(TAG, "ImageSaver thread found new request from queue, size is now: " + queue.size());
                boolean success;
                switch (request.type) {
                    case RAW:
                        if (MyDebug.LOG)
                            Log.d(TAG, "request is raw");
                        success = saveImageNowRaw(request);
                        break;
                    case JPEG:
                        if (MyDebug.LOG)
                            Log.d(TAG, "request is jpeg");
                        success = saveImageNow(request);
                        break;
                    case DUMMY:
                        if (MyDebug.LOG)
                            Log.d(TAG, "request is dummy");
                        success = true;
                        break;
                    default:
                        if (MyDebug.LOG)
                            Log.e(TAG, "request is unknown type!");
                        success = false;
                        break;
                }
                if (test_slow_saving) {
                    Thread.sleep(2000);
                }
                if (MyDebug.LOG) {
                    if (success)
                        Log.d(TAG, "ImageSaver thread successfully saved image");
                    else
                        Log.e(TAG, "ImageSaver thread failed to save image");
                }
                synchronized (this) {
                    n_images_to_save--;
                    if (request.type != Request.Type.DUMMY)
                        n_real_images_to_save--;
                    if (MyDebug.LOG)
                        Log.d(TAG, "ImageSaver thread processed new request from queue, images to save is now: " + n_images_to_save);
                    if (MyDebug.LOG && n_images_to_save < 0) {
                        Log.e(TAG, "images to save has become negative");
                        throw new RuntimeException();
                    } else if (MyDebug.LOG && n_real_images_to_save < 0) {
                        Log.e(TAG, "real images to save has become negative");
                        throw new RuntimeException();
                    }
                    notifyAll();
                    main_activity.runOnUiThread(new Runnable() {
                        public void run() {
                            main_activity.imageQueueChanged();
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (MyDebug.LOG)
                    Log.e(TAG, "interrupted while trying to read from ImageSaver queue");
            }
        }
    }

    boolean saveImageJpeg(boolean do_in_background,
                          boolean is_hdr,
                          boolean force_suffix,
                          int suffix_offset,
                          boolean save_expo,
                          List<byte[]> images,
                          boolean image_capture_intent, Uri image_capture_intent_uri,
                          boolean using_camera2,
                          Request.ImageFormat image_format, int image_quality,
                          boolean do_auto_stabilise, double level_angle,
                          boolean is_front_facing,
                          boolean mirror,
                          Date current_date,
                          String preference_hdr_contrast_enhancement,
                          int iso,
                          long exposure_time,
                          float zoom_factor,
                          String preference_stamp, String preference_textstamp, int font_size, int color, String pref_style, String preference_stamp_dateformat, String preference_stamp_timeformat, String preference_stamp_gpsformat, String preference_stamp_geo_address, String preference_units_distance,
                          boolean panorama_crop,
                          boolean store_location, Location location, boolean store_geo_direction, double geo_direction,
                          String custom_tag_artist,
                          String custom_tag_copyright,
                          int sample_factor) {
        if (MyDebug.LOG) {
            Log.d(TAG, "saveImageJpeg");
            Log.d(TAG, "do_in_background? " + do_in_background);
            Log.d(TAG, "number of images: " + images.size());
        }
        return saveImage(do_in_background,
                false,
                is_hdr,
                force_suffix,
                suffix_offset,
                save_expo,
                images,
                null,
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
                panorama_crop, store_location, location, store_geo_direction, geo_direction,
                custom_tag_artist,
                custom_tag_copyright,
                sample_factor);
    }

    boolean saveImageRaw(boolean do_in_background,
                         boolean force_suffix,
                         int suffix_offset,
                         RawImage raw_image,
                         Date current_date) {
        if (MyDebug.LOG) {
            Log.d(TAG, "saveImageRaw");
            Log.d(TAG, "do_in_background? " + do_in_background);
        }
        return saveImage(do_in_background,
                true,
                false,
                force_suffix,
                suffix_offset,
                false,
                null,
                raw_image,
                false, null,
                false,
                Request.ImageFormat.STD, 0,
                false, 0.0,
                false,
                false,
                current_date,
                null,
                0,
                0,
                1.0f,
                null, null, 0, 0, null, null, null, null, null, null,
                false, false, null, false, 0.0,
                null, null,
                1);
    }

    private Request pending_image_average_request = null;

    void startImageBatch(boolean do_in_background,
                         Request.ProcessType processType,
                         Request.SaveBase save_base,
                         boolean image_capture_intent, Uri image_capture_intent_uri,
                         boolean using_camera2,
                         Request.ImageFormat image_format, int image_quality,
                         boolean do_auto_stabilise, double level_angle, boolean want_gyro_matrices,
                         boolean is_front_facing,
                         boolean mirror,
                         Date current_date,
                         int iso,
                         long exposure_time,
                         float zoom_factor,
                         String preference_stamp, String preference_textstamp, int font_size, int color, String pref_style, String preference_stamp_dateformat, String preference_stamp_timeformat, String preference_stamp_gpsformat, String preference_stamp_geo_address, String preference_units_distance,
                         boolean panorama_crop,
                         boolean store_location, Location location, boolean store_geo_direction, double geo_direction,
                         String custom_tag_artist,
                         String custom_tag_copyright,
                         int sample_factor) {
        if (MyDebug.LOG) {
            Log.d(TAG, "startImageBatch");
            Log.d(TAG, "do_in_background? " + do_in_background);
        }
        pending_image_average_request = new Request(Request.Type.JPEG,
                processType,
                false,
                0,
                save_base,
                new ArrayList<byte[]>(),
                null,
                image_capture_intent, image_capture_intent_uri,
                using_camera2,
                image_format, image_quality,
                do_auto_stabilise, level_angle, want_gyro_matrices ? new ArrayList<float[]>() : null,
                is_front_facing,
                mirror,
                current_date,
                null,
                iso,
                exposure_time,
                zoom_factor,
                preference_stamp, preference_textstamp, font_size, color, pref_style, preference_stamp_dateformat, preference_stamp_timeformat, preference_stamp_gpsformat, preference_stamp_geo_address, preference_units_distance,
                panorama_crop, store_location, location, store_geo_direction, geo_direction,
                custom_tag_artist,
                custom_tag_copyright,
                sample_factor);
    }

    void addImageBatch(byte[] image, float[] gyro_rotation_matrix) {
        if (MyDebug.LOG)
            Log.d(TAG, "addImageBatch");
        if (pending_image_average_request == null) {
            Log.e(TAG, "addImageBatch called but no pending_image_average_request");
            return;
        }
        pending_image_average_request.jpeg_images.add(image);
        if (gyro_rotation_matrix != null) {
            float[] copy = new float[gyro_rotation_matrix.length];
            System.arraycopy(gyro_rotation_matrix, 0, copy, 0, gyro_rotation_matrix.length);
            pending_image_average_request.gyro_rotation_matrix.add(copy);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "image average request images: " + pending_image_average_request.jpeg_images.size());
    }

    Request getImageBatchRequest() {
        return pending_image_average_request;
    }

    void finishImageBatch(boolean do_in_background) {
        if (MyDebug.LOG)
            Log.d(TAG, "finishImageBatch");
        if (pending_image_average_request == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "finishImageBatch called but no pending_image_average_request");
            return;
        }
        if (do_in_background) {
            if (MyDebug.LOG)
                Log.d(TAG, "add background request");
            int cost = computeRequestCost(false, pending_image_average_request.jpeg_images.size());
            addRequest(pending_image_average_request, cost);
        } else {
            // wait for queue to be empty
            waitUntilDone();
            saveImageNow(pending_image_average_request);
        }
        pending_image_average_request = null;
    }

    void flushImageBatch() {
        pending_image_average_request = null;
    }

    private boolean saveImage(boolean do_in_background,
                              boolean is_raw,
                              boolean is_hdr,
                              boolean force_suffix,
                              int suffix_offset,
                              boolean save_expo,
                              List<byte[]> jpeg_images,
                              RawImage raw_image,
                              boolean image_capture_intent, Uri image_capture_intent_uri,
                              boolean using_camera2,
                              Request.ImageFormat image_format, int image_quality,
                              boolean do_auto_stabilise, double level_angle,
                              boolean is_front_facing,
                              boolean mirror,
                              Date current_date,
                              String preference_hdr_contrast_enhancement,
                              int iso,
                              long exposure_time,
                              float zoom_factor,
                              String preference_stamp, String preference_textstamp, int font_size, int color, String pref_style, String preference_stamp_dateformat, String preference_stamp_timeformat, String preference_stamp_gpsformat, String preference_stamp_geo_address, String preference_units_distance,
                              boolean panorama_crop,
                              boolean store_location, Location location, boolean store_geo_direction, double geo_direction,
                              String custom_tag_artist,
                              String custom_tag_copyright,
                              int sample_factor) {
        if (MyDebug.LOG) {
            Log.d(TAG, "saveImage");
            Log.d(TAG, "do_in_background? " + do_in_background);
        }
        boolean success;


        Request request = new Request(is_raw ? Request.Type.RAW : Request.Type.JPEG,
                is_hdr ? Request.ProcessType.HDR : Request.ProcessType.NORMAL,
                force_suffix,
                suffix_offset,
                save_expo ? Request.SaveBase.SAVEBASE_ALL : Request.SaveBase.SAVEBASE_NONE,
                jpeg_images,
                raw_image,
                image_capture_intent, image_capture_intent_uri,
                using_camera2,
                image_format, image_quality,
                do_auto_stabilise, level_angle, null,
                is_front_facing,
                mirror,
                current_date,
                preference_hdr_contrast_enhancement,
                iso,
                exposure_time,
                zoom_factor,
                preference_stamp, preference_textstamp, font_size, color, pref_style, preference_stamp_dateformat, preference_stamp_timeformat, preference_stamp_gpsformat, preference_stamp_geo_address, preference_units_distance,
                panorama_crop, store_location, location, store_geo_direction, geo_direction,
                custom_tag_artist,
                custom_tag_copyright,
                sample_factor);

        if (do_in_background) {
            if (MyDebug.LOG)
                Log.d(TAG, "add background request");
            int cost = computeRequestCost(is_raw, is_raw ? 1 : request.jpeg_images.size());
            addRequest(request, cost);
            success = true; // always return true when done in background
        } else {
            // wait for queue to be empty
            waitUntilDone();
            if (is_raw) {
                success = saveImageNowRaw(request);
            } else {
                success = saveImageNow(request);
            }
        }

        if (MyDebug.LOG)
            Log.d(TAG, "success: " + success);
        return success;
    }


    private void addRequest(Request request, int cost) {
        if (MyDebug.LOG)
            Log.d(TAG, "addRequest, cost: " + cost);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && main_activity.isDestroyed()) {
            Log.e(TAG, "application is destroyed, image lost!");
            return;
        }
        boolean done = false;
        while (!done) {
            try {
                if (MyDebug.LOG)
                    Log.d(TAG, "ImageSaver thread adding to queue, size: " + queue.size());
                synchronized (this) {
                    n_images_to_save++; // increment before adding to the queue, just to make sure the main thread doesn't think we're all done
                    if (request.type != Request.Type.DUMMY)
                        n_real_images_to_save++;

                    main_activity.runOnUiThread(new Runnable() {
                        public void run() {
                            main_activity.imageQueueChanged();
                        }
                    });
                }
                if (queue.size() + 1 > queue_capacity) {
                    Log.e(TAG, "ImageSaver thread is going to block, queue already full: " + queue.size());
                    test_queue_blocked = true;
                    //throw new RuntimeException(); // test
                }
                queue.put(request); // if queue is full, put() blocks until it isn't full
                if (MyDebug.LOG) {
                    synchronized (this) { // keep FindBugs happy
                        Log.d(TAG, "ImageSaver thread added to queue, size is now: " + queue.size());
                        Log.d(TAG, "images still to save is now: " + n_images_to_save);
                        Log.d(TAG, "real images still to save is now: " + n_real_images_to_save);
                    }
                }
                done = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (MyDebug.LOG)
                    Log.e(TAG, "interrupted while trying to add to ImageSaver queue");
            }
        }
        if (cost > 0) {
            // add "dummy" requests to simulate the cost
            for (int i = 0; i < cost - 1; i++) {
                addDummyRequest();
            }
        }
    }

    private void addDummyRequest() {
        Request dummy_request = new Request(Request.Type.DUMMY,
                Request.ProcessType.NORMAL,
                false,
                0,
                Request.SaveBase.SAVEBASE_NONE,
                null,
                null,
                false, null,
                false,
                Request.ImageFormat.STD, 0,
                false, 0.0, null,
                false,
                false,
                null,
                null,
                0,
                0,
                1.0f,
                null, null, 0, 0, null, null, null, null, null, null,
                false, false, null, false, 0.0,
                null, null,
                1);
        if (MyDebug.LOG)
            Log.d(TAG, "add dummy request");
        addRequest(dummy_request, 1); // cost must be 1, so we don't have infinite recursion!
    }

    /**
     * Wait until the queue is empty and all pending images have been saved.
     */
    public void waitUntilDone() {
        if (MyDebug.LOG)
            Log.d(TAG, "waitUntilDone");
        synchronized (this) {
            if (MyDebug.LOG) {
                Log.d(TAG, "waitUntilDone: queue is size " + queue.size());
                Log.d(TAG, "waitUntilDone: images still to save " + n_images_to_save);
            }
            while (n_images_to_save > 0) {
                if (MyDebug.LOG)
                    Log.d(TAG, "wait until done...");
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (MyDebug.LOG)
                        Log.e(TAG, "interrupted while waiting for ImageSaver queue to be empty");
                }
                if (MyDebug.LOG) {
                    Log.d(TAG, "waitUntilDone: queue is size " + queue.size());
                    Log.d(TAG, "waitUntilDone: images still to save " + n_images_to_save);
                }
            }
        }
        if (MyDebug.LOG)
            Log.d(TAG, "waitUntilDone: images all saved");
    }

    private void setBitmapOptionsSampleSize(BitmapFactory.Options options, int inSampleSize) {
        if (MyDebug.LOG)
            Log.d(TAG, "setBitmapOptionsSampleSize: " + inSampleSize);
        //options.inSampleSize = inSampleSize;
        if (inSampleSize > 1) {
            // use inDensity for better quality, as inSampleSize uses nearest neighbour
            options.inDensity = inSampleSize;
            options.inTargetDensity = 1;
        }
    }

    private Bitmap loadBitmap(byte[] jpeg_image, boolean mutable, int inSampleSize) {
        if (MyDebug.LOG) {
            Log.d(TAG, "loadBitmap");
            Log.d(TAG, "mutable?: " + mutable);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (MyDebug.LOG)
            Log.d(TAG, "options.inMutable is: " + options.inMutable);
        options.inMutable = mutable;
        setBitmapOptionsSampleSize(options, inSampleSize);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            // setting is ignored in Android 5 onwards
            options.inPurgeable = true;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg_image, 0, jpeg_image.length, options);
        if (bitmap == null) {
            Log.e(TAG, "failed to decode bitmap");
        }
        return bitmap;
    }

    /**
     * Helper class for loadBitmaps().
     */
    private static class LoadBitmapThread extends Thread {
        Bitmap bitmap;
        final BitmapFactory.Options options;
        final byte[] jpeg;

        LoadBitmapThread(BitmapFactory.Options options, byte[] jpeg) {
            this.options = options;
            this.jpeg = jpeg;
        }

        public void run() {
            this.bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
        }
    }

    /**
     * Converts the array of jpegs to Bitmaps. The bitmap with index mutable_id will be marked as mutable (or set to -1 to have no mutable bitmaps).
     */
    private List<Bitmap> loadBitmaps(List<byte[]> jpeg_images, int mutable_id, int inSampleSize) {
        if (MyDebug.LOG) {
            Log.d(TAG, "loadBitmaps");
            Log.d(TAG, "mutable_id: " + mutable_id);
        }
        BitmapFactory.Options mutable_options = new BitmapFactory.Options();
        mutable_options.inMutable = true; // bitmap that needs to be writable
        setBitmapOptionsSampleSize(mutable_options, inSampleSize);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = false; // later bitmaps don't need to be writable
        setBitmapOptionsSampleSize(options, inSampleSize);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            // setting is ignored in Android 5 onwards
            mutable_options.inPurgeable = true;
            options.inPurgeable = true;
        }
        LoadBitmapThread[] threads = new LoadBitmapThread[jpeg_images.size()];
        for (int i = 0; i < jpeg_images.size(); i++) {
            threads[i] = new LoadBitmapThread(i == mutable_id ? mutable_options : options, jpeg_images.get(i));
        }
        // start threads
        if (MyDebug.LOG)
            Log.d(TAG, "start threads");
        for (int i = 0; i < jpeg_images.size(); i++) {
            threads[i].start();
        }
        // wait for threads to complete
        boolean ok = true;
        if (MyDebug.LOG)
            Log.d(TAG, "wait for threads to complete");
        try {
            for (int i = 0; i < jpeg_images.size(); i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "threads interrupted");
            e.printStackTrace();
            ok = false;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "threads completed");

        List<Bitmap> bitmaps = new ArrayList<>();
        for (int i = 0; i < jpeg_images.size() && ok; i++) {
            Bitmap bitmap = threads[i].bitmap;
            if (bitmap == null) {
                Log.e(TAG, "failed to decode bitmap in thread: " + i);
                ok = false;
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "bitmap " + i + ": " + bitmap + " is mutable? " + bitmap.isMutable());
            }
            bitmaps.add(bitmap);
        }

        if (!ok) {
            if (MyDebug.LOG)
                Log.d(TAG, "cleanup from failure");
            for (int i = 0; i < jpeg_images.size(); i++) {
                if (threads[i].bitmap != null) {
                    threads[i].bitmap.recycle();
                    threads[i].bitmap = null;
                }
            }
            bitmaps.clear();
            System.gc();
            return null;
        }

        return bitmaps;
    }

    public static float getHDRAlpha(String preference_hdr_contrast_enhancement, long exposure_time, int n_bitmaps) {
        boolean use_hdr_alpha;
        if (n_bitmaps == 1) {
            // DRO always applies hdr_alpha
            use_hdr_alpha = true;
        } else {
            // else HDR
            switch (preference_hdr_contrast_enhancement) {
                case "preference_hdr_contrast_enhancement_off":
                    use_hdr_alpha = false;
                    break;
                case "preference_hdr_contrast_enhancement_smart":
                default:
                    use_hdr_alpha = (exposure_time < 1000000000L / 59);
                    break;
                case "preference_hdr_contrast_enhancement_always":
                    use_hdr_alpha = true;
                    break;
            }
        }
        float hdr_alpha = use_hdr_alpha ? 0.5f : 0.0f;
        if (MyDebug.LOG) {
            Log.d(TAG, "preference_hdr_contrast_enhancement: " + preference_hdr_contrast_enhancement);
            Log.d(TAG, "exposure_time: " + exposure_time);
            Log.d(TAG, "hdr_alpha: " + hdr_alpha);
        }
        return hdr_alpha;
    }

    private final static String gyro_info_doc_tag = "open_camera_gyro_info";
    private final static String gyro_info_panorama_pics_per_screen_tag = "panorama_pics_per_screen";
    private final static String gyro_info_camera_view_angle_x_tag = "camera_view_angle_x";
    private final static String gyro_info_camera_view_angle_y_tag = "camera_view_angle_y";
    private final static String gyro_info_image_tag = "image";
    private final static String gyro_info_vector_tag = "vector";
    private final static String gyro_info_vector_right_type = "X";
    private final static String gyro_info_vector_up_type = "Y";
    private final static String gyro_info_vector_screen_type = "Z";

    private void writeGyroDebugXml(Writer writer, Request request) throws IOException {
        XmlSerializer xmlSerializer = Xml.newSerializer();

        xmlSerializer.setOutput(writer);
        xmlSerializer.startDocument("UTF-8", true);
        xmlSerializer.startTag(null, gyro_info_doc_tag);
        xmlSerializer.attribute(null, gyro_info_panorama_pics_per_screen_tag, "" + MyApplicationInterface.getPanoramaPicsPerScreen());
        xmlSerializer.attribute(null, gyro_info_camera_view_angle_x_tag, "" + request.camera_view_angle_x);
        xmlSerializer.attribute(null, gyro_info_camera_view_angle_y_tag, "" + request.camera_view_angle_y);

        float[] inVector = new float[3];
        float[] outVector = new float[3];
        for (int i = 0; i < request.gyro_rotation_matrix.size(); i++) {
            xmlSerializer.startTag(null, gyro_info_image_tag);
            xmlSerializer.attribute(null, "index", "" + i);

            GyroSensor.setVector(inVector, 1.0f, 0.0f, 0.0f); // vector pointing in "right" direction
            GyroSensor.transformVector(outVector, request.gyro_rotation_matrix.get(i), inVector);
            xmlSerializer.startTag(null, gyro_info_vector_tag);
            xmlSerializer.attribute(null, "type", gyro_info_vector_right_type);
            xmlSerializer.attribute(null, "x", "" + outVector[0]);
            xmlSerializer.attribute(null, "y", "" + outVector[1]);
            xmlSerializer.attribute(null, "z", "" + outVector[2]);
            xmlSerializer.endTag(null, gyro_info_vector_tag);

            GyroSensor.setVector(inVector, 0.0f, 1.0f, 0.0f); // vector pointing in "up" direction
            GyroSensor.transformVector(outVector, request.gyro_rotation_matrix.get(i), inVector);
            xmlSerializer.startTag(null, gyro_info_vector_tag);
            xmlSerializer.attribute(null, "type", gyro_info_vector_up_type);
            xmlSerializer.attribute(null, "x", "" + outVector[0]);
            xmlSerializer.attribute(null, "y", "" + outVector[1]);
            xmlSerializer.attribute(null, "z", "" + outVector[2]);
            xmlSerializer.endTag(null, gyro_info_vector_tag);

            GyroSensor.setVector(inVector, 0.0f, 0.0f, -1.0f); // vector pointing behind the device's screen
            GyroSensor.transformVector(outVector, request.gyro_rotation_matrix.get(i), inVector);
            xmlSerializer.startTag(null, gyro_info_vector_tag);
            xmlSerializer.attribute(null, "type", gyro_info_vector_screen_type);
            xmlSerializer.attribute(null, "x", "" + outVector[0]);
            xmlSerializer.attribute(null, "y", "" + outVector[1]);
            xmlSerializer.attribute(null, "z", "" + outVector[2]);
            xmlSerializer.endTag(null, gyro_info_vector_tag);

            xmlSerializer.endTag(null, gyro_info_image_tag);
        }

        xmlSerializer.endTag(null, gyro_info_doc_tag);
        xmlSerializer.endDocument();
        xmlSerializer.flush();
    }

    public static class GyroDebugInfo {
        public static class GyroImageDebugInfo {
            public float[] vectorRight; // X axis
            public float[] vectorUp; // Y axis
            public float[] vectorScreen; // vector into the screen - actually the -Z axis
        }

        public final List<GyroImageDebugInfo> image_info;

        public GyroDebugInfo() {
            image_info = new ArrayList<>();
        }
    }

    /*private Bitmap getResizedBitmap(Bitmap bitmap) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inTempStorage = new byte[16 * 1024];
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPictureSize();

        int height11 = size.height;
        int width11 = size.width;
        float mb = (width11 * height11) / 1024000;
        if (mb > 32f) {
            opt.inSampleSize = 32;
        } else if (mb > 16f) {
            opt.inSampleSize = 16;
        } else if (mb > 8f) {
            opt.inSampleSize = 8;
        } else if (mb > 6f) {
            opt.inSampleSize = 6;
        } else if (mb > 4f) {
            opt.inSampleSize = 4;
        } else if (mb > 3f) {
            opt.inSampleSize = 2;
        } else if (mb >= 1f) {
            opt.inSampleSize = 1;
        }
//        capturedImageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
    }*/

    private int getDeviceRotatedAngle() {
        final int ORIENTATION_0 = 0;
        final int ORIENTATION_270 = 1;
        final int ORIENTATION_180 = 2;
        final int ORIENTATION_90 = 3;

        Display display = ((WindowManager) main_activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int screenOrientation = display.getRotation();

        switch (screenOrientation) {
            default:
            case ORIENTATION_0: // Portrait
                return 0;
            case ORIENTATION_90: // Landscape right
                return 90;
            case ORIENTATION_270: // Landscape left
                return 270;
            case ORIENTATION_180: // Landscape left
                return 180;
        }

    }

    private boolean saveImageNow(final Request request) {
        try {

            ContextWrapper cw = new ContextWrapper(main_activity);
            // path to /data/data/yourapp/app_data/imageDir
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            // Create imageDir
            File mypath = new File(directory, "photoselfie.jpeg");
            if (mypath.exists()) {
                mypath.delete();
            }

            //File photo = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
            int needRotation = 0;
            if (!CameraController.rotatedAngle.equals("-1")) {
                switch (CameraController.rotatedAngle) {
                    case "90":
                        needRotation = 0;
                        break;
                    case "180":
                        needRotation = 270;
                        break;
                    case "270":
                        needRotation = 180;
                        break;
                    case "0":
                    default:
                        needRotation = 90;
                }
            }


            if (mypath.exists()) {
                mypath.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(mypath.getPath());
                fos.write(request.jpeg_images.get(0));
                fos.close();
            } catch (IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            Uri bitmapUri = Uri.fromFile(mypath);

            ExifInterface ei = new ExifInterface(bitmapUri.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Bitmap bitmap = BitmapFactory.decodeByteArray(request.jpeg_images.get(0), 0, request.jpeg_images.get(0).length);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap rotatedBitmap = bitmap;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    needRotation = needRotation + 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    needRotation = needRotation + 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    needRotation = needRotation + 270;
                    break;
            }
            needRotation = main_activity.isFrontCamEnabled() ? needRotation + 180 : needRotation;
            rotatedBitmap = rotateImage(bitmap, needRotation);
            Bitmap frontCamBitmap = null;
            if (main_activity.isFrontCamEnabled()) {
                final Matrix m = new Matrix();
                m.setValues(new float[]{-1, 0, 0, 0, 1, 0, 0, 0, 1});
                frontCamBitmap = Bitmap.createBitmap(rotatedBitmap, 0, 0, rotatedBitmap.getWidth(), rotatedBitmap.getHeight(), m, true);
            }

            if (rotatedBitmap != null && request.jpeg_images.size() > 0) {
                if (main_activity.isFrontCamEnabled()) {
                    frontCamBitmap = getSquareBitmap(frontCamBitmap);
                    handleMatrix(frontCamBitmap);
                } else {
                    rotatedBitmap = getSquareBitmap(rotatedBitmap);
                    handleMatrix(rotatedBitmap);
                }
            } else {
                main_activity.setCaptureClickable(true);
            }

        } catch (Exception e) {
            main_activity.setCaptureClickable(true);
            PSR_Utils.hideProgressDialog();
            e.printStackTrace();
        }
        return false;
    }

    private void handleMatrix(Bitmap rotatedBitmap) {

        String path = PSR_Utils.saveToInternalStorage(rotatedBitmap, main_activity);
        PSR_Utils.hideProgressDialog();
        Intent intent = new Intent(main_activity, BluredImageActivity.class);
        intent.putExtra(PSRConstants.SERVICEREQTYPEID,PSRConstants.LIVESELFIE_SERVICE_REQ_ID);
        //intent.putExtra("ISCAME_FROMPHOTO_SELFIE", false);
        intent.putExtra("FINALSELFIE", path);
        intent.putExtra(PSRConstants.CELEBRITYID, main_activity.getCelebrityId());
        intent.putExtra(PSRConstants.LIVESELFIECOST, main_activity.getLiveSelfieCost());

        if (main_activity.isCameFromHistory()) {
            intent.putExtra(PSRConstants.EVENTID, main_activity.getLiveEventID());
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY,true);
            intent.putExtra(PSRConstants.SERVICEREQID,main_activity.getServiceReqId());

        }else {
            intent.putExtra(PSRConstants.ISCAMEFROMHISTORY,false);
        }
        main_activity.startActivity(intent);
        main_activity.finish();

    }

    public Bitmap viewToBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private Bitmap getSquareBitmap(Bitmap srcBmp) {
        Bitmap dstBmp = null;
        try {
            dstBmp = Bitmap.createBitmap(
                    srcBmp,
                    0,
                    0,
                    srcBmp.getWidth(),
                    srcBmp.getWidth()
            );

        } catch (Exception e) {
            PSR_Utils.hideProgressDialog();

        }
        return dstBmp;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    /*private boolean saveImageNow(final Request request) {
        if( MyDebug.LOG )
            Log.d(TAG, "saveImageNow");

        if( request.type != Request.Type.JPEG ) {
            if( MyDebug.LOG )
                Log.d(TAG, "saveImageNow called with non-jpeg request");
            // throw runtime exception, as this is a programming error
            throw new RuntimeException();
        }
        else if( request.jpeg_images.size() == 0 ) {
            if( MyDebug.LOG )
                Log.d(TAG, "saveImageNow called with zero images");
            // throw runtime exception, as this is a programming error
            throw new RuntimeException();
        }

        boolean success;
        if( request.process_type == Request.ProcessType.AVERAGE ) {
            if( MyDebug.LOG )
                Log.d(TAG, "average");

            saveBaseImages(request, "_");
            main_activity.savingImage(true);

            Bitmap nr_bitmap;
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                try {
                    long time_s = System.currentTimeMillis();
                    int inSampleSize = hdrProcessor.getAvgSampleSize(request.iso);
                    final boolean use_smp = true;
                    final int n_smp_images = 4;
                    long this_time_s = System.currentTimeMillis();
                    List<Bitmap> bitmaps = null;
                    Bitmap bitmap0, bitmap1;
                    if( use_smp ) {
                        int n_remaining = request.jpeg_images.size();
                        int n_load = Math.min(n_smp_images, n_remaining);
                        if( MyDebug.LOG ) {
                            Log.d(TAG, "n_remaining: " + n_remaining);
                            Log.d(TAG, "n_load: " + n_load);
                        }
                        List<byte []> sub_jpeg_list = new ArrayList<>();
                        for(int j=0;j<n_load;j++) {
                            sub_jpeg_list.add(request.jpeg_images.get(j));
                        }
                        bitmaps = loadBitmaps(sub_jpeg_list, -1, inSampleSize);
                        if( MyDebug.LOG )
                            Log.d(TAG, "length of bitmaps list is now: " + bitmaps.size());
                        bitmap0 = bitmaps.get(0);
                        bitmap1 = bitmaps.get(1);
                    }
                    else {
                        bitmap0 = loadBitmap(request.jpeg_images.get(0), false, inSampleSize);
                        bitmap1 = loadBitmap(request.jpeg_images.get(1), false, inSampleSize);
                    }
                    if( MyDebug.LOG ) {
                        Log.d(TAG, "*** time for loading first bitmaps: " + (System.currentTimeMillis() - this_time_s));
                    }
                    int width = bitmap0.getWidth();
                    int height = bitmap0.getHeight();
                    float avg_factor = 1.0f;
                    this_time_s = System.currentTimeMillis();
                    HDRProcessor.AvgData avg_data = hdrProcessor.processAvg(bitmap0, bitmap1, avg_factor, request.iso, request.zoom_factor);
                    if( bitmaps != null ) {
                        bitmaps.set(0, null);
                        bitmaps.set(1, null);
                    }
                    if( MyDebug.LOG ) {
                        Log.d(TAG, "*** time for processing first two bitmaps: " + (System.currentTimeMillis() - this_time_s));
                    }
                    Allocation allocation = avg_data.allocation_out;

                    for(int i=2;i<request.jpeg_images.size();i++) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "processAvg for image: " + i);

                        this_time_s = System.currentTimeMillis();
                        Bitmap new_bitmap;
                        if( use_smp ) {
                            // check if we already loaded the bitmap
                            if( MyDebug.LOG )
                                Log.d(TAG, "length of bitmaps list: " + bitmaps.size());
                            if( i < bitmaps.size() ) {
                                if( MyDebug.LOG ) {
                                    Log.d(TAG, "already loaded bitmap from previous iteration with SMP");
                                }
                                new_bitmap = bitmaps.get(i);
                            }
                            else {
                                int n_remaining = request.jpeg_images.size() - i;
                                int n_load = Math.min(n_smp_images, n_remaining);
                                if( MyDebug.LOG ) {
                                    Log.d(TAG, "n_remaining: " + n_remaining);
                                    Log.d(TAG, "n_load: " + n_load);
                                }
                                List<byte []> sub_jpeg_list = new ArrayList<>();
                                for(int j=i;j<i+n_load;j++) {
                                    sub_jpeg_list.add(request.jpeg_images.get(j));
                                }
                                List<Bitmap> new_bitmaps = loadBitmaps(sub_jpeg_list, -1, inSampleSize);
                                bitmaps.addAll(new_bitmaps);
                                if( MyDebug.LOG )
                                    Log.d(TAG, "length of bitmaps list is now: " + bitmaps.size());
                                new_bitmap = bitmaps.get(i);
                            }
                        }
                        else {
                            new_bitmap = loadBitmap(request.jpeg_images.get(i), false, inSampleSize);
                        }
                        if( MyDebug.LOG ) {
                            Log.d(TAG, "*** time for loading extra bitmap: " + (System.currentTimeMillis() - this_time_s));
                        }
                        avg_factor = (float)i;
                        this_time_s = System.currentTimeMillis();
                        hdrProcessor.updateAvg(avg_data, width, height, new_bitmap, avg_factor, request.iso, request.zoom_factor);
                        // updateAvg recycles new_bitmap
                        if( bitmaps != null ) {
                            bitmaps.set(i, null);
                        }
                        if( MyDebug.LOG ) {
                            Log.d(TAG, "*** time for updating extra bitmap: " + (System.currentTimeMillis() - this_time_s));
                        }
                    }

                    this_time_s = System.currentTimeMillis();
                    nr_bitmap = hdrProcessor.avgBrighten(allocation, width, height, request.iso, request.exposure_time);
                    if( MyDebug.LOG ) {
                        Log.d(TAG, "*** time for brighten: " + (System.currentTimeMillis() - this_time_s));
                    }
                    avg_data.destroy();
                    if( MyDebug.LOG ) {
                        Log.d(TAG, "*** total time for saving NR image: " + (System.currentTimeMillis() - time_s));
                    }
                }
                catch(HDRProcessorException e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
            else {
                Log.e(TAG, "shouldn't have offered NoiseReduction as an option if not on Android 5");
                throw new RuntimeException();
            }

            if( MyDebug.LOG )
                Log.d(TAG, "nr_bitmap: " + nr_bitmap + " is mutable? " + nr_bitmap.isMutable());
            System.gc();
            main_activity.savingImage(false);

            if( MyDebug.LOG )
                Log.d(TAG, "save NR image");
            String suffix = "_NR";
            success = saveSingleImageNow(request, request.jpeg_images.get(0), nr_bitmap, suffix, true, true, true, false);
            if( MyDebug.LOG && !success )
                Log.e(TAG, "saveSingleImageNow failed for nr image");
            nr_bitmap.recycle();
            System.gc();
        }
        else if( request.process_type == Request.ProcessType.HDR ) {
            if( MyDebug.LOG )
                Log.d(TAG, "hdr");
            if( request.jpeg_images.size() != 1 && request.jpeg_images.size() != 3 ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "saveImageNow expected either 1 or 3 images for hdr, not " + request.jpeg_images.size());
                // throw runtime exception, as this is a programming error
                throw new RuntimeException();
            }

            long time_s = System.currentTimeMillis();
            if( request.jpeg_images.size() > 1 ) {
                saveBaseImages(request, "_");
                if( MyDebug.LOG ) {
                    Log.d(TAG, "HDR performance: time after saving base exposures: " + (System.currentTimeMillis() - time_s));
                }
            }

            if( MyDebug.LOG )
                Log.d(TAG, "create HDR image");
            main_activity.savingImage(true);
            int base_bitmap = (request.jpeg_images.size()-1)/2;
            if( MyDebug.LOG )
                Log.d(TAG, "base_bitmap: " + base_bitmap);
            List<Bitmap> bitmaps = loadBitmaps(request.jpeg_images, base_bitmap, 1);
            if( bitmaps == null ) {
                if( MyDebug.LOG )
                    Log.e(TAG, "failed to load bitmaps");
                main_activity.savingImage(false);
                return false;
            }
            if( MyDebug.LOG ) {
                Log.d(TAG, "HDR performance: time after decompressing base exposures: " + (System.currentTimeMillis() - time_s));
            }
            float hdr_alpha = getHDRAlpha(request.preference_hdr_contrast_enhancement, request.exposure_time, bitmaps.size());
            if( MyDebug.LOG )
                Log.d(TAG, "before HDR first bitmap: " + bitmaps.get(0) + " is mutable? " + bitmaps.get(0).isMutable());
            try {
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                    hdrProcessor.processHDR(bitmaps, true, null, true, null, hdr_alpha, 4, true, HDRProcessor.TonemappingAlgorithm.TONEMAPALGORITHM_REINHARD, HDRProcessor.DROTonemappingAlgorithm.DROALGORITHM_GAINGAMMA); // this will recycle all the bitmaps except bitmaps.get(0), which will contain the hdr image
                }
                else {
                    Log.e(TAG, "shouldn't have offered HDR as an option if not on Android 5");
                    throw new RuntimeException();
                }
            }
            catch(HDRProcessorException e) {
                Log.e(TAG, "HDRProcessorException from processHDR: " + e.getCode());
                e.printStackTrace();
                if( e.getCode() == HDRProcessorException.UNEQUAL_SIZES ) {
                    // this can happen on OnePlus 3T with old camera API with front camera, seems to be a bug that resolution changes when exposure compensation is set!
                    Log.e(TAG, "UNEQUAL_SIZES");
                    bitmaps.clear();
                    System.gc();
                    main_activity.savingImage(false);
                    return false;
                }
                else {
                    // throw RuntimeException, as we shouldn't ever get the error INVALID_N_IMAGES, if we do it's a programming error
                    throw new RuntimeException();
                }
            }
            if( MyDebug.LOG ) {
                Log.d(TAG, "HDR performance: time after creating HDR image: " + (System.currentTimeMillis() - time_s));
            }
            if( MyDebug.LOG )
                Log.d(TAG, "after HDR first bitmap: " + bitmaps.get(0) + " is mutable? " + bitmaps.get(0).isMutable());
            Bitmap hdr_bitmap = bitmaps.get(0);
            if( MyDebug.LOG )
                Log.d(TAG, "hdr_bitmap: " + hdr_bitmap + " is mutable? " + hdr_bitmap.isMutable());
            bitmaps.clear();
            System.gc();
            main_activity.savingImage(false);

            if( MyDebug.LOG )
                Log.d(TAG, "save HDR image");
            int base_image_id = ((request.jpeg_images.size()-1)/2);
            if( MyDebug.LOG )
                Log.d(TAG, "base_image_id: " + base_image_id);
            String suffix = request.jpeg_images.size() == 1 ? "_DRO" : "_HDR";
            success = saveSingleImageNow(request, request.jpeg_images.get(base_image_id), hdr_bitmap, suffix, true, true, true, false);
            if( MyDebug.LOG && !success )
                Log.e(TAG, "saveSingleImageNow failed for hdr image");
            if( MyDebug.LOG ) {
                Log.d(TAG, "HDR performance: time after saving HDR image: " + (System.currentTimeMillis() - time_s));
            }
            hdr_bitmap.recycle();
            System.gc();
        }
        else if( request.process_type == Request.ProcessType.PANORAMA ) {
            if( MyDebug.LOG )
                Log.d(TAG, "panorama");

            if( !request.image_capture_intent && request.save_base == Request.SaveBase.SAVEBASE_ALL_PLUS_DEBUG ) {
                try {
                    StringWriter writer = new StringWriter();

                    writeGyroDebugXml(writer, request);

                    StorageUtils storageUtils = main_activity.getStorageUtils();
                    File saveFile = null;
                    Uri saveUri = null;
                    if( storageUtils.isUsingSAF() ) {
                        saveUri = storageUtils.createOutputMediaFileSAF(StorageUtils.MEDIA_TYPE_GYRO_INFO, "", "xml", request.current_date);
                    }
                    else {
                        saveFile = storageUtils.createOutputMediaFile(StorageUtils.MEDIA_TYPE_GYRO_INFO, "", "xml", request.current_date);
                        if( MyDebug.LOG )
                            Log.d(TAG, "save to: " + saveFile.getAbsolutePath());
                    }

                    OutputStream outputStream;
                    if( saveFile != null )
                        outputStream = new FileOutputStream(saveFile);
                    else
                        outputStream = main_activity.getContentResolver().openOutputStream(saveUri);
                    try {
                        //outputStream.write(gyro_text.toString().getBytes());
                        outputStream.write(writer.toString().getBytes(Charset.forName("UTF-8")));
                    }
                    finally {
                        outputStream.close();
                    }

                    if( saveFile != null ) {
                        storageUtils.broadcastFile(saveFile, false, false, false);
                    }
                    else {
                        broadcastSAFFile(saveUri, false);
                    }
                }
                catch(IOException e) {
                    Log.e(TAG, "failed to write gyro text file");
                    e.printStackTrace();
                }
            }
            saveBaseImages(request, "_");

            main_activity.savingImage(true);

            long time_s = System.currentTimeMillis();

            if( MyDebug.LOG )
                Log.d(TAG, "panorama_dir_left_to_right: " + request.panorama_dir_left_to_right);
            if( !request.panorama_dir_left_to_right ) {
                Collections.reverse(request.jpeg_images);
                // shouldn't use gyro_rotation_matrix from this point, but keep in sync with jpeg_images just in case
                Collections.reverse(request.gyro_rotation_matrix);
            }

            List<Bitmap> bitmaps = loadBitmaps(request.jpeg_images, -1, 1);
            if( bitmaps == null ) {
                if( MyDebug.LOG )
                    Log.e(TAG, "failed to load bitmaps");
                main_activity.savingImage(false);
                return false;
            }
            if( MyDebug.LOG ) {
                Log.d(TAG, "panorama performance: time after decompressing base exposures: " + (System.currentTimeMillis() - time_s));
            }

            // rotate the bitmaps if necessary for exif tags
            File exifTempFile = getExifTempFile(request.jpeg_images.get(0));
            for(int i=0;i<bitmaps.size();i++) {
                Bitmap bitmap = bitmaps.get(i);
                bitmap = rotateForExif(bitmap, request.jpeg_images.get(0), exifTempFile);
                bitmaps.set(i, bitmap);
            }
            if( MyDebug.LOG ) {
                Log.d(TAG, "panorama performance: time after rotating for exif: " + (System.currentTimeMillis() - time_s));
            }

            Bitmap panorama;
            try {
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                    panorama = panoramaProcessor.panorama(bitmaps, MyApplicationInterface.getPanoramaPicsPerScreen(), request.camera_view_angle_y, request.panorama_crop);
                }
                else {
                    Log.e(TAG, "shouldn't have offered panorama as an option if not on Android 5");
                    throw new RuntimeException();
                }
            }
            catch(PanoramaProcessorException e) {
                Log.e(TAG, "PanoramaProcessorException from panorama: " + e.getCode());
                e.printStackTrace();
                if( e.getCode() == PanoramaProcessorException.UNEQUAL_SIZES ) {
                    Log.e(TAG, "UNEQUAL_SIZES");
                    bitmaps.clear();
                    System.gc();
                    main_activity.savingImage(false);
                    return false;
                }
                else {
                    // throw RuntimeException, as we shouldn't ever get the error INVALID_N_IMAGES, if we do it's a programming error
                    throw new RuntimeException();
                }
            }
            if( MyDebug.LOG ) {
                Log.d(TAG, "panorama performance: time after creating panorama image: " + (System.currentTimeMillis() - time_s));
            }
            if( MyDebug.LOG )
                Log.d(TAG, "panorama: " + panorama);
            bitmaps.clear();
            System.gc();

            main_activity.savingImage(false);

            if( MyDebug.LOG )
                Log.d(TAG, "save panorama image");
            String suffix = "_PANO";
            success = saveSingleImageNow(request, request.jpeg_images.get(0), panorama, suffix, true, true, true, true);
            if( MyDebug.LOG && !success )
                Log.e(TAG, "saveSingleImageNow failed for panorama image");
            panorama.recycle();
            System.gc();

            if( exifTempFile != null && !exifTempFile.delete() ) {
                if( MyDebug.LOG )
                    Log.e(TAG, "failed to delete temp " + exifTempFile.getAbsolutePath());
            }
        }
        else {
            // see note above how we used to use "_EXP" for the suffix for multiple images
            //String suffix = "_EXP";
            String suffix = "_";
            success = saveImages(request, suffix, false, true, true);
        }

        return success;


    }*/

    private boolean saveImages(Request request, String suffix, boolean first_only, boolean update_thumbnail, boolean share) {
        boolean success = true;
        int mid_image = request.jpeg_images.size() / 2;
        for (int i = 0; i < request.jpeg_images.size(); i++) {
            // note, even if one image fails, we still try saving the other images - might as well give the user as many images as we can...
            byte[] image = request.jpeg_images.get(i);
            boolean multiple_jpegs = request.jpeg_images.size() > 1 && !first_only;
            String filename_suffix = (multiple_jpegs || request.force_suffix) ? suffix + (i + request.suffix_offset) : "";
            boolean share_image = share && (i == mid_image);
            if (!saveSingleImageNow(request, image, null, filename_suffix, update_thumbnail, share_image, false, false)) {
                if (MyDebug.LOG)
                    Log.e(TAG, "saveSingleImageNow failed for image: " + i);
                success = false;
            }
            if (first_only)
                break; // only requested the first
        }
        return success;//editing navigation
    }

    private void saveBaseImages(Request request, String suffix) {
        if (MyDebug.LOG)
            Log.d(TAG, "saveBaseImages");
        if (!request.image_capture_intent && request.save_base != Request.SaveBase.SAVEBASE_NONE) {
            if (MyDebug.LOG)
                Log.d(TAG, "save base images");

            Request base_request = request;
            if (request.process_type == Request.ProcessType.PANORAMA) {
                base_request = request.copy();
                base_request.image_format = Request.ImageFormat.PNG;
                base_request.preference_stamp = "preference_stamp_no";
                base_request.preference_textstamp = "";
                base_request.do_auto_stabilise = false;
                base_request.mirror = false;
            } else if (request.process_type == Request.ProcessType.AVERAGE) {
                base_request = request.copy();
                base_request.image_quality = 100;
            }
            saveImages(base_request, suffix, base_request.save_base == Request.SaveBase.SAVEBASE_FIRST, false, false);
        }
    }

    private Bitmap autoStabilise(byte[] data, Bitmap bitmap, double level_angle, boolean is_front_facing, File exifTempFile) {
        if (MyDebug.LOG) {
            Log.d(TAG, "autoStabilise");
            Log.d(TAG, "level_angle: " + level_angle);
            Log.d(TAG, "is_front_facing: " + is_front_facing);
        }
        while (level_angle < -90)
            level_angle += 180;
        while (level_angle > 90)
            level_angle -= 180;
        if (MyDebug.LOG)
            Log.d(TAG, "auto stabilising... angle: " + level_angle);
        if (bitmap == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "need to decode bitmap to auto-stabilise");
            // bitmap doesn't need to be mutable here, as this won't be the final bitmap returned from the auto-stabilise code
            bitmap = loadBitmapWithRotation(data, false, exifTempFile);
            if (bitmap == null) {
                System.gc();
            }
        }
        if (bitmap != null) {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (MyDebug.LOG) {
                Log.d(TAG, "level_angle: " + level_angle);
                Log.d(TAG, "decoded bitmap size " + width + ", " + height);
                Log.d(TAG, "bitmap size: " + width * height * 4);
            }
            Matrix matrix = new Matrix();
            double level_angle_rad_abs = Math.abs(Math.toRadians(level_angle));
            int w1 = width, h1 = height;
            double w0 = (w1 * Math.cos(level_angle_rad_abs) + h1 * Math.sin(level_angle_rad_abs));
            double h0 = (w1 * Math.sin(level_angle_rad_abs) + h1 * Math.cos(level_angle_rad_abs));
            // apply a scale so that the overall image size isn't increased
            float orig_size = w1 * h1;
            float rotated_size = (float) (w0 * h0);
            float scale = (float) Math.sqrt(orig_size / rotated_size);
            if (main_activity.test_low_memory) {
                if (MyDebug.LOG) {
                    Log.d(TAG, "TESTING LOW MEMORY");
                    Log.d(TAG, "scale was: " + scale);
                }
                if (width * height >= 7500)
                    scale *= 1.5f;
                else
                    scale *= 2.0f;
            }
            matrix.postScale(scale, scale);
            w0 *= scale;
            h0 *= scale;
            w1 *= scale;
            h1 *= scale;
            if (MyDebug.LOG) {
                Log.d(TAG, "after scaling: w0 = " + w0 + " , h0 = " + h0);
                Log.d(TAG, "after scaling: w1 = " + w1 + " , h1 = " + h1);
            }
            if (is_front_facing) {
                matrix.postRotate((float) -level_angle);
            } else {
                matrix.postRotate((float) level_angle);
            }
            Bitmap new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            // careful, as new_bitmap is sometimes not a copy!
            if (new_bitmap != bitmap) {
                bitmap.recycle();
                bitmap = new_bitmap;
            }
            System.gc();
            double tan_theta = Math.tan(level_angle_rad_abs);
            double sin_theta = Math.sin(level_angle_rad_abs);
            double denom = (h0 / w0 + tan_theta);
            double alt_denom = (w0 / h0 + tan_theta);
            if (denom == 0.0 || denom < 1.0e-14) {
                if (MyDebug.LOG)
                    Log.d(TAG, "zero denominator?!");
            } else if (alt_denom == 0.0 || alt_denom < 1.0e-14) {
                if (MyDebug.LOG)
                    Log.d(TAG, "zero alt denominator?!");
            } else {
                int w2 = (int) ((h0 + 2.0 * h1 * sin_theta * tan_theta - w0 * tan_theta) / denom);
                int h2 = (int) (w2 * h0 / w0);
                int alt_h2 = (int) ((w0 + 2.0 * w1 * sin_theta * tan_theta - h0 * tan_theta) / alt_denom);
                int alt_w2 = (int) (alt_h2 * w0 / h0);
                if (MyDebug.LOG) {
                    //Log.d(TAG, "h0 " + h0 + " 2.0*h1*sin_theta*tan_theta " + 2.0*h1*sin_theta*tan_theta + " w0*tan_theta " + w0*tan_theta + " / h0/w0 " + h0/w0 + " tan_theta " + tan_theta);
                    Log.d(TAG, "w2 = " + w2 + " , h2 = " + h2);
                    Log.d(TAG, "alt_w2 = " + alt_w2 + " , alt_h2 = " + alt_h2);
                }
                if (alt_w2 < w2) {
                    if (MyDebug.LOG) {
                        Log.d(TAG, "chose alt!");
                    }
                    w2 = alt_w2;
                    h2 = alt_h2;
                }
                if (w2 <= 0)
                    w2 = 1;
                else if (w2 >= bitmap.getWidth())
                    w2 = bitmap.getWidth() - 1;
                if (h2 <= 0)
                    h2 = 1;
                else if (h2 >= bitmap.getHeight())
                    h2 = bitmap.getHeight() - 1;
                int x0 = (bitmap.getWidth() - w2) / 2;
                int y0 = (bitmap.getHeight() - h2) / 2;
                if (MyDebug.LOG) {
                    Log.d(TAG, "x0 = " + x0 + " , y0 = " + y0);
                }
                new_bitmap = Bitmap.createBitmap(bitmap, x0, y0, w2, h2);
                if (new_bitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = new_bitmap;
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "bitmap is mutable?: " + bitmap.isMutable());
                System.gc();
            }
        }
        return bitmap;
    }

    private Bitmap mirrorImage(byte[] data, Bitmap bitmap, File exifTempFile) {
        if (MyDebug.LOG) {
            Log.d(TAG, "mirrorImage");
        }
        if (bitmap == null) {
            bitmap = loadBitmapWithRotation(data, false, exifTempFile);
            if (bitmap == null) {
                System.gc();
            }
        }
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Bitmap new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
            // careful, as new_bitmap is sometimes not a copy!
            if (new_bitmap != bitmap) {
                bitmap.recycle();
                bitmap = new_bitmap;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "bitmap is mutable?: " + bitmap.isMutable());
        }
        return bitmap;
    }

    private Bitmap stampImage(final Request request, byte[] data, Bitmap bitmap, File exifTempFile) {
        if (MyDebug.LOG) {
            Log.d(TAG, "stampImage");
        }
        final MyApplicationInterface applicationInterface = main_activity.getApplicationInterface();
        boolean dategeo_stamp = request.preference_stamp.equals("preference_stamp_yes");
        boolean text_stamp = request.preference_textstamp.length() > 0;
        if (dategeo_stamp || text_stamp) {
            if (bitmap == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "decode bitmap in order to stamp info");
                bitmap = loadBitmapWithRotation(data, true, exifTempFile);
                if (bitmap == null) {
                    System.gc();
                }
            }
            if (bitmap != null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "stamp info to bitmap: " + bitmap);
                if (MyDebug.LOG)
                    Log.d(TAG, "bitmap is mutable?: " + bitmap.isMutable());
                int font_size = request.font_size;
                int color = request.color;
                String pref_style = request.pref_style;
                if (MyDebug.LOG)
                    Log.d(TAG, "pref_style: " + pref_style);
                String preference_stamp_dateformat = request.preference_stamp_dateformat;
                String preference_stamp_timeformat = request.preference_stamp_timeformat;
                String preference_stamp_gpsformat = request.preference_stamp_gpsformat;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                if (MyDebug.LOG) {
                    Log.d(TAG, "decoded bitmap size " + width + ", " + height);
                    Log.d(TAG, "bitmap size: " + width * height * 4);
                }
                Canvas canvas = new Canvas(bitmap);
                p.setColor(Color.WHITE);
                int smallest_size = (width < height) ? width : height;
                float scale = ((float) smallest_size) / (72.0f * 4.0f);
                int font_size_pixel = (int) (font_size * scale + 0.5f); // convert pt to pixels
                if (MyDebug.LOG) {
                    Log.d(TAG, "scale: " + scale);
                    Log.d(TAG, "font_size: " + font_size);
                    Log.d(TAG, "font_size_pixel: " + font_size_pixel);
                }
                p.setTextSize(font_size_pixel);
                int offset_x = (int) (8 * scale + 0.5f); // convert pt to pixels
                int offset_y = (int) (8 * scale + 0.5f); // convert pt to pixels
                int diff_y = (int) ((font_size + 4) * scale + 0.5f); // convert pt to pixels
                int ypos = height - offset_y;
                p.setTextAlign(Align.RIGHT);
                MyApplicationInterface.Shadow draw_shadowed = MyApplicationInterface.Shadow.SHADOW_NONE;
                switch (pref_style) {
                    case "preference_stamp_style_shadowed":
                        draw_shadowed = MyApplicationInterface.Shadow.SHADOW_OUTLINE;
                        break;
                    case "preference_stamp_style_plain":
                        draw_shadowed = MyApplicationInterface.Shadow.SHADOW_NONE;
                        break;
                    case "preference_stamp_style_background":
                        draw_shadowed = MyApplicationInterface.Shadow.SHADOW_BACKGROUND;
                        break;
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "draw_shadowed: " + draw_shadowed);
                if (dategeo_stamp) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "stamp date");
                    // doesn't respect user preferences such as 12/24 hour - see note about in draw() about DateFormat.getTimeInstance()
                    String date_stamp = TextFormatter.getDateString(preference_stamp_dateformat, request.current_date);
                    String time_stamp = TextFormatter.getTimeString(preference_stamp_timeformat, request.current_date);
                    if (MyDebug.LOG) {
                        Log.d(TAG, "date_stamp: " + date_stamp);
                        Log.d(TAG, "time_stamp: " + time_stamp);
                    }
                    if (date_stamp.length() > 0 || time_stamp.length() > 0) {
                        String datetime_stamp = "";
                        if (date_stamp.length() > 0)
                            datetime_stamp += date_stamp;
                        if (time_stamp.length() > 0) {
                            if (datetime_stamp.length() > 0)
                                datetime_stamp += " ";
                            datetime_stamp += time_stamp;
                        }
                        applicationInterface.drawTextWithBackground(canvas, p, datetime_stamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                    }
                    ypos -= diff_y;
                    String gps_stamp = main_activity.getTextFormatter().getGPSString(preference_stamp_gpsformat, request.preference_units_distance, request.store_location, request.location, request.store_geo_direction, request.geo_direction);
                    if (gps_stamp.length() > 0) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "stamp with location_string: " + gps_stamp);

                        Address address = null;
                        if (request.store_location && !request.preference_stamp_geo_address.equals("preference_stamp_geo_address_no")) {
                            if (Geocoder.isPresent()) {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "geocoder is present");
                                Geocoder geocoder = new Geocoder(main_activity, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(request.location.getLatitude(), request.location.getLongitude(), 1);
                                    if (addresses != null && addresses.size() > 0) {
                                        address = addresses.get(0);
                                        if (MyDebug.LOG) {
                                            Log.d(TAG, "address: " + address);
                                            Log.d(TAG, "max line index: " + address.getMaxAddressLineIndex());
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "failed to read from geocoder");
                                    e.printStackTrace();
                                }
                            } else {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "geocoder not present");
                            }
                        }

                        if (address == null || request.preference_stamp_geo_address.equals("preference_stamp_geo_address_both")) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "display gps coords");
                            // want GPS coords (either in addition to the address, or we don't have an address)
                            // we'll also enter here if store_location is false, but we have geo direction to display
                            applicationInterface.drawTextWithBackground(canvas, p, gps_stamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                            ypos -= diff_y;
                        } else if (request.store_geo_direction) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "not displaying gps coords, but need to display geo direction");
                            // we are displaying an address instead of GPS coords, but we still need to display the geo direction
                            gps_stamp = main_activity.getTextFormatter().getGPSString(preference_stamp_gpsformat, request.preference_units_distance, false, null, request.store_geo_direction, request.geo_direction);
                            if (gps_stamp.length() > 0) {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "gps_stamp is now: " + gps_stamp);
                                applicationInterface.drawTextWithBackground(canvas, p, gps_stamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                                ypos -= diff_y;
                            }
                        }

                        if (address != null) {
                            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                // write in reverse order
                                String addressLine = address.getAddressLine(address.getMaxAddressLineIndex() - i);
                                applicationInterface.drawTextWithBackground(canvas, p, addressLine, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                                ypos -= diff_y;
                            }
                        }
                    }
                }
                if (text_stamp) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "stamp text");
                    applicationInterface.drawTextWithBackground(canvas, p, request.preference_textstamp, color, Color.BLACK, width - offset_x, ypos, MyApplicationInterface.Alignment.ALIGNMENT_BOTTOM, null, draw_shadowed);
                    ypos -= diff_y;
                }
            }
        }
        return bitmap;
    }

    private static class PostProcessBitmapResult {
        final Bitmap bitmap;
        final File exifTempFile;

        PostProcessBitmapResult(Bitmap bitmap, File exifTempFile) {
            this.bitmap = bitmap;
            this.exifTempFile = exifTempFile;
        }
    }

    private File getExifTempFile(byte[] data) {
        File exifTempFile = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            try {
                if (MyDebug.LOG)
                    Log.d(TAG, "write temp file to record EXIF data");
                exifTempFile = File.createTempFile("opencamera_exif", "");
                OutputStream tempOutputStream = new FileOutputStream(exifTempFile);
                try {
                    tempOutputStream.write(data);
                } finally {
                    tempOutputStream.close();
                }
            } catch (IOException e) {
                if (MyDebug.LOG)
                    Log.e(TAG, "exception writing to temp file");
                e.printStackTrace();
            }
        }
        return exifTempFile;
    }

    private PostProcessBitmapResult postProcessBitmap(final Request request, byte[] data, Bitmap bitmap, boolean ignore_exif_orientation) throws IOException {
        if (MyDebug.LOG)
            Log.d(TAG, "postProcessBitmap");
        long time_s = System.currentTimeMillis();

        boolean dategeo_stamp = request.preference_stamp.equals("preference_stamp_yes");
        boolean text_stamp = request.preference_textstamp.length() > 0;
        File exifTempFile = null;
        if (bitmap != null || request.image_format != Request.ImageFormat.STD || request.do_auto_stabilise || request.mirror || dategeo_stamp || text_stamp) {
            if (!ignore_exif_orientation) {
                exifTempFile = getExifTempFile(data);
                if (MyDebug.LOG) {
                    Log.d(TAG, "Save single image performance: time after saving temp photo for EXIF: " + (System.currentTimeMillis() - time_s));
                }

                if (bitmap != null) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "rotate pre-existing bitmap for exif tags?");
                    bitmap = rotateForExif(bitmap, data, exifTempFile);
                }
            }
        }
        if (request.do_auto_stabilise) {
            bitmap = autoStabilise(data, bitmap, request.level_angle, request.is_front_facing, exifTempFile);
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "Save single image performance: time after auto-stabilise: " + (System.currentTimeMillis() - time_s));
        }
        if (request.mirror) {
            bitmap = mirrorImage(data, bitmap, exifTempFile);
        }
        if (request.image_format != Request.ImageFormat.STD && bitmap == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "need to decode bitmap to convert file format");
            bitmap = loadBitmapWithRotation(data, true, exifTempFile);
            if (bitmap == null) {
                // if we can't load bitmap for converting file formats, don't want to continue
                System.gc();
                if (exifTempFile != null && !exifTempFile.delete()) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "failed to delete temp " + exifTempFile.getAbsolutePath());
                }
                throw new IOException();
            }
        }
        bitmap = stampImage(request, data, bitmap, exifTempFile);
        if (MyDebug.LOG) {
            Log.d(TAG, "Save single image performance: time after photostamp: " + (System.currentTimeMillis() - time_s));
        }
        return new PostProcessBitmapResult(bitmap, exifTempFile);
    }

   /* @SuppressLint("SimpleDateFormat")
    private boolean saveSingleImageNow(final Request request, byte [] data, Bitmap bitmap, String filename_suffix, boolean update_thumbnail, boolean share_image, boolean ignore_raw_only, boolean ignore_exif_orientation) {
        SingleTon.getInstance().setCapturedimage(data);
        return true;
    }*/

    @SuppressLint("SimpleDateFormat")
    private boolean saveSingleImageNow(final Request request, byte[] data, Bitmap bitmap, String filename_suffix, boolean update_thumbnail, boolean share_image, boolean ignore_raw_only, boolean ignore_exif_orientation) {
        if (MyDebug.LOG)
            Log.d(TAG, "saveSingleImageNow");

        if (request.type != Request.Type.JPEG) {
            if (MyDebug.LOG)
                Log.d(TAG, "saveImageNow called with non-jpeg request");
            // throw runtime exception, as this is a programming error
            throw new RuntimeException();
        } else if (data == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "saveSingleImageNow called with no data");
            // throw runtime exception, as this is a programming error
            throw new RuntimeException();
        }
        long time_s = System.currentTimeMillis();

        boolean success = false;
        final MyApplicationInterface applicationInterface = main_activity.getApplicationInterface();
        boolean raw_only = !ignore_raw_only && applicationInterface.isRawOnly();
        if (MyDebug.LOG)
            Log.d(TAG, "raw_only: " + raw_only);
        StorageUtils storageUtils = main_activity.getStorageUtils();
        //  StorageUtils storageUtils = main_activity.getStorageUtils();

        String extension;
        switch (request.image_format) {
            case WEBP:
                extension = "webp";
                break;
            case PNG:
                extension = "png";
                break;
            default:
                extension = "jpg";
                break;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "extension: " + extension);

        main_activity.savingImage(true);

        File exifTempFile = null;
        File picFile = null;
        Uri saveUri = null;
        try {
            if (!raw_only) {
                PostProcessBitmapResult postProcessBitmapResult = postProcessBitmap(request, data, bitmap, ignore_exif_orientation);
                bitmap = postProcessBitmapResult.bitmap;
                exifTempFile = postProcessBitmapResult.exifTempFile;
            }

            if (raw_only) {
                // don't save the JPEG
                success = true;
            } else if (request.image_capture_intent) {
                if (MyDebug.LOG)
                    Log.d(TAG, "image_capture_intent");
                if (request.image_capture_intent_uri != null) {
                    // Save the bitmap to the specified URI (use a try/catch block)
                    if (MyDebug.LOG)
                        Log.d(TAG, "save to: " + request.image_capture_intent_uri);
                    saveUri = request.image_capture_intent_uri;
                } else {
                    // If the intent doesn't contain an URI, send the bitmap as a parcel
                    // (it is a good idea to reduce its size to ~50k pixels before)
                    if (MyDebug.LOG)
                        Log.d(TAG, "sent to intent via parcel");
                    if (bitmap == null) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "create bitmap");
                        // bitmap we return doesn't need to be mutable
                        bitmap = loadBitmapWithRotation(data, false, exifTempFile);
                    }
                    if (bitmap != null) {
                        int width = bitmap.getWidth();
                        int height = bitmap.getHeight();
                        if (MyDebug.LOG) {
                            Log.d(TAG, "decoded bitmap size " + width + ", " + height);
                            Log.d(TAG, "bitmap size: " + width * height * 4);
                        }
                        final int small_size_c = 128;
                        if (width > small_size_c) {
                            float scale = ((float) small_size_c) / (float) width;
                            if (MyDebug.LOG)
                                Log.d(TAG, "scale to " + scale);
                            Matrix matrix = new Matrix();
                            matrix.postScale(scale, scale);
                            Bitmap new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                            // careful, as new_bitmap is sometimes not a copy!
                            if (new_bitmap != bitmap) {
                                bitmap.recycle();
                                bitmap = new_bitmap;
                            }
                        }
                    }
                    if (MyDebug.LOG) {
                        if (bitmap != null) {
                            Log.d(TAG, "returned bitmap size " + bitmap.getWidth() + ", " + bitmap.getHeight());
                            Log.d(TAG, "returned bitmap size: " + bitmap.getWidth() * bitmap.getHeight() * 4);
                        } else {
                            Log.e(TAG, "no bitmap created");
                        }
                    }
                    if (bitmap != null)
                        main_activity.setResult(Activity.RESULT_OK, new Intent("inline-data").putExtra("data", bitmap));
                    if (exifTempFile != null && !exifTempFile.delete()) {
                        if (MyDebug.LOG)
                            Log.e(TAG, "failed to delete temp " + exifTempFile.getAbsolutePath());
                    }
                    exifTempFile = null;
                    main_activity.finish();
                }
            } else if (storageUtils.isUsingSAF()) {
                saveUri = storageUtils.createOutputMediaFileSAF(StorageUtils.MEDIA_TYPE_IMAGE, filename_suffix, extension, request.current_date);
            } else {
                picFile = storageUtils.createOutputMediaFile(StorageUtils.MEDIA_TYPE_IMAGE, filename_suffix, extension, request.current_date);
                if (MyDebug.LOG)
                    Log.d(TAG, "save to: " + picFile.getAbsolutePath());
            }
            if (MyDebug.LOG)
                Log.d(TAG, "saveUri: " + saveUri);
            if (saveUri != null && picFile == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                picFile = File.createTempFile("picFile", "jpg", main_activity.getCacheDir());
                if (MyDebug.LOG)
                    Log.d(TAG, "temp picFile: " + picFile.getAbsolutePath());
            }

            if (picFile != null || saveUri != null) {
                OutputStream outputStream;
                if (picFile != null)
                    outputStream = new FileOutputStream(picFile);
                else
                    outputStream = main_activity.getContentResolver().openOutputStream(saveUri);
                try {
                    if (bitmap != null) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "compress bitmap, quality " + request.image_quality);
                        Bitmap.CompressFormat compress_format;
                        switch (request.image_format) {
                            case WEBP:
                                compress_format = Bitmap.CompressFormat.WEBP;
                                break;
                            case PNG:
                                compress_format = Bitmap.CompressFormat.PNG;
                                break;
                            default:
                                compress_format = Bitmap.CompressFormat.JPEG;
                                break;
                        }
                        bitmap.compress(compress_format, request.image_quality, outputStream);
                    } else {
                        outputStream.write(data);
                    }
                } finally {
                    outputStream.close();
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "saveImageNow saved photo");
                if (MyDebug.LOG) {
                    Log.d(TAG, "Save single image performance: time after saving photo: " + (System.currentTimeMillis() - time_s));
                }

                if (saveUri == null) { // if saveUri is non-null, then we haven't succeeded until we've copied to the saveUri
                    success = true;
                }

                if (request.image_format == Request.ImageFormat.STD) {
                    // handle transferring/setting Exif tags (JPEG format only)
                    if (bitmap != null) {
                        // need to update EXIF data! (only supported for JPEG image formats)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "set Exif tags from data");
                            if (picFile != null) {
                                setExifFromData(request, data, picFile);
                            } else {
                                ParcelFileDescriptor parcelFileDescriptor = main_activity.getContentResolver().openFileDescriptor(saveUri, "rw");
                                if (parcelFileDescriptor != null) {
                                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                                    setExifFromData(request, data, fileDescriptor);
                                } else {
                                    Log.e(TAG, "failed to create ParcelFileDescriptor for saveUri: " + saveUri);
                                }
                            }
                        } else {
                            if (MyDebug.LOG)
                                Log.d(TAG, "set Exif tags from file");
                            if (picFile == null) {
                                throw new RuntimeException("should have set picFile on pre-Android 7!");
                            }
                            if (exifTempFile != null) {
                                setExifFromFile(request, exifTempFile, picFile);
                                if (MyDebug.LOG) {
                                    Log.d(TAG, "Save single image performance: time after copying EXIF: " + (System.currentTimeMillis() - time_s));
                                }
                            } else {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "can't set Exif tags without file pre-Android 7");
                            }
                        }
                    } else {
                        updateExif(request, picFile, saveUri);
                        if (MyDebug.LOG) {
                            Log.d(TAG, "Save single image performance: time after updateExif: " + (System.currentTimeMillis() - time_s));
                        }
                    }
                }

                if (picFile != null && saveUri == null) {
                    // broadcast for SAF is done later, when we've actually written out the file
                    storageUtils.broadcastFile(picFile, true, false, update_thumbnail);
                    main_activity.test_last_saved_image = picFile.getAbsolutePath();
                }

                if (request.image_capture_intent) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "finish activity due to being called from intent");
                    main_activity.setResult(Activity.RESULT_OK);
                    main_activity.finish();
                }
                if (storageUtils.isUsingSAF()) {
                    // most Gallery apps don't seem to recognise the SAF-format Uri, so just clear the field
                    storageUtils.clearLastMediaScanned();
                }

                if (saveUri != null) {
                    if (picFile != null) {
                        copyFileToUri(main_activity, saveUri, picFile);
                    }
                    success = true;
                    broadcastSAFFile(saveUri, request.image_capture_intent);
                }
            }
        } catch (FileNotFoundException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "File not found: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "I/O error writing file: " + e.getMessage());
            e.printStackTrace();
        } catch (SecurityException e) {
            // received security exception from copyFileToUri()->openOutputStream() from Google Play
            if (MyDebug.LOG)
                Log.e(TAG, "security exception writing file: " + e.getMessage());
            e.printStackTrace();
        }

        if (exifTempFile != null && !exifTempFile.delete()) {
            if (MyDebug.LOG)
                Log.e(TAG, "failed to delete temp " + exifTempFile.getAbsolutePath());
        }

        if (raw_only) {
            // no saved image to record
        } else if (success && saveUri == null) {
            applicationInterface.addLastImage(picFile, share_image);
        } else if (success && storageUtils.isUsingSAF()) {
            applicationInterface.addLastImageSAF(saveUri, share_image);
        }

        // I have received crashes where camera_controller was null - could perhaps happen if this thread was running just as the camera is closing?
        if (success && main_activity.getPreview().getCameraController() != null && update_thumbnail) {
            // update thumbnail - this should be done after restarting preview, so that the preview is started asap
            CameraController.Size size = main_activity.getPreview().getCameraController().getPictureSize();
            int ratio = (int) Math.ceil((double) size.width / main_activity.getPreview().getView().getWidth());
            int sample_size = Integer.highestOneBit(ratio);
            sample_size *= request.sample_factor;
            if (MyDebug.LOG) {
                Log.d(TAG, "    picture width: " + size.width);
                Log.d(TAG, "    preview width: " + main_activity.getPreview().getView().getWidth());
                Log.d(TAG, "    ratio        : " + ratio);
                Log.d(TAG, "    sample_size  : " + sample_size);
            }
            Bitmap thumbnail;
            if (bitmap == null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = false;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    // setting is ignored in Android 5 onwards
                    options.inPurgeable = true;
                }
                options.inSampleSize = sample_size;
                thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (MyDebug.LOG) {
                    Log.d(TAG, "thumbnail width: " + thumbnail.getWidth());
                    Log.d(TAG, "thumbnail height: " + thumbnail.getHeight());
                }
                // now get the rotation from the Exif data
                if (MyDebug.LOG)
                    Log.d(TAG, "rotate thumbnail for exif tags?");
                thumbnail = rotateForExif(thumbnail, data, picFile);
            } else {
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                Matrix matrix = new Matrix();
                float scale = 1.0f / (float) sample_size;
                matrix.postScale(scale, scale);
                if (MyDebug.LOG)
                    Log.d(TAG, "    scale: " + scale);
                try {
                    thumbnail = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                    if (MyDebug.LOG) {
                        Log.d(TAG, "thumbnail width: " + thumbnail.getWidth());
                        Log.d(TAG, "thumbnail height: " + thumbnail.getHeight());
                    }
                    // don't need to rotate for exif, as we already did that when creating the bitmap
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "can't create thumbnail bitmap due to IllegalArgumentException?!");
                    e.printStackTrace();
                    thumbnail = null;
                }
            }
            if (thumbnail == null) {
                // received crashes on Google Play suggesting that thumbnail could not be created
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to create thumbnail bitmap");
            } else {
                final Bitmap thumbnail_f = thumbnail;
                main_activity.runOnUiThread(new Runnable() {
                    public void run() {
                        applicationInterface.updateThumbnail(thumbnail_f, false);
                    }
                });
                if (MyDebug.LOG) {
                    Log.d(TAG, "Save single image performance: time after creating thumbnail: " + (System.currentTimeMillis() - time_s));
                }
            }
        }

        if (bitmap != null) {
            bitmap.recycle();
        }

        if (picFile != null && saveUri != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "delete temp picFile: " + picFile);
            if (!picFile.delete()) {
                if (MyDebug.LOG)
                    Log.e(TAG, "failed to delete temp picFile: " + picFile);
            }
        }

        System.gc();

        main_activity.savingImage(false);

        if (MyDebug.LOG) {
            Log.d(TAG, "Save single image performance: total time: " + (System.currentTimeMillis() - time_s));
        }
        return success;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setExifFromData(final Request request, byte[] data, File to_file) throws IOException {
        if (MyDebug.LOG) {
            Log.d(TAG, "setExifFromData");
            Log.d(TAG, "to_file: " + to_file);
        }
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(data);
            ExifInterface exif = new ExifInterface(inputStream);
            ExifInterface exif_new = new ExifInterface(to_file.getAbsolutePath());
            setExif(request, exif, exif_new);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void broadcastSAFFile(Uri saveUri, boolean image_capture_intent) {
        if (MyDebug.LOG)
            Log.d(TAG, "broadcastSAFFile");
        StorageUtils storageUtils = main_activity.getStorageUtils();
        File real_file = storageUtils.getFileFromDocumentUriSAF(saveUri, false);
        if (MyDebug.LOG)
            Log.d(TAG, "real_file: " + real_file);
        if (real_file != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "broadcast file");
            storageUtils.broadcastFile(real_file, true, false, true);
            main_activity.test_last_saved_image = real_file.getAbsolutePath();
        } else if (!image_capture_intent) {
            if (MyDebug.LOG)
                Log.d(TAG, "announce SAF uri");
            storageUtils.announceUri(saveUri, true, false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setExifFromData(final Request request, byte[] data, FileDescriptor to_file_descriptor) throws IOException {
        if (MyDebug.LOG) {
            Log.d(TAG, "setExifFromData");
            Log.d(TAG, "to_file_descriptor: " + to_file_descriptor);
        }
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(data);
            ExifInterface exif = new ExifInterface(inputStream);
            ExifInterface exif_new = new ExifInterface(to_file_descriptor);
            setExif(request, exif, exif_new);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void setExifFromFile(final Request request, File from_file, File to_file) throws IOException {
        if (MyDebug.LOG) {
            Log.d(TAG, "setExifFromFile");
            Log.d(TAG, "from_file: " + from_file);
            Log.d(TAG, "to_file: " + to_file);
        }
        try {
            ExifInterface exif = new ExifInterface(from_file.getAbsolutePath());
            ExifInterface exif_new = new ExifInterface(to_file.getAbsolutePath());
            setExif(request, exif, exif_new);
        } catch (NoClassDefFoundError exception) {
            // have had Google Play crashes from new ExifInterface() for Galaxy Ace4 (vivalto3g)
            if (MyDebug.LOG)
                Log.e(TAG, "exif orientation NoClassDefFoundError");
            exception.printStackTrace();
        }
    }

    private void setExif(final Request request, ExifInterface exif, ExifInterface exif_new) throws IOException {
        if (MyDebug.LOG)
            Log.d(TAG, "setExif");

        if (MyDebug.LOG)
            Log.d(TAG, "read back EXIF data");
        String exif_aperture = exif.getAttribute(ExifInterface.TAG_APERTURE); // same as TAG_F_NUMBER
        String exif_datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
        String exif_exposure_time = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
        String exif_flash = exif.getAttribute(ExifInterface.TAG_FLASH);
        String exif_focal_length = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
        String exif_gps_altitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
        String exif_gps_altitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF);
        String exif_gps_datestamp = exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
        String exif_gps_latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String exif_gps_latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String exif_gps_longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String exif_gps_longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        String exif_gps_processing_method = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);
        String exif_gps_timestamp = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
        String exif_iso = exif.getAttribute(ExifInterface.TAG_ISO); // same as TAG_ISO_SPEED_RATINGS
        String exif_make = exif.getAttribute(ExifInterface.TAG_MAKE);
        String exif_model = exif.getAttribute(ExifInterface.TAG_MODEL);
        String exif_white_balance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE);

        String exif_datetime_digitized = null;
        String exif_subsec_time = null;
        String exif_subsec_time_dig = null;
        String exif_subsec_time_orig = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            exif_datetime_digitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
            exif_subsec_time = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME);
            exif_subsec_time_dig = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_DIG); // same as TAG_SUBSEC_TIME_DIGITIZED
            exif_subsec_time_orig = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIG); // same as TAG_SUBSEC_TIME_ORIGINAL
        }

        String exif_aperture_value = null;
        String exif_brightness_value = null;
        String exif_cfa_pattern = null;
        String exif_color_space = null;
        String exif_components_configuration = null;
        String exif_compressed_bits_per_pixel = null;
        String exif_compression = null;
        String exif_contrast = null;
        String exif_datetime_original = null;
        String exif_device_setting_description = null;
        String exif_digital_zoom_ratio = null;
        String exif_exposure_bias_value = null;
        String exif_exposure_index = null;
        String exif_exposure_mode = null;
        String exif_exposure_program = null;
        String exif_flash_energy = null;
        String exif_focal_length_in_35mm_film = null;
        String exif_focal_plane_resolution_unit = null;
        String exif_focal_plane_x_resolution = null;
        String exif_focal_plane_y_resolution = null;
        String exif_gain_control = null;
        String exif_gps_area_information = null;
        String exif_gps_differential = null;
        String exif_gps_dop = null;
        String exif_gps_measure_mode = null;
        String exif_image_description = null;
        String exif_light_source = null;
        String exif_maker_note = null;
        String exif_max_aperture_value = null;
        String exif_metering_mode = null;
        String exif_oecf = null;
        String exif_photometric_interpretation = null;
        String exif_saturation = null;
        String exif_scene_capture_type = null;
        String exif_scene_type = null;
        String exif_sensing_method = null;
        String exif_sharpness = null;
        String exif_shutter_speed_value = null;
        String exif_software = null;
        String exif_user_comment = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            exif_aperture_value = exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE);
            exif_brightness_value = exif.getAttribute(ExifInterface.TAG_BRIGHTNESS_VALUE);
            exif_cfa_pattern = exif.getAttribute(ExifInterface.TAG_CFA_PATTERN);
            exif_color_space = exif.getAttribute(ExifInterface.TAG_COLOR_SPACE);
            exif_components_configuration = exif.getAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION);
            exif_compressed_bits_per_pixel = exif.getAttribute(ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL);
            exif_compression = exif.getAttribute(ExifInterface.TAG_COMPRESSION);
            exif_contrast = exif.getAttribute(ExifInterface.TAG_CONTRAST);
            exif_datetime_original = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            exif_device_setting_description = exif.getAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION);
            exif_digital_zoom_ratio = exif.getAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO);
            exif_exposure_bias_value = exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
            exif_exposure_index = exif.getAttribute(ExifInterface.TAG_EXPOSURE_INDEX);
            exif_exposure_mode = exif.getAttribute(ExifInterface.TAG_EXPOSURE_MODE);
            exif_exposure_program = exif.getAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM);
            exif_flash_energy = exif.getAttribute(ExifInterface.TAG_FLASH_ENERGY);
            exif_focal_length_in_35mm_film = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM);
            exif_focal_plane_resolution_unit = exif.getAttribute(ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT);
            exif_focal_plane_x_resolution = exif.getAttribute(ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION);
            exif_focal_plane_y_resolution = exif.getAttribute(ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION);
            exif_gain_control = exif.getAttribute(ExifInterface.TAG_GAIN_CONTROL);
            exif_gps_area_information = exif.getAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION);
            // don't care about TAG_GPS_DEST_*
            exif_gps_differential = exif.getAttribute(ExifInterface.TAG_GPS_DIFFERENTIAL);
            exif_gps_dop = exif.getAttribute(ExifInterface.TAG_GPS_DOP);
            exif_gps_measure_mode = exif.getAttribute(ExifInterface.TAG_GPS_MEASURE_MODE);
            exif_image_description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION);
            exif_light_source = exif.getAttribute(ExifInterface.TAG_LIGHT_SOURCE);
            exif_maker_note = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE);
            exif_max_aperture_value = exif.getAttribute(ExifInterface.TAG_MAX_APERTURE_VALUE);
            exif_metering_mode = exif.getAttribute(ExifInterface.TAG_METERING_MODE);
            exif_oecf = exif.getAttribute(ExifInterface.TAG_OECF);
            exif_photometric_interpretation = exif.getAttribute(ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION);
            exif_saturation = exif.getAttribute(ExifInterface.TAG_SATURATION);
            exif_scene_capture_type = exif.getAttribute(ExifInterface.TAG_SCENE_CAPTURE_TYPE);
            exif_scene_type = exif.getAttribute(ExifInterface.TAG_SCENE_TYPE);
            exif_sensing_method = exif.getAttribute(ExifInterface.TAG_SENSING_METHOD);
            exif_sharpness = exif.getAttribute(ExifInterface.TAG_SHARPNESS);
            exif_shutter_speed_value = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE);
            exif_software = exif.getAttribute(ExifInterface.TAG_SOFTWARE);
            exif_user_comment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT);
        }

        if (MyDebug.LOG)
            Log.d(TAG, "now write new EXIF data");
        if (exif_aperture != null)
            exif_new.setAttribute(ExifInterface.TAG_APERTURE, exif_aperture);
        if (exif_datetime != null)
            exif_new.setAttribute(ExifInterface.TAG_DATETIME, exif_datetime);
        if (exif_exposure_time != null)
            exif_new.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, exif_exposure_time);
        if (exif_flash != null)
            exif_new.setAttribute(ExifInterface.TAG_FLASH, exif_flash);
        if (exif_focal_length != null)
            exif_new.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exif_focal_length);
        if (exif_gps_altitude != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exif_gps_altitude);
        if (exif_gps_altitude_ref != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exif_gps_altitude_ref);
        if (exif_gps_datestamp != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exif_gps_datestamp);
        if (exif_gps_latitude != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exif_gps_latitude);
        if (exif_gps_latitude_ref != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exif_gps_latitude_ref);
        if (exif_gps_longitude != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exif_gps_longitude);
        if (exif_gps_longitude_ref != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exif_gps_longitude_ref);
        if (exif_gps_processing_method != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exif_gps_processing_method);
        if (exif_gps_timestamp != null)
            exif_new.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, exif_gps_timestamp);
        if (exif_iso != null)
            exif_new.setAttribute(ExifInterface.TAG_ISO, exif_iso);
        if (exif_make != null)
            exif_new.setAttribute(ExifInterface.TAG_MAKE, exif_make);
        if (exif_model != null)
            exif_new.setAttribute(ExifInterface.TAG_MODEL, exif_model);
        if (exif_white_balance != null)
            exif_new.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exif_white_balance);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (exif_datetime_digitized != null)
                exif_new.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, exif_datetime_digitized);
            if (exif_subsec_time != null)
                exif_new.setAttribute(ExifInterface.TAG_SUBSEC_TIME, exif_subsec_time);
            if (exif_subsec_time_dig != null)
                exif_new.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIG, exif_subsec_time_dig);
            if (exif_subsec_time_orig != null)
                exif_new.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIG, exif_subsec_time_orig);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (exif_aperture_value != null)
                exif_new.setAttribute(ExifInterface.TAG_APERTURE_VALUE, exif_aperture_value);
            if (exif_brightness_value != null)
                exif_new.setAttribute(ExifInterface.TAG_BRIGHTNESS_VALUE, exif_brightness_value);
            if (exif_cfa_pattern != null)
                exif_new.setAttribute(ExifInterface.TAG_CFA_PATTERN, exif_cfa_pattern);
            if (exif_color_space != null)
                exif_new.setAttribute(ExifInterface.TAG_COLOR_SPACE, exif_color_space);
            if (exif_components_configuration != null)
                exif_new.setAttribute(ExifInterface.TAG_COMPONENTS_CONFIGURATION, exif_components_configuration);
            if (exif_compressed_bits_per_pixel != null)
                exif_new.setAttribute(ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL, exif_compressed_bits_per_pixel);
            if (exif_compression != null)
                exif_new.setAttribute(ExifInterface.TAG_COMPRESSION, exif_compression);
            if (exif_contrast != null)
                exif_new.setAttribute(ExifInterface.TAG_CONTRAST, exif_contrast);
            if (exif_datetime_original != null)
                exif_new.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exif_datetime_original);
            if (exif_device_setting_description != null)
                exif_new.setAttribute(ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION, exif_device_setting_description);
            if (exif_digital_zoom_ratio != null)
                exif_new.setAttribute(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, exif_digital_zoom_ratio);
            if (exif_exposure_bias_value != null)
                exif_new.setAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE, exif_exposure_bias_value);
            if (exif_exposure_index != null)
                exif_new.setAttribute(ExifInterface.TAG_EXPOSURE_INDEX, exif_exposure_index);
            if (exif_exposure_mode != null)
                exif_new.setAttribute(ExifInterface.TAG_EXPOSURE_MODE, exif_exposure_mode);
            if (exif_exposure_program != null)
                exif_new.setAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM, exif_exposure_program);
            if (exif_flash_energy != null)
                exif_new.setAttribute(ExifInterface.TAG_FLASH_ENERGY, exif_flash_energy);
            if (exif_focal_length_in_35mm_film != null)
                exif_new.setAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, exif_focal_length_in_35mm_film);
            if (exif_focal_plane_resolution_unit != null)
                exif_new.setAttribute(ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT, exif_focal_plane_resolution_unit);
            if (exif_focal_plane_x_resolution != null)
                exif_new.setAttribute(ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION, exif_focal_plane_x_resolution);
            if (exif_focal_plane_y_resolution != null)
                exif_new.setAttribute(ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION, exif_focal_plane_y_resolution);
            if (exif_gain_control != null)
                exif_new.setAttribute(ExifInterface.TAG_GAIN_CONTROL, exif_gain_control);
            if (exif_gps_area_information != null)
                exif_new.setAttribute(ExifInterface.TAG_GPS_AREA_INFORMATION, exif_gps_area_information);
            if (exif_gps_differential != null)
                exif_new.setAttribute(ExifInterface.TAG_GPS_DIFFERENTIAL, exif_gps_differential);
            if (exif_gps_dop != null)
                exif_new.setAttribute(ExifInterface.TAG_GPS_DOP, exif_gps_dop);
            if (exif_gps_measure_mode != null)
                exif_new.setAttribute(ExifInterface.TAG_GPS_MEASURE_MODE, exif_gps_measure_mode);
            if (exif_image_description != null)
                exif_new.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, exif_image_description);
            if (exif_light_source != null)
                exif_new.setAttribute(ExifInterface.TAG_LIGHT_SOURCE, exif_light_source);
            if (exif_maker_note != null)
                exif_new.setAttribute(ExifInterface.TAG_MAKER_NOTE, exif_maker_note);
            if (exif_max_aperture_value != null)
                exif_new.setAttribute(ExifInterface.TAG_MAX_APERTURE_VALUE, exif_max_aperture_value);
            if (exif_metering_mode != null)
                exif_new.setAttribute(ExifInterface.TAG_METERING_MODE, exif_metering_mode);
            if (exif_oecf != null)
                exif_new.setAttribute(ExifInterface.TAG_OECF, exif_oecf);
            if (exif_photometric_interpretation != null)
                exif_new.setAttribute(ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION, exif_photometric_interpretation);
            if (exif_saturation != null)
                exif_new.setAttribute(ExifInterface.TAG_SATURATION, exif_saturation);
            if (exif_scene_capture_type != null)
                exif_new.setAttribute(ExifInterface.TAG_SCENE_CAPTURE_TYPE, exif_scene_capture_type);
            if (exif_scene_type != null)
                exif_new.setAttribute(ExifInterface.TAG_SCENE_TYPE, exif_scene_type);
            if (exif_sensing_method != null)
                exif_new.setAttribute(ExifInterface.TAG_SENSING_METHOD, exif_sensing_method);
            if (exif_sharpness != null)
                exif_new.setAttribute(ExifInterface.TAG_SHARPNESS, exif_sharpness);
            if (exif_shutter_speed_value != null)
                exif_new.setAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE, exif_shutter_speed_value);
            if (exif_software != null)
                exif_new.setAttribute(ExifInterface.TAG_SOFTWARE, exif_software);
            if (exif_user_comment != null)
                exif_new.setAttribute(ExifInterface.TAG_USER_COMMENT, exif_user_comment);
        }

        modifyExif(exif_new, request.type == Request.Type.JPEG, request.using_camera2, request.current_date, request.store_location, request.store_geo_direction, request.geo_direction, request.custom_tag_artist, request.custom_tag_copyright);
        setDateTimeExif(exif_new);
        exif_new.saveAttributes();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean saveImageNowRaw(Request request) {
        if (MyDebug.LOG)
            Log.d(TAG, "saveImageNowRaw");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (MyDebug.LOG)
                Log.e(TAG, "RAW requires LOLLIPOP or higher");
            return false;
        }
        StorageUtils storageUtils = main_activity.getStorageUtils();
        boolean success = false;

        main_activity.savingImage(true);

        OutputStream output = null;
        RawImage raw_image = request.raw_image;
        try {
            File picFile = null;
            Uri saveUri = null;

            String suffix = "_";
            String filename_suffix = (request.force_suffix) ? suffix + (request.suffix_offset) : "";
            if (storageUtils.isUsingSAF()) {
                saveUri = storageUtils.createOutputMediaFileSAF(StorageUtils.MEDIA_TYPE_IMAGE, filename_suffix, "dng", request.current_date);
                if (MyDebug.LOG)
                    Log.d(TAG, "saveUri: " + saveUri);
            } else {
                picFile = storageUtils.createOutputMediaFile(StorageUtils.MEDIA_TYPE_IMAGE, filename_suffix, "dng", request.current_date);
                if (MyDebug.LOG)
                    Log.d(TAG, "save to: " + picFile.getAbsolutePath());
            }

            if (picFile != null) {
                output = new FileOutputStream(picFile);
            } else {
                output = main_activity.getContentResolver().openOutputStream(saveUri);
            }
            raw_image.writeImage(output);
            raw_image.close();
            raw_image = null;
            output.close();
            output = null;
            success = true;

            MyApplicationInterface applicationInterface = main_activity.getApplicationInterface();
            boolean raw_only = applicationInterface.isRawOnly();
            if (MyDebug.LOG)
                Log.d(TAG, "raw_only: " + raw_only);
            if (saveUri == null) {
                applicationInterface.addLastImage(picFile, raw_only);
            } else if (storageUtils.isUsingSAF()) {
                applicationInterface.addLastImageSAF(saveUri, raw_only);
            }

            if (saveUri == null) {
                storageUtils.broadcastFile(picFile, true, false, false);
            } else {
                storageUtils.broadcastUri(saveUri, true, false, false);
            }
        } catch (FileNotFoundException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "File not found: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            if (MyDebug.LOG)
                Log.e(TAG, "ioexception writing raw image file");
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "ioexception closing raw output");
                    e.printStackTrace();
                }
            }
            if (raw_image != null) {
                raw_image.close();
            }
        }

        System.gc();

        main_activity.savingImage(false);

        return success;
    }

    private Bitmap rotateForExif(Bitmap bitmap, byte[] data, File exifTempFile) {
        if (MyDebug.LOG)
            Log.d(TAG, "rotateForExif");
        InputStream inputStream = null;
        try {
            ExifInterface exif;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (MyDebug.LOG)
                    Log.d(TAG, "Android 7: use data stream to read exif tags");
                inputStream = new ByteArrayInputStream(data);
                exif = new ExifInterface(inputStream);
            } else {
                if (MyDebug.LOG)
                    Log.d(TAG, "pre-Android 7: use file to read exif tags: " + exifTempFile);
                if (exifTempFile != null) {
                    exif = new ExifInterface(exifTempFile.getAbsolutePath());
                } else {
                    if (MyDebug.LOG)
                        Log.d(TAG, "but no file available to read exif tags from");
                    return bitmap;
                }
            }

            int exif_orientation_s = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            if (MyDebug.LOG)
                Log.d(TAG, "    exif orientation string: " + exif_orientation_s);
            boolean needs_tf = false;
            int exif_orientation = 0;
            switch (exif_orientation_s) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                case ExifInterface.ORIENTATION_NORMAL:
                    // leave unchanged
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    needs_tf = true;
                    exif_orientation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    needs_tf = true;
                    exif_orientation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    needs_tf = true;
                    exif_orientation = 270;
                    break;
                default:
                    if (MyDebug.LOG)
                        Log.e(TAG, "    unsupported exif orientation: " + exif_orientation_s);
                    break;
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
        } catch (IOException exception) {
            if (MyDebug.LOG)
                Log.e(TAG, "exif orientation ioexception");
            exception.printStackTrace();
        } catch (NoClassDefFoundError exception) {
            if (MyDebug.LOG)
                Log.e(TAG, "exif orientation NoClassDefFoundError");
            exception.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    private Bitmap loadBitmapWithRotation(byte[] data, boolean mutable, File exifTempFile) {
        Bitmap bitmap = loadBitmap(data, mutable, 1);
        if (bitmap != null) {
            // rotate the bitmap if necessary for exif tags
            if (MyDebug.LOG)
                Log.d(TAG, "rotate bitmap for exif tags?");
            bitmap = rotateForExif(bitmap, data, exifTempFile);
        }
        return bitmap;
    }

    private ExifInterface createExifInterface(File picFile, Uri saveUri) throws IOException {
        ExifInterface exif = null;
        if (picFile != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "write to picFile: " + picFile);
            exif = new ExifInterface(picFile.getAbsolutePath());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (MyDebug.LOG)
                Log.d(TAG, "write direct to saveUri: " + saveUri);
            ParcelFileDescriptor parcelFileDescriptor = main_activity.getContentResolver().openFileDescriptor(saveUri, "rw");
            if (parcelFileDescriptor != null) {
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                exif = new ExifInterface(fileDescriptor);
            } else {
                Log.e(TAG, "failed to create ParcelFileDescriptor for saveUri: " + saveUri);
            }
        } else {
            throw new RuntimeException("picFile==null but Android version is not 7 or later");
        }
        return exif;
    }

    private void updateExif(Request request, File picFile, Uri saveUri) throws IOException {
        if (MyDebug.LOG)
            Log.d(TAG, "updateExif: " + picFile);
        if (request.store_geo_direction || hasCustomExif(request.custom_tag_artist, request.custom_tag_copyright)) {
            long time_s = System.currentTimeMillis();
            if (MyDebug.LOG)
                Log.d(TAG, "add additional exif info");
            try {
                ExifInterface exif = createExifInterface(picFile, saveUri);
                if (exif != null) {
                    modifyExif(exif, request.type == Request.Type.JPEG, request.using_camera2, request.current_date, request.store_location, request.store_geo_direction, request.geo_direction, request.custom_tag_artist, request.custom_tag_copyright);
                    exif.saveAttributes();
                }
            } catch (NoClassDefFoundError exception) {
                // have had Google Play crashes from new ExifInterface() elsewhere for Galaxy Ace4 (vivalto3g), Galaxy S Duos3 (vivalto3gvn), so also catch here just in case
                if (MyDebug.LOG)
                    Log.e(TAG, "exif orientation NoClassDefFoundError");
                exception.printStackTrace();
            }
            if (MyDebug.LOG)
                Log.d(TAG, "*** time to add additional exif info: " + (System.currentTimeMillis() - time_s));
        } else if (needGPSTimestampHack(request.type == Request.Type.JPEG, request.using_camera2, request.store_location)) {
            if (MyDebug.LOG)
                Log.d(TAG, "remove GPS timestamp hack");
            try {
                ExifInterface exif = createExifInterface(picFile, saveUri);
                if (exif != null) {
                    fixGPSTimestamp(exif, request.current_date);
                    exif.saveAttributes();
                }
            } catch (NoClassDefFoundError exception) {
                // have had Google Play crashes from new ExifInterface() elsewhere for Galaxy Ace4 (vivalto3g), Galaxy S Duos3 (vivalto3gvn), so also catch here just in case
                if (MyDebug.LOG)
                    Log.e(TAG, "exif orientation NoClassDefFoundError");
                exception.printStackTrace();
            }
        } else {
            if (MyDebug.LOG)
                Log.d(TAG, "no exif data to update for: " + picFile);
        }
    }

    private void modifyExif(ExifInterface exif, boolean is_jpeg, boolean using_camera2, Date current_date, boolean store_location, boolean store_geo_direction, double geo_direction, String custom_tag_artist, String custom_tag_copyright) {
        if (MyDebug.LOG)
            Log.d(TAG, "modifyExif");
        setGPSDirectionExif(exif, store_geo_direction, geo_direction);
        setCustomExif(exif, custom_tag_artist, custom_tag_copyright);
        if (needGPSTimestampHack(is_jpeg, using_camera2, store_location)) {
            fixGPSTimestamp(exif, current_date);
        }
    }

    private void setGPSDirectionExif(ExifInterface exif, boolean store_geo_direction, double geo_direction) {
        if (MyDebug.LOG)
            Log.d(TAG, "setGPSDirectionExif");
        if (store_geo_direction) {
            float geo_angle = (float) Math.toDegrees(geo_direction);
            if (geo_angle < 0.0f) {
                geo_angle += 360.0f;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "save geo_angle: " + geo_angle);
            // see http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/GPS.html
            String GPSImgDirection_string = Math.round(geo_angle * 100) + "/100";
            if (MyDebug.LOG)
                Log.d(TAG, "GPSImgDirection_string: " + GPSImgDirection_string);
            exif.setAttribute(TAG_GPS_IMG_DIRECTION, GPSImgDirection_string);
            exif.setAttribute(TAG_GPS_IMG_DIRECTION_REF, "M");
        }
    }

    private boolean hasCustomExif(String custom_tag_artist, String custom_tag_copyright) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && custom_tag_artist != null && custom_tag_artist.length() > 0)
            return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && custom_tag_copyright != null && custom_tag_copyright.length() > 0)
            return true;
        return false;
    }

    private void setCustomExif(ExifInterface exif, String custom_tag_artist, String custom_tag_copyright) {
        if (MyDebug.LOG)
            Log.d(TAG, "setCustomExif");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && custom_tag_artist != null && custom_tag_artist.length() > 0) {
            if (MyDebug.LOG)
                Log.d(TAG, "apply TAG_ARTIST: " + custom_tag_artist);
            exif.setAttribute(ExifInterface.TAG_ARTIST, custom_tag_artist);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && custom_tag_copyright != null && custom_tag_copyright.length() > 0) {
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, custom_tag_copyright);
            if (MyDebug.LOG)
                Log.d(TAG, "apply TAG_COPYRIGHT: " + custom_tag_copyright);
        }
    }

    private void setDateTimeExif(ExifInterface exif) {
        if (MyDebug.LOG)
            Log.d(TAG, "setDateTimeExif");
        String exif_datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
        if (exif_datetime != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "write datetime tags: " + exif_datetime);
            exif.setAttribute(TAG_DATETIME_ORIGINAL, exif_datetime);
            exif.setAttribute(TAG_DATETIME_DIGITIZED, exif_datetime);
        }
    }

    private void fixGPSTimestamp(ExifInterface exif, Date current_date) {
        if (MyDebug.LOG) {
            Log.d(TAG, "fixGPSTimestamp");
            Log.d(TAG, "current datestamp: " + exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
            Log.d(TAG, "current timestamp: " + exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP));
            Log.d(TAG, "current datetime: " + exif.getAttribute(ExifInterface.TAG_DATETIME));
        }
        SimpleDateFormat date_fmt = new SimpleDateFormat("yyyy:MM:dd", Locale.US);
        date_fmt.setTimeZone(TimeZone.getTimeZone("UTC")); // needs to be UTC time
        String datestamp = date_fmt.format(current_date);

        SimpleDateFormat time_fmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
        time_fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = time_fmt.format(current_date);

        if (MyDebug.LOG) {
            Log.d(TAG, "datestamp: " + datestamp);
            Log.d(TAG, "timestamp: " + timestamp);
        }
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, datestamp);
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, timestamp);

        if (MyDebug.LOG)
            Log.d(TAG, "fixGPSTimestamp exit");
    }

    private boolean needGPSTimestampHack(boolean is_jpeg, boolean using_camera2, boolean store_location) {
        if (is_jpeg && using_camera2) {
            return store_location;
        }
        return false;
    }

    private void copyFileToUri(Context context, Uri saveUri, File picFile) throws IOException {
        if (MyDebug.LOG) {
            Log.d(TAG, "copyFileToUri");
            Log.d(TAG, "saveUri: " + saveUri);
            Log.d(TAG, "picFile: " + saveUri);
        }
        InputStream inputStream = null;
        OutputStream realOutputStream = null;
        try {
            inputStream = new FileInputStream(picFile);
            realOutputStream = context.getContentResolver().openOutputStream(saveUri);
            // Transfer bytes from in to out
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                realOutputStream.write(buffer, 0, len);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (realOutputStream != null) {
                realOutputStream.close();
            }
        }
    }

    HDRProcessor getHDRProcessor() {
        return hdrProcessor;
    }

    public PanoramaProcessor getPanoramaProcessor() {
        return panoramaProcessor;
    }
}

