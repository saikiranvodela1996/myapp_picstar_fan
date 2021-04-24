package com.picstar.picstarapp.campkg.camui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;

import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerException;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.preview.CameraSurface;
import com.picstar.picstarapp.campkg.preview.Preview;


/** Provides support for the surface used for the preview, using a TextureView.
 */
public class MyTextureView extends TextureView implements CameraSurface {
    private static final String TAG = "MyTextureView";

    private final Preview preview;
    private final int [] measure_spec = new int[2];

    public MyTextureView(Context context, Preview preview) {
        super(context);
        this.preview = preview;
        this.setSurfaceTextureListener(preview);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void setPreviewDisplay(CameraController camera_controller) {
        if( MyDebug.LOG )
            Log.d(TAG, "setPreviewDisplay");
        try {
            camera_controller.setPreviewTexture(this);
        }
        catch(CameraControllerException e) {
            if( MyDebug.LOG )
                Log.e(TAG, "Failed to set preview display: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return preview.touchEvent(event);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if( MyDebug.LOG )
            Log.d(TAG, "onMeasure: " + widthSpec + " x " + heightSpec);
        preview.getMeasureSpec(measure_spec, widthSpec, heightSpec);
        super.onMeasure(measure_spec[0], measure_spec[1]);
    }

    @Override
    public void setTransform(Matrix matrix) {
        super.setTransform(matrix);
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }
}
