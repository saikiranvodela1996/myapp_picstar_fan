package com.picstar.picstarapp.campkg.others;


public class PreferenceKeys {

    /** If this preference is set, no longer show the intro dialog.
     */
    public static final String FirstTimePreferenceKey = "done_first_time";

    /** This preference stores the version number seen by the user - used to show "What's New" dialog.
     */
    public static final String LatestVersionPreferenceKey = "latest_version";

    /** If this preference is set, no longer show the auto-stabilise info dialog.
     */
    public static final String AutoStabiliseInfoPreferenceKey = "done_auto_stabilise_info";

    /** If this preference is set, no longer show the dialog for poor magnetic accuracy
     */
    public static final String MagneticAccuracyPreferenceKey = "done_magnetic_accuracy";

    public static final String CameraAPIPreferenceDefault = "preference_camera_api_old";
    public static final String CameraAPIPreferenceKey = "preference_camera_api";
    public static final String MultiCamButtonPreferenceKey = "preference_multi_cam_button";

    public static String getFlashPreferenceKey(int cameraId) {
        return "flash_value_" + cameraId;
    }

    public static String getFocusPreferenceKey(int cameraId, boolean is_video) {
        return "focus_value_" + cameraId + "_" + is_video;
    }

    public static String getResolutionPreferenceKey(int cameraId) {
        return "camera_resolution_" + cameraId;
    }


    public static final String ImageFormatPreferenceKey = "preference_image_format";


    public static final String ExposurePreferenceKey = "preference_exposure";


    public static final String SceneModePreferenceKey = "preference_scene_mode";

    public static final String ISOPreferenceKey = "preference_iso";

    public static final String ExposureTimePreferenceKey = "preference_exposure_time";

    public static final String RawPreferenceKey = "preference_raw";

    public static final String AllowRawForExpoBracketingPreferenceKey = "preference_raw_expo_bracketing";

    public static final String AllowRawForFocusBracketingPreferenceKey = "preference_raw_focus_bracketing";

    public static final String PanoramaCropPreferenceKey = "preference_panorama_crop";

    public static final String PanoramaSaveExpoPreferenceKey = "preference_panorama_save";

    public static final String ExpoBracketingNImagesPreferenceKey = "preference_expo_bracketing_n_images";

    public static final String ExpoBracketingStopsPreferenceKey = "preference_expo_bracketing_stops";

    public static final String FocusDistancePreferenceKey = "preference_focus_distance";

    public static final String FocusBracketingTargetDistancePreferenceKey = "preference_focus_bracketing_target_distance";

    public static final String FocusBracketingNImagesPreferenceKey = "preference_focus_bracketing_n_images";

    public static final String FocusBracketingAddInfinityPreferenceKey = "preference_focus_bracketing_add_infinity";

    public static final String VolumeKeysPreferenceKey = "preference_volume_keys";

    public static final String AudioControlPreferenceKey = "preference_audio_control";

    public static final String QualityPreferenceKey = "preference_quality";

    public static final String AutoStabilisePreferenceKey = "preference_auto_stabilise";

    public static final String PhotoModePreferenceKey = "preference_photo_mode";

    public static final String HDRSaveExpoPreferenceKey = "preference_hdr_save_expo";

    public static final String HDRContrastEnhancementPreferenceKey = "preference_hdr_contrast_enhancement";

    public static final String NRSaveExpoPreferenceKey = "preference_nr_save";

    public static final String FastBurstNImagesPreferenceKey = "preference_fast_burst_n_images";

    public static final String LocationPreferenceKey = "preference_location";

    public static final String GPSDirectionPreferenceKey = "preference_gps_direction";

    public static final String RequireLocationPreferenceKey = "preference_require_location";

    public static final String ExifArtistPreferenceKey = "preference_exif_artist";

    public static final String ExifCopyrightPreferenceKey = "preference_exif_copyright";

    public static final String StampPreferenceKey = "preference_stamp";

    public static final String StampDateFormatPreferenceKey = "preference_stamp_dateformat";

    public static final String StampTimeFormatPreferenceKey = "preference_stamp_timeformat";

    public static final String StampGPSFormatPreferenceKey = "preference_stamp_gpsformat";

    public static final String StampGeoAddressPreferenceKey = "preference_stamp_geo_address";

    public static final String UnitsDistancePreferenceKey = "preference_units_distance";

    public static final String TextStampPreferenceKey = "preference_textstamp";

    public static final String StampFontSizePreferenceKey = "preference_stamp_fontsize";

    public static final String StampFontColorPreferenceKey = "preference_stamp_font_color";

    public static final String StampStyleKey = "preference_stamp_style";

    public static final String FrontCameraMirrorKey = "preference_front_camera_mirror";

    public static final String WaterType = "preference_water_type";

    public static final String Camera2FakeFlashPreferenceKey = "preference_camera2_fake_flash";

    public static final String Camera2FastBurstPreferenceKey = "preference_camera2_fast_burst";

    public static final String UIPlacementPreferenceKey = "preference_ui_placement";

    public static final String TouchCapturePreferenceKey = "preference_touch_capture";

    public static final String PausePreviewPreferenceKey = "preference_pause_preview";

    public static final String ThumbnailAnimationPreferenceKey = "preference_thumbnail_animation";

    public static final String TakePhotoBorderPreferenceKey = "preference_take_photo_border";

    public static String getShowWhenLockedPreferenceKey() {
        return "preference_show_when_locked";
    }

    public static String getStartupFocusPreferenceKey() {
        return "preference_startup_focus";
    }

    public static String getKeepDisplayOnPreferenceKey() {
        return "preference_keep_display_on";
    }

    public static String getMaxBrightnessPreferenceKey() {
        return "preference_max_brightness";
    }

    public static String getUsingSAFPreferenceKey() {
        return "preference_using_saf";
    }

    public static String getSaveLocationPreferenceKey() {
        return "preference_save_location";
    }

    public static String getSaveLocationSAFPreferenceKey() {
        return "preference_save_location_saf";
    }

    public static String getSavePhotoPrefixPreferenceKey() {
        return "preference_save_photo_prefix";
    }

    public static String getSaveVideoPrefixPreferenceKey() {
        return "preference_save_video_prefix";
    }

    public static String getSaveZuluTimePreferenceKey() {
        return "preference_save_zulu_time";
    }

    public static final String ShowTakePhotoPreferenceKey = "preference_show_take_photo";

    public static final String ShowZoomPreferenceKey = "preference_show_zoom";

    public static final String ShowISOPreferenceKey = "preference_show_iso";

    public static final String HistogramPreferenceKey = "preference_histogram";

    public static final String ZebraStripesPreferenceKey = "preference_zebra_stripes";

    public static final String FocusPeakingPreferenceKey = "preference_focus_peaking";

    public static final String FocusPeakingColorPreferenceKey = "preference_focus_peaking_color";

    public static final String ShowVideoMaxAmpPreferenceKey = "preference_show_video_max_amp";

    public static final String ShowAnglePreferenceKey = "preference_show_angle";

    public static final String ShowAngleLinePreferenceKey = "preference_show_angle_line";

    public static final String ShowPitchLinesPreferenceKey = "preference_show_pitch_lines";

    public static final String ShowGeoDirectionLinesPreferenceKey = "preference_show_geo_direction_lines";

    public static final String ShowAngleHighlightColorPreferenceKey = "preference_angle_highlight_color";

    public static final String CalibratedLevelAnglePreferenceKey = "preference_calibrate_level_angle";

    public static final String ShowGeoDirectionPreferenceKey = "preference_show_geo_direction";

    public static final String ShowFreeMemoryPreferenceKey = "preference_free_memory";

    public static final String ShowTimePreferenceKey = "preference_show_time";

    public static final String ShowBatteryPreferenceKey = "preference_show_battery";

    public static final String ShowGridPreferenceKey = "preference_grid";

    public static final String ShowCropGuidePreferenceKey = "preference_crop_guide";

    public static final String FaceDetectionPreferenceKey = "preference_face_detection";

    public static final String GhostImagePreferenceKey = "preference_ghost_image";

    public static final String GhostSelectedImageSAFPreferenceKey = "preference_ghost_selected_image_saf";


    public static String getRecordAudioPreferenceKey() {
        return "preference_record_audio";
    }

    public static final String PreviewSizePreferenceKey = "preference_preview_size";

    public static String getRotatePreviewPreferenceKey() {
        return "preference_rotate_preview";
    }

    public static String getLockOrientationPreferenceKey() {
        return "preference_lock_orientation";
    }

    public static String getTimerPreferenceKey() {
        return "preference_timer";
    }

    public static String getTimerBeepPreferenceKey() {
        return "preference_timer_beep";
    }

    public static String getTimerSpeakPreferenceKey() {
        return "preference_timer_speak";
    }

    public static String getRepeatModePreferenceKey() {
        return "preference_burst_mode";
    }

    public static String getRepeatIntervalPreferenceKey() {
        return "preference_burst_interval";
    }

    public static String getShutterSoundPreferenceKey() {
        return "preference_shutter_sound";
    }

    public static final String ImmersiveModePreferenceKey = "preference_immersive_mode";
}


