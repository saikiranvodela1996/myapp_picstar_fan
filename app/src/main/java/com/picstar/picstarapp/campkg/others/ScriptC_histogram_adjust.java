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
public class ScriptC_histogram_adjust extends ScriptC {
    private static final String __rs_resource_name = "histogram_adjust";
    // Constructor
    public  ScriptC_histogram_adjust(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                histogram_adjustBitCode.getBitCode32(),
                histogram_adjustBitCode.getBitCode64());
        __U8_4 = Element.U8_4(rs);
    }

    private Element __U8_4;
    private final static int mExportVarIdx_c_histogram = 0;
    private Allocation mExportVar_c_histogram;
    public synchronized void set_c_histogram(Allocation v) {
        setVar(mExportVarIdx_c_histogram, v);
        mExportVar_c_histogram = v;
    }


    private final static int mExportVarIdx_hdr_alpha = 1;
    public synchronized void set_hdr_alpha(float v) {
        setVar(mExportVarIdx_hdr_alpha, v);
    }


    private final static int mExportVarIdx_n_tiles = 2;
    public synchronized void set_n_tiles(int v) {
        setVar(mExportVarIdx_n_tiles, v);
    }

    private final static int mExportVarIdx_width = 3;
    public synchronized void set_width(int v) {
        setVar(mExportVarIdx_width, v);
    }


    private final static int mExportVarIdx_height = 4;
    public synchronized void set_height(int v) {
        setVar(mExportVarIdx_height, v);
    }

    //private final static int mExportForEachIdx_root = 0;
    private final static int mExportForEachIdx_histogram_adjust = 1;

    public void forEach_histogram_adjust(Allocation ain, Allocation aout) {
        forEach_histogram_adjust(ain, aout, null);
    }

    public void forEach_histogram_adjust(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        forEach(mExportForEachIdx_histogram_adjust, ain, aout, null, sc);
    }

}


