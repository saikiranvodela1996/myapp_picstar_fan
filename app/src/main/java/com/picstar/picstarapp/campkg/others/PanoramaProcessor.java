package com.picstar.picstarapp.campkg.others;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSInvalidStateException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.Type;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.picstar.picstarapp.activities.PhotoSelfieCameraActivity;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PanoramaProcessor {
    private static final String TAG = "PanoramaProcessor";

    private final Context context;
    private final HDRProcessor hdrProcessor;
    private RenderScript rs; // lazily created, so we don't take up resources if application isn't using panorama
    private ScriptC_pyramid_blending pyramidBlendingScript = null;
    private ScriptC_feature_detector featureDetectorScript = null;

    public PanoramaProcessor(Context context, HDRProcessor hdrProcessor) {
        this.context = context;
        this.hdrProcessor = hdrProcessor;
    }

    private void freeScripts() {
        if (MyDebug.LOG)
            Log.d(TAG, "freeScripts");

        pyramidBlendingScript = null;
        featureDetectorScript = null;
    }

    public void onDestroy() {
        if (MyDebug.LOG)
            Log.d(TAG, "onDestroy");

        freeScripts(); // just in case

        if (rs != null) {
            try {
                rs.destroy(); // on Android M onwards this is a NOP - instead we call RenderScript.releaseAllContexts(); in MainActivity.onDestroy()
            } catch (RSInvalidStateException e) {
                e.printStackTrace();
            }
            rs = null;
        }
    }

    private void initRenderscript() {
        if (rs == null) {
            this.rs = RenderScript.create(context);
            if (MyDebug.LOG)
                Log.d(TAG, "create renderscript object");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Allocation reduceBitmap(ScriptC_pyramid_blending script, Allocation allocation) {
        if (MyDebug.LOG)
            Log.d(TAG, "reduceBitmap");
        int width = allocation.getType().getX();
        int height = allocation.getType().getY();

        Allocation reduced_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.RGBA_8888(rs), width / 2, height / 2));

        script.set_bitmap(allocation);
        script.forEach_reduce(reduced_allocation, reduced_allocation);

        return reduced_allocation;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Allocation expandBitmap(ScriptC_pyramid_blending script, Allocation allocation) {
        if (MyDebug.LOG)
            Log.d(TAG, "expandBitmap");
        long time_s = 0;
        if (MyDebug.LOG)
            time_s = System.currentTimeMillis();

        int width = allocation.getType().getX();
        int height = allocation.getType().getY();
        Allocation result_allocation;

        Allocation expanded_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.RGBA_8888(rs), 2 * width, 2 * height));
        if (MyDebug.LOG)
            Log.d(TAG, "### expandBitmap: time after creating expanded_allocation: " + (System.currentTimeMillis() - time_s));

        script.set_bitmap(allocation);
        script.forEach_expand(expanded_allocation, expanded_allocation);
        if (MyDebug.LOG)
            Log.d(TAG, "### expandBitmap: time after expand: " + (System.currentTimeMillis() - time_s));

        final boolean use_blur_2d = false; // faster to do blue as two 1D passes
        if (use_blur_2d) {
            result_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.RGBA_8888(rs), 2 * width, 2 * height));
            if (MyDebug.LOG)
                Log.d(TAG, "### expandBitmap: time after creating result_allocation: " + (System.currentTimeMillis() - time_s));
            script.set_bitmap(expanded_allocation);
            script.forEach_blur(expanded_allocation, result_allocation);
            if (MyDebug.LOG)
                Log.d(TAG, "### expandBitmap: time after blur: " + (System.currentTimeMillis() - time_s));
            expanded_allocation.destroy();
        } else {
            Allocation temp_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.RGBA_8888(rs), 2 * width, 2 * height));
            if (MyDebug.LOG)
                Log.d(TAG, "### expandBitmap: time after creating temp_allocation: " + (System.currentTimeMillis() - time_s));
            script.set_bitmap(expanded_allocation);
            script.forEach_blur1dX(expanded_allocation, temp_allocation);
            result_allocation = expanded_allocation;
            script.set_bitmap(temp_allocation);
            script.forEach_blur1dY(temp_allocation, result_allocation);
            if (MyDebug.LOG)
                Log.d(TAG, "### expandBitmap: time after blur1dY: " + (System.currentTimeMillis() - time_s));

            temp_allocation.destroy();
        }

        return result_allocation;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Allocation subtractBitmap(ScriptC_pyramid_blending script, Allocation allocation0, Allocation allocation1) {
        if (MyDebug.LOG)
            Log.d(TAG, "subtractBitmap");
        int width = allocation0.getType().getX();
        int height = allocation0.getType().getY();
        if (allocation1.getType().getX() != width || allocation1.getType().getY() != height) {
            Log.e(TAG, "allocations of different dimensions");
            throw new RuntimeException();
        }
        Allocation result_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.F32_3(rs), width, height));
        script.set_bitmap(allocation1);
        script.forEach_subtract(allocation0, result_allocation);

        return result_allocation;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void addBitmap(ScriptC_pyramid_blending script, Allocation allocation0, Allocation allocation1) {
        if (MyDebug.LOG)
            Log.d(TAG, "addBitmap");
        int width = allocation0.getType().getX();
        int height = allocation0.getType().getY();
        if (allocation1.getType().getX() != width || allocation1.getType().getY() != height) {
            Log.e(TAG, "allocations of different dimensions");
            throw new RuntimeException();
        }
        script.set_bitmap(allocation1);
        script.forEach_add(allocation0, allocation0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<Allocation> createGaussianPyramid(ScriptC_pyramid_blending script, Bitmap bitmap, int n_levels) {
        if (MyDebug.LOG)
            Log.d(TAG, "createGaussianPyramid");
        List<Allocation> pyramid = new ArrayList<>();

        Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
        pyramid.add(allocation);
        for (int i = 0; i < n_levels; i++) {
            allocation = reduceBitmap(script, allocation);
            pyramid.add(allocation);
        }

        return pyramid;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<Allocation> createLaplacianPyramid(ScriptC_pyramid_blending script, Bitmap bitmap, int n_levels, String name) {
        if (MyDebug.LOG)
            Log.d(TAG, "createLaplacianPyramid");
        long time_s = 0;
        if (MyDebug.LOG)
            time_s = System.currentTimeMillis();

        List<Allocation> gaussianPyramid = createGaussianPyramid(script, bitmap, n_levels);

        List<Allocation> pyramid = new ArrayList<>();

        for (int i = 0; i < gaussianPyramid.size() - 1; i++) {
            if (MyDebug.LOG)
                Log.d(TAG, "createLaplacianPyramid: i = " + i);
            Allocation this_gauss = gaussianPyramid.get(i);
            Allocation next_gauss = gaussianPyramid.get(i + 1);
            Allocation next_gauss_expanded = expandBitmap(script, next_gauss);
            if (MyDebug.LOG)
                Log.d(TAG, "### createLaplacianPyramid: time after expandBitmap for level " + i + ": " + (System.currentTimeMillis() - time_s));
            if (MyDebug.LOG) {
                Log.d(TAG, "this_gauss: " + this_gauss.getType().getX() + " , " + this_gauss.getType().getY());
                Log.d(TAG, "next_gauss: " + next_gauss.getType().getX() + " , " + next_gauss.getType().getY());
                Log.d(TAG, "next_gauss_expanded: " + next_gauss_expanded.getType().getX() + " , " + next_gauss_expanded.getType().getY());
            }
            Allocation difference = subtractBitmap(script, this_gauss, next_gauss_expanded);

            pyramid.add(difference);

            this_gauss.destroy();
            gaussianPyramid.set(i, null); // to help garbage collection
            next_gauss_expanded.destroy();
        }
        pyramid.add(gaussianPyramid.get(gaussianPyramid.size() - 1));

        return pyramid;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Bitmap collapseLaplacianPyramid(ScriptC_pyramid_blending script, List<Allocation> pyramid) {
        if (MyDebug.LOG)
            Log.d(TAG, "collapseLaplacianPyramid");

        Allocation allocation = pyramid.get(pyramid.size() - 1);
        boolean first = true;
        for (int i = pyramid.size() - 2; i >= 0; i--) {
            Allocation expanded_allocation = expandBitmap(script, allocation);
            if (!first) {
                allocation.destroy();
            }
            addBitmap(script, expanded_allocation, pyramid.get(i));
            allocation = expanded_allocation;
            first = false;
        }

        int width = allocation.getType().getX();
        int height = allocation.getType().getY();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        allocation.copyTo(bitmap);
        if (!first) {
            allocation.destroy();
        }
        return bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void mergePyramids(ScriptC_pyramid_blending script, List<Allocation> pyramid0, List<Allocation> pyramid1, int[] best_path, int best_path_n_x) {
        if (MyDebug.LOG)
            Log.d(TAG, "mergePyramids");

        if (best_path == null) {
            best_path = new int[1];
            best_path_n_x = 3;
            best_path[0] = 1;
        }
        if (MyDebug.LOG) {
            for (int i = 0; i < best_path.length; i++)
                Log.d(TAG, "best_path[" + i + "]: " + best_path[i]);
        }
        int max_height = 0;
        for (int i = 0; i < pyramid0.size(); i++) {
            Allocation allocation0 = pyramid0.get(i);
            int height = allocation0.getType().getY();
            max_height = Math.max(max_height, height);
        }

        Allocation interpolatedbestPathAllocation = Allocation.createSized(rs, Element.I32(rs), max_height);
        script.bind_interpolated_best_path(interpolatedbestPathAllocation);
        int[] interpolated_best_path = new int[max_height];

        for (int i = 0; i < pyramid0.size(); i++) {
            Allocation allocation0 = pyramid0.get(i);
            Allocation allocation1 = pyramid1.get(i);

            int width = allocation0.getType().getX();
            int height = allocation0.getType().getY();
            if (allocation1.getType().getX() != width || allocation1.getType().getY() != height) {
                Log.e(TAG, "allocations of different dimensions");
                throw new RuntimeException();
            } else if (allocation0.getType().getElement().getDataType() != allocation1.getType().getElement().getDataType()) {
                Log.e(TAG, "allocations of different data types");
                throw new RuntimeException();
            }

            script.set_bitmap(allocation1);

            int blend_window_width = width / 2;
            //int blend_width = (i==pyramid0.size()-1) ? blend_window_width : 2;
            int blend_width;
            if (i == pyramid0.size() - 1) {
                blend_width = blend_window_width;
            } else {
                blend_width = 2;
                for (int j = 0; j < i; j++) {
                    blend_width *= 2;
                }
                blend_width = Math.min(blend_width, blend_window_width);
            }
            float best_path_y_scale = best_path.length / (float) height;
            for (int y = 0; y < height; y++) {
                if (false) {
                    int best_path_y_index = (int) ((y + 0.5f) * best_path_y_scale);
                    int best_path_value = best_path[best_path_y_index];
                    float alpha = best_path_value / (best_path_n_x - 1.0f);
                    float frac = (1.0f - alpha) * 0.25f + alpha * 0.75f;
                    interpolated_best_path[y] = (int) (frac * width + 0.5f);
                }
                {
                    float best_path_y_index = ((y + 0.5f) * best_path_y_scale);
                    float best_path_value;
                    if (best_path_y_index <= 0.5f) {
                        best_path_value = best_path[0];
                    } else if (best_path_y_index >= best_path.length - 1 + 0.5f) {
                        best_path_value = best_path[best_path.length - 1];
                    } else {
                        best_path_y_index -= 0.5f;
                        int best_path_y_index_i = (int) best_path_y_index;
                        float linear_alpha = best_path_y_index - best_path_y_index_i;
                        //float alpha = linear_alpha;
                        //final float edge_length = 0.25f;
                        final float edge_length = 0.1f;
                        float alpha;
                        if (linear_alpha < edge_length)
                            alpha = 0.0f;
                        else if (linear_alpha > 1.0f - edge_length)
                            alpha = 1.0f;
                        else
                            alpha = (linear_alpha - edge_length) / (1.0f - 2.0f * edge_length);
                        int prev_best_path = best_path[best_path_y_index_i];
                        int next_best_path = best_path[best_path_y_index_i + 1];
                        best_path_value = (1.0f - alpha) * prev_best_path + alpha * next_best_path;
                    }
                    float alpha = best_path_value / (best_path_n_x - 1.0f);
                    float frac = (1.0f - alpha) * 0.25f + alpha * 0.75f;
                    interpolated_best_path[y] = (int) (frac * width + 0.5f);
                }
                if (interpolated_best_path[y] - blend_width / 2 < 0) {
                    Log.e(TAG, "    interpolated_best_path[" + y + "]: " + interpolated_best_path[y]);
                    Log.e(TAG, "    blend_width: " + blend_width);
                    Log.e(TAG, "    width: " + width);
                    throw new RuntimeException("blend window runs off left hand size");
                } else if (interpolated_best_path[y] + blend_width / 2 > width) {
                    Log.e(TAG, "    interpolated_best_path[" + y + "]: " + interpolated_best_path[y]);
                    Log.e(TAG, "    blend_width: " + blend_width);
                    Log.e(TAG, "    width: " + width);
                    throw new RuntimeException("blend window runs off right hand size");
                }
            }
            interpolatedbestPathAllocation.copyFrom(interpolated_best_path);

            script.invoke_setBlendWidth(blend_width, width);

            if (allocation0.getType().getElement().getDataType() == Element.DataType.FLOAT_32) {
                script.forEach_merge_f(allocation0, allocation0);
            } else {
                script.forEach_merge(allocation0, allocation0);
            }
        }

        interpolatedbestPathAllocation.destroy();
    }

    private void saveBitmap(Bitmap bitmap, String name) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/" + name);
            OutputStream outputStream = new FileOutputStream(file);
            if (name.toLowerCase().endsWith(".png"))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            else
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.close();
            PhotoSelfieCameraActivity mActivity = (PhotoSelfieCameraActivity) context;
            mActivity.getStorageUtils().broadcastFile(file, true, false, true);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void saveAllocation(String name, Allocation allocation) {
        Bitmap bitmap;
        int width = allocation.getType().getX();
        int height = allocation.getType().getY();
        Log.d(TAG, "count: " + allocation.getType().getCount());
        Log.d(TAG, "byte size: " + allocation.getType().getElement().getBytesSize());
        if (allocation.getType().getElement().getDataType() == Element.DataType.FLOAT_32) {
            float[] bytes = new float[width * height * 4];
            allocation.copyTo(bytes);
            int[] pixels = new int[width * height];
            for (int j = 0; j < width * height; j++) {
                float r = bytes[4 * j];
                float g = bytes[4 * j + 1];
                float b = bytes[4 * j + 2];
                // each value should be from -255 to +255, we compress to be in the range [0, 255]
                int ir = (int) (255.0f * ((r / 510.0f) + 0.5f) + 0.5f);
                int ig = (int) (255.0f * ((g / 510.0f) + 0.5f) + 0.5f);
                int ib = (int) (255.0f * ((b / 510.0f) + 0.5f) + 0.5f);
                ir = Math.max(Math.min(ir, 255), 0);
                ig = Math.max(Math.min(ig, 255), 0);
                ib = Math.max(Math.min(ib, 255), 0);
                pixels[j] = Color.argb(255, ir, ig, ib);
            }
            bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } else if (allocation.getType().getElement().getDataType() == Element.DataType.UNSIGNED_8) {
            byte[] bytes = new byte[width * height];
            allocation.copyTo(bytes);
            int[] pixels = new int[width * height];
            for (int j = 0; j < width * height; j++) {
                int b = bytes[j];
                if (b < 0)
                    b += 255;
                pixels[j] = Color.argb(255, b, b, b);
            }
            bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            allocation.copyTo(bitmap);
        }
        saveBitmap(bitmap, name);
        bitmap.recycle();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Bitmap blendPyramids(Bitmap lhs, Bitmap rhs) {
        long time_s = 0;
        if (MyDebug.LOG)
            time_s = System.currentTimeMillis();

        if (pyramidBlendingScript == null) {
            pyramidBlendingScript = new ScriptC_pyramid_blending(rs);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### blendPyramids: time after creating ScriptC_pyramid_blending: " + (System.currentTimeMillis() - time_s));


        if (lhs.getWidth() != rhs.getWidth() || lhs.getHeight() != rhs.getHeight()) {
            Log.e(TAG, "lhs/rhs bitmaps of different dimensions");
            throw new RuntimeException();
        }

        final int blend_dimension = 0;
        if (lhs.getWidth() % blend_dimension != 0) {
            Log.e(TAG, "bitmap width " + lhs.getWidth() + " not a multiple of " + blend_dimension);
            throw new RuntimeException();
        } else if (lhs.getHeight() % blend_dimension != 0) {
            Log.e(TAG, "bitmap height " + lhs.getHeight() + " not a multiple of " + blend_dimension);
            throw new RuntimeException();
        }

        //final boolean find_best_path = false;
        final boolean find_best_path = true;
        //final int best_path_n_x = 3;
        final int best_path_n_x = 7;
        final int best_path_n_y = 8;
        //final int best_path_n_y = 16;
        int[] best_path = null;
        if (find_best_path) {
            best_path = new int[best_path_n_y];


            final int scale_factor = 4;
            Bitmap best_path_lhs = Bitmap.createScaledBitmap(lhs, lhs.getWidth() / scale_factor, lhs.getHeight() / scale_factor, true);
            Bitmap best_path_rhs = Bitmap.createScaledBitmap(rhs, rhs.getWidth() / scale_factor, rhs.getHeight() / scale_factor, true);

            Allocation lhs_allocation = Allocation.createFromBitmap(rs, best_path_lhs);
            Allocation rhs_allocation = Allocation.createFromBitmap(rs, best_path_rhs);

            int[] errors = new int[1];
            Allocation errorsAllocation = Allocation.createSized(rs, Element.I32(rs), 1);
            pyramidBlendingScript.bind_errors(errorsAllocation);

            Script.LaunchOptions launch_options = new Script.LaunchOptions();
            if (MyDebug.LOG)
                Log.d(TAG, "### blendPyramids: time after creating allocations for best path: " + (System.currentTimeMillis() - time_s));

            pyramidBlendingScript.set_bitmap(rhs_allocation);

            int window_width = Math.max(2, best_path_lhs.getWidth() / 8);
            int start_y = 0, stop_y;
            for (int y = 0; y < best_path_n_y; y++) {
                best_path[y] = -1;
                int best_error = -1;

                stop_y = ((y + 1) * best_path_lhs.getHeight()) / best_path_n_y;
                launch_options.setY(start_y, stop_y);
                start_y = stop_y; // set for next iteration

                for (int x = 0; x < best_path_n_x; x++) {
                    float alpha = ((float) x) / (best_path_n_x - 1.0f);
                    float frac = (1.0f - alpha) * 0.25f + alpha * 0.75f;
                    int mid_x = (int) (frac * best_path_lhs.getWidth() + 0.5f);
                    int start_x = mid_x - window_width / 2;
                    int stop_x = mid_x + window_width / 2;
                    launch_options.setX(start_x, stop_x);
                    pyramidBlendingScript.invoke_init_errors();
                    pyramidBlendingScript.forEach_compute_error(lhs_allocation, launch_options);
                    errorsAllocation.copyTo(errors);

                    int this_error = errors[0];
                    if (MyDebug.LOG)
                        Log.d(TAG, "    best_path error[" + x + "][" + y + "]: " + this_error);
                    if (best_path[y] == -1 || this_error < best_error) {
                        best_path[y] = x;
                        best_error = this_error;
                    }
                }

            }

            lhs_allocation.destroy();
            rhs_allocation.destroy();
            errorsAllocation.destroy();

            if (best_path_lhs != lhs) {
                best_path_lhs.recycle();
            }
            if (best_path_rhs != rhs) {
                best_path_rhs.recycle();
            }

            if (MyDebug.LOG)
                Log.d(TAG, "### blendPyramids: time after finding best path: " + (System.currentTimeMillis() - time_s));
        }

        List<Allocation> lhs_pyramid = createLaplacianPyramid(pyramidBlendingScript, lhs, 0, "lhs");
        if (MyDebug.LOG)
            Log.d(TAG, "### blendPyramids: time after createLaplacianPyramid 1st call: " + (System.currentTimeMillis() - time_s));
        List<Allocation> rhs_pyramid = createLaplacianPyramid(pyramidBlendingScript, rhs, 0, "rhs");
        if (MyDebug.LOG)
            Log.d(TAG, "### blendPyramids: time after createLaplacianPyramid 2nd call: " + (System.currentTimeMillis() - time_s));


        mergePyramids(pyramidBlendingScript, lhs_pyramid, rhs_pyramid, best_path, best_path_n_x);
        Bitmap merged_bitmap = collapseLaplacianPyramid(pyramidBlendingScript, lhs_pyramid);


        for (Allocation allocation : lhs_pyramid) {
            allocation.destroy();
        }
        for (Allocation allocation : rhs_pyramid) {
            allocation.destroy();
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### blendPyramids: time taken: " + (System.currentTimeMillis() - time_s));
        return merged_bitmap;
    }

    private static class FeatureMatch implements Comparable<FeatureMatch> {
        private final int index0, index1;
        private float distance; // from 0 to 1, higher means poorer match

        private FeatureMatch(int index0, int index1) {
            this.index0 = index0;
            this.index1 = index1;
        }

        @Override
        public int compareTo(FeatureMatch that) {

            return Float.compare(this.distance, that.distance);
        }

        @Override
        public boolean equals(Object that) {
            return (that instanceof FeatureMatch) && compareTo((FeatureMatch) that) == 0;
        }
    }

    private static void computeDistancesBetweenMatches(List<FeatureMatch> matches, int st_indx, int nd_indx, int feature_descriptor_radius, List<Bitmap> bitmaps, int[] pixels0, int[] pixels1) {
        final int wid = 2 * feature_descriptor_radius + 1;
        final int wid2 = wid * wid;
        for (int indx = st_indx; indx < nd_indx; indx++) {
            FeatureMatch match = matches.get(indx);

            float fsum = 0, gsum = 0;
            float f2sum = 0, g2sum = 0;
            float fgsum = 0;


            int pixel_idx0 = match.index0 * wid2;
            int pixel_idx1 = match.index1 * wid2;

            for (int dy = -feature_descriptor_radius; dy <= feature_descriptor_radius; dy++) {
                for (int dx = -feature_descriptor_radius; dx <= feature_descriptor_radius; dx++) {

                    int value0 = pixels0[pixel_idx0];
                    int value1 = pixels1[pixel_idx1];
                    pixel_idx0++;
                    pixel_idx1++;

                    fsum += value0;
                    f2sum += value0 * value0;
                    gsum += value1;
                    g2sum += value1 * value1;
                    fgsum += value0 * value1;
                }
            }
            float fden = wid2 * f2sum - fsum * fsum;
            float f_recip = fden == 0 ? 0.0f : 1 / fden;
            float gden = wid2 * g2sum - gsum * gsum;
            float g_recip = gden == 0 ? 0.0f : 1 / gden;
            float fg_corr = wid2 * fgsum - fsum * gsum;

            match.distance = 1.0f - Math.abs((fg_corr * fg_corr * f_recip * g_recip));
        }
    }

    private static class ComputeDistancesBetweenMatchesThread extends Thread {
        private final List<FeatureMatch> matches;
        private final int st_indx;
        private final int nd_indx;
        private final int feature_descriptor_radius;
        private final List<Bitmap> bitmaps;
        private final int[] pixels0;
        private final int[] pixels1;

        ComputeDistancesBetweenMatchesThread(List<FeatureMatch> matches, int st_indx, int nd_indx, int feature_descriptor_radius, List<Bitmap> bitmaps, int[] pixels0, int[] pixels1) {
            this.matches = matches;
            this.st_indx = st_indx;
            this.nd_indx = nd_indx;
            this.feature_descriptor_radius = feature_descriptor_radius;
            this.bitmaps = bitmaps;
            this.pixels0 = pixels0;
            this.pixels1 = pixels1;
        }

        public void run() {
            computeDistancesBetweenMatches(matches, st_indx, nd_indx, feature_descriptor_radius, bitmaps, pixels0, pixels1);
        }
    }

    static class AutoAlignmentByFeatureResult {
        final int offset_x;
        final int offset_y;
        final float rotation;
        final float y_scale;

        AutoAlignmentByFeatureResult(int offset_x, int offset_y, float rotation, float y_scale) {
            this.offset_x = offset_x;
            this.offset_y = offset_y;
            this.rotation = rotation;
            this.y_scale = y_scale;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AutoAlignmentByFeatureResult autoAlignmentByFeature(int width, int height, List<Bitmap> bitmaps, int debug_index) throws PanoramaProcessorException {
        if (MyDebug.LOG) {
            Log.d(TAG, "autoAlignmentByFeature");
            Log.d(TAG, "width: " + width);
            Log.d(TAG, "height: " + height);
        }
        long time_s = 0;
        if (MyDebug.LOG)
            time_s = System.currentTimeMillis();
        if (bitmaps.size() != 2) {
            Log.e(TAG, "must have 2 bitmaps");
            throw new PanoramaProcessorException(PanoramaProcessorException.INVALID_N_IMAGES);
        }

        initRenderscript();
        if (MyDebug.LOG)
            Log.d(TAG, "### autoAlignmentByFeature: time after initRenderscript: " + (System.currentTimeMillis() - time_s));
        Allocation[] allocations = new Allocation[bitmaps.size()];
        for (int i = 0; i < bitmaps.size(); i++) {
            allocations[i] = Allocation.createFromBitmap(rs, bitmaps.get(i));
        }

        if (featureDetectorScript == null) {
            featureDetectorScript = new ScriptC_feature_detector(rs);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### autoAlignmentByFeature: time after create featureDetectorScript: " + (System.currentTimeMillis() - time_s));


        final int feature_descriptor_radius = 3; // radius of square used to compare features
        Point[][] points_arrays = new Point[2][];

        for (int i = 0; i < bitmaps.size(); i++) {
            if (MyDebug.LOG)
                Log.d(TAG, "detect features for image: " + i);

            if (MyDebug.LOG)
                Log.d(TAG, "convert to greyscale");
            Allocation gs_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.U8(rs), width, height));
            featureDetectorScript.forEach_create_greyscale(allocations[i], gs_allocation);

            if (MyDebug.LOG)
                Log.d(TAG, "compute derivatives");
            Allocation ix_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.U8(rs), width, height));
            Allocation iy_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.U8(rs), width, height));
            featureDetectorScript.set_bitmap(gs_allocation);
            featureDetectorScript.set_bitmap_Ix(ix_allocation);
            featureDetectorScript.set_bitmap_Iy(iy_allocation);
            featureDetectorScript.forEach_compute_derivatives(gs_allocation);


            if (MyDebug.LOG)
                Log.d(TAG, "call corner detector script for image: " + i);
            Allocation strength_allocation = Allocation.createTyped(rs, Type.createXY(rs, Element.F32(rs), width, height));
            featureDetectorScript.set_bitmap(gs_allocation);
            featureDetectorScript.set_bitmap_Ix(ix_allocation);
            featureDetectorScript.set_bitmap_Iy(iy_allocation);
            featureDetectorScript.forEach_corner_detector(gs_allocation, strength_allocation);


            ix_allocation.destroy();
            iy_allocation.destroy();


            Allocation local_max_features_allocation = gs_allocation;

            featureDetectorScript.set_bitmap(strength_allocation);
            final int n_y_chunks = 2;
            final int total_max_corners = 200;
            final int max_corners = total_max_corners / n_y_chunks;
            final int min_corners = max_corners / 2;
            byte[] bytes = new byte[width * height];

            List<Point> all_points = new ArrayList<>();
            for (int cy = 0; cy < n_y_chunks; cy++) {
                if (MyDebug.LOG)
                    Log.d(TAG, ">>> find corners, chunk " + cy + " / " + n_y_chunks);
                float threshold = 5000000.0f;
                final float min_threshold = 1250000.0f;
                float low_threshold = 0.0f;
                float high_threshold = -1.0f;
                int start_y = (cy * height) / n_y_chunks;
                int stop_y = ((cy + 1) * height) / n_y_chunks;
                if (MyDebug.LOG) {
                    Log.d(TAG, "    start_y: " + start_y);
                    Log.d(TAG, "    stop_y: " + stop_y);
                }
                final int max_iter = 10;
                for (int count = 0; ; count++) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "### attempt " + count + " try threshold: " + threshold + " [ " + low_threshold + " : " + high_threshold + " ]");
                    featureDetectorScript.set_corner_threshold(threshold);
                    Script.LaunchOptions launch_options = new Script.LaunchOptions();
                    launch_options.setX(0, width);
                    launch_options.setY(start_y, stop_y);
                    featureDetectorScript.forEach_local_maximum(strength_allocation, local_max_features_allocation, launch_options);

                    // collect points
                    local_max_features_allocation.copyTo(bytes);
                    // find points
                    List<Point> points = new ArrayList<>();
                    for (int y = Math.max(start_y, feature_descriptor_radius); y < Math.min(stop_y, height - feature_descriptor_radius); y++) {
                        for (int x = feature_descriptor_radius; x < width - feature_descriptor_radius; x++) {
                            int j = y * width + x;
                            // remember, bytes are signed!
                            if (bytes[j] != 0) {
                                Point point = new Point(x, y);
                                points.add(point);
                            }
                        }
                    }
                    if (MyDebug.LOG)
                        Log.d(TAG, "    " + points.size() + " points");
                    if (points.size() >= min_corners && points.size() <= max_corners) {
                        all_points.addAll(points);
                        break;
                    } else if (points.size() < min_corners) {
                        if (threshold <= min_threshold) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "    hit minimum threshold: " + threshold);
                            all_points.addAll(points);
                            break;
                        } else if (count + 1 == max_iter) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "    too few points but hit max iterations: " + points.size());
                            all_points.addAll(points);
                            //if( true )
                            //    throw new RuntimeException("too few points: " + points.size()); // test
                            break;
                        } else {
                            high_threshold = threshold;
                            threshold = 0.5f * (low_threshold + threshold);
                            if (MyDebug.LOG)
                                Log.d(TAG, "    reduced threshold to: " + threshold);
                        }
                    } else if (count + 1 == max_iter) {
                        points.subList(max_corners, points.size()).clear();
                        all_points.addAll(points);

                        break;
                    } else {
                        low_threshold = threshold;
                        if (high_threshold < 0.0f) {
                            threshold *= 10.0f;
                        } else
                            threshold = 0.5f * (threshold + high_threshold);
                        if (MyDebug.LOG)
                            Log.d(TAG, "    increased threshold to: " + threshold);
                    }
                }
            }
            points_arrays[i] = all_points.toArray(new Point[0]);

            if (MyDebug.LOG)
                Log.d(TAG, "### image: " + i + " has " + points_arrays[i].length + " points");

            strength_allocation.destroy();

            local_max_features_allocation.destroy();
        }

        final int min_required_corners = 10;
        if (points_arrays[0].length < min_required_corners || points_arrays[1].length < min_required_corners) {
            for (int i = 0; i < allocations.length; i++) {
                if (allocations[i] != null) {
                    allocations[i].destroy();
                    allocations[i] = null;
                }
            }
            return new AutoAlignmentByFeatureResult(0, 0, 0.0f, 1.0f);
        }


        final int max_match_dist_x = width;
        final int max_match_dist_y = height / 16;
        final int max_match_dist2 = max_match_dist_x * max_match_dist_x + max_match_dist_y * max_match_dist_y;
        if (MyDebug.LOG) {
            Log.d(TAG, "max_match_dist_x: " + max_match_dist_x);
            Log.d(TAG, "max_match_dist_y: " + max_match_dist_y);
            Log.d(TAG, "max_match_dist2: " + max_match_dist2);
        }
        List<FeatureMatch> matches = new ArrayList<>();
        for (int i = 0; i < points_arrays[0].length; i++) {
            int x0 = points_arrays[0][i].x;
            int y0 = points_arrays[0][i].y;
            for (int j = 0; j < points_arrays[1].length; j++) {
                int x1 = points_arrays[1][j].x;
                int y1 = points_arrays[1][j].y;
                // only consider a match if close enough in actual distance
                int dx = x1 - x0;
                int dy = y1 - y0;
                int dist2 = dx * dx + dy * dy;
                if (dist2 < max_match_dist2) {
                    FeatureMatch match = new FeatureMatch(i, j);
                    matches.add(match);
                }
            }
        }
        {
            final int wid = 2 * feature_descriptor_radius + 1;
            final int wid2 = wid * wid;
            int[] pixels0 = new int[points_arrays[0].length * wid2];
            int[] pixels1 = new int[points_arrays[1].length * wid2];
            for (int i = 0; i < points_arrays[0].length; i++) {
                int x = points_arrays[0][i].x;
                int y = points_arrays[0][i].y;
                bitmaps.get(0).getPixels(pixels0, i * wid2, wid, x - feature_descriptor_radius, y - feature_descriptor_radius, wid, wid);
            }
            for (int i = 0; i < points_arrays[1].length; i++) {
                int x = points_arrays[1][i].x;
                int y = points_arrays[1][i].y;
                bitmaps.get(1).getPixels(pixels1, i * wid2, wid, x - feature_descriptor_radius, y - feature_descriptor_radius, wid, wid);
            }
            for (int i = 0; i < pixels0.length; i++) {
                int pixel = pixels0[i];
                pixels0[i] = (int) (0.3 * Color.red(pixel) + 0.59 * Color.green(pixel) + 0.11 * Color.blue(pixel));
            }
            for (int i = 0; i < pixels1.length; i++) {
                int pixel = pixels1[i];
                pixels1[i] = (int) (0.3 * Color.red(pixel) + 0.59 * Color.green(pixel) + 0.11 * Color.blue(pixel));
            }

            final boolean use_smp = true;
            if (use_smp) {
                int n_threads = Math.min(matches.size(), 2);
                if (MyDebug.LOG)
                    Log.d(TAG, "n_threads: " + n_threads);
                ComputeDistancesBetweenMatchesThread[] threads = new ComputeDistancesBetweenMatchesThread[n_threads];
                int st_indx = 0;
                for (int i = 0; i < n_threads; i++) {
                    int nd_indx = (((i + 1) * matches.size()) / n_threads);
                    if (MyDebug.LOG)
                        Log.d(TAG, "thread " + i + " from " + st_indx + " to " + nd_indx);
                    threads[i] = new ComputeDistancesBetweenMatchesThread(matches, st_indx, nd_indx, feature_descriptor_radius, bitmaps, pixels0, pixels1);
                    st_indx = nd_indx;
                }
                // start threads
                if (MyDebug.LOG)
                    Log.d(TAG, "start threads");
                for (int i = 0; i < n_threads; i++) {
                    threads[i].start();
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "wait for threads to complete");
                try {
                    for (int i = 0; i < n_threads; i++) {
                        threads[i].join();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "ComputeDistancesBetweenMatchesThread threads interrupted");
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "threads completed");
            } else {
                int st_indx = 0, nd_indx = matches.size();
                computeDistancesBetweenMatches(matches, st_indx, nd_indx, feature_descriptor_radius, bitmaps, pixels0, pixels1);
            }
        }
        Collections.sort(matches);
        if (MyDebug.LOG) {
            FeatureMatch best_match = matches.get(0);
            FeatureMatch worst_match = matches.get(matches.size() - 1);
            Log.d(TAG, "best match between " + best_match.index0 + " and " + best_match.index1 + " distance: " + best_match.distance);
            Log.d(TAG, "worst match between " + worst_match.index0 + " and " + worst_match.index1 + " distance: " + worst_match.distance);
        }
        boolean[] rejected0 = new boolean[points_arrays[0].length];
        boolean[] has_matched0 = new boolean[points_arrays[0].length];
        boolean[] has_matched1 = new boolean[points_arrays[1].length];
        List<FeatureMatch> actual_matches = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++) {
            FeatureMatch match = matches.get(i);
            if (has_matched0[match.index0] || has_matched1[match.index1]) {
                continue;
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "    match between " + match.index0 + " and " + match.index1 + " distance: " + match.distance);
            }

            // Lowe's test
            boolean found = false;
            boolean reject = false;
            for (int j = i + 1; j < matches.size() && !found; j++) {
                FeatureMatch match2 = matches.get(j);
                if (match.index0 == match2.index0) {
                    found = true;
                    float ratio = match.distance / match2.distance;
                    if (MyDebug.LOG) {
                        Log.d(TAG, "        next best match for index0 " + match.index0 + " is with " + match2.index1 + " distance: " + match2.distance + " , ratio: " + ratio);
                    }
                    if (ratio + 1.0e-5 > 0.8f) {
                        if (MyDebug.LOG) {
                            Log.d(TAG, "        reject due to Lowe's test, ratio: " + ratio);
                        }
                        reject = true;
                    }
                }
            }
            if (reject) {
                has_matched0[match.index0] = true;
                rejected0[match.index0] = true;
                continue;
            }

            actual_matches.add(match);
            has_matched0[match.index0] = true;
            has_matched1[match.index1] = true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### autoAlignmentByFeature: time after initial matching: " + (System.currentTimeMillis() - time_s));
        if (MyDebug.LOG)
            Log.d(TAG, "### found: " + actual_matches.size() + " matches");
        Log.d(TAG, "### autoAlignmentByFeature: time after finding possible matches: " + (System.currentTimeMillis() - time_s));


        int n_matches = (int) (actual_matches.size() * 0.4) + 1;
        final int n_minimum_matches_c = 5;
        n_matches = Math.max(n_minimum_matches_c, n_matches);
        if (n_matches < actual_matches.size())
            actual_matches.subList(n_matches, actual_matches.size()).clear();
        has_matched0 = new boolean[points_arrays[0].length];
        has_matched1 = new boolean[points_arrays[1].length];
        for (FeatureMatch match : actual_matches) {
            has_matched0[match.index0] = true;
            has_matched1[match.index1] = true;
            if (MyDebug.LOG)
                Log.d(TAG, "    actual match between " + match.index0 + " and " + match.index1 + " distance: " + match.distance);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### autoAlignmentByFeature: time after choosing best matches: " + (System.currentTimeMillis() - time_s));

        if (actual_matches.size() == 0) {
            for (int i = 0; i < allocations.length; i++) {
                if (allocations[i] != null) {
                    allocations[i].destroy();
                    allocations[i] = null;
                }
            }

            return new AutoAlignmentByFeatureResult(0, 0, 0.0f, 1.0f);
        }

        final boolean use_ransac = true;
        final boolean estimate_rotation = true;
        final boolean estimate_y_scale = false;
        boolean use_rotation = false;
        boolean use_y_scale = false;
        final float max_y_scale = 1.05f + 1.0e-5f;
        final float min_rotation_dist = Math.max(5.0f, Math.max(width, height) / 4.0f);
        final float min_rotation_dist2 = min_rotation_dist * min_rotation_dist;

        List<FeatureMatch> ransac_matches = new ArrayList<>(); // used for debugging: the matches that were used to define the transform
        if (use_ransac) {
            List<FeatureMatch> best_inliers = new ArrayList<>();
            List<FeatureMatch> inliers = new ArrayList<>();
            final float max_inlier_dist = Math.max(5.01f, Math.max(width, height) / 100.0f);
            final float max_inlier_dist2 = max_inlier_dist * max_inlier_dist;
            for (int i = 0; i < actual_matches.size(); i++) {
                FeatureMatch match = actual_matches.get(i);
                {
                    int candidate_offset_x = points_arrays[1][match.index1].x - points_arrays[0][match.index0].x;
                    int candidate_offset_y = points_arrays[1][match.index1].y - points_arrays[0][match.index0].y;
                    inliers.clear();
                    for (FeatureMatch other_match : actual_matches) {
                        int x0 = points_arrays[0][other_match.index0].x;
                        int y0 = points_arrays[0][other_match.index0].y;
                        int x1 = points_arrays[1][other_match.index1].x;
                        int y1 = points_arrays[1][other_match.index1].y;
                        int transformed_x0 = x0 + candidate_offset_x;
                        int transformed_y0 = y0 + candidate_offset_y;
                        float dx = transformed_x0 - x1;
                        float dy = transformed_y0 - y1;
                        float error2 = dx * dx + dy * dy;
                        if (error2 + 1.0e-5 <= max_inlier_dist2) {
                            inliers.add(other_match);
                        }
                    }
                    if (inliers.size() > best_inliers.size()) {
                        ransac_matches.clear();
                        ransac_matches.add(match);
                        best_inliers.clear();
                        best_inliers.addAll(inliers);
                        use_rotation = false;
                        use_y_scale = false;
                        if (best_inliers.size() == actual_matches.size()) {
                            break;
                        }
                    }
                }

                if (estimate_rotation) {
                    for (int j = 0; j < i; j++) {
                        FeatureMatch match2 = actual_matches.get(j);
                        final int c0_x = (points_arrays[0][match.index0].x + points_arrays[0][match2.index0].x) / 2;
                        final int c0_y = (points_arrays[0][match.index0].y + points_arrays[0][match2.index0].y) / 2;
                        final int c1_x = (points_arrays[1][match.index1].x + points_arrays[1][match2.index1].x) / 2;
                        final int c1_y = (points_arrays[1][match.index1].y + points_arrays[1][match2.index1].y) / 2;
                        // model is a (scale about c0, followed by) rotation about c0, followed by translation
                        final float dx0 = (points_arrays[0][match.index0].x - points_arrays[0][match2.index0].x);
                        final float dy0 = (points_arrays[0][match.index0].y - points_arrays[0][match2.index0].y);
                        final float dx1 = (points_arrays[1][match.index1].x - points_arrays[1][match2.index1].x);
                        final float dy1 = (points_arrays[1][match.index1].y - points_arrays[1][match2.index1].y);
                        final float mag_sq0 = dx0 * dx0 + dy0 * dy0;
                        final float mag_sq1 = dx1 * dx1 + dy1 * dy1;
                        if (mag_sq0 < min_rotation_dist2 || mag_sq1 < min_rotation_dist2) {
                            continue;
                        }
                        final float min_height = 0.3f * height;
                        final float max_height = 0.7f * height;
                        if (points_arrays[0][match.index0].y < min_height || points_arrays[0][match.index0].y > max_height ||
                                points_arrays[1][match.index1].y < min_height || points_arrays[1][match.index1].y > max_height ||
                                points_arrays[0][match2.index0].y < min_height || points_arrays[0][match2.index0].y > max_height ||
                                points_arrays[1][match2.index1].y < min_height || points_arrays[1][match2.index1].y > max_height
                        ) {
                            continue;
                        }


                        float angle = (float) (Math.atan2(dy1, dx1) - Math.atan2(dy0, dx0));
                        if (angle < -Math.PI)
                            angle += 2.0f * Math.PI;
                        else if (angle > Math.PI)
                            angle -= 2.0f * Math.PI;
                        if (Math.abs(angle) > 30.0f * Math.PI / 180.0f) {
                            // reject too large angles
                            continue;
                        }

                        float y_scale = 1.0f;
                        boolean found_y_scale = false;
                        if (estimate_y_scale) {
                            //int transformed_dx0 = (int)(dx0 * Math.cos(angle) - dy0 * Math.sin(angle));
                            int transformed_dy0 = (int) (dx0 * Math.sin(angle) + dy0 * Math.cos(angle));
                            if (Math.abs(transformed_dy0) > min_rotation_dist && Math.abs(dy1) > min_rotation_dist) {
                                y_scale = dy1 / transformed_dy0;
                                if (y_scale <= max_y_scale && y_scale >= 1.0f / max_y_scale) {
                                    found_y_scale = true;
                                } else {
                                    y_scale = 1.0f;
                                }
                            }
                        }

                        // find the inliers from this
                        inliers.clear();
                        for (FeatureMatch other_match : actual_matches) {
                            int x0 = points_arrays[0][other_match.index0].x;
                            int y0 = points_arrays[0][other_match.index0].y;
                            int x1 = points_arrays[1][other_match.index1].x;
                            int y1 = points_arrays[1][other_match.index1].y;
                            x0 -= c0_x;
                            y0 -= c0_y;
                            //y0 *= y_scale;
                            int transformed_x0 = (int) (x0 * Math.cos(angle) - y0 * Math.sin(angle));
                            int transformed_y0 = (int) (x0 * Math.sin(angle) + y0 * Math.cos(angle));
                            transformed_y0 *= y_scale;
                            transformed_x0 += c1_x;
                            transformed_y0 += c1_y;

                            float dx = transformed_x0 - x1;
                            float dy = transformed_y0 - y1;
                            float error2 = dx * dx + dy * dy;
                            if (error2 + 1.0e-5 <= max_inlier_dist2) {
                                inliers.add(other_match);
                            }
                        }

                        if (inliers.size() > best_inliers.size() && inliers.size() >= 5) {
                            ransac_matches.clear();
                            ransac_matches.add(match);
                            ransac_matches.add(match2);
                            best_inliers.clear();
                            best_inliers.addAll(inliers);
                            use_rotation = true;
                            use_y_scale = found_y_scale;
                            if (best_inliers.size() == actual_matches.size()) {
                                if (MyDebug.LOG)
                                    Log.d(TAG, "all matches are inliers");
                                // no point trying any further
                                break;
                            }
                        }
                    }

                    if (best_inliers.size() == actual_matches.size()) {
                        if (MyDebug.LOG)
                            Log.d(TAG, "all matches are inliers");
                        // no point trying any further
                        break;
                    }
                }
            }
            actual_matches = best_inliers;
            if (MyDebug.LOG)
                Log.d(TAG, "### autoAlignmentByFeature: time after RANSAC: " + (System.currentTimeMillis() - time_s));
            if (MyDebug.LOG) {
                for (FeatureMatch match : actual_matches) {
                    Log.d(TAG, "    after ransac: actual match between " + match.index0 + " and " + match.index1 + " distance: " + match.distance);
                }
            }
        }

        Point[] centres = new Point[2];
        for (int i = 0; i < 2; i++) {
            centres[i] = new Point();
        }
        for (FeatureMatch match : actual_matches) {
            centres[0].x += points_arrays[0][match.index0].x;
            centres[0].y += points_arrays[0][match.index0].y;
            centres[1].x += points_arrays[1][match.index1].x;
            centres[1].y += points_arrays[1][match.index1].y;
        }
        for (int i = 0; i < 2; i++) {
            centres[i].x /= actual_matches.size();
            centres[i].y /= actual_matches.size();
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "centres[0]: " + centres[0].x + " , " + centres[0].y);
            Log.d(TAG, "centres[1]: " + centres[1].x + " , " + centres[1].y);
        }

        int offset_x = centres[1].x - centres[0].x;
        int offset_y = centres[1].y - centres[0].y;
        float rotation = 0.0f;
        float y_scale = 1.0f;

        if (estimate_rotation && use_rotation) {
            float angle_sum = 0.0f;
            int n_angles = 0;
            for (FeatureMatch match : actual_matches) {
                float dx0 = points_arrays[0][match.index0].x - centres[0].x;
                float dy0 = points_arrays[0][match.index0].y - centres[0].y;
                float dx1 = points_arrays[1][match.index1].x - centres[1].x;
                float dy1 = points_arrays[1][match.index1].y - centres[1].y;
                float mag_sq0 = dx0 * dx0 + dy0 * dy0;
                float mag_sq1 = dx1 * dx1 + dy1 * dy1;
                if (mag_sq0 < 1.0e-5 || mag_sq1 < 1.0e-5) {
                    continue;
                }
                float angle = (float) (Math.atan2(dy1, dx1) - Math.atan2(dy0, dx0));
                if (angle < -Math.PI)
                    angle += 2.0f * Math.PI;
                else if (angle > Math.PI)
                    angle -= 2.0f * Math.PI;
                if (MyDebug.LOG)
                    Log.d(TAG, "    match has angle: " + angle);
                angle_sum += angle;
                n_angles++;
            }
            if (n_angles > 0) {
                rotation = angle_sum / n_angles;
            }

            if (estimate_y_scale && use_y_scale) {
                float y_scale_sum = 0.0f;
                int n_y_scale = 0;
                for (FeatureMatch match : actual_matches) {
                    float dx0 = (points_arrays[0][match.index0].x - centres[0].x);
                    float dy0 = (points_arrays[0][match.index0].y - centres[0].y);
                    //float dx1 = (points_arrays[1][match.index1].x - centres[1].x);
                    float dy1 = (points_arrays[1][match.index1].y - centres[1].y);
                    int transformed_dy0 = (int) (dx0 * Math.sin(rotation) + dy0 * Math.cos(rotation));
                    if (Math.abs(transformed_dy0) > min_rotation_dist && Math.abs(dy1) > min_rotation_dist) {
                        float this_y_scale = dy1 / transformed_dy0;
                        y_scale_sum += this_y_scale;
                        n_y_scale++;
                        if (MyDebug.LOG)
                            Log.d(TAG, "    match has scale: " + this_y_scale);
                    }
                }
                if (n_y_scale > 0) {
                    y_scale = y_scale_sum / n_y_scale;
                }
            }

            float rotated_centre_x = (float) (centres[0].x * Math.cos(rotation) - centres[0].y * Math.sin(rotation));
            float rotated_centre_y = (float) (centres[0].x * Math.sin(rotation) + centres[0].y * Math.cos(rotation));
            rotated_centre_y *= y_scale;
            if (MyDebug.LOG) {
                Log.d(TAG, "offset_x before rotation: " + offset_x);
                Log.d(TAG, "offset_y before rotation: " + offset_y);
                Log.d(TAG, "rotated_centre: " + rotated_centre_x + " , " + rotated_centre_y);
            }
            offset_x += centres[0].x - rotated_centre_x;
            offset_y += centres[0].y - rotated_centre_y;

        }


        if (false && MyDebug.LOG) {
            Bitmap bitmap = Bitmap.createBitmap(2 * width, height, Bitmap.Config.ARGB_8888);
            Paint p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            Canvas canvas = new Canvas(bitmap);

            canvas.drawBitmap(bitmaps.get(0), 0, 0, p);
            canvas.drawBitmap(bitmaps.get(1), width, 0, p);

            // draw feature points
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < points_arrays[i].length; j++) {
                    int off_x = (i == 0) ? 0 : width;
                    boolean was_matched;
                    if (i == 0) {
                        was_matched = has_matched0[j];
                    } else {
                        was_matched = has_matched1[j];
                    }
                    if (i == 0 && rejected0[j])
                        p.setColor(Color.CYAN);
                    else
                        p.setColor(was_matched ? Color.YELLOW : Color.RED);
                    canvas.drawRect(points_arrays[i][j].x + off_x - feature_descriptor_radius - 1, points_arrays[i][j].y - feature_descriptor_radius - 1, points_arrays[i][j].x + off_x + feature_descriptor_radius + 1, points_arrays[i][j].y + feature_descriptor_radius + 1, p);
                }
            }
            // draw matches
            for (FeatureMatch match : actual_matches) {
                int x0 = points_arrays[0][match.index0].x;
                int y0 = points_arrays[0][match.index0].y;
                int x1 = points_arrays[1][match.index1].x;
                int y1 = points_arrays[1][match.index1].y;
                p.setColor(ransac_matches.contains(match) ? Color.BLUE : Color.MAGENTA);
                p.setAlpha((int) (255.0f * (1.0f - match.distance)));
                canvas.drawLine(x0, y0, width + x1, y1, p);

                // also draw where the match is actually translated to
                //int t_cx = (int)(x0 * Math.cos(rotation) - y_scale * y0 * Math.sin(rotation));
                //int t_cy = (int)(x0 * Math.sin(rotation) + y_scale * y0 * Math.cos(rotation));
                int t_cx = (int) (x0 * Math.cos(rotation) - y0 * Math.sin(rotation));
                int t_cy = (int) (x0 * Math.sin(rotation) + y0 * Math.cos(rotation));
                t_cy *= y_scale;
                t_cx += offset_x;
                t_cy += offset_y;
                // draw on right hand side
                t_cx += width;
                p.setColor(Color.GREEN);
                canvas.drawPoint(t_cx, t_cy, p);
            }
            p.setAlpha(255);

            // draw centres
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.CYAN);
            p.setAlpha(127);
            for (int i = 0; i < 2; i++) {
                int off_x = (i == 0) ? 0 : width;
                canvas.drawCircle(centres[i].x + off_x, centres[i].y, 5.0f, p);
                // draw the rotation and scale:
                int dir_r_x = 50, dir_r_y = 0;
                int dir_u_x = 0, dir_u_y = -50;
                if (i == 1) {
                    // transform
                    dir_r_y *= y_scale;
                    dir_u_y *= y_scale;
                    int n_dir_r_x = (int) (dir_r_x * Math.cos(rotation) - dir_r_y * Math.sin(rotation));
                    int n_dir_r_y = (int) (dir_r_x * Math.sin(rotation) + dir_r_y * Math.cos(rotation));
                    int n_dir_u_x = (int) (dir_u_x * Math.cos(rotation) - dir_u_y * Math.sin(rotation));
                    int n_dir_u_y = (int) (dir_u_x * Math.sin(rotation) + dir_u_y * Math.cos(rotation));
                    dir_r_x = n_dir_r_x;
                    dir_r_y = n_dir_r_y;
                    dir_u_x = n_dir_u_x;
                    dir_u_y = n_dir_u_y;
                }
                canvas.drawLine(centres[i].x + off_x, centres[i].y, centres[i].x + off_x + dir_r_x, centres[i].y + dir_r_y, p);
                canvas.drawLine(centres[i].x + off_x, centres[i].y, centres[i].x + off_x + dir_u_x, centres[i].y + dir_u_y, p);
            }
            // also draw a grid that shows the affect of the offset translation we've chosen
            final int n_x = 3;
            final int n_y = 10;
            p.setColor(Color.BLUE);
            p.setAlpha(127);
            for (int i = 0; i < n_x; i++) {
                int cx = (width * (i + 1)) / (n_x + 1);
                for (int j = 0; j < n_y; j++) {
                    int cy = (height * (j + 1)) / (n_y + 1);
                    for (int k = 0; k < 2; k++) {
                        int t_cx = cx, t_cy = cy;
                        if (k == 1) {
                            t_cx = (int) (cx * Math.cos(rotation) - cy * Math.sin(rotation));
                            t_cy = (int) (cx * Math.sin(rotation) + cy * Math.cos(rotation));
                            t_cy *= y_scale;
                            t_cx += offset_x;
                            t_cy += offset_y;
                            t_cx += width;
                        }
                        canvas.drawCircle(t_cx, t_cy, 5.0f, p);
                    }
                }
            }
            p.setAlpha(255);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/matched_bitmap_" + debug_index + ".png");
            try {
                OutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
                PhotoSelfieCameraActivity mActivity = (PhotoSelfieCameraActivity) context;
                mActivity.getStorageUtils().broadcastFile(file, true, false, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bitmap.recycle();
        }

        // free allocations
        for (int i = 0; i < allocations.length; i++) {
            if (allocations[i] != null) {
                allocations[i].destroy();
                allocations[i] = null;
            }
        }

        if (MyDebug.LOG)
            Log.d(TAG, "### autoAlignmentByFeature: total time: " + (System.currentTimeMillis() - time_s));
        return new AutoAlignmentByFeatureResult(offset_x, offset_y, rotation, y_scale);
    }

    private Bitmap blend_panorama_alpha(Bitmap lhs, Bitmap rhs) {
        int width = lhs.getWidth();
        int height = lhs.getHeight();
        if (width != rhs.getWidth()) {
            Log.e(TAG, "bitmaps have different widths");
            throw new RuntimeException();
        } else if (height != rhs.getHeight()) {
            Log.e(TAG, "bitmaps have different heights");
            throw new RuntimeException();
        }
        Paint p = new Paint();
        Rect rect = new Rect();
        Bitmap blended_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas blended_canvas = new Canvas(blended_bitmap);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        for (int x = 0; x < width; x++) {
            rect.set(x, 0, x + 1, height);
            float frac = (width - 1.0f - x) / (width - 1.0f);
            p.setAlpha((int) (255.0f * frac));
            blended_canvas.drawBitmap(lhs, rect, rect, p);

            frac = ((float) x) / (width - 1.0f);
            p.setAlpha((int) (255.0f * frac));
            blended_canvas.drawBitmap(rhs, rect, rect, p);
        }
        return blended_bitmap;
    }

    private static int nextMultiple(int value, int multiple) {
        int remainder = value % multiple;
        if (remainder > 0) {
            value += multiple - remainder;
        }
        return value;
    }

    private Bitmap createProjectedBitmap(final Rect src_rect_workspace, final Rect dst_rect_workspace, final Bitmap bitmap, final Paint p, final int bitmap_width, final int bitmap_height, final double camera_angle, final int centre_shift_x) {
        Bitmap projected_bitmap = Bitmap.createBitmap(bitmap_width, bitmap_height, Bitmap.Config.ARGB_8888);
        {
            // project
            Canvas projected_canvas = new Canvas(projected_bitmap);
            int prev_x = 0;
            int prev_y0 = -1, prev_y1 = -1;
            for (int x = 0; x < bitmap_width; x++) {
                float dx = (float) (x - (bitmap_width / 2 + centre_shift_x));
                float theta = (float) (dx * camera_angle) / (float) bitmap_width;
                float new_height = bitmap_height * (float) Math.cos(theta);

                int dst_y0 = (int) ((bitmap_height - new_height) / 2.0f + 0.5f);
                int dst_y1 = (int) ((bitmap_height + new_height) / 2.0f + 0.5f);

                final int y_tol = 1;
                if (x == 0) {
                    prev_y0 = dst_y0;
                    prev_y1 = dst_y1;
                } else if (Math.abs(dst_y0 - prev_y0) > y_tol || Math.abs(dst_y1 - prev_y1) > y_tol) {
                    src_rect_workspace.set(prev_x, 0, x, bitmap_height);
                    dst_rect_workspace.set(prev_x, dst_y0, x, dst_y1);
                    projected_canvas.drawBitmap(bitmap, src_rect_workspace, dst_rect_workspace, p);
                    prev_x = x;
                    prev_y0 = dst_y0;
                    prev_y1 = dst_y1;
                }

                if (x == bitmap_width - 1) {
                    src_rect_workspace.set(prev_x, 0, x + 1, bitmap_height);
                    dst_rect_workspace.set(prev_x, dst_y0, x + 1, dst_y1);
                    projected_canvas.drawBitmap(bitmap, src_rect_workspace, dst_rect_workspace, p);
                }

            }
        }
        return projected_bitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void renderPanoramaImage(final int i, final int n_bitmaps, final Rect src_rect_workspace, final Rect dst_rect_workspace,
                                     final Bitmap bitmap, final Paint p, final int bitmap_width, final int bitmap_height,
                                     final int blend_hwidth, final int slice_width, final int offset_x,
                                     final Bitmap panorama, final Canvas canvas, final int crop_x0, final int crop_y0,
                                     final int align_x, final int align_y, final int dst_offset_x, final int shift_stop_x, final int centre_shift_x,
                                     final double camera_angle, long time_s) {
        Bitmap projected_bitmap = createProjectedBitmap(src_rect_workspace, dst_rect_workspace, bitmap, p, bitmap_width, bitmap_height, camera_angle, centre_shift_x);

        if (i > 0 && blend_hwidth > 0) {
            if (MyDebug.LOG)
                Log.d(TAG, "### time before blending for " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
            // first blend right hand side of previous image with left hand side of new image
            final int blend_dimension = 0;

            int blend_width = nextMultiple(2 * blend_hwidth, blend_dimension);
            int blend_height = nextMultiple(bitmap_height, blend_dimension);

            Bitmap lhs = Bitmap.createBitmap(blend_width, blend_height, Bitmap.Config.ARGB_8888);
            {
                Canvas lhs_canvas = new Canvas(lhs);
                src_rect_workspace.set(offset_x + dst_offset_x - blend_hwidth, 0, offset_x + dst_offset_x + blend_hwidth, bitmap_height);
                // n.b., shouldn't shift by align_x, align_y
                src_rect_workspace.offset(-crop_x0, 0);
                dst_rect_workspace.set(0, 0, blend_width, blend_height);
                lhs_canvas.drawBitmap(panorama, src_rect_workspace, dst_rect_workspace, p);
            }

            Bitmap rhs = Bitmap.createBitmap(blend_width, blend_height, Bitmap.Config.ARGB_8888);
            {
                Canvas rhs_canvas = new Canvas(rhs);
                src_rect_workspace.set(offset_x - blend_hwidth, 0, offset_x + blend_hwidth, bitmap_height);
                src_rect_workspace.offset(align_x, align_y);
                dst_rect_workspace.set(0, -crop_y0, blend_width, blend_height - crop_y0);
                rhs_canvas.drawBitmap(projected_bitmap, src_rect_workspace, dst_rect_workspace, p);
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "lhs dimensions: " + lhs.getWidth() + " x " + lhs.getHeight());
                Log.d(TAG, "rhs dimensions: " + rhs.getWidth() + " x " + rhs.getHeight());
            }
            Bitmap blended_bitmap = blendPyramids(lhs, rhs);

            canvas.drawBitmap(blended_bitmap, offset_x + dst_offset_x - blend_hwidth - crop_x0, 0, p);

            lhs.recycle();
            rhs.recycle();
            blended_bitmap.recycle();
            if (MyDebug.LOG)
                Log.d(TAG, "### time after blending for " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
        }

        int start_x = blend_hwidth;
        int stop_x = slice_width + blend_hwidth;
        if (i == 0)
            start_x = -offset_x;
        if (i == n_bitmaps - 1) {
            stop_x = slice_width + offset_x;
            stop_x -= align_x; // to undo the shift of src_rect_workspace by align_x below
        }
        stop_x -= shift_stop_x;
        if (MyDebug.LOG) {
            Log.d(TAG, "    offset_x: " + offset_x);
            Log.d(TAG, "    dst_offset_x: " + dst_offset_x);
            Log.d(TAG, "    start_x: " + start_x);
            Log.d(TAG, "    stop_x: " + stop_x);
        }

        // draw rest of this image
        if (MyDebug.LOG)
            Log.d(TAG, "### time before drawing non-blended region for " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
        src_rect_workspace.set(offset_x + start_x, 0, offset_x + stop_x, bitmap_height);
        src_rect_workspace.offset(align_x, align_y);
        dst_rect_workspace.set(offset_x + dst_offset_x + start_x - crop_x0, -crop_y0, offset_x + dst_offset_x + stop_x - crop_x0, bitmap_height - crop_y0);
        if (MyDebug.LOG) {
            Log.d(TAG, "    src_rect_workspace: " + src_rect_workspace);
            Log.d(TAG, "    dst_rect_workspace: " + dst_rect_workspace);
        }
        canvas.drawBitmap(projected_bitmap, src_rect_workspace, dst_rect_workspace, p);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after drawing non-blended region for " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));


        projected_bitmap.recycle();

    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private float adjustExposuresLocal(List<Bitmap> bitmaps, int bitmap_width, int bitmap_height, int slice_width, long time_s) {
        final int exposure_hwidth = bitmap_width / 10;
        final int offset_x = (bitmap_width - slice_width) / 2;

        List<Float> relative_brightness = new ArrayList<>();
        float current_relative_brightness = 1.0f;
        relative_brightness.add(current_relative_brightness);
        float min_relative_brightness = current_relative_brightness;
        float max_relative_brightness = current_relative_brightness;

        if (MyDebug.LOG)
            Log.d(TAG, "### time before computing brightnesses: " + (System.currentTimeMillis() - time_s));

        for (int i = 0; i < bitmaps.size() - 1; i++) {
            // compute brightness difference between i-th and (i+1)-th images
            Bitmap bitmap_l = bitmaps.get(i);
            Bitmap bitmap_r = bitmaps.get(i + 1);
            if (MyDebug.LOG)
                Log.d(TAG, "### time before cropping bitmaps: " + (System.currentTimeMillis() - time_s));

            // scale down for performance
            Matrix scale_matrix = new Matrix();
            scale_matrix.postScale(0.5f, 0.5f);

            bitmap_l = Bitmap.createBitmap(bitmap_l, offset_x + slice_width - exposure_hwidth, 0, 2 * exposure_hwidth, bitmap_height, scale_matrix, true);
            bitmap_r = Bitmap.createBitmap(bitmap_r, offset_x - exposure_hwidth, 0, 2 * exposure_hwidth, bitmap_height, scale_matrix, true);
            if (MyDebug.LOG)
                Log.d(TAG, "### time after cropping bitmaps: " + (System.currentTimeMillis() - time_s));

            int[] histo_l = hdrProcessor.computeHistogram(bitmap_l, false);
            HDRProcessor.HistogramInfo histogramInfo_l = hdrProcessor.getHistogramInfo(histo_l);
            int[] histo_r = hdrProcessor.computeHistogram(bitmap_r, false);
            HDRProcessor.HistogramInfo histogramInfo_r = hdrProcessor.getHistogramInfo(histo_r);

            float brightness_scale = ((float) Math.max(histogramInfo_r.median_brightness, 1)) / (float) Math.max(histogramInfo_l.median_brightness, 1);
            current_relative_brightness *= brightness_scale;
            relative_brightness.add(current_relative_brightness);

            min_relative_brightness = Math.min(min_relative_brightness, current_relative_brightness);
            max_relative_brightness = Math.max(max_relative_brightness, current_relative_brightness);

            if (bitmap_l != bitmaps.get(i))
                bitmap_l.recycle();
            if (bitmap_r != bitmaps.get(i + 1))
                bitmap_r.recycle();
        }

        float ratio_brightnesses = (max_relative_brightness / min_relative_brightness);
        List<HDRProcessor.HistogramInfo> histogramInfos = new ArrayList<>();
        float mean_median_brightness = 0.0f; // mean of the global median brightnesse
        float mean_equalised_brightness = 0.0f; // mean of the brightnesses if all adjusted to match exposure of the first image
        for (int i = 0; i < bitmaps.size(); i++) {
            Bitmap bitmap = bitmaps.get(i);
            int[] histo = hdrProcessor.computeHistogram(bitmap, false);
            HDRProcessor.HistogramInfo histogramInfo = hdrProcessor.getHistogramInfo(histo);
            histogramInfos.add(histogramInfo);
            mean_median_brightness += histogramInfo.median_brightness;
            float equalised_brightness = histogramInfo.median_brightness / relative_brightness.get(i);
            mean_equalised_brightness += equalised_brightness;
            if (MyDebug.LOG) {
                Log.d(TAG, "image " + i + " has median brightness " + histogramInfo.median_brightness);
                Log.d(TAG, "    and equalised_brightness " + equalised_brightness);
            }
        }
        mean_median_brightness /= bitmaps.size();
        mean_equalised_brightness /= bitmaps.size();
        if (MyDebug.LOG) {
            Log.d(TAG, "mean_median_brightness: " + mean_median_brightness);
            Log.d(TAG, "mean_equalised_brightness: " + mean_equalised_brightness);
        }

        float avg_relative_brightness = mean_median_brightness / Math.max(mean_equalised_brightness, 1.0f);

        float min_preferred_scale = 1000.0f, max_preferred_scale = 0.0f;
        for (int i = 0; i < bitmaps.size(); i++) {
            if (MyDebug.LOG)
                Log.d(TAG, "    adjust exposure for image: " + i);

            Bitmap bitmap = bitmaps.get(i);
            HDRProcessor.HistogramInfo histogramInfo = histogramInfos.get(i);

            int brightness_target = (int) (histogramInfo.median_brightness * avg_relative_brightness / relative_brightness.get(i) + 0.1f);
            brightness_target = Math.min(255, brightness_target);

            min_preferred_scale = Math.min(min_preferred_scale, brightness_target / (float) histogramInfo.median_brightness);
            max_preferred_scale = Math.max(max_preferred_scale, brightness_target / (float) histogramInfo.median_brightness);
            int min_brightness = (int) (histogramInfo.median_brightness * 0.5f + 0.5f);
            int max_brightness = (int) (histogramInfo.median_brightness * 2.0f + 0.5f);
            int this_brightness_target = brightness_target;
            this_brightness_target = Math.max(this_brightness_target, min_brightness);
            this_brightness_target = Math.min(this_brightness_target, max_brightness);
            if (MyDebug.LOG) {
                Log.d(TAG, "    brightness_target: " + brightness_target);
                Log.d(TAG, "    preferred brightness scale: " + brightness_target / (float) histogramInfo.median_brightness);
                Log.d(TAG, "    this_brightness_target: " + this_brightness_target);
                Log.d(TAG, "    actual brightness scale: " + this_brightness_target / (float) histogramInfo.median_brightness);
            }

            hdrProcessor.brightenImage(bitmap, histogramInfo.median_brightness, histogramInfo.max_brightness, this_brightness_target);
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "min_preferred_scale: " + min_preferred_scale);
            Log.d(TAG, "max_preferred_scale: " + max_preferred_scale);
            Log.d(TAG, "### time after adjusting brightnesses: " + (System.currentTimeMillis() - time_s));
        }
        return ratio_brightnesses;
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void computePanoramaTransforms(List<Matrix> cumulative_transforms, List<Integer> align_x_values, List<Integer> dst_offset_x_values,
                                           List<Bitmap> bitmaps, final int bitmap_width, final int bitmap_height,
                                           final int offset_x, final int slice_width, final int align_hwidth,
                                           long time_s) throws PanoramaProcessorException {
        Matrix cumulative_transform = new Matrix();
        int align_x = 0, align_y = 0;
        int dst_offset_x = 0;
        List<Integer> align_y_values = new ArrayList<>();

        final boolean use_auto_align = true;
        //final boolean use_auto_align = false;

        for (int i = 0; i < bitmaps.size(); i++) {
            if (MyDebug.LOG)
                Log.d(TAG, "process bitmap: " + i);

            double angle_z = 0.0;

            if (use_auto_align && i > 0) {
                List<Bitmap> alignment_bitmaps = new ArrayList<>();

                final boolean use_align_by_feature = true;
                float align_downsample = 1.0f;
                if (use_align_by_feature) {
                    align_downsample = bitmap_height / 520.0f;
                    for (int k = 0, power = 1; k <= 4; k++, power *= 2) {
                        double ratio = power / align_downsample;
                        if (ratio >= 0.95f && ratio <= 1.05f) {
                            align_downsample = power;
                            if (MyDebug.LOG)
                                Log.d(TAG, "snapped downscale to: " + align_downsample);
                            break;
                        }
                    }
                }

                Matrix align_scale_matrix = new Matrix();
                align_scale_matrix.postScale(1.0f / align_downsample, 1.0f / align_downsample);
                alignment_bitmaps.add(Bitmap.createBitmap(bitmaps.get(i), align_x + offset_x - align_hwidth, (bitmap_height - 0) / 2, 2 * align_hwidth, 0, align_scale_matrix, true));
                alignment_bitmaps.add(Bitmap.createBitmap(bitmaps.get(i - 1), align_x + offset_x + slice_width - align_hwidth, (bitmap_height - 0) / 2, 2 * align_hwidth, 0, align_scale_matrix, true));

                int this_align_x, this_align_y;
                float y_scale = 1.0f;
                if (MyDebug.LOG)
                    Log.d(TAG, "### time before auto-alignment for " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
                if (use_align_by_feature) {
                    AutoAlignmentByFeatureResult res = autoAlignmentByFeature(alignment_bitmaps.get(0).getWidth(), alignment_bitmaps.get(0).getHeight(), alignment_bitmaps, i);
                    this_align_x = res.offset_x;
                    this_align_y = res.offset_y;
                    angle_z = res.rotation;
                    y_scale = res.y_scale;
                } else {
                    final boolean use_mtb = false;
                    int[] offsets_x = new int[alignment_bitmaps.size()];
                    int[] offsets_y = new int[alignment_bitmaps.size()];
                    hdrProcessor.autoAlignment(offsets_x, offsets_y, alignment_bitmaps.get(0).getWidth(), alignment_bitmaps.get(0).getHeight(), alignment_bitmaps, 0, use_mtb, 8);
                    this_align_x = offsets_x[1];
                    this_align_y = offsets_y[1];
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "### time after auto-alignment for " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
                this_align_x *= align_downsample;
                this_align_y *= align_downsample;
                for (Bitmap alignment_bitmap : alignment_bitmaps) {
                    alignment_bitmap.recycle();
                }
                alignment_bitmaps.clear();
                if (MyDebug.LOG) {
                    Log.d(TAG, "    this_align_x: " + this_align_x);
                    Log.d(TAG, "    this_align_y: " + this_align_y);
                }

                Matrix this_transform = new Matrix();
                this_transform.postRotate((float) Math.toDegrees(angle_z), align_x + offset_x - align_hwidth, 0);
                this_transform.postScale(1.0f, y_scale);
                this_transform.postTranslate(this_align_x, this_align_y);

                {
                    cumulative_transform.preTranslate(slice_width, 0.0f);
                    cumulative_transform.postTranslate(-slice_width, 0.0f);

                    cumulative_transform.preConcat(this_transform);
                }

                {
                    float[] points = new float[2];
                    points[0] = bitmap_width / 2.0f;
                    points[1] = bitmap_height / 2.0f;
                    cumulative_transform.mapPoints(points);
                    float trans_x = points[0] - bitmap_width / 2.0f;
                    align_x = -(int) trans_x;
                }

                if (MyDebug.LOG) {
                    Log.d(TAG, "    align_x is now: " + align_x);
                    Log.d(TAG, "    align_y is now: " + align_y);
                }
            }

            align_x_values.add(align_x);
            align_y_values.add(align_y);
            dst_offset_x_values.add(dst_offset_x);
            cumulative_transforms.add(new Matrix(cumulative_transform));

            {
                dst_offset_x += slice_width;
                // set back to zero after we've saved them, so we don't use them in the later iterations of this loop
                align_x = 0;
                align_y = 0;
            }
            if (MyDebug.LOG)
                Log.d(TAG, "    dst_offset_x is now: " + dst_offset_x);

            if (MyDebug.LOG)
                Log.d(TAG, "### time after processing " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
        }
    }

    private void adjustPanoramaTransforms(List<Bitmap> bitmaps, List<Matrix> cumulative_transforms,
                                          int panorama_width, int slice_width, int bitmap_width, int bitmap_height) {
        float[] values = new float[9];

        float min_rotation = 1000, max_rotation = -1000;
        for (int i = 0; i < bitmaps.size(); i++) {
            cumulative_transforms.get(i).getValues(values);
            float rotation = (float) Math.toDegrees(Math.atan2(values[Matrix.MSKEW_X], values[Matrix.MSCALE_X]));
            if (MyDebug.LOG)
                Log.d(TAG, "bitmap " + i + " has rotation " + rotation + " degrees");
            min_rotation = Math.min(min_rotation, rotation);
            max_rotation = Math.max(max_rotation, rotation);
        }

        float[] points = new float[2];
        points[0] = 0.0f;
        points[1] = bitmap_height / 2.0f;
        cumulative_transforms.get(0).mapPoints(points);
        float x0 = points[0];
        float y0 = points[1];
        points[0] = bitmap_width - 1.0f;
        points[1] = bitmap_height / 2.0f;
        cumulative_transforms.get(cumulative_transforms.size() - 1).mapPoints(points);
        float x1 = points[0] + (cumulative_transforms.size() - 1) * slice_width;
        float y1 = points[1];
        float dx = x1 - x0;
        float dy = y1 - y0;
        float mid_rotation = -(float) Math.toDegrees(Math.atan2(dy, dx));
        if (MyDebug.LOG) {
            Log.d(TAG, "x0: " + x0);
            Log.d(TAG, "y0: " + y0);
            Log.d(TAG, "x1: " + x1);
            Log.d(TAG, "y1: " + y1);
            Log.d(TAG, "dx: " + dx);
            Log.d(TAG, "dy: " + dy);
            Log.d(TAG, "mid_rotation: " + mid_rotation + " degrees");
        }
        mid_rotation = Math.max(mid_rotation, min_rotation);
        mid_rotation = Math.min(mid_rotation, max_rotation);
        if (MyDebug.LOG) {
            Log.d(TAG, "limited mid_rotation to: " + mid_rotation + " degrees");
        }

        for (int i = 0; i < bitmaps.size(); i++) {
            float centre_x = panorama_width / 2.0f - i * slice_width;
            float centre_y = bitmap_height / 2.0f;
            // apply a post rotate of mid_rotation clockwise about (centre_x, centre_y)
            cumulative_transforms.get(i).postRotate(mid_rotation, centre_x, centre_y);
            {
                cumulative_transforms.get(i).getValues(values);
                float rotation = (float) Math.toDegrees(Math.atan2(values[Matrix.MSKEW_X], values[Matrix.MSCALE_X]));
                if (MyDebug.LOG)
                    Log.d(TAG, "bitmap " + i + " now has rotation " + rotation + " degrees");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void renderPanorama(List<Bitmap> bitmaps, int bitmap_width, int bitmap_height,
                                List<Matrix> cumulative_transforms, List<Integer> align_x_values, List<Integer> dst_offset_x_values,
                                final int blend_hwidth, final int slice_width, final int offset_x,
                                final Bitmap panorama, final int crop_x0, final int crop_y0,
                                final double camera_angle, long time_s) {

        Rect src_rect = new Rect();
        Rect dst_rect = new Rect();
        //Paint p = new Paint();
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        Canvas canvas = new Canvas(panorama);

        for (int i = 0; i < bitmaps.size(); i++) {
            if (MyDebug.LOG)
                Log.d(TAG, "render bitmap: " + i);
            Bitmap bitmap = bitmaps.get(i);
            int align_x = align_x_values.get(i);
            //int align_y = align_y_values.get(i);
            int align_y = 0;
            int dst_offset_x = dst_offset_x_values.get(i);

            boolean free_bitmap = false;
            int shift_stop_x = align_x;
            int centre_shift_x;

            {
                final boolean shift_transition = true;
                //final boolean shift_transition = false;
                centre_shift_x = -align_x;
                align_x = 0;
                //align_y = 0;
                if (!shift_transition) {
                    shift_stop_x = 0;
                }

                if (i != 0 && shift_transition) {
                    int shift_start_x = align_x_values.get(i - 1); // +ve means shift to the left
                    dst_offset_x -= shift_start_x;
                    align_x = -shift_start_x;
                    shift_stop_x -= shift_start_x;
                }

                if (align_x != 0) {
                    float[] points = new float[2];
                    points[0] = bitmap_width / 2.0f;
                    points[1] = bitmap_height / 2.0f;
                    cumulative_transforms.get(i).mapPoints(points);
                    int trans_x = (int) (points[0] - bitmap_width / 2.0f);

                    int bake_trans_x = -align_x;
                    if (i == bitmaps.size() - 1 && trans_x < 0 && bake_trans_x + trans_x > 0) {
                        bake_trans_x = -trans_x;
                    }

                    cumulative_transforms.get(i).postTranslate(bake_trans_x, 0.0f);
                    centre_shift_x += bake_trans_x;
                    align_x += bake_trans_x;
                }

                {
                    Bitmap rotated_bitmap = Bitmap.createBitmap(bitmap_width, bitmap_height, Bitmap.Config.ARGB_8888);
                    Canvas rotated_canvas = new Canvas(rotated_bitmap);
                    rotated_canvas.save();

                    rotated_canvas.setMatrix(cumulative_transforms.get(i));

                    rotated_canvas.drawBitmap(bitmap, 0, 0, p);
                    rotated_canvas.restore();

                    bitmap = rotated_bitmap;
                    free_bitmap = true;
                }
            }

            renderPanoramaImage(i, bitmaps.size(), src_rect, dst_rect,
                    bitmap, p, bitmap_width, bitmap_height,
                    blend_hwidth, slice_width, offset_x,
                    panorama, canvas, crop_x0, crop_y0,
                    align_x, align_y, dst_offset_x, shift_stop_x, centre_shift_x,
                    camera_angle, time_s);

            if (free_bitmap) {
                bitmap.recycle();
            }

            if (MyDebug.LOG)
                Log.d(TAG, "### time after rendering " + i + "th bitmap: " + (System.currentTimeMillis() - time_s));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Bitmap panorama(List<Bitmap> bitmaps, float panorama_pics_per_screen, float camera_angle_y, final boolean crop) throws PanoramaProcessorException {
        if (MyDebug.LOG) {
            Log.d(TAG, "panorama");
            Log.d(TAG, "camera_angle_y: " + camera_angle_y);
        }

        long time_s = 0;
        if (MyDebug.LOG)
            time_s = System.currentTimeMillis();

        int bitmap_width = bitmaps.get(0).getWidth();
        int bitmap_height = bitmaps.get(0).getHeight();
        if (MyDebug.LOG) {
            Log.d(TAG, "bitmap_width: " + bitmap_width);
            Log.d(TAG, "bitmap_height: " + bitmap_height);
        }

        for (int i = 1; i < bitmaps.size(); i++) {
            Bitmap bitmap = bitmaps.get(i);
            if (bitmap.getWidth() != bitmap_width || bitmap.getHeight() != bitmap_height) {
                Log.e(TAG, "bitmaps not of equal sizes");
                throw new PanoramaProcessorException(PanoramaProcessorException.UNEQUAL_SIZES);
            }
        }
        final int slice_width = (int) (bitmap_width / panorama_pics_per_screen);
        if (MyDebug.LOG)
            Log.d(TAG, "slice_width: " + slice_width);

        final double camera_angle = Math.toRadians(camera_angle_y);
        if (MyDebug.LOG) {
            Log.d(TAG, "camera_angle_y: " + camera_angle_y);
            Log.d(TAG, "camera_angle: " + camera_angle);
        }
        final int offset_x = (bitmap_width - slice_width) / 2;
        final int blend_hwidth = nextMultiple((int) (bitmap_width / 6.1f + 0.5f), 0);
        final int align_hwidth = bitmap_width / 10;
        if (MyDebug.LOG) {
            Log.d(TAG, "    blend_hwidth: " + blend_hwidth);
            Log.d(TAG, "    align_hwidth: " + align_hwidth);
        }

        List<Matrix> cumulative_transforms = new ArrayList<>(); // i-th entry is the transform to apply to the i-th bitmap so that it's aligned to the same space as the 1st bitmap

        List<Integer> align_x_values = new ArrayList<>();
        List<Integer> dst_offset_x_values = new ArrayList<>();

        computePanoramaTransforms(cumulative_transforms, align_x_values, dst_offset_x_values, bitmaps,
                bitmap_width, bitmap_height, offset_x, slice_width, align_hwidth, time_s);

        int panorama_width = (bitmaps.size() * slice_width + 2 * offset_x);
        if (MyDebug.LOG) {
            Log.d(TAG, "original panorama_width: " + panorama_width);
        }

        adjustPanoramaTransforms(bitmaps, cumulative_transforms, panorama_width, slice_width, bitmap_width, bitmap_height);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after adjusting transforms: " + (System.currentTimeMillis() - time_s));

        float ratio_brightnesses = adjustExposuresLocal(bitmaps, bitmap_width, bitmap_height, slice_width, time_s);

        int panorama_height = bitmap_height;
        int crop_x0 = 0;
        int crop_y0 = 0;

        if (crop) {
            int crop_x1 = bitmap_width - 1;
            int crop_y1 = bitmap_height - 1;
            for (int i = 0; i < bitmaps.size(); i++) {
                float[] points = new float[8];

                points[0] = 0.0f;
                points[1] = 0.0f;

                points[2] = bitmap_width - 1.0f;
                points[3] = 0.0f;

                points[4] = 0.0f;
                points[5] = bitmap_height - 1.0f;

                points[6] = bitmap_width - 1.0f;
                points[7] = bitmap_height - 1.0f;

                cumulative_transforms.get(i).mapPoints(points);

                crop_y0 = Math.max(crop_y0, (int) points[1]);
                crop_y0 = Math.max(crop_y0, (int) points[3]);

                crop_y1 = Math.min(crop_y1, (int) points[5]);
                crop_y1 = Math.min(crop_y1, (int) points[7]);

                if (MyDebug.LOG) {
                    Log.d(TAG, "i: " + i);
                    Log.d(TAG, "    points[0]: " + points[0]);
                    Log.d(TAG, "    points[1]: " + points[1]);
                    Log.d(TAG, "    points[2]: " + points[2]);
                    Log.d(TAG, "    points[3]: " + points[3]);
                    Log.d(TAG, "    points[4]: " + points[4]);
                    Log.d(TAG, "    points[5]: " + points[5]);
                    Log.d(TAG, "    points[6]: " + points[6]);
                    Log.d(TAG, "    points[7]: " + points[7]);
                }
                if (i == 0) {
                    crop_x0 = Math.max(crop_x0, (int) points[0]);
                    crop_x0 = Math.max(crop_x0, (int) points[4]);
                }
                if (i == bitmaps.size() - 1) {
                    crop_x1 = Math.min(crop_x1, (int) points[2]);
                    crop_x1 = Math.min(crop_x1, (int) points[6]);
                }
            }

            panorama_width -= (bitmap_width - 1) - crop_x1;
            panorama_width -= crop_x0;
            if (MyDebug.LOG) {
                Log.d(TAG, "crop_x0: " + crop_x0);
                Log.d(TAG, "crop_x1: " + crop_x1);
                Log.d(TAG, "panorama_width: " + panorama_width);
            }

            float theta = (float) ((bitmap_width / 2) * camera_angle) / (float) bitmap_width;
            float yscale = (float) Math.cos(theta);
            crop_y0 = (int) (bitmap_height / 2.0f + yscale * (crop_y0 - bitmap_height / 2.0f) + 0.5f);
            crop_y1 = (int) (bitmap_height / 2.0f + yscale * (crop_y1 - bitmap_height / 2.0f) + 0.5f);

            panorama_height = crop_y1 - crop_y0 + 1;
        }

        Bitmap panorama = Bitmap.createBitmap(panorama_width, panorama_height, Bitmap.Config.ARGB_8888);

        if (MyDebug.LOG)
            Log.d(TAG, "### time before rendering bitmaps: " + (System.currentTimeMillis() - time_s));
        renderPanorama(bitmaps, bitmap_width, bitmap_height, cumulative_transforms, align_x_values, dst_offset_x_values,
                blend_hwidth, slice_width, offset_x, panorama, crop_x0, crop_y0, camera_angle, time_s);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after rendering bitmaps: " + (System.currentTimeMillis() - time_s));

        for (Bitmap bitmap : bitmaps) {
            bitmap.recycle();
        }
        bitmaps.clear();

        if (ratio_brightnesses >= 3.0f) {
            Allocation allocation = Allocation.createFromBitmap(rs, panorama);
            hdrProcessor.adjustHistogram(allocation, allocation, panorama.getWidth(), panorama.getHeight(), 0.25f, 1, true, time_s);
            allocation.copyTo(panorama);
            allocation.destroy();
            if (MyDebug.LOG)
                Log.d(TAG, "### time after copying to bitmap: " + (System.currentTimeMillis() - time_s));
        }

        freeScripts();

        return panorama;
    }

}

