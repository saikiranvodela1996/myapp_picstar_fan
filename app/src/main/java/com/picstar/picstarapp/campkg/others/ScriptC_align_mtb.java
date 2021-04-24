package com.picstar.picstarapp.campkg.others;



import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.FieldPacker;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.Script;
import android.renderscript.ScriptC;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScriptC_align_mtb extends ScriptC {
    private static final String __rs_resource_name = "align_mtb";
    // Constructor
    public  ScriptC_align_mtb(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                align_mtbBitCode.getBitCode32(),
                align_mtbBitCode.getBitCode64());
        __U8 = Element.U8(rs);
    }

    private Element __U8;
    private final static int mExportVarIdx_bitmap0 = 0;
    public synchronized void set_bitmap0(Allocation v) {
        setVar(mExportVarIdx_bitmap0, v);
    }


    private final static int mExportVarIdx_bitmap1 = 1;
    public synchronized void set_bitmap1(Allocation v) {
        setVar(mExportVarIdx_bitmap1, v);
    }

    private final static int mExportVarIdx_step_size = 2;
    public synchronized void set_step_size(int v) {
        setVar(mExportVarIdx_step_size, v);
    }

    private final static int mExportVarIdx_off_x = 3;
    public synchronized void set_off_x(int v) {
        setVar(mExportVarIdx_off_x, v);
    }

    private final static int mExportVarIdx_off_y = 4;
    public synchronized void set_off_y(int v) {
        setVar(mExportVarIdx_off_y, v);
    }

    private final static int mExportVarIdx_errors = 5;
    private Allocation mExportVar_errors;
    public void bind_errors(Allocation v) {
        mExportVar_errors = v;
        if (v == null) bindAllocation(null, mExportVarIdx_errors);
        else bindAllocation(v, mExportVarIdx_errors);
    }


    //private final static int mExportForEachIdx_root = 0;
    private final static int mExportForEachIdx_align_mtb = 1;

    public void forEach_align_mtb(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8)) {
            throw new RSRuntimeException("Type mismatch with U8!");
        }
        forEach(mExportForEachIdx_align_mtb, ain, null, null, sc);
    }

    private final static int mExportForEachIdx_align = 2;

    public void forEach_align(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8)) {
            throw new RSRuntimeException("Type mismatch with U8!");
        }
        forEach(mExportForEachIdx_align, ain, null, null, sc);
    }

    private final static int mExportFuncIdx_init_errors = 0;
    public void invoke_init_errors() {
        invoke(mExportFuncIdx_init_errors);
    }

}


