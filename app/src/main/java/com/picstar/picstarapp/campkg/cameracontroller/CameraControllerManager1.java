package com.picstar.picstarapp.campkg.cameracontroller;


import android.hardware.Camera;
import android.util.Log;

import com.picstar.picstarapp.campkg.others.MyDebug;


/** Provides support using Android's original camera API
 *  android.hardware.Camera.
 */
public class CameraControllerManager1 extends CameraControllerManager {
    private static final String TAG = "CControllerManager1";
    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public boolean isFrontFacing(int cameraId) {
        try {
            Camera.CameraInfo camera_info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, camera_info);
            return (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        }
        catch(RuntimeException e) {
            if( MyDebug.LOG )
                Log.d(TAG, "failed to set parameters");
            e.printStackTrace();
            return false;
        }
    }
    @Override
    public CameraController.Facing getFacing(int cameraId) {
        try {
            Camera.CameraInfo camera_info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, camera_info);
            switch( camera_info.facing ) {
                case Camera.CameraInfo.CAMERA_FACING_FRONT:
                    return CameraController.Facing.FACING_FRONT;
                case Camera.CameraInfo.CAMERA_FACING_BACK:
                    return CameraController.Facing.FACING_BACK;
            }
            Log.e(TAG, "unknown camera_facing: " + camera_info.facing);
        }
        catch(RuntimeException e) {
            // Had a report of this crashing on Galaxy Nexus - may be device specific issue, see http://stackoverflow.com/questions/22383708/java-lang-runtimeexception-fail-to-get-camera-info
            // but good to catch it anyway
            Log.e(TAG, "failed to get facing");
            e.printStackTrace();
        }
        return CameraController.Facing.FACING_UNKNOWN;
    }
}

