package com.picstar.picstarapp.campkg.others;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/** Handles gyro sensor.
 */
public class GyroSensor implements SensorEventListener {
    private static final String TAG = "GyroSensor";

    final private SensorManager mSensorManager;
    final private Sensor mSensor;
    final private Sensor mSensorAccel;

    private boolean is_recording;
    private long timestamp;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float [] deltaRotationVector = new float[4];
    private boolean has_gyroVector;
    private final float [] gyroVector = new float[3];
    private final float [] currentRotationMatrix = new float[9];
    private final float [] currentRotationMatrixGyroOnly = new float[9];
    private final float [] deltaRotationMatrix = new float[9];
    private final float [] tempMatrix = new float[9];
    private final float [] temp2Matrix = new float[9];

    private boolean has_init_accel = false;
    private final float [] initAccelVector = new float[3];
    private final float [] accelVector = new float[3];

    private boolean has_original_rotation_matrix;
    private final float [] originalRotationMatrix = new float[9];
    private boolean has_rotationVector;
    private final float [] rotationVector = new float[3];

    private final float [] tempVector = new float[3];
    private final float [] inVector = new float[3];

    public interface TargetCallback {

        void onAchieved(int indx);
        void onTooFar();
    }

    private boolean hasTarget;
    private final List<float []> targetVectors = new ArrayList<>();
    private float targetAngle; // target angle in radians
    private float uprightAngleTol; // in radians
    private boolean targetAchieved;
    private float tooFarAngle; // in radians
    private TargetCallback targetCallback;
    private boolean has_lastTargetAngle;
    private float lastTargetAngle;
    private int is_upright; // if hasTarget==true, this stores whether the "upright" orientation of the device is close enough to the orientation when recording was started: 0 for yes, otherwise -1 for too anti-clockwise, +1 for too clockwise

    GyroSensor(Context context) {
        mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if( MyDebug.LOG ) {
            Log.d(TAG, "GyroSensor");
            if( mSensor == null )
                Log.d(TAG, "gyroscope not available");
            else if( mSensorAccel == null )
                Log.d(TAG, "accelerometer not available");
        }
        setToIdentity();
    }

    public boolean hasSensors() {
        // even though the gyro sensor works if mSensorAccel is not present, for best behaviour we require them both
        return mSensor != null && mSensorAccel != null;
    }

    private void setToIdentity() {
        for(int i=0;i<9;i++) {
            currentRotationMatrix[i] = 0.0f;
        }
        currentRotationMatrix[0] = 1.0f;
        currentRotationMatrix[4] = 1.0f;
        currentRotationMatrix[8] = 1.0f;
        System.arraycopy(currentRotationMatrix, 0, currentRotationMatrixGyroOnly, 0, 9);

        for(int i=0;i<3;i++) {
            initAccelVector[i] = 0.0f;
            // don't set accelVector, rotationVector, gyroVector to 0 here, as we continually smooth the values even when not recording
        }
        has_init_accel = false;
        has_original_rotation_matrix = false;
    }

    /** Helper method to set a 3D vector.
     */
    static void setVector(final float[] vector, float x, float y, float z) {
        vector[0] = x;
        vector[1] = y;
        vector[2] = z;
    }

    private static float getMatrixComponent(final float [] matrix, int row, int col) {
        return matrix[row*3+col];
    }

    private static void setMatrixComponent(final float [] matrix, int row, int col, float value) {
        matrix[row*3+col] = value;
    }

    public static void transformVector(final float [] result, final float [] matrix, final float [] vector) {
        // result[i] = matrix[ij] . vector[j]
        for(int i=0;i<3;i++) {
            result[i] = 0.0f;
            for(int j=0;j<3;j++) {
                result[i] += getMatrixComponent(matrix, i, j) * vector[j];
            }
        }
    }

    private void transformTransposeVector(final float [] result, final float [] matrix, final float [] vector) {
        for(int i=0;i<3;i++) {
            result[i] = 0.0f;
            for(int j=0;j<3;j++) {
                result[i] += getMatrixComponent(matrix, j, i) * vector[j];
            }
        }
    }

    public void enableSensors() {
        if( MyDebug.LOG )
            Log.d(TAG, "enableSensors");
        has_rotationVector = false;
        has_gyroVector = false;
        for(int i=0;i<3;i++) {
            accelVector[i] = 0.0f;
            rotationVector[i] = 0.0f;
            gyroVector[i] = 0.0f;
        }

        if( mSensor != null )
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
        if( mSensorAccel != null )
            mSensorManager.registerListener(this, mSensorAccel, SensorManager.SENSOR_DELAY_UI);
    }

    public void disableSensors() {
        if( MyDebug.LOG )
            Log.d(TAG, "disableSensors");
        mSensorManager.unregisterListener(this);
    }

    void startRecording() {
        if( MyDebug.LOG )
            Log.d(TAG, "startRecording");
        is_recording = true;
        timestamp = 0;
        setToIdentity();
    }

    void stopRecording() {
        if( is_recording ) {
            if( MyDebug.LOG )
                Log.d(TAG, "stopRecording");
            is_recording = false;
            timestamp = 0;
        }
    }

    public boolean isRecording() {
        return this.is_recording;
    }

    void setTarget(float target_x, float target_y, float target_z, float targetAngle, float uprightAngleTol, float tooFarAngle, TargetCallback targetCallback) {
        this.hasTarget = true;
        this.targetVectors.clear();
        addTarget(target_x, target_y, target_z);
        this.targetAngle = targetAngle;
        this.uprightAngleTol = uprightAngleTol;
        this.tooFarAngle = tooFarAngle;
        this.targetCallback = targetCallback;
        this.has_lastTargetAngle = false;
        this.lastTargetAngle = 0.0f;
    }

    void addTarget(float target_x, float target_y, float target_z) {
        float [] vector = new float[]{target_x, target_y, target_z};
        this.targetVectors.add(vector);
    }

    void clearTarget() {
        this.hasTarget = false;
        this.targetVectors.clear();
        this.targetCallback = null;
        this.has_lastTargetAngle = false;
        this.lastTargetAngle = 0.0f;
    }

    void disableTargetCallback() {
        this.targetCallback = null;
    }

    boolean hasTarget() {
        return this.hasTarget;
    }

    boolean isTargetAchieved() {
        return this.hasTarget && this.targetAchieved;
    }

    public int isUpright() {
        return this.is_upright;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void adjustGyroForAccel() {
        if( timestamp == 0 ) {
            // don't have a gyro matrix yet
            return;
        }
        else if( !has_init_accel ) {
            return;
        }
        transformVector(tempVector, currentRotationMatrix, accelVector);
        double cos_angle = (tempVector[0] * initAccelVector[0] + tempVector[1] * initAccelVector[1] + tempVector[2] * initAccelVector[2]);
        if( cos_angle >= 0.99999999995 ) {
            return;
        }

        double angle = Math.acos(cos_angle);
        angle *= 0.02f; // filter
        cos_angle = Math.cos(angle);

        double a_x = tempVector[1] * initAccelVector[2] - tempVector[2] * initAccelVector[1];
        double a_y = tempVector[2] * initAccelVector[0] - tempVector[0] * initAccelVector[2];
        double a_z = tempVector[0] * initAccelVector[1] - tempVector[1] * initAccelVector[0];
        double a_mag = Math.sqrt(a_x*a_x + a_y*a_y + a_z*a_z);
        if( a_mag < 1.0e-5 ) {
            return;
        }
        a_x /= a_mag;
        a_y /= a_mag;
        a_z /= a_mag;
        double sin_angle = Math.sqrt(1.0-cos_angle*cos_angle);
        setMatrixComponent(tempMatrix, 0, 0, (float)(a_x*a_x*(1.0-cos_angle)+cos_angle));
        setMatrixComponent(tempMatrix, 0, 1, (float)(a_x*a_y*(1.0-cos_angle)-sin_angle*a_z));
        setMatrixComponent(tempMatrix, 0, 2, (float)(a_x*a_z*(1.0-cos_angle)+sin_angle*a_y));
        setMatrixComponent(tempMatrix, 1, 0, (float)(a_x*a_y*(1.0-cos_angle)+sin_angle*a_z));
        setMatrixComponent(tempMatrix, 1, 1, (float)(a_y*a_y*(1.0-cos_angle)+cos_angle));
        setMatrixComponent(tempMatrix, 1, 2, (float)(a_y*a_z*(1.0-cos_angle)-sin_angle*a_x));
        setMatrixComponent(tempMatrix, 2, 0, (float)(a_x*a_z*(1.0-cos_angle)-sin_angle*a_y));
        setMatrixComponent(tempMatrix, 2, 1, (float)(a_y*a_z*(1.0-cos_angle)+sin_angle*a_x));
        setMatrixComponent(tempMatrix, 2, 2, (float)(a_z*a_z*(1.0-cos_angle)+cos_angle));
        for(int i=0;i<3;i++) {
            for(int j=0;j<3;j++) {
                float value = 0.0f;
                // temp2Matrix[ij] = tempMatrix[ik] * currentRotationMatrix[kj]
                for(int k=0;k<3;k++) {
                    value += getMatrixComponent(tempMatrix, i, k) * getMatrixComponent(currentRotationMatrix, k, j);
                }
                setMatrixComponent(temp2Matrix, i, j, value);
            }
        }

        System.arraycopy(temp2Matrix, 0, currentRotationMatrix, 0, 9);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*if( MyDebug.LOG )
            Log.d(TAG, "onSensorChanged: " + event);*/
        if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            final float sensor_alpha = 0.8f; // for filter
            for(int i=0;i<3;i++) {
                //this.accelVector[i] = event.values[i];
                this.accelVector[i] = sensor_alpha * this.accelVector[i] + (1.0f-sensor_alpha) * event.values[i];
            }

            double mag = Math.sqrt(accelVector[0]*accelVector[0] + accelVector[1]*accelVector[1] + accelVector[2]*accelVector[2]);
            if( mag > 1.0e-8 ) {
                accelVector[0] /= mag;
                accelVector[1] /= mag;
                accelVector[2] /= mag;
            }

            if( !has_init_accel ) {
                System.arraycopy(accelVector, 0, initAccelVector, 0, 3);
                has_init_accel = true;
            }

            adjustGyroForAccel();
        }
        else if( event.sensor.getType() == Sensor.TYPE_GYROSCOPE ) {
            if( has_gyroVector ) {
                final float sensor_alpha = 0.5f; // for filter
                for(int i=0;i<3;i++) {
                    //this.gyroVector[i] = event.values[i];
                    this.gyroVector[i] = sensor_alpha * this.gyroVector[i] + (1.0f-sensor_alpha) * event.values[i];
                }
            }
            else {
                System.arraycopy(event.values, 0, this.gyroVector, 0, 3);
                has_gyroVector = true;
            }
            if( timestamp != 0 ) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = gyroVector[0];
                float axisY = gyroVector[1];
                float axisZ = gyroVector[2];
                double omegaMagnitude = Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                if( omegaMagnitude > 1.0e-5 ) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }
                double thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
                SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
                for(int i=0;i<3;i++) {
                    for(int j=0;j<3;j++) {
                        float value = 0.0f;
                        for(int k=0;k<3;k++) {
                            value += getMatrixComponent(currentRotationMatrix, i, k) * getMatrixComponent(deltaRotationMatrix, k, j);
                        }
                        setMatrixComponent(tempMatrix, i, j, value);
                    }
                }

                System.arraycopy(tempMatrix, 0, currentRotationMatrix, 0, 9);

                for(int i=0;i<3;i++) {
                    for(int j=0;j<3;j++) {
                        float value = 0.0f;
                        for(int k=0;k<3;k++) {
                            value += getMatrixComponent(currentRotationMatrixGyroOnly, i, k) * getMatrixComponent(deltaRotationMatrix, k, j);
                        }
                        setMatrixComponent(tempMatrix, i, j, value);
                    }
                }
                System.arraycopy(tempMatrix, 0, currentRotationMatrixGyroOnly, 0, 9);
                adjustGyroForAccel();

            }

            timestamp = event.timestamp;
        }
        else if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR || event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR ) {
            if( has_rotationVector ) {
                final float sensor_alpha = 0.8f; // for filter
                for(int i=0;i<3;i++) {
                    this.rotationVector[i] = sensor_alpha * this.rotationVector[i] + (1.0f-sensor_alpha) * event.values[i];
                }
            }
            else {
                System.arraycopy(event.values, 0, this.rotationVector, 0, 3);
                has_rotationVector = true;
            }

            SensorManager.getRotationMatrixFromVector(tempMatrix, rotationVector);

            if( !has_original_rotation_matrix ) {
                System.arraycopy(tempMatrix, 0, originalRotationMatrix, 0, 9);
                has_original_rotation_matrix = true;
                if( event.values[3] == 1.0 ) {
                    has_original_rotation_matrix = false;
                }
            }

            for(int i=0;i<3;i++) {
                for(int j=0;j<3;j++) {
                    float value = 0.0f;
                    for(int k=0;k<3;k++) {
                        value += getMatrixComponent(originalRotationMatrix, k, i) * getMatrixComponent(tempMatrix, k, j);
                    }
                    setMatrixComponent(currentRotationMatrix, i, j, value);
                }
            }

            if( MyDebug.LOG ) {
                Log.d(TAG, "### values: " + event.values[0] + " , " + event.values[1] + " , " + event.values[2] + " , " + event.values[3]);
                Log.d(TAG, "    " + currentRotationMatrix[0] + " , " + currentRotationMatrix[1] + " , " + currentRotationMatrix[2]);
                Log.d(TAG, "    " + currentRotationMatrix[3] + " , " + currentRotationMatrix[4] + " , " + currentRotationMatrix[5]);
                Log.d(TAG, "    " + currentRotationMatrix[6] + " , " + currentRotationMatrix[7] + " , " + currentRotationMatrix[8]);
            }
        }

        if( hasTarget ) {
            int n_too_far = 0;
            targetAchieved = false;
            for(int indx=0;indx<targetVectors.size();indx++) {
                float [] targetVector = targetVectors.get(indx);
                setVector(inVector, 0.0f, 1.0f, 0.0f); // vector pointing in "up" direction
                transformVector(tempVector, currentRotationMatrix, inVector);
                is_upright = 0;

                float ux = tempVector[0];
                float uy = tempVector[1];
                float uz = tempVector[2];

                float u_dot_n = ux * targetVector[0] + uy * targetVector[1] + uz * targetVector[2];
                float p_ux = ux - u_dot_n * targetVector[0];
                float p_uy = uy - u_dot_n * targetVector[1];
                float p_uz = uz - u_dot_n * targetVector[2];
                double p_u_mag = Math.sqrt(p_ux*p_ux + p_uy*p_uy + p_uz*p_uz);
                if( p_u_mag > 1.0e-5 ) {
                    p_ux /= p_u_mag;
                    p_uz /= p_u_mag;
                    float cx = - p_uz;
                    float cy = 0.0f;
                    float cz = p_ux;
                    float sin_angle_up = (float)Math.sqrt(cx*cx + cy*cy + cz*cz);
                    float angle_up = (float)Math.asin(sin_angle_up);

                    setVector(inVector, 0.0f, 0.0f, -1.0f); // vector pointing behind the device's screen
                    transformVector(tempVector, currentRotationMatrix, inVector);

                    if( Math.abs(angle_up) > this.uprightAngleTol ) {
                        float dot = cx*tempVector[0] + cy*tempVector[1] + cz*tempVector[2];
                        is_upright = (dot < 0) ? 1 : -1;
                    }
                }

                float cos_angle = tempVector[0] * targetVector[0] + tempVector[1] * targetVector[1] + tempVector[2] * targetVector[2];
                float angle = (float)Math.acos(cos_angle);
                if( is_upright == 0 ) {
                    /*if( MyDebug.LOG )
                        Log.d(TAG, "gyro vector angle with target: " + Math.toDegrees(angle) + " degrees");*/
                    if( angle <= targetAngle ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "    ### achieved target angle: " + Math.toDegrees(angle) + " degrees");
                        targetAchieved = true;
                        if( targetCallback != null ) {
                            //targetCallback.onAchieved(indx);
                            if( has_lastTargetAngle ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "        last target angle: " + Math.toDegrees(lastTargetAngle) + " degrees");
                                if( angle > lastTargetAngle ) {
                                    // started to get worse, so call callback
                                    targetCallback.onAchieved(indx);
                                }
                                // else, don't call callback yet, as we may get closer to the target
                            }
                        }
                        // only bother setting the lastTargetAngle if within the target angle - otherwise we'll have problems if there is more than one target set
                        has_lastTargetAngle = true;
                        lastTargetAngle = angle;
                    }
                }

                if( angle > tooFarAngle ) {
                    n_too_far++;
                }
            }
            if( n_too_far > 0 && n_too_far == targetVectors.size() ) {
                if( targetCallback != null ) {
                    targetCallback.onTooFar();
                }
            }
        }
    }



    public void getRelativeInverseVector(float [] out, float [] in) {
        transformTransposeVector(out, currentRotationMatrix, in);
    }

    public void getRotationMatrix(float [] out) {
        System.arraycopy(currentRotationMatrix, 0, out, 0, 9);
    }


}
