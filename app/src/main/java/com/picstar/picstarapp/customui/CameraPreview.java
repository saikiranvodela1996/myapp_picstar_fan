package com.picstar.picstarapp.customui;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.helpers.PSR_PrefsManager;
import com.picstar.picstarapp.utils.PSRConstants;

import java.io.IOException;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG2 = "CameraPreview";
    private static Activity mContext;
    private final boolean portrait;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private List<Camera.Size> mSupportedPicSizes;
    private Camera.Size mPicSize;
    private Camera.Size mPreviewSize;
    private int mCameraId;

    public CameraPreview(Activity context, int cameraId) {
        super(context);
        mContext = context;
        portrait = isPortrait();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (Camera.getNumberOfCameras() > cameraId) {
                mCameraId = cameraId;
            } else {
                mCameraId = 0;
            }
        } else {
            mCameraId = 0;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                mCamera = Camera.open(mCameraId);
            } else {
                mCamera = Camera.open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Camera.Parameters cameraParams = mCamera.getParameters();
        mSupportedPreviewSizes = cameraParams.getSupportedPreviewSizes();
        mSupportedPicSizes = cameraParams.getSupportedPictureSizes();
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    mCamera = Camera.open(mCameraId);
                } else {
                    mCamera = Camera.open();
                }
            }
            mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null == mCamera) {
            return;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mHolder.getSurface() == null) {
            return;
        }
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            try {
                parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            } catch (Exception e) {
                e.printStackTrace();
                int optimalPreview[] = getOptimalPreviewSize(parameters);
                parameters.setPreviewSize(optimalPreview[0], optimalPreview[1]);
            }
            try {
                WindowManager wm = (WindowManager) getContext().getSystemService(WINDOW_SERVICE);
                final DisplayMetrics displayMetrics = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(displayMetrics);
                if (getResources().getDisplayMetrics().densityDpi == DisplayMetrics.DENSITY_XXXHIGH) {
                    parameters.setPictureSize(mPicSize.width, mPicSize.height);
                } else {
                    parameters.setPictureSize(mPreviewSize.width, mPreviewSize.height);
                }
            } catch (Exception e) {
                e.printStackTrace();

                int optimalPicture[] = getOptimalPicSize(parameters);
                parameters.setPictureSize(optimalPicture[0], optimalPicture[1]);
            }
            try {
                List<?> focus = parameters.getSupportedFocusModes();
                if (focus != null && focus.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                setCameraDisplayOrientation(mContext, mCameraId, mCamera, portrait);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG2, "Error starting camera preview: " + e.getMessage());
        }
    }

    private int[] getOptimalPicSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        int requiredPos = 0;
        if (sizes.size() >= 3) {
            requiredPos = sizes.size() / 2;
        }
        return new int[]{sizes.get(requiredPos).width, sizes.get(requiredPos).height};
    }

    private int[] getOptimalPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        int requiredPos = 0;
        if (sizes.size() >= 3) {
            requiredPos = sizes.size() / 2;
        }
        return new int[]{sizes.get(requiredPos).width, sizes.get(requiredPos).height};
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, true);
        }
        if (mSupportedPicSizes != null) {
            mPicSize = getOptimalPreviewSize(mSupportedPicSizes, false);
        }
    }

    /*private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, boolean isPreviewsize) {
        float bestSizeRatio=50000;
        Camera.Size optimalSize = null;
        for(Camera.Size size:sizes) {
            if(size.width*1000/size.height==1000) {
                optimalSize=size;
                break;
            } else {
                if( (size.width*1000/size.height)<bestSizeRatio){
                    bestSizeRatio=size.width*1000/size.height;
                    optimalSize=size;
                }
            }
        }
        return optimalSize;
    }*/

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, boolean isPreviewsize) {
        Display display = mContext.getWindowManager().getDefaultDisplay();
        Point sizee = new Point();
        display.getSize(sizee);
        int width = sizee.x;
        int height = sizee.y;
        if (isPreviewsize) {
            height = sizee.y - 250;
        }
        if (sizes == null) return null;
        Camera.Size optimalSize = null;
        long diff = (height * 1000 / width);
        long cdistance = Integer.MAX_VALUE;
        for (int i = 0; i < sizes.size(); i++) {
            long value = (long) (sizes.get(i).width * 1000) / sizes.get(i).height;
            if (value > diff && value < cdistance) {
                optimalSize = sizes.get(i);
                cdistance = value;
            }
        }
        if (optimalSize == null) {
            double minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }
        return optimalSize;
    }

    public void refreshCamera() {

        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            setCameraDisplayOrientation(mContext, mCameraId, mCamera, isPortrait());
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            // NTF_Utils.writeLog(mContext, "Exception during startPreview : " + e);
        }
    }

    public int setCameraDisplayOrientation(Activity activity, int mCameraId, Camera mCamera, boolean portrait) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        new PSR_PrefsManager(mContext).save(PSRConstants.Prefs_Keys.INFO_ORIENTATION, String.valueOf(info.orientation));

        mCamera.setDisplayOrientation(result);

        return result;
    }

    public boolean isPortrait() {
        return (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    public Camera getCamera() {
        return mCamera;
    }
}

