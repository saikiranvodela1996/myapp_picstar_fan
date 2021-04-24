
package com.picstar.picstarapp.liveselfiecam;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import com.picstar.picstarapp.R;
import com.picstar.picstarapp.activities.LiveSelfieCameraActivity;
import com.picstar.picstarapp.campkg.cameracontroller.CameraController;
import com.picstar.picstarapp.campkg.others.MyDebug;
import com.picstar.picstarapp.campkg.others.PreferenceKeys;

/** This contains functionality related to the main UI.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainUI {
    private static final String TAG = "MainUI";
    private final LiveSelfieCameraActivity main_activity;
    private volatile boolean popup_view_is_open; // must be volatile for test project reading the state
    private int current_orientation;

    enum UIPlacement {
        UIPLACEMENT_RIGHT,
        UIPLACEMENT_LEFT,
        UIPLACEMENT_TOP
    }

    private UIPlacement ui_placement = UIPlacement.UIPLACEMENT_RIGHT;

    private boolean immersive_mode;
    private boolean show_gui_photo = true; // result of call to showGUI() - false means a "reduced" GUI is displayed, whilst taking photo or video
    private boolean show_gui_video = true;

    private boolean keydown_volume_up;
    private boolean keydown_volume_down;
    private boolean remote_control_mode; // whether remote control mode is enabled
    private View mHighlightedIcon;
    private boolean mSelectingIcons = false;

    public MainUI(LiveSelfieCameraActivity main_activity) {
        if (MyDebug.LOG)
            Log.d(TAG, "MainUI");
        this.main_activity = main_activity;

    }

    public void layoutUI() {
        layoutUI(false);
    }

    private UIPlacement computeUIPlacement() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
        String ui_placement_string = sharedPreferences.getString(PreferenceKeys.UIPlacementPreferenceKey, "ui_top");
        switch (ui_placement_string) {
            case "ui_left":
                return UIPlacement.UIPLACEMENT_LEFT;
            case "ui_top":
                return UIPlacement.UIPLACEMENT_TOP;
            default:
                return UIPlacement.UIPLACEMENT_RIGHT;
        }
    }

    private void layoutUI(boolean popup_container_only) {
        long debug_time = 0;
        if (MyDebug.LOG) {
            Log.d(TAG, "layoutUI");
            debug_time = System.currentTimeMillis();
        }

        this.ui_placement = computeUIPlacement();
        int rotation = main_activity.getWindowManager().getDefaultDisplay().getRotation();
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
            default:
                break;
        }
        int relative_orientation = (current_orientation + degrees) % 360;
        if (MyDebug.LOG) {
            Log.d(TAG, "    current_orientation = " + current_orientation);
            Log.d(TAG, "    degrees = " + degrees);
            Log.d(TAG, "    relative_orientation = " + relative_orientation);
        }
        final int ui_rotation = (360 - relative_orientation) % 360;
        main_activity.getPreview().setUIRotation(ui_rotation);
    }

    public void setTakePhotoIcon() {
        if( MyDebug.LOG )
            Log.d(TAG, "setTakePhotoIcon()");
        if( main_activity.getPreview() != null ) {
            ImageView view = main_activity.findViewById(R.id.take_photo);
            int resource;
            int content_description;
            if( main_activity.getApplicationInterface().getPhotoMode() == MyApplicationInterface.PhotoMode.Panorama &&
                    main_activity.getApplicationInterface().getGyroSensor().isRecording() ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "set icon to recording panorama");
                resource = R.drawable.ic_launcher_background;
                content_description = R.string.finish_panorama;
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "set icon to photo");
                resource = R.drawable.ic_capture;
                content_description = R.string.take_photo;
            }
            view.setImageResource(resource);
            view.setContentDescription( main_activity.getResources().getString(content_description) );
            view.setTag(resource); // for testing
        }
    }

    public void onOrientationChanged(int orientation) {
        if( orientation == OrientationEventListener.ORIENTATION_UNKNOWN )
            return;
        int diff = Math.abs(orientation - current_orientation);
        if( diff > 180 )
            diff = 360 - diff;
        if( diff > 60 ) {
            orientation = (orientation + 45) / 90 * 90;
            orientation = orientation % 360;
            if( orientation != current_orientation ) {
                this.current_orientation = orientation;
                if( MyDebug.LOG ) {
                    Log.d(TAG, "current_orientation is now: " + current_orientation);
                }
                layoutUI();
            }
        }
    }

    public void setImmersiveMode(final boolean immersive_mode) {
       /* if( MyDebug.LOG )
            Log.d(TAG, "setImmersiveMode: " + immersive_mode);
        this.immersive_mode = immersive_mode;
        main_activity.runOnUiThread(new Runnable() {
            public void run() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
                final int visibility = immersive_mode ? View.GONE : View.VISIBLE;
                if( MyDebug.LOG )
                    Log.d(TAG, "setImmersiveMode: set visibility: " + visibility);
                View galleryButton = main_activity.findViewById(R.id.gallery);
                galleryButton.setVisibility(visibility);
                if( MyDebug.LOG ) {
                    Log.d(TAG, "has_zoom: " + main_activity.getPreview().supportsZoom());
                }
                String pref_immersive_mode = sharedPreferences.getString(PreferenceKeys.ImmersiveModePreferenceKey, "immersive_mode_low_profile");
                if( pref_immersive_mode.equals("immersive_mode_everything") ) {
                    if( sharedPreferences.getBoolean(PreferenceKeys.ShowTakePhotoPreferenceKey, true) ) {
                        View takePhotoButton = main_activity.findViewById(R.id.take_photo);
                        takePhotoButton.setVisibility(visibility);
                    }
                }
                if( !immersive_mode ) {
                    showGUI();
                }
            }
        });*/
    }

    public boolean inImmersiveMode() {
        return immersive_mode;
    }

    public void showGUI(final boolean show, final boolean is_video) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "showGUI: " + show);
            Log.d(TAG, "is_video: " + is_video);
        }
        if( is_video )
            this.show_gui_video = show;
        else
            this.show_gui_photo = show;
        showGUI();
    }

    public void showGUI() {
        if( MyDebug.LOG ) {
            Log.d(TAG, "showGUI");
            Log.d(TAG, "show_gui_photo: " + show_gui_photo);
            Log.d(TAG, "show_gui_video: " + show_gui_video);
        }
        if( inImmersiveMode() )
            return;
        if( (show_gui_photo || show_gui_video) && main_activity.usingKitKatImmersiveMode() ) {
            // call to reset the timer
            main_activity.initImmersiveMode();
        }
        main_activity.runOnUiThread(new Runnable() {
            public void run() {
                final boolean is_panorama_recording = main_activity.getApplicationInterface().getGyroSensor().isRecording();
                if( !(show_gui_photo && show_gui_video) ) {
                    closePopup(); // we still allow the popup when recording video, but need to update the UI (so it only shows flash options), so easiest to just close
                }
                if( show_gui_photo && show_gui_video ) {
                    layoutUI(); // needed for "top" UIPlacement, to auto-arrange the buttons
                }
            }
        });
    }

    /**
     * Opens or close the exposure settings (ISO, white balance, etc)
     */
    public void toggleExposureUI() {
        if( MyDebug.LOG )
            Log.d(TAG, "toggleExposureUI");
        closePopup();
        if( main_activity.getPreview().getCameraController() != null ) {
        }
    }

    public boolean processRemoteUpButton() {
        if( MyDebug.LOG )
            Log.d(TAG, "processRemoteUpButton");
        boolean didProcess = false;
        if (popupIsOpen()) {
            didProcess = true;
        }
        return didProcess;
    }

    public boolean processRemoteDownButton() {
        if( MyDebug.LOG )
            Log.d(TAG, "processRemoteDownButton");
        boolean didProcess = false;
        if (popupIsOpen()) {
            didProcess = true;
        }
        return didProcess;
    }

    public void closePopup() {
        if( MyDebug.LOG )
            Log.d(TAG, "close popup");
        if( popupIsOpen() ) {
            clearRemoteControlForPopup(); // must be called before we set popup_view_is_open to false; and before clearSelectionState() so we know which highlighting to disable
            clearSelectionState();

            popup_view_is_open = false;
            main_activity.initImmersiveMode(); // to reset the timer when closing the popup
        }
    }

    public boolean popupIsOpen() {
        return popup_view_is_open;
    }

    public boolean selectingIcons() {
        return mSelectingIcons;
    }


    private void clickSelectedIcon() {
        if( MyDebug.LOG )
            Log.d(TAG, "clickSelectedIcon: " + mHighlightedIcon);
        if (mHighlightedIcon != null) {
            mHighlightedIcon.callOnClick();
        }
    }

    private void clearSelectionState() {
        if( MyDebug.LOG )
            Log.d(TAG, "clearSelectionState");
        mSelectingIcons = false;
        mHighlightedIcon= null;
    }

    private void initRemoteControlForPopup() {
        if( MyDebug.LOG )
            Log.d(TAG, "initRemoteControlForPopup");
        if( popupIsOpen() ) {
            clearSelectionState();
            remote_control_mode = true;
        }
    }

    private void clearRemoteControlForPopup() {
        if( MyDebug.LOG )
            Log.d(TAG, "clearRemoteControlForPopup");
        if( popupIsOpen() && remote_control_mode ) {
            remote_control_mode = false;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if( MyDebug.LOG )
            Log.d(TAG, "onKeyDown: " + keyCode);
        switch( keyCode ) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: // media codes are for "selfie sticks" buttons
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            {
                if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
                    keydown_volume_up = true;
                else if( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN )
                    keydown_volume_down = true;

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
                String volume_keys = sharedPreferences.getString(PreferenceKeys.VolumeKeysPreferenceKey, "volume_take_photo");

                if((keyCode==KeyEvent.KEYCODE_MEDIA_PREVIOUS
                        ||keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        ||keyCode==KeyEvent.KEYCODE_MEDIA_STOP)
                        &&!(volume_keys.equals("volume_take_photo"))) {
                    AudioManager audioManager = (AudioManager) main_activity.getSystemService(Context.AUDIO_SERVICE);
                    if(audioManager==null) break;
                    if(!audioManager.isWiredHeadsetOn()) break; // isWiredHeadsetOn() is deprecated, but comment says "Use only to check is a headset is connected or not."
                }

                switch(volume_keys) {
                    case "volume_take_photo":
                        main_activity.takePicture(false);
                        return true;
                    case "volume_focus":
                        if(keydown_volume_up && keydown_volume_down) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "take photo rather than focus, as both volume keys are down");
                            main_activity.takePicture(false);
                        }
                        else if (main_activity.getPreview().getCurrentFocusValue() != null && main_activity.getPreview().getCurrentFocusValue().equals("focus_mode_manual2")) {
                        }
                        else {
                            if(event.getDownTime() == event.getEventTime() && !main_activity.getPreview().isFocusWaiting()) {
                                if(MyDebug.LOG)
                                    Log.d(TAG, "request focus due to volume key");
                                main_activity.getPreview().requestAutoFocus();
                            }
                        }
                        return true;
                    case "volume_exposure":
                        if(main_activity.getPreview().getCameraController() != null) {
                            String value = sharedPreferences.getString(PreferenceKeys.ISOPreferenceKey, CameraController.ISO_DEFAULT);
                            if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                            }
                            else {
                            }
                        }
                        return true;
                    case "volume_auto_stabilise":
                        if( main_activity.supportsAutoStabilise() ) {
                            boolean auto_stabilise = sharedPreferences.getBoolean(PreferenceKeys.AutoStabilisePreferenceKey, false);
                            auto_stabilise = !auto_stabilise;
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(PreferenceKeys.AutoStabilisePreferenceKey, auto_stabilise);
                            editor.apply();
                            main_activity.getApplicationInterface().getDrawPreview().updateSettings(); // because we cache the auto-stabilise setting
                        }
                        return true;
                    case "volume_really_nothing":
                        // do nothing, but still return true so we don't change volume either
                        return true;
                }
                // else do nothing here, but still allow changing of volume (i.e., the default behaviour)
                break;
            }
            case KeyEvent.KEYCODE_MENU:
            {
                main_activity.openSettings();
                return true;
            }
            case KeyEvent.KEYCODE_CAMERA:
            {
                if( event.getRepeatCount() == 0 ) {
                    main_activity.takePicture(false);
                    return true;
                }
            }
            case KeyEvent.KEYCODE_FOCUS:
            {
                if( event.getDownTime() == event.getEventTime() && !main_activity.getPreview().isFocusWaiting() ) {
                    if( MyDebug.LOG )
                        Log.d(TAG, "request focus due to focus key");
                    main_activity.getPreview().requestAutoFocus();
                }
                return true;
            }
            case KeyEvent.KEYCODE_ZOOM_IN:
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_ZOOM_OUT:
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_NUMPAD_5:
            {
                if( popupIsOpen() && remote_control_mode ) {
                    commandMenuPopup();
                    return true;
                }
                else if( event.getRepeatCount() == 0 ) {
                    main_activity.takePicture(false);
                    return true;
                }
                break;
            }
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_NUMPAD_8:
                //case KeyEvent.KEYCODE_VOLUME_UP: // test
                if( !remote_control_mode ) {
                    if( popupIsOpen() ) {
                        initRemoteControlForPopup();
                        return true;
                    }
                }
                else if( processRemoteUpButton() )
                    return true;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_NUMPAD_2:
                //case KeyEvent.KEYCODE_VOLUME_DOWN: // test
                if( !remote_control_mode ) {
                    if( popupIsOpen() ) {
                        initRemoteControlForPopup();
                        return true;
                    }
                }
                else if( processRemoteDownButton() )
                    return true;
                break;
            case KeyEvent.KEYCODE_SLASH:
            case KeyEvent.KEYCODE_NUMPAD_DIVIDE:
                toggleExposureUI();
                break;
        }
        return false;
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if( MyDebug.LOG )
            Log.d(TAG, "onKeyUp: " + keyCode);
        if( keyCode == KeyEvent.KEYCODE_VOLUME_UP )
            keydown_volume_up = false;
        else if( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN )
            keydown_volume_down = false;
    }


    public void commandMenuPopup() {
        if( MyDebug.LOG )
            Log.d(TAG, "commandMenuPopup");
        if( popupIsOpen() ) {
            if( selectingIcons() ) {
                clickSelectedIcon();
            }
        }
    }
    public AlertDialog showInfoDialog(int title_id, int info_id, final String info_preference_key) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(main_activity);
        alertDialog.setTitle(title_id);
        if( info_id != 0 )
            alertDialog.setMessage(info_id);
        alertDialog.setPositiveButton(android.R.string.ok, null);
        alertDialog.setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if( MyDebug.LOG )
                    Log.d(TAG, "user clicked dont_show_again for info dialog");
                final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(main_activity);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(info_preference_key, true);
                editor.apply();
            }
        });

        main_activity.setWindowFlagsForSettings(false); // set set_lock_protect to false, otherwise if screen is locked, user will need to unlock to see the info dialog!
        AlertDialog alert = alertDialog.create();
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                if( MyDebug.LOG )
                    Log.d(TAG, "info dialog dismissed");
                main_activity.setWindowFlagsForCamera();
            }
        });
        main_activity.showAlert(alert);
        return alert;
    }

    public String getEntryForAntiBanding(String value) {
        int id = -1;
        switch( value ) {
            case CameraController.ANTIBANDING_DEFAULT:
                id = R.string.anti_banding_auto;
                break;
            case "50hz":
                id = R.string.anti_banding_50hz;
                break;
            case "60hz":
                id = R.string.anti_banding_60hz;
                break;
            case "off":
                id = R.string.anti_banding_off;
                break;
            default:
                break;
        }
        String entry;
        if( id != -1 ) {
            entry = main_activity.getResources().getString(id);
        }
        else {
            entry = value;
        }
        return entry;
    }

    public String getEntryForNoiseReductionMode(String value) {
        int id = -1;
        switch( value ) {
            case CameraController.NOISE_REDUCTION_MODE_DEFAULT:
                id = R.string.noise_reduction_mode_default;
                break;
            case "off":
                id = R.string.noise_reduction_mode_off;
                break;
            case "minimal":
                id = R.string.noise_reduction_mode_minimal;
                break;
            case "fast":
                id = R.string.noise_reduction_mode_fast;
                break;
            case "high_quality":
                id = R.string.noise_reduction_mode_high_quality;
                break;
            default:
                break;
        }
        String entry;
        if( id != -1 ) {
            entry = main_activity.getResources().getString(id);
        }
        else {
            entry = value;
        }
        return entry;
    }
}

