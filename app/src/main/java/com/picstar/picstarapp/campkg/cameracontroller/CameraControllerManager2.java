package com.picstar.picstarapp.campkg.cameracontroller;


import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Log;

import com.picstar.picstarapp.campkg.others.MyDebug;


/** Provides support using Android 5's Camera 2 API
 *  android.hardware.camera2.*.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraControllerManager2 extends CameraControllerManager {
    private static final String TAG = "CControllerManager2";

    private final Context context;

    public CameraControllerManager2(Context context) {
        this.context = context;
    }

    @Override
    public int getNumberOfCameras() {
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
            return manager.getCameraIdList().length;
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean isFrontFacing(int cameraId) {
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraIdS = manager.getCameraIdList()[cameraId];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
            return characteristics.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_FRONT;
        }
        catch(Throwable e) {
            if( MyDebug.LOG )
                Log.e(TAG, "exception trying to get camera characteristics");
            e.printStackTrace();
        }
        return false;
    }
    static boolean isHardwareLevelSupported(CameraCharacteristics c, int requiredLevel) {
        int deviceLevel = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if( MyDebug.LOG ) {
            switch (deviceLevel) {
                case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    Log.d(TAG, "Camera has LEGACY Camera2 support");
                    break;
                case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                    Log.d(TAG, "Camera has EXTERNAL Camera2 support");
                    break;
                case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    Log.d(TAG, "Camera has LIMITED Camera2 support");
                    break;
                case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    Log.d(TAG, "Camera has FULL Camera2 support");
                    break;
                case CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                    Log.d(TAG, "Camera has Level 3 Camera2 support");
                    break;
                default:
                    Log.d(TAG, "Camera has unknown Camera2 support: " + deviceLevel);
                    break;
            }
        }

        // need to treat legacy and external as special cases; otherwise can then use numerical comparison

        if( deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ) {
            return requiredLevel == deviceLevel;
        }

        if( deviceLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ) {
            deviceLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
        }
        if( requiredLevel == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ) {
            requiredLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
        }

        return requiredLevel <= deviceLevel;
    }

    public boolean allowCamera2Support(int cameraId) {
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraIdS = manager.getCameraIdList()[cameraId];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
            return isHardwareLevelSupported(characteristics, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
        }
        catch(Throwable e) {

            if( MyDebug.LOG )
                Log.e(TAG, "exception trying to get camera characteristics");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public CameraController.Facing getFacing(int cameraId) {
        CameraManager manager = (CameraManager)context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraIdS = manager.getCameraIdList()[cameraId];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdS);
            switch( characteristics.get(CameraCharacteristics.LENS_FACING) ) {
                case CameraMetadata.LENS_FACING_FRONT:
                    return CameraController.Facing.FACING_FRONT;
                case CameraMetadata.LENS_FACING_BACK:
                    return CameraController.Facing.FACING_BACK;
                case CameraMetadata.LENS_FACING_EXTERNAL:
                    return CameraController.Facing.FACING_EXTERNAL;
            }
            Log.e(TAG, "unknown camera_facing: " + characteristics.get(CameraCharacteristics.LENS_FACING));
        }
        catch(Throwable e) {
            // in theory we should only get CameraAccessException, but Google Play shows we can get a variety of exceptions
            // from some devices, e.g., AssertionError, IllegalArgumentException, RuntimeException, so just catch everything!
            // We don't want users to experience a crash just because of buggy camera2 drivers - instead the user can switch
            // back to old camera API.
            if( MyDebug.LOG )
                Log.e(TAG, "exception trying to get camera characteristics");
            e.printStackTrace();
        }
        return CameraController.Facing.FACING_UNKNOWN;
    }
}

