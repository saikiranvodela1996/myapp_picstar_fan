package com.picstar.picstarapp.campkg.camui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.cameracontroller.CameraControllerException;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.preview.CameraSurface;
import com.picstar.picstarapp.campkg.preview.Preview;


/** Provides support for the surface used for the preview, using a SurfaceView.
 */
public class MySurfaceView extends SurfaceView implements CameraSurface {
    private static final String TAG = "MySurfaceView";

    private final Preview preview;
    private final int [] measure_spec = new int[2];
    private final Handler handler = new Handler();
    private final Runnable tick;

    public
    MySurfaceView(Context context, final Preview preview) {
        super(context);
        this.preview = preview;
        if( MyDebug.LOG ) {
            Log.d(TAG, "new MySurfaceView");
        }

        getHolder().addCallback(preview);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated

        tick = new Runnable() {
            public void run() {
                preview.test_ticker_called = true;
                invalidate();
                handler.postDelayed(this, preview.getFrameRate());
            }
        };
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
            camera_controller.setPreviewDisplay(this.getHolder());
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
    public void onDraw(Canvas canvas) {
        preview.draw(canvas);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        preview.getMeasureSpec(measure_spec, widthSpec, heightSpec);
        super.onMeasure(measure_spec[0], measure_spec[1]);
    }

    @Override
    public void setTransform(Matrix matrix) {
        throw new RuntimeException();
    }

    @Override
    public void onPause() {
        if( MyDebug.LOG )
            Log.d(TAG, "onPause()");
        handler.removeCallbacks(tick);
    }

    @Override
    public void onResume() {
        if( MyDebug.LOG )
            Log.d(TAG, "onResume()");
        tick.run();
    }
}
