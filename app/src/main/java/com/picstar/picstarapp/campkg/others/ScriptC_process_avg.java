package com.picstar.picstarapp.campkg.others;


import android.os.Build;
import android.os.Process;

import java.lang.reflect.Field;

import android.renderscript.*;

import androidx.annotation.RequiresApi;

/**
 * @hide
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScriptC_process_avg extends ScriptC {
    private static final String __rs_resource_name = "process_avg";

    // Constructor
    public ScriptC_process_avg(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                process_avgBitCode.getBitCode32(),
                process_avgBitCode.getBitCode64());
        __U8_4 = Element.U8_4(rs);
        __F32_3 = Element.F32_3(rs);
    }

    private Element __F32_3;
    private Element __U8_4;
    private final static int mExportVarIdx_bitmap_new = 0;

    public synchronized void set_bitmap_new(Allocation v) {
        setVar(mExportVarIdx_bitmap_new, v);
    }

    private final static int mExportVarIdx_offset_x_new = 2;

    public synchronized void set_offset_x_new(int v) {
        setVar(mExportVarIdx_offset_x_new, v);
    }


    private final static int mExportVarIdx_offset_y_new = 3;

    public synchronized void set_offset_y_new(int v) {
        setVar(mExportVarIdx_offset_y_new, v);
    }


    private final static int mExportVarIdx_avg_factor = 5;

    public synchronized void set_avg_factor(float v) {
        setVar(mExportVarIdx_avg_factor, v);
    }


    private final static int mExportVarIdx_wiener_C = 6;

    public synchronized void set_wiener_C(float v) {
        setVar(mExportVarIdx_wiener_C, v);
    }

    private final static int mExportVarIdx_wiener_C_cutoff = 7;

    public synchronized void set_wiener_C_cutoff(float v) {
        setVar(mExportVarIdx_wiener_C_cutoff, v);
    }

    private final static int mExportForEachIdx_avg_f = 2;

    public void forEach_avg_f(Allocation ain, Allocation aout) {
        forEach_avg_f(ain, aout, null);
    }

    public void forEach_avg_f(Allocation ain, Allocation aout, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
        }
        Type t0, t1;        // Verify dimensions
        t0 = ain.getType();
        t1 = aout.getType();
        if ((t0.getCount() != t1.getCount()) ||
                (t0.getX() != t1.getX()) ||
                (t0.getY() != t1.getY()) ||
                (t0.getZ() != t1.getZ()) ||
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_avg_f, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_avg = 3;

    public void forEach_avg(Allocation ain, Allocation aout) {
        forEach_avg(ain, aout, null);
    }

    public void forEach_avg(Allocation ain, Allocation aout, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        // check aout
        if (!aout.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
        }
        Type t0, t1;        // Verify dimensions
        t0 = ain.getType();
        t1 = aout.getType();
        if ((t0.getCount() != t1.getCount()) ||
                (t0.getX() != t1.getX()) ||
                (t0.getY() != t1.getY()) ||
                (t0.getZ() != t1.getZ()) ||
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_avg, ain, aout, null, sc);
    }


}


