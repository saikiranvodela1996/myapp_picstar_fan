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
public class ScriptC_pyramid_blending extends ScriptC {
    private static final String __rs_resource_name = "pyramid_blending";

    // Constructor
    public ScriptC_pyramid_blending(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                pyramid_blendingBitCode.getBitCode32(),
                pyramid_blendingBitCode.getBitCode64());
        __U8_4 = Element.U8_4(rs);
        __F32_3 = Element.F32_3(rs);
    }

    private Element __F32_3;
    private Element __U8_4;
    private final static int mExportVarIdx_bitmap = 0;
    private Allocation mExportVar_bitmap;

    public synchronized void set_bitmap(Allocation v) {
        setVar(mExportVarIdx_bitmap, v);
        mExportVar_bitmap = v;
    }


    private final static int mExportVarIdx_interpolated_best_path = 4;

    public void bind_interpolated_best_path(Allocation v) {
        if (v == null) bindAllocation(null, mExportVarIdx_interpolated_best_path);
        else bindAllocation(v, mExportVarIdx_interpolated_best_path);
    }


    private final static int mExportVarIdx_errors = 5;

    public void bind_errors(Allocation v) {
        if (v == null) bindAllocation(null, mExportVarIdx_errors);
        else bindAllocation(v, mExportVarIdx_errors);
    }

    private final static int mExportForEachIdx_reduce = 1;

    public void forEach_reduce(Allocation ain, Allocation aout) {
        forEach_reduce(ain, aout, null);
    }

    public void forEach_reduce(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_reduce, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_expand = 2;

    public void forEach_expand(Allocation ain, Allocation aout) {
        forEach_expand(ain, aout, null);
    }

    public void forEach_expand(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_expand, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_blur = 3;

    public void forEach_blur(Allocation ain, Allocation aout) {
        forEach_blur(ain, aout, null);
    }

    public void forEach_blur(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_blur, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_blur1dX = 4;

    public void forEach_blur1dX(Allocation ain, Allocation aout) {
        forEach_blur1dX(ain, aout, null);
    }

    public void forEach_blur1dX(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_blur1dX, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_blur1dY = 5;

    public void forEach_blur1dY(Allocation ain, Allocation aout) {
        forEach_blur1dY(ain, aout, null);
    }

    public void forEach_blur1dY(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_blur1dY, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_subtract = 6;

    public void forEach_subtract(Allocation ain, Allocation aout) {
        forEach_subtract(ain, aout, null);
    }

    public void forEach_subtract(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        forEach(mExportForEachIdx_subtract, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_add = 7;

    public void forEach_add(Allocation ain, Allocation aout) {
        forEach_add(ain, aout, null);
    }

    public void forEach_add(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_add, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_merge = 8;

    public void forEach_merge(Allocation ain, Allocation aout) {
        forEach_merge(ain, aout, null);
    }

    public void forEach_merge(Allocation ain, Allocation aout, LaunchOptions sc) {
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
                (t0.hasFaces() != t1.hasFaces()) ||
                (t0.hasMipmaps() != t1.hasMipmaps())) {
            throw new RSRuntimeException("Dimension mismatch between parameters ain and aout!");
        }

        forEach(mExportForEachIdx_merge, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_merge_f = 9;

    public void forEach_merge_f(Allocation ain, Allocation aout) {
        forEach_merge_f(ain, aout, null);
    }

    public void forEach_merge_f(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        forEach(mExportForEachIdx_merge_f, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_compute_error = 10;

    public void forEach_compute_error(Allocation ain, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        forEach(mExportForEachIdx_compute_error, ain, null, null, sc);
    }

    private final static int mExportFuncIdx_setBlendWidth = 0;

    public void invoke_setBlendWidth(int blend_width, int full_width) {
        FieldPacker setBlendWidth_fp = new FieldPacker(8);
        setBlendWidth_fp.addI32(blend_width);
        setBlendWidth_fp.addI32(full_width);
        invoke(mExportFuncIdx_setBlendWidth, setBlendWidth_fp);
    }

    private final static int mExportFuncIdx_init_errors = 1;

    public void invoke_init_errors() {
        invoke(mExportFuncIdx_init_errors);
    }

}



