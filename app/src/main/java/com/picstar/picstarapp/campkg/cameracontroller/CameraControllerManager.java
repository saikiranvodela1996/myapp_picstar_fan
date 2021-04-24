package com.picstar.picstarapp.campkg.cameracontroller;


public abstract class CameraControllerManager {
    public abstract int getNumberOfCameras();
    public abstract boolean isFrontFacing(int cameraId);
    public abstract CameraController.Facing getFacing(int cameraId);
}
