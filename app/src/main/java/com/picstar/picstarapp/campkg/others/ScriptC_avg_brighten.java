package com.picstar.picstarapp.campkg.others;


import android.os.Build;
import android.renderscript.*;
import androidx.annotation.RequiresApi;

/**
 * @hide
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScriptC_avg_brighten extends ScriptC {
    private static final String __rs_resource_name = "avg_brighten";

    // Constructor
    public ScriptC_avg_brighten(RenderScript rs) {
        super(rs,
                __rs_resource_name,
                avg_brightenBitCode.getBitCode32(),
                avg_brightenBitCode.getBitCode64());
        __F32_3 = Element.F32_3(rs);
        __U8_4 = Element.U8_4(rs);
    }

    private Element __F32_3;
    private Element __U8_4;
    private final static int mExportVarIdx_bitmap = 0;

    public synchronized void set_bitmap(Allocation v) {
        setVar(mExportVarIdx_bitmap, v);
    }


    private final static int mExportVarIdx_median_filter_strength = 1;

    public synchronized void set_median_filter_strength(float v) {
        setVar(mExportVarIdx_median_filter_strength, v);
    }

    //private final static int mExportForEachIdx_root = 0;
    private final static int mExportForEachIdx_avg_brighten_f = 1;

    public void forEach_avg_brighten_f(Allocation ain, Allocation aout) {
        forEach_avg_brighten_f(ain, aout, null);
    }

    public void forEach_avg_brighten_f(Allocation ain, Allocation aout, LaunchOptions sc) {
        // check ain
        if (!ain.getType().getElement().isCompatible(__F32_3)) {
            throw new RSRuntimeException("Type mismatch with F32_3!");
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

        forEach(mExportForEachIdx_avg_brighten_f, ain, aout, null, sc);
    }

    private final static int mExportForEachIdx_dro_brighten = 2;

    public void forEach_dro_brighten(Allocation ain, Allocation aout) {
        forEach_dro_brighten(ain, aout, null);
    }

    public void forEach_dro_brighten(Allocation ain, Allocation aout, LaunchOptions sc) {
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

        forEach(mExportForEachIdx_dro_brighten, ain, aout, null, sc);
    }

    private final static int mExportFuncIdx_setBlackLevel = 0;

    public void invoke_setBlackLevel(float value) {
        FieldPacker setBlackLevel_fp = new FieldPacker(4);
        setBlackLevel_fp.addF32(value);
        invoke(mExportFuncIdx_setBlackLevel, setBlackLevel_fp);
    }

    private final static int mExportFuncIdx_setBrightenParameters = 1;

    public void invoke_setBrightenParameters(float _gain, float _gamma, float _low_x, float _mid_x, float _max_x) {
        FieldPacker setBrightenParameters_fp = new FieldPacker(20);
        setBrightenParameters_fp.addF32(_gain);
        setBrightenParameters_fp.addF32(_gamma);
        setBrightenParameters_fp.addF32(_low_x);
        setBrightenParameters_fp.addF32(_mid_x);
        setBrightenParameters_fp.addF32(_max_x);
        invoke(mExportFuncIdx_setBrightenParameters, setBrightenParameters_fp);
    }

}


