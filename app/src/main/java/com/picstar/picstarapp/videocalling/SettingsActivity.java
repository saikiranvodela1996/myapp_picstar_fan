package com.picstar.picstarapp.videocalling;


import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.picstar.picstarapp.R;
import com.twilio.video.OpusCodec;
import com.twilio.video.Vp8Codec;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREF_AUDIO_CODEC = "audio_codec";
    public static final String PREF_AUDIO_CODEC_DEFAULT = OpusCodec.NAME;
    public static final String PREF_VIDEO_CODEC = "video_codec";
    public static final String PREF_VIDEO_CODEC_DEFAULT = Vp8Codec.NAME;
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE = "sender_max_audio_bitrate";
    public static final String PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT = "0";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE = "sender_max_video_bitrate";
    public static final String PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT = "0";
    public static final String PREF_VP8_SIMULCAST = "vp8_simulcast";
    public static final String PREF_ENABLE_AUTOMATIC_SUBSCRIPTION = "enable_automatic_subscription";
    public static final boolean PREF_ENABLE_AUTOMATIC_SUBSCRIPTION_DEFAULT = true;
    public static final boolean PREF_VP8_SIMULCAST_DEFAULT = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videocalll_settings);
    }
}