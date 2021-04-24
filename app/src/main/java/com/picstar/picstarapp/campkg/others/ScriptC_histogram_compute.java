package com.picstar.picstarapp.campkg.others;

import android.os.Build;
import android.os.Process;
import java.lang.reflect.Field;
import android.renderscript.*;

import androidx.annotation.RequiresApi;

/**
 * @hide
 */
public class ScriptC_histogram_compute extends ScriptC {
    private static final String __rs_resource_name = "histogram_compute";
    // Constructor
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public  ScriptC_histogram_compute(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                histogram_computeBitCode.getBitCode32(),
                histogram_computeBitCode.getBitCode64());
        mExportVar_zebra_stripes_threshold = 255;
        mExportVar_zebra_stripes_width = 40;
        __U8_4 = Element.U8_4(rs);
        __F32_3 = Element.F32_3(rs);
    }

    private Element __F32_3;
    private Element __U8_4;
    private final static int mExportVarIdx_histogram = 0;
    public void bind_histogram(Allocation v) {
        if (v == null) bindAllocation(null, mExportVarIdx_histogram);
        else bindAllocation(v, mExportVarIdx_histogram);
    }


    private final static int mExportVarIdx_histogram_r = 1;
    public void bind_histogram_r(Allocation v) {
        if (v == null) bindAllocation(null, mExportVarIdx_histogram_r);
        else bindAllocation(v, mExportVarIdx_histogram_r);
    }


    private final static int mExportVarIdx_histogram_g = 2;
    public void bind_histogram_g(Allocation v) {
        if (v == null) bindAllocation(null, mExportVarIdx_histogram_g);
        else bindAllocation(v, mExportVarIdx_histogram_g);
    }


    private final static int mExportVarIdx_histogram_b = 3;
    public void bind_histogram_b(Allocation v) {
        if (v == null) bindAllocation(null, mExportVarIdx_histogram_b);
        else bindAllocation(v, mExportVarIdx_histogram_b);
    }


    private final static int mExportVarIdx_zebra_stripes_threshold = 4;
    private int mExportVar_zebra_stripes_threshold;
    public synchronized void set_zebra_stripes_threshold(int v) {
        setVar(mExportVarIdx_zebra_stripes_threshold, v);
        mExportVar_zebra_stripes_threshold = v;
    }


    private final static int mExportVarIdx_zebra_stripes_width = 5;
    private int mExportVar_zebra_stripes_width;
    public synchronized void set_zebra_stripes_width(int v) {
        setVar(mExportVarIdx_zebra_stripes_width, v);
        mExportVar_zebra_stripes_width = v;
    }

    private final static int mExportVarIdx_bitmap = 6;
    private Allocation mExportVar_bitmap;
    public synchronized void set_bitmap(Allocation v) {
        setVar(mExportVarIdx_bitmap, v);
        mExportVar_bitmap = v;
    }


    //private final static int mExportForEachIdx_root = 0;
    private final static int mExportForEachIdx_histogram_compute_by_luminance = 1;
    public void forEach_histogram_compute_by_luminance(Allocation ain) {
        forEach_histogram_compute_by_luminance(ain, null);
    }

    public void forEach_histogram_compute_by_luminance(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_by_luminance, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_histogram_compute_by_value = 2;

    public void forEach_histogram_compute_by_value(Allocation ain) {
        forEach_histogram_compute_by_value(ain, null);
    }

    public void forEach_histogram_compute_by_value(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_by_value, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_histogram_compute_by_value_f = 3;

    public void forEach_histogram_compute_by_value_f(Allocation ain) {
        forEach_histogram_compute_by_value_f(ain, null);
    }

    public void forEach_histogram_compute_by_value_f(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_by_value_f, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_histogram_compute_by_intensity = 4;

    public void forEach_histogram_compute_by_intensity(Allocation ain) {
        forEach_histogram_compute_by_intensity(ain, null);
    }

    public void forEach_histogram_compute_by_intensity(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_by_intensity, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_histogram_compute_by_intensity_f = 5;

    public void forEach_histogram_compute_by_intensity_f(Allocation ain) {
        forEach_histogram_compute_by_intensity_f(ain, null);
    }

    public void forEach_histogram_compute_by_intensity_f(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_by_intensity_f, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_histogram_compute_by_lightness = 6;

    public void forEach_histogram_compute_by_lightness(Allocation ain) {
        forEach_histogram_compute_by_lightness(ain, null);
    }

    public void forEach_histogram_compute_by_lightness(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_by_lightness, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_histogram_compute_rgb = 7;

    public void forEach_histogram_compute_rgb(Allocation ain) {
        forEach_histogram_compute_rgb(ain, null);
    }

    public void forEach_histogram_compute_rgb(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_histogram_compute_rgb, ain, null, null, sc);
        }
    }

    private final static int mExportForEachIdx_generate_zebra_stripes = 8;

    public void forEach_generate_zebra_stripes(Allocation ain, Allocation aout) {
        forEach_generate_zebra_stripes(ain, aout, null);
    }

    public void forEach_generate_zebra_stripes(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_generate_zebra_stripes, ain, aout, null, sc);
        }
    }

    private final static int mExportForEachIdx_generate_focus_peaking = 9;

    public void forEach_generate_focus_peaking(Allocation ain, Allocation aout) {
        forEach_generate_focus_peaking(ain, aout, null);
    }

    public void forEach_generate_focus_peaking(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_generate_focus_peaking, ain, aout, null, sc);
        }
    }

    private final static int mExportForEachIdx_generate_focus_peaking_filtered = 10;

    public void forEach_generate_focus_peaking_filtered(Allocation ain, Allocation aout) {
        forEach_generate_focus_peaking_filtered(ain, aout, null);
    }

    public void forEach_generate_focus_peaking_filtered(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            forEach(mExportForEachIdx_generate_focus_peaking_filtered, ain, aout, null, sc);
        }
    }

    private final static int mExportFuncIdx_init_histogram = 0;
    public void invoke_init_histogram() {
        invoke(mExportFuncIdx_init_histogram);
    }

    private final static int mExportFuncIdx_init_histogram_rgb = 1;
    public void invoke_init_histogram_rgb() {
        invoke(mExportFuncIdx_init_histogram_rgb);
    }

}

