package com.picstar.picstarapp.campkg.others;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSInvalidStateException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptIntrinsicHistogram;
//import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.Type;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class HDRProcessor {
    private static final String TAG = "HDRProcessor";
    private final Context context;
    private final boolean is_test;
    private RenderScript rs; // lazily created, so we don't take up resources if application isn't using HDR
    private ScriptC_process_avg processAvgScript;
    private ScriptC_create_mtb createMTBScript;
    private ScriptC_align_mtb alignMTBScript;
    public int[] offsets_x = null;
    public int[] offsets_y = null;

    private enum HDRAlgorithm {
        HDRALGORITHM_STANDARD,
        HDRALGORITHM_SINGLE_IMAGE
    }

    public enum TonemappingAlgorithm {
        TONEMAPALGORITHM_CLAMP,
        TONEMAPALGORITHM_EXPONENTIAL,
        TONEMAPALGORITHM_REINHARD,
        TONEMAPALGORITHM_FILMIC,
        TONEMAPALGORITHM_ACES
    }

    public enum DROTonemappingAlgorithm {
        DROALGORITHM_NONE,
        DROALGORITHM_GAINGAMMA
    }

    public HDRProcessor(Context context, boolean is_test) {
        this.context = context;
        this.is_test = is_test;
    }

    private void freeScripts() {
        if (MyDebug.LOG)
            Log.d(TAG, "freeScripts");
        processAvgScript = null;
        createMTBScript = null;
        alignMTBScript = null;
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

    private static class ResponseFunction {
        float parameter_A;
        float parameter_B;

        private ResponseFunction(float parameter_A, float parameter_B) {
            this.parameter_A = parameter_A;
            this.parameter_B = parameter_B;
        }

        static ResponseFunction createIdentity() {
            return new ResponseFunction(1.0f, 0.0f);
        }

        ResponseFunction(Context context, int id, List<Double> x_samples, List<Double> y_samples, List<Double> weights) {
            if (MyDebug.LOG)
                Log.d(TAG, "ResponseFunction");

            if (x_samples.size() != y_samples.size()) {
                if (MyDebug.LOG)
                    Log.e(TAG, "unequal number of samples");
                // throw RuntimeException, as this is a programming error
                throw new RuntimeException();
            } else if (x_samples.size() != weights.size()) {
                if (MyDebug.LOG)
                    Log.e(TAG, "unequal number of samples");
                // throw RuntimeException, as this is a programming error
                throw new RuntimeException();
            } else if (x_samples.size() <= 3) {
                if (MyDebug.LOG)
                    Log.e(TAG, "not enough samples");
                // throw RuntimeException, as this is a programming error
                throw new RuntimeException();
            }

            // linear Y = AX + B
            boolean done = false;
            double sum_wx = 0.0;
            double sum_wx2 = 0.0;
            double sum_wxy = 0.0;
            double sum_wy = 0.0;
            double sum_w = 0.0;
            for (int i = 0; i < x_samples.size(); i++) {
                double x = x_samples.get(i);
                double y = y_samples.get(i);
                double w = weights.get(i);
                sum_wx += w * x;
                sum_wx2 += w * x * x;
                sum_wxy += w * x * y;
                sum_wy += w * y;
                sum_w += w;
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "sum_wx = " + sum_wx);
                Log.d(TAG, "sum_wx2 = " + sum_wx2);
                Log.d(TAG, "sum_wxy = " + sum_wxy);
                Log.d(TAG, "sum_wy = " + sum_wy);
                Log.d(TAG, "sum_w = " + sum_w);
            }
            double A_numer = sum_wy * sum_wx - sum_w * sum_wxy;
            double A_denom = sum_wx * sum_wx - sum_w * sum_wx2;
            if (MyDebug.LOG) {
                Log.d(TAG, "A_numer = " + A_numer);
                Log.d(TAG, "A_denom = " + A_denom);
            }
            if (Math.abs(A_denom) < 1.0e-5) {
                if (MyDebug.LOG)
                    Log.e(TAG, "denom too small");
            } else {
                parameter_A = (float) (A_numer / A_denom);
                parameter_B = (float) ((sum_wy - parameter_A * sum_wx) / sum_w);
                if (MyDebug.LOG) {
                    Log.d(TAG, "parameter_A = " + parameter_A);
                    Log.d(TAG, "parameter_B = " + parameter_B);
                }
                if (parameter_A < 1.0e-5) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "parameter A too small or negative: " + parameter_A);
                } else if (parameter_B < 1.0e-5) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "parameter B too small or negative: " + parameter_B);
                } else {
                    done = true;
                }
            }

            if (!done) {
                if (MyDebug.LOG)
                    Log.e(TAG, "falling back to linear Y = AX");
                // linear Y = AX
                double numer = 0.0;
                double denom = 0.0;
                for (int i = 0; i < x_samples.size(); i++) {
                    double x = x_samples.get(i);
                    double y = y_samples.get(i);
                    double w = weights.get(i);
                    numer += w * x * y;
                    denom += w * x * x;
                }
                if (MyDebug.LOG) {
                    Log.d(TAG, "numer = " + numer);
                    Log.d(TAG, "denom = " + denom);
                }

                if (denom < 1.0e-5) {
                    if (MyDebug.LOG)
                        Log.e(TAG, "denom too small");
                    parameter_A = 1.0f;
                } else {
                    parameter_A = (float) (numer / denom);
                    // we don't want a function that is not monotonic!
                    if (parameter_A < 1.0e-5) {
                        if (MyDebug.LOG)
                            Log.e(TAG, "parameter A too small or negative: " + parameter_A);
                        parameter_A = 1.0e-5f;
                    }
                }
                parameter_B = 0.0f;
            }

            if (MyDebug.LOG) {
                Log.d(TAG, "parameter_A = " + parameter_A);
                Log.d(TAG, "parameter_B = " + parameter_B);
            }

            if (MyDebug.LOG) {
                // log samples to a CSV file
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "/net.sourceforge.opencamera.hdr_samples_" + id + ".csv");
                if (file.exists()) {
                    if (!file.delete()) {
                        // keep FindBugs happy by checking return argument
                        Log.e(TAG, "failed to delete csv file");
                    }
                }
                FileWriter writer = null;
                try {
                    writer = new FileWriter(file);
                    //writer.append("Parameter," + parameter + "\n");
                    writer.append("Parameters," + parameter_A + "," + parameter_B + "\n");
                    writer.append("X,Y,Weight\n");
                    for (int i = 0; i < x_samples.size(); i++) {
                        //Log.d(TAG, "log: " + i + " / " + x_samples.size());
                        double x = x_samples.get(i);
                        double y = y_samples.get(i);
                        double w = weights.get(i);
                        writer.append(x + "," + y + "," + w + "\n");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "failed to open csv file");
                    e.printStackTrace();
                } finally {
                    try {
                        if (writer != null)
                            writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "failed to close csv file");
                        e.printStackTrace();
                    }
                }
                MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
            }
        }
    }

    public interface SortCallback {

        void sortOrder(List<Integer> sort_order);
    }

     @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void processHDR(List<Bitmap> bitmaps, boolean release_bitmaps, Bitmap output_bitmap, boolean assume_sorted, SortCallback sort_cb, float hdr_alpha, int n_tiles, boolean ce_preserve_blacks, TonemappingAlgorithm tonemapping_algorithm, DROTonemappingAlgorithm dro_tonemapping_algorithm) throws HDRProcessorException {
        if (MyDebug.LOG)
            Log.d(TAG, "processHDR");
        if (!assume_sorted && !release_bitmaps) {
            if (MyDebug.LOG)
                Log.d(TAG, "take a copy of bitmaps array");
            // if !release_bitmaps, then we shouldn't be modifying the input bitmaps array - but if !assume_sorted, we need to sort them
            // so make sure we take a copy
            bitmaps = new ArrayList<>(bitmaps);
        }
        int n_bitmaps = bitmaps.size();
        //if( n_bitmaps != 1 && n_bitmaps != 3 && n_bitmaps != 5 && n_bitmaps != 7 ) {
        if (n_bitmaps < 1 || n_bitmaps > 7) {
            if (MyDebug.LOG)
                Log.e(TAG, "n_bitmaps not supported: " + n_bitmaps);
            throw new HDRProcessorException(HDRProcessorException.INVALID_N_IMAGES);
        }
        for (int i = 1; i < n_bitmaps; i++) {
            if (bitmaps.get(i).getWidth() != bitmaps.get(0).getWidth() ||
                    bitmaps.get(i).getHeight() != bitmaps.get(0).getHeight()) {
                if (MyDebug.LOG) {
                    Log.e(TAG, "bitmaps not of same resolution");
                    for (int j = 0; j < n_bitmaps; j++) {
                        Log.e(TAG, "bitmaps " + j + " : " + bitmaps.get(j).getWidth() + " x " + bitmaps.get(j).getHeight());
                    }
                }
                throw new HDRProcessorException(HDRProcessorException.UNEQUAL_SIZES);
            }
        }

        final HDRAlgorithm algorithm = n_bitmaps == 1 ? HDRAlgorithm.HDRALGORITHM_SINGLE_IMAGE : HDRAlgorithm.HDRALGORITHM_STANDARD;

        switch (algorithm) {
            case HDRALGORITHM_SINGLE_IMAGE:
                if (!assume_sorted && sort_cb != null) {
                    List<Integer> sort_order = new ArrayList<>();
                    sort_order.add(0);
                    sort_cb.sortOrder(sort_order);
                }
                processSingleImage(bitmaps, release_bitmaps, output_bitmap, hdr_alpha, n_tiles, ce_preserve_blacks, dro_tonemapping_algorithm);
                break;
            case HDRALGORITHM_STANDARD:
                processHDRCore(bitmaps, release_bitmaps, output_bitmap, assume_sorted, sort_cb, hdr_alpha, n_tiles, ce_preserve_blacks, tonemapping_algorithm);
                break;
            default:
                if (MyDebug.LOG)
                    Log.e(TAG, "unknown algorithm " + algorithm);
                // throw RuntimeException, as this is a programming error
                throw new RuntimeException();
        }
    }

    private ResponseFunction createFunctionFromBitmaps(int id, Bitmap in_bitmap, Bitmap out_bitmap, int offset_x, int offset_y) {
        if (MyDebug.LOG)
            Log.d(TAG, "createFunctionFromBitmaps");
        List<Double> x_samples = new ArrayList<>();
        List<Double> y_samples = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        final int n_samples_c = 100;
        final int n_w_samples = (int) Math.sqrt(n_samples_c);
        final int n_h_samples = n_samples_c / n_w_samples;

        double avg_in = 0.0;
        double avg_out = 0.0;
        for (int y = 0; y < n_h_samples; y++) {
            double alpha = ((double) y + 1.0) / ((double) n_h_samples + 1.0);
            int y_coord = (int) (alpha * in_bitmap.getHeight());
            for (int x = 0; x < n_w_samples; x++) {
                double beta = ((double) x + 1.0) / ((double) n_w_samples + 1.0);
                int x_coord = (int) (beta * in_bitmap.getWidth());
				/*if( MyDebug.LOG )
					Log.d(TAG, "sample response from " + x_coord + " , " + y_coord);*/
                if (x_coord + offset_x < 0 || x_coord + offset_x >= in_bitmap.getWidth() || y_coord + offset_y < 0 || y_coord + offset_y >= in_bitmap.getHeight()) {
                    continue;
                }
                int in_col = in_bitmap.getPixel(x_coord + offset_x, y_coord + offset_y);
                int out_col = out_bitmap.getPixel(x_coord, y_coord);
                double in_value = averageRGB(in_col);
                double out_value = averageRGB(out_col);
                avg_in += in_value;
                avg_out += out_value;
                x_samples.add(in_value);
                y_samples.add(out_value);
            }
        }
        if (x_samples.size() == 0) {
            Log.e(TAG, "no samples for response function!");
            // shouldn't happen, but could do with a very large offset - just make up a dummy sample
            double in_value = 255.0;
            double out_value = 255.0;
            avg_in += in_value;
            avg_out += out_value;
            x_samples.add(in_value);
            y_samples.add(out_value);
        }
        avg_in /= x_samples.size();
        avg_out /= x_samples.size();
        boolean is_dark_exposure = avg_in < avg_out;
        if (MyDebug.LOG) {
            Log.d(TAG, "avg_in: " + avg_in);
            Log.d(TAG, "avg_out: " + avg_out);
            Log.d(TAG, "is_dark_exposure: " + is_dark_exposure);
        }
        {
            // calculate weights
            double min_value = x_samples.get(0);
            double max_value = x_samples.get(0);
            for (int i = 1; i < x_samples.size(); i++) {
                double value = x_samples.get(i);
                if (value < min_value)
                    min_value = value;
                if (value > max_value)
                    max_value = value;
            }
            double med_value = 0.5 * (min_value + max_value);
            if (MyDebug.LOG) {
                Log.d(TAG, "min_value: " + min_value);
                Log.d(TAG, "max_value: " + max_value);
                Log.d(TAG, "med_value: " + med_value);
            }
            double min_value_y = y_samples.get(0);
            double max_value_y = y_samples.get(0);
            for (int i = 1; i < y_samples.size(); i++) {
                double value = y_samples.get(i);
                if (value < min_value_y)
                    min_value_y = value;
                if (value > max_value_y)
                    max_value_y = value;
            }
            double med_value_y = 0.5 * (min_value_y + max_value_y);
            if (MyDebug.LOG) {
                Log.d(TAG, "min_value_y: " + min_value_y);
                Log.d(TAG, "max_value_y: " + max_value_y);
                Log.d(TAG, "med_value_y: " + med_value_y);
            }
            for (int i = 0; i < x_samples.size(); i++) {
                double value = x_samples.get(i);
                double value_y = y_samples.get(i);
                if (is_dark_exposure) {
                    // for dark exposure, also need to worry about the y values (which will be brighter than x) being overexposed
                    double weight = (value <= med_value) ? value - min_value : max_value - value;
                    double weight_y = (value_y <= med_value_y) ? value_y - min_value_y : max_value_y - value_y;
                    if (weight_y < weight)
                        weight = weight_y;
                    weights.add(weight);
                } else {
                    double weight = (value <= med_value) ? value - min_value : max_value - value;
                    weights.add(weight);
                }
            }
        }

        return new ResponseFunction(context, id, x_samples, y_samples, weights);
    }

    /**
     * Calculates average of RGB values for the supplied color.
     */
    private double averageRGB(int color) {
        int r = (color & 0xFF0000) >> 16;
        int g = (color & 0xFF00) >> 8;
        int b = (color & 0xFF);
        return (r + g + b) / 3.0;
    }

    private void processHDRCore(List<Bitmap> bitmaps, boolean release_bitmaps, Bitmap output_bitmap, boolean assume_sorted, SortCallback sort_cb, float hdr_alpha, int n_tiles, boolean ce_preserve_blacks, TonemappingAlgorithm tonemapping_algorithm) {
        if (MyDebug.LOG)
            Log.d(TAG, "processHDRCore");

        long time_s = System.currentTimeMillis();

        int n_bitmaps = bitmaps.size();
        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();
        ResponseFunction[] response_functions = new ResponseFunction[n_bitmaps]; // ResponseFunction for each image (the ResponseFunction entry can be left null to indicate the Identity)
        offsets_x = new int[n_bitmaps];
        offsets_y = new int[n_bitmaps];
        initRenderscript();
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating renderscript: " + (System.currentTimeMillis() - time_s));
        // create allocations
        Allocation[] allocations = new Allocation[n_bitmaps];
        for (int i = 0; i < n_bitmaps; i++) {
            allocations[i] = Allocation.createFromBitmap(rs, bitmaps.get(i));
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating allocations from bitmaps: " + (System.currentTimeMillis() - time_s));
        //final int base_bitmap = (n_bitmaps - 1) / 2; // index of the bitmap with the base exposure and offsets
        final int base_bitmap = n_bitmaps % 2 == 0 ? n_bitmaps / 2 : (n_bitmaps - 1) / 2; // index of the bitmap with the base exposure and offsets
        // for even number of images, round up to brighter image

        // perform auto-alignment
        // if assume_sorted if false, this function will also sort the allocations and bitmaps from darkest to brightest.
        BrightnessDetails brightnessDetails = autoAlignment(offsets_x, offsets_y, allocations, width, height, bitmaps, base_bitmap, assume_sorted, sort_cb, true, false, 1, true, 1, width, height, time_s);
        int median_brightness = brightnessDetails.median_brightness;
        if (MyDebug.LOG) {
            Log.d(TAG, "### time after autoAlignment: " + (System.currentTimeMillis() - time_s));
            Log.d(TAG, "median_brightness: " + median_brightness);
        }

        //final boolean use_hdr_n = true; // test always using hdr_n
        final boolean use_hdr_n = n_bitmaps != 3;

        // compute response_functions
        for (int i = 0; i < n_bitmaps; i++) {
            ResponseFunction function = null;
            if (i != base_bitmap) {
                function = createFunctionFromBitmaps(i, bitmaps.get(i), bitmaps.get(base_bitmap), offsets_x[i], offsets_y[i]);
            } else if (use_hdr_n) {
                // for hdr_n, need to still create the identity response function
                function = ResponseFunction.createIdentity();
            }
            response_functions[i] = function;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating response functions: " + (System.currentTimeMillis() - time_s));

        if (n_bitmaps % 2 == 0) {
            // need to remap so that we aim for a brightness between the middle two images
            float a = (float) Math.sqrt(response_functions[base_bitmap - 1].parameter_A);
            float b = response_functions[base_bitmap - 1].parameter_B / (a + 1.0f);
            if (MyDebug.LOG) {
                Log.d(TAG, "remap for even number of images");
                Log.d(TAG, "    a: " + a);
                Log.d(TAG, "    b: " + b);
            }
            if (a < 1.0e-5f) {
                // avoid risk of division by 0
                a = 1.0e-5f;
                if (MyDebug.LOG)
                    Log.e(TAG, "    clamp a to: " + a);
            }
            for (int i = 0; i < n_bitmaps; i++) {
                float this_A = response_functions[i].parameter_A;
                float this_B = response_functions[i].parameter_B;
                response_functions[i].parameter_A = this_A / a;
                response_functions[i].parameter_B = this_B - this_A * b / a;
                if (MyDebug.LOG) {
                    Log.d(TAG, "remapped: " + i);
                    Log.d(TAG, "    A: " + this_A + " -> " + response_functions[i].parameter_A);
                    Log.d(TAG, "    B: " + this_B + " -> " + response_functions[i].parameter_B);
                }
            }
        }
        // write new hdr image

        // create RenderScript
        ScriptC_process_hdr processHDRScript = new ScriptC_process_hdr(rs);

        // set allocations
        processHDRScript.set_bitmap0(allocations[0]);
        if (n_bitmaps > 2) {
            processHDRScript.set_bitmap2(allocations[2]);
        }

        // set offsets
        processHDRScript.set_offset_x0(offsets_x[0]);
        processHDRScript.set_offset_y0(offsets_y[0]);
        // no offset for middle image
        if (n_bitmaps > 2) {
            processHDRScript.set_offset_x2(offsets_x[2]);
            processHDRScript.set_offset_y2(offsets_y[2]);
        }

        // set response functions
        processHDRScript.set_parameter_A0(response_functions[0].parameter_A);
        processHDRScript.set_parameter_B0(response_functions[0].parameter_B);
        // no response function for middle image
        if (n_bitmaps > 2) {
            processHDRScript.set_parameter_A2(response_functions[2].parameter_A);
            processHDRScript.set_parameter_B2(response_functions[2].parameter_B);
        }

        if (use_hdr_n) {
            // now need to set values for image 1
            processHDRScript.set_bitmap1(allocations[1]);
            processHDRScript.set_offset_x1(offsets_x[1]);
            processHDRScript.set_offset_y1(offsets_y[1]);
            processHDRScript.set_parameter_A1(response_functions[1].parameter_A);
            processHDRScript.set_parameter_B1(response_functions[1].parameter_B);
        }

        if (n_bitmaps > 3) {
            processHDRScript.set_bitmap3(allocations[3]);
            processHDRScript.set_offset_x3(offsets_x[3]);
            processHDRScript.set_offset_y3(offsets_y[3]);
            processHDRScript.set_parameter_A3(response_functions[3].parameter_A);
            processHDRScript.set_parameter_B3(response_functions[3].parameter_B);

            if (n_bitmaps > 4) {
                processHDRScript.set_bitmap4(allocations[4]);
                processHDRScript.set_offset_x4(offsets_x[4]);
                processHDRScript.set_offset_y4(offsets_y[4]);
                processHDRScript.set_parameter_A4(response_functions[4].parameter_A);
                processHDRScript.set_parameter_B4(response_functions[4].parameter_B);

                if (n_bitmaps > 5) {
                    processHDRScript.set_bitmap5(allocations[5]);
                    processHDRScript.set_offset_x5(offsets_x[5]);
                    processHDRScript.set_offset_y5(offsets_y[5]);
                    processHDRScript.set_parameter_A5(response_functions[5].parameter_A);
                    processHDRScript.set_parameter_B5(response_functions[5].parameter_B);

                    if (n_bitmaps > 6) {
                        processHDRScript.set_bitmap6(allocations[6]);
                        processHDRScript.set_offset_x6(offsets_x[6]);
                        processHDRScript.set_offset_y6(offsets_y[6]);
                        processHDRScript.set_parameter_A6(response_functions[6].parameter_A);
                        processHDRScript.set_parameter_B6(response_functions[6].parameter_B);
                    }
                }
            }
        }

        // set globals

        // set tonemapping algorithm
        switch (tonemapping_algorithm) {
            case TONEMAPALGORITHM_CLAMP:
                if (MyDebug.LOG)
                    Log.d(TAG, "tonemapping algorithm: clamp");
                processHDRScript.set_tonemap_algorithm(processHDRScript.get_tonemap_algorithm_clamp_c());
                break;
            case TONEMAPALGORITHM_EXPONENTIAL:
                if (MyDebug.LOG)
                    Log.d(TAG, "tonemapping algorithm: exponential");
                processHDRScript.set_tonemap_algorithm(processHDRScript.get_tonemap_algorithm_exponential_c());
                break;
            case TONEMAPALGORITHM_REINHARD:
                if (MyDebug.LOG)
                    Log.d(TAG, "tonemapping algorithm: reinhard");
                processHDRScript.set_tonemap_algorithm(processHDRScript.get_tonemap_algorithm_reinhard_c());
                break;
            case TONEMAPALGORITHM_FILMIC:
                if (MyDebug.LOG)
                    Log.d(TAG, "tonemapping algorithm: filmic");
                processHDRScript.set_tonemap_algorithm(processHDRScript.get_tonemap_algorithm_filmic_c());
                break;
            case TONEMAPALGORITHM_ACES:
                if (MyDebug.LOG)
                    Log.d(TAG, "tonemapping algorithm: aces");
                processHDRScript.set_tonemap_algorithm(processHDRScript.get_tonemap_algorithm_aces_c());
                break;
        }

        float max_possible_value = response_functions[0].parameter_A * 255 + response_functions[0].parameter_B;
        if (MyDebug.LOG)
            Log.d(TAG, "max_possible_value: " + max_possible_value);
        if (max_possible_value < 255.0f) {
            max_possible_value = 255.0f; // don't make dark images too bright, see below about linear_scale for more details
            if (MyDebug.LOG)
                Log.d(TAG, "clamp max_possible_value to: " + max_possible_value);
        }
        float tonemap_scale_c = 255.0f;

        int median_target = getBrightnessTarget(median_brightness, 2, 119);

        if (MyDebug.LOG) {
            Log.d(TAG, "median_target: " + median_target);
            Log.d(TAG, "compare: " + 255.0f / max_possible_value);
            Log.d(TAG, "to: " + (((float) median_target) / (float) median_brightness + median_target / 255.0f - 1.0f));
        }
        if (255.0f / max_possible_value < ((float) median_target) / (float) median_brightness + median_target / 255.0f - 1.0f) {
            final float tonemap_denom = ((float) median_target) / (float) median_brightness - (255.0f / max_possible_value);
            if (MyDebug.LOG)
                Log.d(TAG, "tonemap_denom: " + tonemap_denom);
            if (tonemap_denom != 0.0f) // just in case
                tonemap_scale_c = (255.0f - median_target) / tonemap_denom;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "tonemap_scale_c: " + tonemap_scale_c);
        processHDRScript.set_tonemap_scale(tonemap_scale_c);

        switch (tonemapping_algorithm) {
            case TONEMAPALGORITHM_EXPONENTIAL: {
                float E = processHDRScript.get_exposure();
                float linear_scale = (float) (1.0 / (1.0 - Math.exp(-E * max_possible_value / 255.0)));
                if (MyDebug.LOG)
                    Log.d(TAG, "linear_scale: " + linear_scale);
                processHDRScript.set_linear_scale(linear_scale);
                break;
            }
            case TONEMAPALGORITHM_REINHARD: {
                float linear_scale = (max_possible_value + tonemap_scale_c) / max_possible_value;
                if (MyDebug.LOG)
                    Log.d(TAG, "linear_scale: " + linear_scale);
                processHDRScript.set_linear_scale(linear_scale);
                break;
            }
            case TONEMAPALGORITHM_FILMIC: {
                float E = processHDRScript.get_filmic_exposure_bias();
                float W = E * max_possible_value;
                if (MyDebug.LOG)
                    Log.d(TAG, "filmic W: " + W);
                processHDRScript.set_W(W);
                break;
            }
        }

        if (MyDebug.LOG)
            Log.d(TAG, "call processHDRScript");
        Allocation output_allocation;
        boolean free_output_allocation = false;
        if (release_bitmaps) {
            output_allocation = allocations[base_bitmap];
        } else {
            output_allocation = Allocation.createFromBitmap(rs, output_bitmap);
            free_output_allocation = true;
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### time before processHDRScript: " + (System.currentTimeMillis() - time_s));
        if (use_hdr_n) {
            processHDRScript.set_n_bitmaps_g(n_bitmaps);
            processHDRScript.forEach_hdr_n(allocations[base_bitmap], output_allocation);
        } else {
            processHDRScript.forEach_hdr(allocations[base_bitmap], output_allocation);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "### time after processHDRScript: " + (System.currentTimeMillis() - time_s));

        if (release_bitmaps) {
            if (MyDebug.LOG)
                Log.d(TAG, "release bitmaps");
            for (int i = 0; i < bitmaps.size(); i++) {
                if (i != base_bitmap) {
                    Bitmap bitmap = bitmaps.get(i);
                    bitmap.recycle();
                }
            }
        }

        if (hdr_alpha != 0.0f) {
            adjustHistogram(output_allocation, output_allocation, width, height, hdr_alpha, n_tiles, ce_preserve_blacks, time_s);
            if (MyDebug.LOG)
                Log.d(TAG, "### time after adjustHistogram: " + (System.currentTimeMillis() - time_s));
        }

        if (release_bitmaps) {
            // must be the base_bitmap we copy to - see note above about using allocations[base_bitmap] as the output
            allocations[base_bitmap].copyTo(bitmaps.get(base_bitmap));
            if (MyDebug.LOG)
                Log.d(TAG, "### time after copying to bitmap: " + (System.currentTimeMillis() - time_s));

            // make it so that we store the output bitmap as first in the list
            bitmaps.set(0, bitmaps.get(base_bitmap));
            for (int i = 1; i < bitmaps.size(); i++) {
                bitmaps.set(i, null);
            }
        } else {
            output_allocation.copyTo(output_bitmap);
            if (MyDebug.LOG)
                Log.d(TAG, "### time after copying to bitmap: " + (System.currentTimeMillis() - time_s));
        }

        if (free_output_allocation)
            output_allocation.destroy();
        for (int i = 0; i < n_bitmaps; i++) {
            allocations[i].destroy();
            allocations[i] = null;
        }
        freeScripts();
        if (MyDebug.LOG)
            Log.d(TAG, "### time for processHDRCore: " + (System.currentTimeMillis() - time_s));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void processSingleImage(List<Bitmap> bitmaps, boolean release_bitmaps, Bitmap output_bitmap, float hdr_alpha, int n_tiles, boolean ce_preserve_blacks, DROTonemappingAlgorithm dro_tonemapping_algorithm) throws HDRProcessorException {
        if (MyDebug.LOG)
            Log.d(TAG, "processSingleImage");

        long time_s = System.currentTimeMillis();

        int width = bitmaps.get(0).getWidth();
        int height = bitmaps.get(0).getHeight();

        initRenderscript();
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating renderscript: " + (System.currentTimeMillis() - time_s));

        Allocation allocation = Allocation.createFromBitmap(rs, bitmaps.get(0));

        Allocation output_allocation;
        boolean free_output_allocation = false;
        if (release_bitmaps) {
            output_allocation = allocation;
        } else {
            free_output_allocation = true;
            output_allocation = Allocation.createFromBitmap(rs, output_bitmap);
        }

        if (dro_tonemapping_algorithm == DROTonemappingAlgorithm.DROALGORITHM_GAINGAMMA) {
            // brighten?
            int[] histo = computeHistogram(allocation, false, false);
            HistogramInfo histogramInfo = getHistogramInfo(histo);
            int brightness = histogramInfo.median_brightness;
            int max_brightness = histogramInfo.max_brightness;
            if (MyDebug.LOG)
                Log.d(TAG, "### time after computeHistogram: " + (System.currentTimeMillis() - time_s));
            if (MyDebug.LOG) {
                Log.d(TAG, "median brightness: " + brightness);
                Log.d(TAG, "max brightness: " + max_brightness);
            }
            BrightenFactors brighten_factors = computeBrightenFactors(false, 0, 0, brightness, max_brightness);
            float gain = brighten_factors.gain;
            float gamma = brighten_factors.gamma;
            float low_x = brighten_factors.low_x;
            float mid_x = brighten_factors.mid_x;

            if (Math.abs(gain - 1.0) > 1.0e-5 || max_brightness != 255 || Math.abs(gamma - 1.0) > 1.0e-5) {
                if (MyDebug.LOG)
                    Log.d(TAG, "apply gain/gamma");

                ScriptC_avg_brighten script = new ScriptC_avg_brighten(rs);
                script.invoke_setBrightenParameters(gain, gamma, low_x, mid_x, max_brightness);

                script.forEach_dro_brighten(allocation, output_allocation);

                if (free_output_allocation) {
                    allocation.destroy();
                    free_output_allocation = false;
                }
                allocation = output_allocation;
                if (MyDebug.LOG)
                    Log.d(TAG, "### time after dro_brighten: " + (System.currentTimeMillis() - time_s));
            }
        }

        adjustHistogram(allocation, output_allocation, width, height, hdr_alpha, n_tiles, ce_preserve_blacks, time_s);

        if (release_bitmaps) {
            allocation.copyTo(bitmaps.get(0));
            if (MyDebug.LOG)
                Log.d(TAG, "time after copying to bitmap: " + (System.currentTimeMillis() - time_s));
        } else {
            output_allocation.copyTo(output_bitmap);
            if (MyDebug.LOG)
                Log.d(TAG, "time after copying to bitmap: " + (System.currentTimeMillis() - time_s));
        }

        if (free_output_allocation)
            allocation.destroy();
        output_allocation.destroy();
        freeScripts();

    }

    void brightenImage(Bitmap bitmap, int brightness, int max_brightness, int brightness_target) {
        BrightenFactors brighten_factors = computeBrightenFactors(false, 0, 0, brightness, max_brightness, brightness_target, false);
        float gain = brighten_factors.gain;
        float gamma = brighten_factors.gamma;
        float low_x = brighten_factors.low_x;
        float mid_x = brighten_factors.mid_x;
        if (MyDebug.LOG) {
            Log.d(TAG, "gain: " + gain);
            Log.d(TAG, "gamma: " + gamma);
            Log.d(TAG, "low_x: " + low_x);
            Log.d(TAG, "mid_x: " + mid_x);
        }

        if (Math.abs(gain - 1.0) > 1.0e-5 || max_brightness != 255 || Math.abs(gamma - 1.0) > 1.0e-5) {
            if (MyDebug.LOG)
                Log.d(TAG, "apply gain/gamma");

            initRenderscript();

            Allocation allocation = Allocation.createFromBitmap(rs, bitmap);
            ScriptC_avg_brighten script = new ScriptC_avg_brighten(rs);
            script.invoke_setBrightenParameters(gain, gamma, low_x, mid_x, max_brightness);

            script.forEach_dro_brighten(allocation, allocation);

            allocation.copyTo(bitmap);
            allocation.destroy();

            freeScripts();
        }
    }

    private void initRenderscript() {
        if (MyDebug.LOG)
            Log.d(TAG, "initRenderscript");
        if (rs == null) {
            this.rs = RenderScript.create(context);
            if (MyDebug.LOG)
                Log.d(TAG, "create renderscript object");
        }
    }

    private int cached_avg_sample_size = 1;

    public int getAvgSampleSize(int capture_result_iso) {
        this.cached_avg_sample_size = (capture_result_iso >= 1100) ? 2 : 1;
        return cached_avg_sample_size;
    }

    public static class AvgData {
        public Allocation allocation_out;
        Bitmap bitmap_avg_align;
        Allocation allocation_avg_align;

        AvgData(Allocation allocation_out, Bitmap bitmap_avg_align, Allocation allocation_avg_align) {
            this.allocation_out = allocation_out;
            this.bitmap_avg_align = bitmap_avg_align;
            this.allocation_avg_align = allocation_avg_align;
        }

        public void destroy() {
            if (MyDebug.LOG)
                Log.d(TAG, "AvgData.destroy()");
            if (allocation_out != null) {
                allocation_out.destroy();
                allocation_out = null;
            }
            if (bitmap_avg_align != null) {
                bitmap_avg_align.recycle();
                bitmap_avg_align = null;
            }
            if (allocation_avg_align != null) {
                allocation_avg_align.destroy();
                allocation_avg_align = null;
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AvgData processAvg(Bitmap bitmap_avg, Bitmap bitmap_new, float avg_factor, int iso, float zoom_factor) throws HDRProcessorException {
        if (MyDebug.LOG) {
            Log.d(TAG, "processAvg");
            Log.d(TAG, "avg_factor: " + avg_factor);
        }
        if (bitmap_avg.getWidth() != bitmap_new.getWidth() ||
                bitmap_avg.getHeight() != bitmap_new.getHeight()) {
            if (MyDebug.LOG) {
                Log.e(TAG, "bitmaps not of same resolution");
            }
            throw new HDRProcessorException(HDRProcessorException.UNEQUAL_SIZES);
        }

        long time_s = System.currentTimeMillis();

        int width = bitmap_avg.getWidth();
        int height = bitmap_avg.getHeight();

        initRenderscript();
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating renderscript: " + (System.currentTimeMillis() - time_s));

        AvgData avg_data = processAvgCore(null, null, bitmap_avg, bitmap_new, width, height, avg_factor, iso, zoom_factor, null, null, time_s);


        if (MyDebug.LOG)
            Log.d(TAG, "### time for processAvg: " + (System.currentTimeMillis() - time_s));

        return avg_data;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void updateAvg(AvgData avg_data, int width, int height, Bitmap bitmap_new, float avg_factor, int iso, float zoom_factor) throws HDRProcessorException {
        if (MyDebug.LOG) {
            Log.d(TAG, "updateAvg");
            Log.d(TAG, "avg_factor: " + avg_factor);
        }
        if (width != bitmap_new.getWidth() ||
                height != bitmap_new.getHeight()) {
            if (MyDebug.LOG) {
                Log.e(TAG, "bitmaps not of same resolution");
            }
            throw new HDRProcessorException(HDRProcessorException.UNEQUAL_SIZES);
        }

        long time_s = System.currentTimeMillis();

        processAvgCore(avg_data.allocation_out, avg_data.allocation_out, null, bitmap_new, width, height, avg_factor, iso, zoom_factor, avg_data.allocation_avg_align, avg_data.bitmap_avg_align, time_s);

        if (MyDebug.LOG)
            Log.d(TAG, "### time for updateAvg: " + (System.currentTimeMillis() - time_s));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AvgData processAvgCore(Allocation allocation_out, Allocation allocation_avg, Bitmap bitmap_avg, Bitmap bitmap_new, int width, int height, float avg_factor, int iso, float zoom_factor, Allocation allocation_avg_align, Bitmap bitmap_avg_align, long time_s) throws HDRProcessorException {
        if (MyDebug.LOG) {
            Log.d(TAG, "processAvgCore");
            Log.d(TAG, "iso: " + iso);
            Log.d(TAG, "zoom_factor: " + zoom_factor);
        }

        Allocation allocation_new = null;
        boolean free_allocation_avg = false;
        offsets_x = new int[2];
        offsets_y = new int[2];
        boolean floating_point = bitmap_avg == null;
        {
            boolean floating_point_align = floating_point;
            // perform auto-alignment
            List<Bitmap> align_bitmaps = new ArrayList<>();
            Allocation[] allocations = new Allocation[2];
            Bitmap bitmap_new_align = null;
            Allocation allocation_new_align = null;
            int alignment_width = width;
            int alignment_height = height;
            int full_alignment_width = width;
            int full_alignment_height = height;
            final boolean scale_align = true;
            final int scale_align_size = (zoom_factor > 3.9f) ?
                    1 :
                    Math.max(4 / this.getAvgSampleSize(iso), 1);
            if (MyDebug.LOG)
                Log.d(TAG, "scale_align_size: " + scale_align_size);
            boolean crop_to_centre = true;
            if (scale_align) {
                if (MyDebug.LOG)
                    Log.d(TAG, "### time before creating allocations for autoalignment: " + (System.currentTimeMillis() - time_s));
                Matrix align_scale_matrix = new Matrix();
                align_scale_matrix.postScale(1.0f / scale_align_size, 1.0f / scale_align_size);
                full_alignment_width /= scale_align_size;
                full_alignment_height /= scale_align_size;

                final boolean full_align = false; // whether alignment images should be created as being cropped to the centre
                int align_width = width;
                int align_height = height;
                int align_x = 0;
                int align_y = 0;
                if (!full_align) {
                    align_width = width / 2;
                    align_height = height / 2;
                    align_x = (width - align_width) / 2;
                    align_y = (height - align_height) / 2;
                    crop_to_centre = false; // no need to crop in autoAlignment, as we're cropping here
                }

                final boolean filter_align = false;
                if (allocation_avg_align == null) {
                    bitmap_avg_align = Bitmap.createBitmap(bitmap_avg, align_x, align_y, align_width, align_height, align_scale_matrix, filter_align);
                    allocation_avg_align = Allocation.createFromBitmap(rs, bitmap_avg_align);
                    if (MyDebug.LOG)
                        Log.d(TAG, "### time after creating avg allocation for autoalignment: " + (System.currentTimeMillis() - time_s));
                }
                bitmap_new_align = Bitmap.createBitmap(bitmap_new, align_x, align_y, align_width, align_height, align_scale_matrix, filter_align);
                allocation_new_align = Allocation.createFromBitmap(rs, bitmap_new_align);

                alignment_width = bitmap_new_align.getWidth();
                alignment_height = bitmap_new_align.getHeight();

                align_bitmaps.add(bitmap_avg_align);
                align_bitmaps.add(bitmap_new_align);
                allocations[0] = allocation_avg_align;
                allocations[1] = allocation_new_align;
                floating_point_align = false;
                if (MyDebug.LOG)
                    Log.d(TAG, "### time after creating allocations for autoalignment: " + (System.currentTimeMillis() - time_s));
            } else {
                if (allocation_avg == null) {
                    allocation_avg = Allocation.createFromBitmap(rs, bitmap_avg);
                    free_allocation_avg = true;
                }
                allocation_new = Allocation.createFromBitmap(rs, bitmap_new);
                if (MyDebug.LOG)
                    Log.d(TAG, "### time after creating allocations from bitmaps: " + (System.currentTimeMillis() - time_s));
                align_bitmaps.add(bitmap_avg);
                align_bitmaps.add(bitmap_new);
                allocations[0] = allocation_avg;
                allocations[1] = allocation_new;
            }
            boolean wider = iso >= 1100;
            autoAlignment(offsets_x, offsets_y, allocations, alignment_width, alignment_height, align_bitmaps, 0, true, null, false, floating_point_align, 1, crop_to_centre, wider ? 2 : 1, full_alignment_width, full_alignment_height, time_s);

            if (scale_align) {
                for (int i = 0; i < offsets_x.length; i++) {
                    offsets_x[i] *= scale_align_size;
                }
                for (int i = 0; i < offsets_y.length; i++) {
                    offsets_y[i] *= scale_align_size;
                }
            }

            if (bitmap_new_align != null) {
                bitmap_new_align.recycle();
            }
            if (allocation_new_align != null) {
                allocation_new_align.destroy();
            }

            if (MyDebug.LOG) {
                Log.d(TAG, "### time after autoAlignment: " + (System.currentTimeMillis() - time_s));
            }
        }

        if (allocation_out == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "need to create allocation_out");
            allocation_out = Allocation.createTyped(rs, Type.createXY(rs, Element.F32_3(rs), width, height));
            if (MyDebug.LOG)
                Log.d(TAG, "### time after create allocation_out: " + (System.currentTimeMillis() - time_s));
        }
        if (allocation_avg == null) {
            allocation_avg = Allocation.createFromBitmap(rs, bitmap_avg);
            free_allocation_avg = true;
            if (MyDebug.LOG)
                Log.d(TAG, "### time after creating allocation_avg from bitmap: " + (System.currentTimeMillis() - time_s));
        }

        if (processAvgScript == null) {
            processAvgScript = new ScriptC_process_avg(rs);
        }
        if (allocation_new == null) {
            allocation_new = Allocation.createFromBitmap(rs, bitmap_new);
            if (MyDebug.LOG)
                Log.d(TAG, "### time after creating allocation_new from bitmap: " + (System.currentTimeMillis() - time_s));
        }
        processAvgScript.set_bitmap_new(allocation_new);
        processAvgScript.set_offset_x_new(offsets_x[1]);
        processAvgScript.set_offset_y_new(offsets_y[1]);
        processAvgScript.set_avg_factor(avg_factor);

        float limited_iso = Math.min(iso, 400);
        float wiener_cutoff_factor = 1.0f;
        if (iso >= 700) {
            limited_iso = 800;
            if (iso >= 1100) {
                wiener_cutoff_factor = 8.0f;
            }
        }
        limited_iso = Math.max(limited_iso, 100);
        float wiener_C = 10.0f * limited_iso;
        float tapered_wiener_scale = 1.0f - (float) Math.pow(0.5, avg_factor);
        if (MyDebug.LOG) {
            Log.d(TAG, "avg_factor: " + avg_factor);
            Log.d(TAG, "tapered_wiener_scale: " + tapered_wiener_scale);
        }
        wiener_C /= tapered_wiener_scale;

        float wiener_C_cutoff = wiener_cutoff_factor * wiener_C;
        if (MyDebug.LOG) {
            Log.d(TAG, "wiener_C: " + wiener_C);
            Log.d(TAG, "wiener_cutoff_factor: " + wiener_cutoff_factor);
        }
        processAvgScript.set_wiener_C(wiener_C);
        processAvgScript.set_wiener_C_cutoff(wiener_C_cutoff);

        if (MyDebug.LOG)
            Log.d(TAG, "call processAvgScript");
        if (MyDebug.LOG)
            Log.d(TAG, "### time before processAvgScript: " + (System.currentTimeMillis() - time_s));
        if (floating_point)
            processAvgScript.forEach_avg_f(allocation_avg, allocation_out);
        else
            processAvgScript.forEach_avg(allocation_avg, allocation_out);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after processAvgScript: " + (System.currentTimeMillis() - time_s));

        allocation_new.destroy();
        if (free_allocation_avg) {
            allocation_avg.destroy();
        }
        if (bitmap_avg != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "release bitmap_avg");
            bitmap_avg.recycle();
        }
        if (bitmap_new != null) {
            if (MyDebug.LOG)
                Log.d(TAG, "release bitmap_new");
            bitmap_new.recycle();
        }

        if (MyDebug.LOG)
            Log.d(TAG, "### time for processAvgCore: " + (System.currentTimeMillis() - time_s));
        return new AvgData(allocation_out, bitmap_avg_align, allocation_avg_align);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void autoAlignment(int[] offsets_x, int[] offsets_y, int width, int height, List<Bitmap> bitmaps, int base_bitmap, boolean use_mtb, int max_align_scale) {
        if (MyDebug.LOG)
            Log.d(TAG, "autoAlignment");
        initRenderscript();
        Allocation[] allocations = new Allocation[bitmaps.size()];
        for (int i = 0; i < bitmaps.size(); i++) {
            allocations[i] = Allocation.createFromBitmap(rs, bitmaps.get(i));
        }

        autoAlignment(offsets_x, offsets_y, allocations, width, height, bitmaps, base_bitmap, true, null, use_mtb, false, 1, false, max_align_scale, width, height, 0);

        for (int i = 0; i < allocations.length; i++) {
            if (allocations[i] != null) {
                allocations[i].destroy();
                allocations[i] = null;
            }
        }
        freeScripts();
    }

    static class BrightnessDetails {
        final int median_brightness; // median brightness value of the median image

        BrightnessDetails(int median_brightness) {
            this.median_brightness = median_brightness;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private BrightnessDetails autoAlignment(int[] offsets_x, int[] offsets_y, Allocation[] allocations, int width, int height, List<Bitmap> bitmaps, int base_bitmap, boolean assume_sorted, SortCallback sort_cb, boolean use_mtb, boolean floating_point, int min_step_size, boolean crop_to_centre, int max_align_scale, int full_width, int full_height, long time_s) {
        if (MyDebug.LOG) {
            Log.d(TAG, "autoAlignment");
            Log.d(TAG, "width: " + width);
            Log.d(TAG, "height: " + height);
            Log.d(TAG, "use_mtb: " + use_mtb);
            Log.d(TAG, "max_align_scale: " + max_align_scale);
            Log.d(TAG, "allocations: " + allocations.length);
            for (Allocation allocation : allocations) {
                Log.d(TAG, "    allocation:");
                Log.d(TAG, "    element: " + allocation.getElement());
                Log.d(TAG, "    type X: " + allocation.getType().getX());
                Log.d(TAG, "    type Y: " + allocation.getType().getY());
            }
        }

        // initialise
        for (int i = 0; i < offsets_x.length; i++) {
            offsets_x[i] = 0;
            offsets_y[i] = 0;
        }

        Allocation[] mtb_allocations = new Allocation[allocations.length];
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating mtb_allocations: " + (System.currentTimeMillis() - time_s));
        int mtb_width = width;
        int mtb_height = height;
        int mtb_x = 0;
        int mtb_y = 0;
        if (crop_to_centre) {
            mtb_width = width / 2;
            mtb_height = height / 2;
            mtb_x = mtb_width / 2;
            mtb_y = mtb_height / 2;
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "mtb_x: " + mtb_x);
            Log.d(TAG, "mtb_y: " + mtb_y);
            Log.d(TAG, "mtb_width: " + mtb_width);
            Log.d(TAG, "mtb_height: " + mtb_height);
        }

        // create RenderScript
        if (createMTBScript == null) {
            createMTBScript = new ScriptC_create_mtb(rs);
            if (MyDebug.LOG)
                Log.d(TAG, "### time after creating createMTBScript: " + (System.currentTimeMillis() - time_s));
        }
        //ScriptC_create_mtb createMTBScript = new ScriptC_create_mtb(rs);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating createMTBScript: " + (System.currentTimeMillis() - time_s));

        LuminanceInfo[] luminanceInfos = null;
        if (use_mtb) {
            luminanceInfos = new LuminanceInfo[allocations.length];
            for (int i = 0; i < allocations.length; i++) {
                luminanceInfos[i] = computeMedianLuminance(bitmaps.get(i), mtb_x, mtb_y, mtb_width, mtb_height);
                if (MyDebug.LOG)
                    Log.d(TAG, i + ": median_value: " + luminanceInfos[i].median_value);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "time after computeMedianLuminance: " + (System.currentTimeMillis() - time_s));
        }

        if (!assume_sorted && use_mtb) {
            if (MyDebug.LOG)
                Log.d(TAG, "sort bitmaps");
            class BitmapInfo {
                final LuminanceInfo luminanceInfo;
                final Bitmap bitmap;
                final Allocation allocation;
                final int index;

                BitmapInfo(LuminanceInfo luminanceInfo, Bitmap bitmap, Allocation allocation, int index) {
                    this.luminanceInfo = luminanceInfo;
                    this.bitmap = bitmap;
                    this.allocation = allocation;
                    this.index = index;
                }

            }

            List<BitmapInfo> bitmapInfos = new ArrayList<>(bitmaps.size());
            for (int i = 0; i < bitmaps.size(); i++) {
                BitmapInfo bitmapInfo = new BitmapInfo(luminanceInfos[i], bitmaps.get(i), allocations[i], i);
                bitmapInfos.add(bitmapInfo);
            }
            Collections.sort(bitmapInfos, new Comparator<BitmapInfo>() {
                @Override
                public int compare(BitmapInfo o1, BitmapInfo o2) {
                    return o1.luminanceInfo.median_value - o2.luminanceInfo.median_value;
                }
            });
            bitmaps.clear();
            for (int i = 0; i < bitmapInfos.size(); i++) {
                bitmaps.add(bitmapInfos.get(i).bitmap);
                luminanceInfos[i] = bitmapInfos.get(i).luminanceInfo;
                allocations[i] = bitmapInfos.get(i).allocation;
            }
            if (MyDebug.LOG) {
                for (int i = 0; i < allocations.length; i++) {
                    Log.d(TAG, i + ": median_value: " + luminanceInfos[i].median_value);
                }
            }
            if (sort_cb != null) {
                List<Integer> sort_order = new ArrayList<>();
                for (int i = 0; i < bitmapInfos.size(); i++) {
                    sort_order.add(bitmapInfos.get(i).index);
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "sort_order: " + sort_order);
                sort_cb.sortOrder(sort_order);
            }
        }

        int median_brightness = -1;
        if (use_mtb) {
            median_brightness = luminanceInfos[base_bitmap].median_value;
            if (MyDebug.LOG)
                Log.d(TAG, "median_brightness: " + median_brightness);
        }

        for (int i = 0; i < allocations.length; i++) {
            int median_value = -1;
            if (use_mtb) {
                median_value = luminanceInfos[i].median_value;
                if (MyDebug.LOG)
                    Log.d(TAG, i + ": median_value: " + median_value); }

            if (use_mtb && luminanceInfos[i].noisy) {
                if (MyDebug.LOG)
                    Log.d(TAG, "unable to compute median luminance safely");
                mtb_allocations[i] = null;
                continue;
            }

            mtb_allocations[i] = Allocation.createTyped(rs, Type.createXY(rs, Element.U8(rs), mtb_width, mtb_height));

            if (use_mtb)
                createMTBScript.set_median_value(median_value);
            createMTBScript.set_start_x(mtb_x);
            createMTBScript.set_start_y(mtb_y);
            createMTBScript.set_out_bitmap(mtb_allocations[i]);

            if (MyDebug.LOG)
                Log.d(TAG, "call createMTBScript");
            Script.LaunchOptions launch_options = new Script.LaunchOptions();
            launch_options.setX(mtb_x, mtb_x + mtb_width);
            launch_options.setY(mtb_y, mtb_y + mtb_height);
            if (use_mtb)
                createMTBScript.forEach_create_mtb(allocations[i], launch_options);
            else {
                if (floating_point && i == 0)
                    createMTBScript.forEach_create_greyscale_f(allocations[i], launch_options);
                else
                    createMTBScript.forEach_create_greyscale(allocations[i], launch_options);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "time after createMTBScript: " + (System.currentTimeMillis() - time_s));

        }
        int max_dim = Math.max(full_width, full_height); // n.b., use the full width and height here, not the mtb_width, height
        int max_ideal_size = (max_align_scale * max_dim) / 150;
        int initial_step_size = 1;
        while (initial_step_size < max_ideal_size) {
            initial_step_size *= 2;
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "max_dim: " + max_dim);
            Log.d(TAG, "max_ideal_size: " + max_ideal_size);
            Log.d(TAG, "initial_step_size: " + initial_step_size);
        }

        if (mtb_allocations[base_bitmap] == null) {
            if (MyDebug.LOG)
                Log.d(TAG, "base image not suitable for image alignment");
            for (int i = 0; i < mtb_allocations.length; i++) {
                if (mtb_allocations[i] != null) {
                    mtb_allocations[i].destroy();
                    mtb_allocations[i] = null;
                }
            }
            return new BrightnessDetails(median_brightness);
        }

        if (alignMTBScript == null) {
            alignMTBScript = new ScriptC_align_mtb(rs);
        }
        alignMTBScript.set_bitmap0(mtb_allocations[base_bitmap]);

        for (int i = 0; i < allocations.length; i++) {
            if (i == base_bitmap) {
                continue;
            }
            if (mtb_allocations[i] == null) {
                if (MyDebug.LOG)
                    Log.d(TAG, "image " + i + " not suitable for image alignment");
                continue;
            }
            alignMTBScript.set_bitmap1(mtb_allocations[i]);
            final int pixel_step = 1;
            int step_size = initial_step_size;
            while (step_size > min_step_size) {
                step_size /= 2;
                int pixel_step_size = step_size * pixel_step;
                if (pixel_step_size > mtb_width || pixel_step_size > mtb_height)
                    pixel_step_size = step_size;

                if (MyDebug.LOG) {
                    Log.d(TAG, "call alignMTBScript for image: " + i);
                    Log.d(TAG, "    versus base image: " + base_bitmap);
                    Log.d(TAG, "step_size: " + step_size);
                    Log.d(TAG, "pixel_step_size: " + pixel_step_size);
                }

                final boolean use_pyramid = false;
                {
                    alignMTBScript.set_off_x(offsets_x[i]);
                    alignMTBScript.set_off_y(offsets_y[i]);
                    alignMTBScript.set_step_size(pixel_step_size);
                }

                Allocation errorsAllocation = Allocation.createSized(rs, Element.I32(rs), 9);
                alignMTBScript.bind_errors(errorsAllocation);
                alignMTBScript.invoke_init_errors();

                Script.LaunchOptions launch_options = new Script.LaunchOptions();
                if (!use_pyramid) {
                    // see note inside align_mtb.rs/align_mtb() for why we sample over a subset of the image
                    int stop_x = mtb_width / pixel_step_size;
                    int stop_y = mtb_height / pixel_step_size;
                    if (MyDebug.LOG) {
                        Log.d(TAG, "stop_x: " + stop_x);
                        Log.d(TAG, "stop_y: " + stop_y);
                    }
                    launch_options.setX(0, stop_x);
                    launch_options.setY(0, stop_y);
                }
                long this_time_s = System.currentTimeMillis();
                if (use_mtb)
                    alignMTBScript.forEach_align_mtb(mtb_allocations[base_bitmap], launch_options);
                else
                    alignMTBScript.forEach_align(mtb_allocations[base_bitmap], launch_options);
                if (MyDebug.LOG) {
                    Log.d(TAG, "time for alignMTBScript: " + (System.currentTimeMillis() - this_time_s));
                    Log.d(TAG, "time after alignMTBScript: " + (System.currentTimeMillis() - time_s));
                }

                int best_error = -1;
                int best_id = -1;
                int[] errors = new int[9];
                errorsAllocation.copyTo(errors);
                errorsAllocation.destroy();
                for (int j = 0; j < 9; j++) {
                    int this_error = errors[j];
                    if (MyDebug.LOG)
                        Log.d(TAG, "    errors[" + j + "]: " + this_error);
                    if (best_id == -1 || this_error < best_error) {
                        best_error = this_error;
                        best_id = j;
                    }
                }
                if (MyDebug.LOG)
                    Log.d(TAG, "    best_id " + best_id + " error: " + best_error);
                if (best_error >= 2000000000) {
                    Log.e(TAG, "    auto-alignment failed due to overflow");
                    best_id = 4; // default to centre
                    if (is_test) {
                        throw new RuntimeException();
                    }
                }
                if (best_id != -1) {
                    int this_off_x = best_id % 3;
                    int this_off_y = best_id / 3;
                    this_off_x--;
                    this_off_y--;
                    if (MyDebug.LOG) {
                        Log.d(TAG, "this_off_x: " + this_off_x);
                        Log.d(TAG, "this_off_y: " + this_off_y);
                    }
                    offsets_x[i] += this_off_x * step_size;
                    offsets_y[i] += this_off_y * step_size;
                    if (MyDebug.LOG) {
                        Log.d(TAG, "offsets_x is now: " + offsets_x[i]);
                        Log.d(TAG, "offsets_y is now: " + offsets_y[i]);
                    }
                }
            }
            if (MyDebug.LOG) {
                Log.d(TAG, "resultant offsets for image: " + i);
                Log.d(TAG, "resultant offsets_x: " + offsets_x[i]);
                Log.d(TAG, "resultant offsets_y: " + offsets_y[i]);
            }
        }

        for (int i = 0; i < mtb_allocations.length; i++) {
            if (mtb_allocations[i] != null) {
                mtb_allocations[i].destroy();
                mtb_allocations[i] = null;
            }
        }
        return new BrightnessDetails(median_brightness);
    }

    private static class LuminanceInfo {
        final int median_value;
        final boolean noisy;

        LuminanceInfo(int median_value, boolean noisy) {
            this.median_value = median_value;
            this.noisy = noisy;
        }
    }

    private LuminanceInfo computeMedianLuminance(Bitmap bitmap, int mtb_x, int mtb_y, int mtb_width, int mtb_height) {
        if (MyDebug.LOG) {
            Log.d(TAG, "computeMedianLuminance");
            Log.d(TAG, "mtb_x: " + mtb_x);
            Log.d(TAG, "mtb_y: " + mtb_y);
            Log.d(TAG, "mtb_width: " + mtb_width);
            Log.d(TAG, "mtb_height: " + mtb_height);
        }
        final int n_samples_c = 100;
        final int n_w_samples = (int) Math.sqrt(n_samples_c);
        final int n_h_samples = n_samples_c / n_w_samples;

        int[] histo = new int[256];
        for (int i = 0; i < 256; i++)
            histo[i] = 0;
        int total = 0;
        for (int y = 0; y < n_h_samples; y++) {
            double alpha = ((double) y + 1.0) / ((double) n_h_samples + 1.0);
            int y_coord = mtb_y + (int) (alpha * mtb_height);
            for (int x = 0; x < n_w_samples; x++) {
                double beta = ((double) x + 1.0) / ((double) n_w_samples + 1.0);
                int x_coord = mtb_x + (int) (beta * mtb_width);
                int color = bitmap.getPixel(x_coord, y_coord);
                int r = (color & 0xFF0000) >> 16;
                int g = (color & 0xFF00) >> 8;
                int b = (color & 0xFF);
                int luminance = Math.max(r, g);
                luminance = Math.max(luminance, b);
                histo[luminance]++;
                total++;
            }
        }
        int middle = total / 2;
        int count = 0;
        boolean noisy = false;
        for (int i = 0; i < 256; i++) {
            count += histo[i];
            if (count >= middle) {
                if (MyDebug.LOG)
                    Log.d(TAG, "median luminance " + i);
                final int noise_threshold = 4;
                int n_below = 0, n_above = 0;
                for (int j = 0; j <= i - noise_threshold; j++) {
                    n_below += histo[j];
                }
                for (int j = 0; j <= i + noise_threshold && j < 256; j++) {
                    n_above += histo[j];
                }
                if (n_below< 0.2 ){
                    if (MyDebug.LOG)
                        Log.d(TAG, "too dark/noisy");
                    noisy = true;
                }
                return new LuminanceInfo(i, noisy);
            }
        }
        Log.e(TAG, "computeMedianLuminance failed");
        return new LuminanceInfo(127, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void adjustHistogram(Allocation allocation_in, Allocation allocation_out, int width, int height, float hdr_alpha, int n_tiles, boolean ce_preserve_blacks, long time_s) {
        if (MyDebug.LOG)
            Log.d(TAG, "adjustHistogram");
        final boolean adjust_histogram = false;

        if (adjust_histogram) {
            // create histogram
            int[] histogram = new int[256];
            if (MyDebug.LOG)
                Log.d(TAG, "time before creating histogram: " + (System.currentTimeMillis() - time_s));
            Allocation histogramAllocation = computeHistogramAllocation(allocation_in, false, false, time_s);
            if (MyDebug.LOG)
                Log.d(TAG, "time after creating histogram: " + (System.currentTimeMillis() - time_s));
            histogramAllocation.copyTo(histogram);
            int[] c_histogram = new int[256];
            c_histogram[0] = histogram[0];
            for (int x = 1; x < 256; x++) {
                c_histogram[x] = c_histogram[x - 1] + histogram[x];
            }
            histogramAllocation.copyFrom(c_histogram);

            ScriptC_histogram_adjust histogramAdjustScript = new ScriptC_histogram_adjust(rs);
            histogramAdjustScript.set_c_histogram(histogramAllocation);
            histogramAdjustScript.set_hdr_alpha(hdr_alpha);

            if (MyDebug.LOG)
                Log.d(TAG, "call histogramAdjustScript");
            histogramAdjustScript.forEach_histogram_adjust(allocation_in, allocation_out);
            if (MyDebug.LOG)
                Log.d(TAG, "time after histogramAdjustScript: " + (System.currentTimeMillis() - time_s));
            histogramAllocation.destroy();
        }

        final boolean adjust_histogram_local = true;

        if (adjust_histogram_local) {
            Allocation histogramAllocation = Allocation.createSized(rs, Element.I32(rs), 256);
            if (MyDebug.LOG)
                Log.d(TAG, "create histogramScript");
            ScriptC_histogram_compute histogramScript = new ScriptC_histogram_compute(rs);
            if (MyDebug.LOG)
                Log.d(TAG, "bind histogram allocation");
            histogramScript.bind_histogram(histogramAllocation);

            int[] c_histogram = new int[n_tiles * n_tiles * 256];
            int[] temp_c_histogram = new int[256];
            for (int i = 0; i < n_tiles; i++) {
                double a0 = ((double) i) / (double) n_tiles;
                double a1 = ((double) i + 1.0) / (double) n_tiles;
                int start_x = (int) (a0 * width);
                int stop_x = (int) (a1 * width);
                if (stop_x == start_x)
                    continue;
                for (int j = 0; j < n_tiles; j++) {
                    double b0 = ((double) j) / (double) n_tiles;
                    double b1 = ((double) j + 1.0) / (double) n_tiles;
                    int start_y = (int) (b0 * height);
                    int stop_y = (int) (b1 * height);
                    if (stop_y == start_y)
                        continue;
                    Script.LaunchOptions launch_options = new Script.LaunchOptions();
                    launch_options.setX(start_x, stop_x);
                    launch_options.setY(start_y, stop_y);
                    histogramScript.invoke_init_histogram();
                    histogramScript.forEach_histogram_compute_by_value(allocation_in, launch_options);

                    int[] histogram = new int[256];
                    histogramAllocation.copyTo(histogram);

                    int n_pixels = (stop_x - start_x) * (stop_y - start_y);
                    int clip_limit = (5 * n_pixels) / 256;
                    {
                        // find real clip limit
                        int bottom = 0, top = clip_limit;
                        while (top - bottom > 1) {
                            int middle = (top + bottom) / 2;
                            int sum = 0;
                            for (int x = 0; x < 256; x++) {
                                if (histogram[x] > middle) {
                                    sum += (histogram[x] - clip_limit);
                                }
                            }
                            if (sum > (clip_limit - middle) * 256)
                                top = middle;
                            else
                                bottom = middle;
                        }
                        clip_limit = (top + bottom) / 2;
                    }
                    int n_clipped = 0;
                    for (int x = 0; x < 256; x++) {
                        if (histogram[x] > clip_limit) {
                            n_clipped += (histogram[x] - clip_limit);
                            histogram[x] = clip_limit;
                        }
                    }
                    int n_clipped_per_bucket = n_clipped / 256;
                    for (int x = 0; x < 256; x++) {
                        histogram[x] += n_clipped_per_bucket;
                    }

                    if (ce_preserve_blacks) {
                        if (MyDebug.LOG) {
                            for (int x = 0; x < 256; x++) {
                                Log.d(TAG, "pre-brighten histogram[" + x + "] = " + histogram[x]);
                            }
                        }

                        temp_c_histogram[0] = histogram[0];
                        for (int x = 1; x < 256; x++) {
                            temp_c_histogram[x] = temp_c_histogram[x - 1] + histogram[x];
                        }

                        int equal_limit = n_pixels / 256;
                        final int dark_threshold_c = 128;
                        for (int x = 0; x < dark_threshold_c; x++) {
                            int c_equal_limit = equal_limit * (x + 1);
                            if (temp_c_histogram[x] >= c_equal_limit) {
                                continue;
                            }
                            float alpha = 1.0f - ((float) x) / ((float) dark_threshold_c);
                            int limit = (int) (alpha * equal_limit);
                            if (MyDebug.LOG)
                                Log.d(TAG, "x: " + x + " ; limit: " + limit);
                            if (histogram[x] < limit) {
                                for (int y = x + 1; y < 256 && histogram[x] < limit; y++) {
                                    if (histogram[y] > equal_limit) {
                                        int move = histogram[y] - equal_limit;
                                        move = Math.min(move, limit - histogram[x]);
                                        histogram[x] += move;
                                        histogram[y] -= move;
                                    }
                                }
                                if (MyDebug.LOG)
                                    Log.d(TAG, "    histogram pulled up to: " + histogram[x]);
                            }
                        }
                    }

                    int histogram_offset = 256 * (i * n_tiles + j);
                    c_histogram[histogram_offset] = histogram[0];
                    for (int x = 1; x < 256; x++) {
                        c_histogram[histogram_offset + x] = c_histogram[histogram_offset + x - 1] + histogram[x];
                    }
                    if (MyDebug.LOG) {
                        for (int x = 0; x < 256; x++) {
                            Log.d(TAG, "histogram[" + x + "] = " + histogram[x] + " cumulative: " + c_histogram[histogram_offset + x]);
                        }
                    }
                }
            }

            if (MyDebug.LOG)
                Log.d(TAG, "time after creating histograms: " + (System.currentTimeMillis() - time_s));

            Allocation c_histogramAllocation = Allocation.createSized(rs, Element.I32(rs), n_tiles * n_tiles * 256);
            c_histogramAllocation.copyFrom(c_histogram);

            ScriptC_histogram_adjust histogramAdjustScript = new ScriptC_histogram_adjust(rs);
            histogramAdjustScript.set_c_histogram(c_histogramAllocation);
            histogramAdjustScript.set_hdr_alpha(hdr_alpha);
            histogramAdjustScript.set_n_tiles(n_tiles);
            histogramAdjustScript.set_width(width);
            histogramAdjustScript.set_height(height);

            if (MyDebug.LOG)
                Log.d(TAG, "time before histogramAdjustScript: " + (System.currentTimeMillis() - time_s));
            histogramAdjustScript.forEach_histogram_adjust(allocation_in, allocation_out);
            if (MyDebug.LOG)
                Log.d(TAG, "time after histogramAdjustScript: " + (System.currentTimeMillis() - time_s));

            histogramAllocation.destroy();
            c_histogramAllocation.destroy();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Allocation computeHistogramAllocation(Allocation allocation_in, boolean avg, boolean floating_point, long time_s) {
        if (MyDebug.LOG)
            Log.d(TAG, "computeHistogramAllocation");
        Allocation histogramAllocation = Allocation.createSized(rs, Element.I32(rs), 256);
        final boolean use_custom_histogram = true;
        if (use_custom_histogram) {
            if (MyDebug.LOG)
                Log.d(TAG, "create histogramScript");
            ScriptC_histogram_compute histogramScript = new ScriptC_histogram_compute(rs);
            if (MyDebug.LOG)
                Log.d(TAG, "bind histogram allocation");
            histogramScript.bind_histogram(histogramAllocation);
            histogramScript.invoke_init_histogram();
            if (MyDebug.LOG)
                Log.d(TAG, "call histogramScript");
            if (MyDebug.LOG)
                Log.d(TAG, "time before histogramScript: " + (System.currentTimeMillis() - time_s));
            if (avg) {
                if (floating_point)
                    histogramScript.forEach_histogram_compute_by_intensity_f(allocation_in);
                else
                    histogramScript.forEach_histogram_compute_by_intensity(allocation_in);
            } else {
                if (floating_point)
                    histogramScript.forEach_histogram_compute_by_value_f(allocation_in);
                else
                    histogramScript.forEach_histogram_compute_by_value(allocation_in);
            }
            if (MyDebug.LOG)
                Log.d(TAG, "time after histogramScript: " + (System.currentTimeMillis() - time_s));
        } else {
            ScriptIntrinsicHistogram histogramScriptIntrinsic = ScriptIntrinsicHistogram.create(rs, Element.U8_4(rs));
            histogramScriptIntrinsic.setOutput(histogramAllocation);
            if (MyDebug.LOG)
                Log.d(TAG, "call histogramScriptIntrinsic");
            histogramScriptIntrinsic.forEach_Dot(allocation_in); // use forEach_dot(); using forEach would simply compute a histogram for red values!
        }

        return histogramAllocation;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int[] computeHistogram(Bitmap bitmap, boolean avg) {
        if (MyDebug.LOG)
            Log.d(TAG, "computeHistogram");
        long time_s = System.currentTimeMillis();
        initRenderscript();
        Allocation allocation_in = Allocation.createFromBitmap(rs, bitmap);
        if (MyDebug.LOG)
            Log.d(TAG, "time after createFromBitmap: " + (System.currentTimeMillis() - time_s));
        int[] histogram = computeHistogram(allocation_in, avg, false);
        allocation_in.destroy();
        freeScripts();
        return histogram;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int[] computeHistogram(Allocation allocation, boolean avg, boolean floating_point) {
        if (MyDebug.LOG)
            Log.d(TAG, "computeHistogram");
        long time_s = System.currentTimeMillis();
        int[] histogram = new int[256];
        Allocation histogramAllocation = computeHistogramAllocation(allocation, avg, floating_point, time_s);
        histogramAllocation.copyTo(histogram);
        histogramAllocation.destroy();
        return histogram;
    }

    static class HistogramInfo {
        final int total;
        final int mean_brightness;
        final int median_brightness;
        final int max_brightness;

        HistogramInfo(int total, int mean_brightness, int median_brightness, int max_brightness) {
            this.total = total;
            this.mean_brightness = mean_brightness;
            this.median_brightness = median_brightness;
            this.max_brightness = max_brightness;
        }
    }

    HistogramInfo getHistogramInfo(int[] histo) {
        int total = 0;
        for (int value : histo)
            total += value;
        int middle = total / 2;
        int count = 0;
        double sum_brightness = 0.0f;
        int median_brightness = -1;
        int max_brightness = 0;
        for (int i = 0; i < histo.length; i++) {
            count += histo[i];
            sum_brightness += (double) (histo[i] * i);
            if (count >= middle && median_brightness == -1) {
                median_brightness = i;
            }
            if (histo[i] > 0) {
                max_brightness = i;
            }
        }
        int mean_brightness = (int) (sum_brightness / count + 0.1);

        return new HistogramInfo(total, mean_brightness, median_brightness, max_brightness);
    }

    private static int getBrightnessTarget(int brightness, float max_gain_factor, int ideal_brightness) {
        if (brightness <= 0)
            brightness = 1;
        if (MyDebug.LOG) {
            Log.d(TAG, "brightness: " + brightness);
            Log.d(TAG, "max_gain_factor: " + max_gain_factor);
            Log.d(TAG, "ideal_brightness: " + ideal_brightness);
        }
        int median_target = Math.min(ideal_brightness, (int) (max_gain_factor * brightness));
        return Math.max(brightness, median_target); // don't make darker
    }

    public static class BrightenFactors {
        public final float gain;
        public final float low_x;
        public final float mid_x;
        public final float gamma;

        BrightenFactors(float gain, float low_x, float mid_x, float gamma) {
            this.gain = gain;
            this.low_x = low_x;
            this.mid_x = mid_x;
            this.gamma = gamma;
        }
    }

    public static BrightenFactors computeBrightenFactors(boolean has_iso_exposure, int iso, long exposure_time, int brightness, int max_brightness) {

        float max_gain_factor = 1.5f;
        int ideal_brightness = 119;
        if (has_iso_exposure && iso < 1100 && exposure_time < 1000000000L / 59) {
            ideal_brightness = 199;
        }
        int brightness_target = getBrightnessTarget(brightness, max_gain_factor, ideal_brightness);

        return computeBrightenFactors(has_iso_exposure, iso, exposure_time, brightness, max_brightness, brightness_target, true);
    }

    private static BrightenFactors computeBrightenFactors(boolean has_iso_exposure, int iso, long exposure_time, int brightness, int max_brightness, int brightness_target, boolean brighten_only) {

        if (brightness <= 0)
            brightness = 1;
        float gain = brightness_target / (float) brightness;
        if (MyDebug.LOG)
            Log.d(TAG, "gain " + gain);
        if (gain < 1.0f && brighten_only) {
            gain = 1.0f;
            if (MyDebug.LOG) {
                Log.d(TAG, "clamped gain to: " + gain);
            }
        }
        float gamma = 1.0f;
        float max_possible_value = gain * max_brightness;
        if (MyDebug.LOG)
            Log.d(TAG, "max_possible_value: " + max_possible_value);

        float mid_x = 255.5f;
        if (max_possible_value > 255.0f) {
            if (MyDebug.LOG)
                Log.d(TAG, "use piecewise gain/gamma");
            float mid_y = (has_iso_exposure && iso < 1100 && exposure_time < 1000000000L / 59) ? 0.6f * 255.0f : 0.8f * 255.0f;
            mid_x = mid_y / gain;
            gamma = (float) (Math.log(mid_y / 255.0f) / Math.log(mid_x / max_brightness));
        } else if (brighten_only && max_possible_value < 255.0f && max_brightness > 0) {
            float alt_gain = 255.0f / max_brightness;
            alt_gain = Math.min(alt_gain, 4.0f);
            if (MyDebug.LOG)
                Log.d(TAG, "alt_gain: " + alt_gain);
            if (alt_gain > gain) {
                gain = alt_gain;
                if (MyDebug.LOG)
                    Log.d(TAG, "increased gain to: " + gain);
            }
        }
        float low_x = 0.0f;
        if (has_iso_exposure && iso >= 400) {
            float piecewise_mid_y = 0.5f * 255.0f;
            float piecewise_mid_x = piecewise_mid_y / gain;
            low_x = Math.min(8.0f, 0.125f * piecewise_mid_x);
        }
        if (MyDebug.LOG) {
            Log.d(TAG, "low_x " + low_x);
            Log.d(TAG, "mid_x " + mid_x);
            Log.d(TAG, "gamma " + gamma);
        }

        return new BrightenFactors(gain, low_x, mid_x, gamma);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Bitmap avgBrighten(Allocation input, int width, int height, int iso, long exposure_time) {
        if (MyDebug.LOG) {
            Log.d(TAG, "avgBrighten");
            Log.d(TAG, "iso: " + iso);
            Log.d(TAG, "exposure_time: " + exposure_time);
        }
        initRenderscript();

        long time_s = System.currentTimeMillis();

        int[] histo = computeHistogram(input, false, true);
        HistogramInfo histogramInfo = getHistogramInfo(histo);
        int brightness = histogramInfo.median_brightness;
        int max_brightness = histogramInfo.max_brightness;
        if (MyDebug.LOG)
            Log.d(TAG, "### time after computeHistogram: " + (System.currentTimeMillis() - time_s));

        if (MyDebug.LOG) {
            Log.d(TAG, "median brightness: " + histogramInfo.median_brightness);
            Log.d(TAG, "mean brightness: " + histogramInfo.mean_brightness);
            Log.d(TAG, "max brightness: " + max_brightness);
        }

        BrightenFactors brighten_factors = computeBrightenFactors(true, iso, exposure_time, brightness, max_brightness);
        float gain = brighten_factors.gain;
        float low_x = brighten_factors.low_x;
        float mid_x = brighten_factors.mid_x;
        float gamma = brighten_factors.gamma;

        ScriptC_avg_brighten avgBrightenScript = new ScriptC_avg_brighten(rs);
        avgBrightenScript.set_bitmap(input);
        float black_level = 0.0f;
        {
            int total = histogramInfo.total;
            int percentile = (int) (total * 0.001f);
            int count = 0;
            int darkest_brightness = -1;
            for (int i = 0; i < histo.length; i++) {
                count += histo[i];
                if (count >= percentile && darkest_brightness == -1) {
                    darkest_brightness = i;
                }
            }
            black_level = Math.max(black_level, darkest_brightness);
            black_level = Math.min(black_level, iso <= 700 ? 18 : 4);
            if (MyDebug.LOG) {
                Log.d(TAG, "percentile: " + percentile);
                Log.d(TAG, "darkest_brightness: " + darkest_brightness);
                Log.d(TAG, "black_level is now: " + black_level);
            }
        }
        avgBrightenScript.invoke_setBlackLevel(black_level);

        float median_filter_strength = (cached_avg_sample_size >= 2) ? 0.5f : 1.0f;
        if (MyDebug.LOG)
            Log.d(TAG, "median_filter_strength: " + median_filter_strength);
        avgBrightenScript.set_median_filter_strength(median_filter_strength);
        avgBrightenScript.invoke_setBrightenParameters(gain, gamma, low_x, mid_x, max_brightness);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Allocation allocation_out = Allocation.createFromBitmap(rs, bitmap);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after creating allocation_out: " + (System.currentTimeMillis() - time_s));

        avgBrightenScript.forEach_avg_brighten_f(input, allocation_out);
        if (MyDebug.LOG)
            Log.d(TAG, "### time after avg_brighten: " + (System.currentTimeMillis() - time_s));

        if (iso < 1100 && exposure_time < 1000000000L / 59) {
            final int median_lo = 60, median_hi = 35;
            float alpha = (histogramInfo.median_brightness - median_lo) / (float) (median_hi - median_lo);
            alpha = Math.max(alpha, 0.0f);
            alpha = Math.min(alpha, 1.0f);
            float amount = (1.0f - alpha) * 0.25f + alpha * 0.5f;
            if (MyDebug.LOG) {
                Log.d(TAG, "dro alpha: " + alpha);
                Log.d(TAG, "dro amount: " + amount);
            }
            adjustHistogram(allocation_out, allocation_out, width, height, amount, 1, true, time_s);
            if (MyDebug.LOG)
                Log.d(TAG, "### time after adjustHistogram: " + (System.currentTimeMillis() - time_s));
        }

        allocation_out.copyTo(bitmap);
        allocation_out.destroy();

        freeScripts();
        if (MyDebug.LOG)
            Log.d(TAG, "### total time for avgBrighten: " + (System.currentTimeMillis() - time_s));
        return bitmap;
    }
}

