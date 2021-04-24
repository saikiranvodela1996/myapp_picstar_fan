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
public class ScriptC_create_mtb extends ScriptC {
    private static final String __rs_resource_name = "create_mtb";
    // Constructor
    public  ScriptC_create_mtb(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                create_mtbBitCode.getBitCode32(),
                create_mtbBitCode.getBitCode64());
        mExportVar_start_y = 0;
        __U8_4 = Element.U8_4(rs);
        __F32_3 = Element.F32_3(rs);
    }

    private Element __F32_3;
    private Element __U8_4;
    private final static int mExportVarIdx_out_bitmap = 0;
    private Allocation mExportVar_out_bitmap;
    public synchronized void set_out_bitmap(Allocation v) {
        setVar(mExportVarIdx_out_bitmap, v);
        mExportVar_out_bitmap = v;
    }


    private final static int mExportVarIdx_median_value = 1;
    public synchronized void set_median_value(int v) {
        setVar(mExportVarIdx_median_value, v);
    }

    private final static int mExportVarIdx_start_x = 2;
    public synchronized void set_start_x(int v) {
        setVar(mExportVarIdx_start_x, v);
    }


    private final static int mExportVarIdx_start_y = 3;
    private int mExportVar_start_y;
    public synchronized void set_start_y(int v) {
        setVar(mExportVarIdx_start_y, v);
        mExportVar_start_y = v;
    }

    private final static int mExportForEachIdx_create_mtb = 1;
    public void forEach_create_mtb(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        forEach(mExportForEachIdx_create_mtb, ain, null, null, sc);
    }

    private final static int mExportForEachIdx_create_greyscale = 2;

    public void forEach_create_greyscale(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        forEach(mExportForEachIdx_create_greyscale, ain, null, null, sc);
    }

    private final static int mExportForEachIdx_create_greyscale_f = 3;

    public void forEach_create_greyscale_f(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
        }
        forEach(mExportForEachIdx_create_greyscale_f, ain, null, null, sc);
    }

}

