package com.picstar.picstarapp.campkg.others;



import android.os.Build;
import android.renderscript.*;

import androidx.annotation.RequiresApi;

/**
 * @hide
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScriptC_process_hdr extends ScriptC {
    private static final String __rs_resource_name = "process_hdr";
    // Constructor
    public  ScriptC_process_hdr(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                process_hdrBitCode.getBitCode32(),
                process_hdrBitCode.getBitCode64());
        mExportVar_tonemap_algorithm_clamp_c = 0;
        mExportVar_tonemap_algorithm_exponential_c = 1;
        mExportVar_tonemap_algorithm_reinhard_c = 2;
        mExportVar_tonemap_algorithm_filmic_c = 3;
        mExportVar_tonemap_algorithm_aces_c = 4;
        mExportVar_exposure = 1.20000005f;
        mExportVar_filmic_exposure_bias = 0.00784313772f;
        __U8_4 = Element.U8_4(rs);
    }

    private Element __U8_4;
    private final static int mExportVarIdx_bitmap0 = 0;
    public synchronized void set_bitmap0(Allocation v) {
        setVar(mExportVarIdx_bitmap0, v);
    }

    private final static int mExportVarIdx_bitmap1 = 1;
    public synchronized void set_bitmap1(Allocation v) {
        setVar(mExportVarIdx_bitmap1, v);
    }


    private final static int mExportVarIdx_bitmap2 = 2;
    public synchronized void set_bitmap2(Allocation v) {
        setVar(mExportVarIdx_bitmap2, v);
    }


    private final static int mExportVarIdx_bitmap3 = 3;
    private Allocation mExportVar_bitmap3;
    public synchronized void set_bitmap3(Allocation v) {
        setVar(mExportVarIdx_bitmap3, v);
        mExportVar_bitmap3 = v;
    }


    private final static int mExportVarIdx_bitmap4 = 4;
    private Allocation mExportVar_bitmap4;
    public synchronized void set_bitmap4(Allocation v) {
        setVar(mExportVarIdx_bitmap4, v);
        mExportVar_bitmap4 = v;
    }

    private final static int mExportVarIdx_bitmap5 = 5;
    public synchronized void set_bitmap5(Allocation v) {
        setVar(mExportVarIdx_bitmap5, v);
    }

    private final static int mExportVarIdx_bitmap6 = 6;
    public synchronized void set_bitmap6(Allocation v) {
        setVar(mExportVarIdx_bitmap6, v);
    }
    private final static int mExportVarIdx_offset_x0 = 7;
    public synchronized void set_offset_x0(int v) {
        setVar(mExportVarIdx_offset_x0, v);
    }
    private final static int mExportVarIdx_offset_y0 = 8;
    public synchronized void set_offset_y0(int v) {
        setVar(mExportVarIdx_offset_y0, v);
    }

    private final static int mExportVarIdx_offset_x1 = 9;
    public synchronized void set_offset_x1(int v) {
        setVar(mExportVarIdx_offset_x1, v);
    }


    private final static int mExportVarIdx_offset_y1 = 10;
    public synchronized void set_offset_y1(int v) {
        setVar(mExportVarIdx_offset_y1, v);
    }


    private final static int mExportVarIdx_offset_x2 = 11;
    public synchronized void set_offset_x2(int v) {
        setVar(mExportVarIdx_offset_x2, v);
    }


    private final static int mExportVarIdx_offset_y2 = 12;
    public synchronized void set_offset_y2(int v) {
        setVar(mExportVarIdx_offset_y2, v);
    }


    private final static int mExportVarIdx_offset_x3 = 13;
    public synchronized void set_offset_x3(int v) {
        setVar(mExportVarIdx_offset_x3, v);
    }


    private final static int mExportVarIdx_offset_y3 = 14;
    public synchronized void set_offset_y3(int v) {
        setVar(mExportVarIdx_offset_y3, v);
    }


    private final static int mExportVarIdx_offset_x4 = 15;
    public synchronized void set_offset_x4(int v) {
        setVar(mExportVarIdx_offset_x4, v);
    }

    private final static int mExportVarIdx_offset_y4 = 16;
    public synchronized void set_offset_y4(int v) {
        setVar(mExportVarIdx_offset_y4, v);
    }

    private final static int mExportVarIdx_offset_x5 = 17;
    public synchronized void set_offset_x5(int v) {
        setVar(mExportVarIdx_offset_x5, v);
    }
    private final static int mExportVarIdx_offset_y5 = 18;
    public synchronized void set_offset_y5(int v) {
        setVar(mExportVarIdx_offset_y5, v);
    }


    private final static int mExportVarIdx_offset_x6 = 19;
    public synchronized void set_offset_x6(int v) {
        setVar(mExportVarIdx_offset_x6, v);
    }

    private final static int mExportVarIdx_offset_y6 = 20;
    public synchronized void set_offset_y6(int v) {
        setVar(mExportVarIdx_offset_y6, v);
    }

    private final static int mExportVarIdx_parameter_A0 = 21;
    public synchronized void set_parameter_A0(float v) {
        setVar(mExportVarIdx_parameter_A0, v);
    }

    private final static int mExportVarIdx_parameter_B0 = 22;
    public synchronized void set_parameter_B0(float v) {
        setVar(mExportVarIdx_parameter_B0, v);
    }


    private final static int mExportVarIdx_parameter_A1 = 23;
    public synchronized void set_parameter_A1(float v) {
        setVar(mExportVarIdx_parameter_A1, v);
    }


    private final static int mExportVarIdx_parameter_B1 = 24;
    public synchronized void set_parameter_B1(float v) {
        setVar(mExportVarIdx_parameter_B1, v);
    }


    private final static int mExportVarIdx_parameter_A2 = 25;
    public synchronized void set_parameter_A2(float v) {
        setVar(mExportVarIdx_parameter_A2, v);
    }

    private final static int mExportVarIdx_parameter_B2 = 26;
    public synchronized void set_parameter_B2(float v) {
        setVar(mExportVarIdx_parameter_B2, v);
    }

    private final static int mExportVarIdx_parameter_A3 = 27;
    public synchronized void set_parameter_A3(float v) {
        setVar(mExportVarIdx_parameter_A3, v);
    }

    private final static int mExportVarIdx_parameter_B3 = 28;
    public synchronized void set_parameter_B3(float v) {
        setVar(mExportVarIdx_parameter_B3, v);
    }


    private final static int mExportVarIdx_parameter_A4 = 29;
    public synchronized void set_parameter_A4(float v) {
        setVar(mExportVarIdx_parameter_A4, v);
    }


    private final static int mExportVarIdx_parameter_B4 = 30;
    public synchronized void set_parameter_B4(float v) {
        setVar(mExportVarIdx_parameter_B4, v);
    }


    private final static int mExportVarIdx_parameter_A5 = 31;
    public synchronized void set_parameter_A5(float v) {
        setVar(mExportVarIdx_parameter_A5, v);
    }


    private final static int mExportVarIdx_parameter_B5 = 32;
    public synchronized void set_parameter_B5(float v) {
        setVar(mExportVarIdx_parameter_B5, v);
    }


    private final static int mExportVarIdx_parameter_A6 = 33;
    public synchronized void set_parameter_A6(float v) {
        setVar(mExportVarIdx_parameter_A6, v);
    }

    private final static int mExportVarIdx_parameter_B6 = 34;
    public synchronized void set_parameter_B6(float v) {
        setVar(mExportVarIdx_parameter_B6, v);
    }



    private int mExportVar_tonemap_algorithm_clamp_c;
    public int get_tonemap_algorithm_clamp_c() {
        return mExportVar_tonemap_algorithm_clamp_c;
    }


    private int mExportVar_tonemap_algorithm_exponential_c;

    public int get_tonemap_algorithm_exponential_c() {
        return mExportVar_tonemap_algorithm_exponential_c;
    }


    private int mExportVar_tonemap_algorithm_reinhard_c;
    public int get_tonemap_algorithm_reinhard_c() {
        return mExportVar_tonemap_algorithm_reinhard_c;
    }

    private int mExportVar_tonemap_algorithm_filmic_c;
    public int get_tonemap_algorithm_filmic_c() {
        return mExportVar_tonemap_algorithm_filmic_c;
    }


    private int mExportVar_tonemap_algorithm_aces_c;
    public int get_tonemap_algorithm_aces_c() {
        return mExportVar_tonemap_algorithm_aces_c;
    }


    private final static int mExportVarIdx_tonemap_algorithm = 41;
    public synchronized void set_tonemap_algorithm(int v) {
        setVar(mExportVarIdx_tonemap_algorithm, v);
    }


    private float mExportVar_exposure;
    public float get_exposure() {
        return mExportVar_exposure;
    }

    private final static int mExportVarIdx_tonemap_scale = 43;
    public synchronized void set_tonemap_scale(float v) {
        setVar(mExportVarIdx_tonemap_scale, v);
    }

    private float mExportVar_filmic_exposure_bias;
    public float get_filmic_exposure_bias() {
        return mExportVar_filmic_exposure_bias;
    }


    private final static int mExportVarIdx_W = 45;
    public synchronized void set_W(float v) {
        setVar(mExportVarIdx_W, v);
    }

    private final static int mExportVarIdx_linear_scale = 46;
    public synchronized void set_linear_scale(float v) {
        setVar(mExportVarIdx_linear_scale, v);
    }


    private final static int mExportVarIdx_n_bitmaps_g = 47;
    public synchronized void set_n_bitmaps_g(int v) {
        setVar(mExportVarIdx_n_bitmaps_g, v);
    }

    private final static int mExportForEachIdx_hdr = 1;

    public void forEach_hdr(Allocation ain, Allocation aout) {
        forEach_hdr(ain, aout, null);
    }

    public void forEach_hdr(Allocation ain, Allocation aout, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        Type t0, t1;        // Verify dimensions
        t0 = ain.getType();
        t1 = aout.getType();
        if ((t0.getCount() != t1.getCount()) ||
                (t0.getX() != t1.getX()) ||
                (t0.getY() != t1.getY()) ||
                (t0.getZ() != t1.getZ()) ||
                (t0.hasFaces()   != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_hdr, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_hdr_n = 2;

    public void forEach_hdr_n(Allocation ain, Allocation aout) {
        forEach_hdr_n(ain, aout, null);
    }

    public void forEach_hdr_n(Allocation ain, Allocation aout, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        Type t0, t1;        // Verify dimensions
        t0 = ain.getType();
        t1 = aout.getType();
        if ((t0.getCount() != t1.getCount()) ||
                (t0.getX() != t1.getX()) ||
                (t0.getY() != t1.getY()) ||
                (t0.getZ() != t1.getZ()) ||
                (t0.hasFaces()   != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_hdr_n, ain, aout, null, sc);
    }

}

