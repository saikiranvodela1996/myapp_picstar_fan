package com.picstar.picstarapp.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.picstar.picstarapp.R;
import com.picstar.picstarapp.utils.PSRConstants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class VideoPlayerActivity extends BaseActivity {
    @BindView(R.id.left_side_menu_option)
    ImageView leftSideMenu;
    SimpleExoPlayer simpleExoPlayer;
    PlayerView playerView;
    ProgressBar progressBar;
    ImageView buttondownload;
    String videoUrl1 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_video_view);
        ButterKnife.bind(this);
        leftSideMenu.setImageResource(R.drawable.ic_back);
        playerView = findViewById(R.id.exoplayer_fullscreen);
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        if (getIntent() != null) {
            videoUrl1 = getIntent().getStringExtra(PSRConstants.VIDEOURL);
            //getVideoFromHttp(videoUrl);
        }
       Uri videoUrl = Uri.parse(videoUrl1);
      //  Uri uri= Uri.parse("content://media/external_primary/video/media/158887");

       // Uri uri=Uri.parse("content://com.android.providers.media.documents/document/video%3A158887");

      /*  Uri uri=Uri.parse("content://com.android.providers.media.documents/document/video%3A158522");
        String path=PSRUtils.getPath(this,uri);
        uri=Uri.parse(path);*/
        progressBar = playerView.findViewById(R.id.progress_Bar);


        LoadControl loadControl = new DefaultLoadControl();
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelector trackSelector = new DefaultTrackSelector(
                new AdaptiveTrackSelection.Factory(bandwidthMeter)
        );

        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(VideoPlayerActivity.this, trackSelector, loadControl);

        DefaultHttpDataSourceFactory factory = new DefaultHttpDataSourceFactory("exoplayer_video");


        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource mediaSource = new ExtractorMediaSource(videoUrl, factory, extractorsFactory, null, null);

        //SimpleExoPlayer simpleExoPlayer = new SimpleExoPlayer.Builder(this).build();

       // MediaSource mediaSource=buildMediaSource(uri);

        playerView.setPlayer(simpleExoPlayer);
        playerView.setKeepScreenOn(true);
        simpleExoPlayer.prepare(mediaSource);
        simpleExoPlayer.setPlayWhenReady(true);
        simpleExoPlayer.addListener(new Player.EventListener() {

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else if (playbackState == Player.STATE_READY) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {

            }
        });




    }







    private  MediaSource buildMediaSource(Uri uri)
    {
        DataSource.Factory datasf=new DefaultDataSourceFactory(this,getString(R.string.app_name));
        return new ProgressiveMediaSource.Factory(datasf).createMediaSource(uri);
    }

    @OnClick(R.id.left_side_menu_option)
    void onClickBack(View view) {
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        simpleExoPlayer.setPlayWhenReady(false);
        simpleExoPlayer.getPlaybackState();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        simpleExoPlayer.setPlayWhenReady(true);
        simpleExoPlayer.getPlaybackState();
    }
}