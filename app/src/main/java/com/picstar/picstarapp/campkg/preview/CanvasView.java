package com.picstar.picstarapp.campkg.preview;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.View;

/** Overlay for the Preview - this just redirects to Preview.onDraw to do the
 *  work. Only used if using a MyTextureView (if using MySurfaceView, then that
 *  class can handle the onDraw()). TextureViews can't be used for both a
 *  camera preview, and used for drawing on.
 */
public class CanvasView extends View {
    private final Preview preview;
    private final int [] measure_spec = new int[2];
    private final Handler handler = new Handler();
    private final Runnable tick;

    CanvasView(Context context, final Preview preview) {
        super(context);
        this.preview = preview;
        tick = new Runnable() {
            public void run() {
                preview.test_ticker_called = true;
                invalidate();
                handler.postDelayed(this, preview.getFrameRate());
            }
        };
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

    void onPause() {
        handler.removeCallbacks(tick);
    }

    void onResume() {
        tick.run();
    }
}

