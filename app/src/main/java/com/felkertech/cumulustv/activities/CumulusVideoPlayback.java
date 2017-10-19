package com.felkertech.cumulustv.activities;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.felkertech.commons.model.ChannelDatabase;
import com.felkertech.commons.model.JsonChannel;
import com.felkertech.commons.player.CumulusTvPlayer;
import com.felkertech.commons.player.CumulusWebPlayer;
import com.felkertech.commons.player.MediaSourceFactory;
import com.felkertech.n.cumulustv.R;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Built-in video playback activity. Just pass URL in an intent.
 */
public class CumulusVideoPlayback extends AppCompatActivity {
    private static final String TAG = CumulusVideoPlayback.class.getSimpleName();

    private String urlStream;
    private VideoView myVideoView;
    public static final String KEY_VIDEO_URL = "VIDEO_URL";
    private com.felkertech.commons.player.CumulusTvPlayer mTvPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* Doing it the native way */
        Intent parameters = getIntent();
        if(parameters == null) {
            setContentView(R.layout.fullvideo);//***************
            myVideoView = (VideoView) this.findViewById(R.id.myVideoView);
            MediaController mc = new MediaController(this);
            myVideoView.setMediaController(mc);
            String ABCNews = "http://abclive.abcnews.com/i/abc_live4@136330/index_1200_av-b.m3u8";
            String Brazil = "http://stream331.overseebrasil.com.br/live_previd_155/_definst_/live_previd_155/playlist.m3u8";
            String NASA = "http://www.nasa.gov/multimedia/nasatv/NTV-Public-IPS.m3u8";
            urlStream = ABCNews;
            Log.d(TAG, "About to open " + urlStream.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "On UI Thread");
                    myVideoView.setVideoURI(Uri.parse(urlStream));
                    Log.d(TAG, "Now play");
                    myVideoView.start();
                }
            });
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getSupportActionBar().hide();

            updateLauncherShortcut();

            urlStream = parameters.getStringExtra(KEY_VIDEO_URL);
            if(!urlStream.isEmpty()) {
                mTvPlayer = new CumulusTvPlayer(this);
                setContentView(R.layout.full_surfaceview);
                SurfaceView sv = (SurfaceView) findViewById(R.id.surface);
                mTvPlayer.setSurface(sv.getHolder().getSurface());
                mTvPlayer.setVolume(1);
                mTvPlayer.registerErrorListener(new CumulusTvPlayer.ErrorListener() {
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, error.toString());
                        if(error instanceof MediaSourceFactory.NotMediaException) {
                            Log.d(TAG, "Cannot play the stream, try loading it as a website");
                            Toast.makeText(CumulusVideoPlayback.this,
                                    R.string.msg_open_web,
                                    Toast.LENGTH_SHORT).show();
                            CumulusWebPlayer wv = new CumulusWebPlayer(CumulusVideoPlayback.this,
                                    new CumulusWebPlayer.WebViewListener() {
                                @Override
                                public void onPageFinished() {
                                    //Don't do anything
                                }
                            });
                            wv.load(urlStream);
                            setContentView(wv);
                        }
                    }
                });
                Log.d(TAG, "Start playing " + urlStream);
                mTvPlayer.startPlaying(Uri.parse(urlStream));
                mTvPlayer.play();
            } else {
                Toast.makeText(this, R.string.msg_no_url_found, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTvPlayer.stop();
        mTvPlayer.release();
    }

    private void updateLauncherShortcut() {
        // We will have one dynamic shortcut - to whichever stream was played last
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    JsonChannel jsonChannel = ChannelDatabase.getInstance(getApplicationContext())
                            .findChannelByMediaUrl(urlStream);
                    if (jsonChannel == null) {
                        // Don't create a shortcut because we don't have metadata
                        return;
                    }
                    Log.d(TAG, "Adding dynamic shortcut to " + jsonChannel.getName());
                    String logo = ChannelDatabase.getNonNullChannelLogo(jsonChannel);
                    try {
                        Bitmap logoBitmap = Glide.with(getApplicationContext())
                                .load(logo)
                                .asBitmap()
                                .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .get();
                        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                        shortcutManager.removeAllDynamicShortcuts();
                        Intent playVideo = new Intent(getApplicationContext(), CumulusVideoPlayback.class);
                        playVideo.setAction("play");
                        playVideo.putExtra(KEY_VIDEO_URL, urlStream);
                        ShortcutInfo shortcut = new ShortcutInfo.Builder(CumulusVideoPlayback.this, "id1")
                                .setShortLabel(jsonChannel.getName()+"")
                                .setLongLabel(jsonChannel.getNumber() + " - " + jsonChannel.getName())
                                .setIcon(Icon.createWithBitmap(logoBitmap))
                                .setIntent(playVideo)
                                .build();
                        shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
