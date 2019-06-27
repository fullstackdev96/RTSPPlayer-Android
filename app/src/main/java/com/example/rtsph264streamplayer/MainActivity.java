
package com.example.rtsph264streamplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.rtsph264streamplayer.dialog.CustomDialog;
import com.google.gson.Gson;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;


public class MainActivity extends Activity implements IVLCVout.Callback {

    static SharedPreferences sp ;
    static ProgressBar pb;
    public Gson gson;
    public static int index = 0;

    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private int currentApiVersion;
    public static CustomDialog dialog;

    private static final String TAG = "MainActivity";
    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private static int CURRENT_SIZE = SURFACE_BEST_FIT;

    long numpad0Time, numpad1Time ,numpad2Time;
    static String path_first, path_second;
    private static Context context;
    private Thread mThread;
    private static boolean bSleepThread;
    private static boolean bStop;
    private static long mLastChecked;
    private static long mCurrentPostion;

    private static FrameLayout mVideoSurfaceFrame = null;
    private static SurfaceView mVideoSurface = null;

    private final Handler mHandler = new Handler();

    private static LibVLC mLibVLC = null;
    private static IVLCVout vlcVout = null;
    private static MediaPlayer mMediaPlayer = null;
    private static Media media = null;
    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mVideoSarNum = 0;
    private int mVideoSarDen = 0;
    private static int mReconnectCnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentApiVersion = android.os.Build.VERSION.SDK_INT;
        hideNavigationAndStatusbar();
        setContentView(R.layout.activity_main);

        context = this;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        pb = (ProgressBar) findViewById(R.id.progressbar);
        dialog = new CustomDialog(MainActivity.this);

        String url_first = sp.getString("url_first",null);
        String url_second = sp.getString("url_second",null);

        path_first = url_first;
        path_second = url_second;
        mLastChecked = 0;
        bSleepThread = false;

        bStop = false;

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        args.add("--no-drop-late-frames");
        args.add("--no-skip-frames");
        args.add("--network-caching=2000");
        args.add("--loop");
        args.add("--http-reconnect");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoSurfaceFrame = (FrameLayout) findViewById(R.id.video_surface_frame);
        mVideoSurface = (SurfaceView) findViewById(R.id.video_surface);
                vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mVideoSurface);
        vlcVout.attachViews();

        if (path_first == null || path_first == "" || path_second == null || path_second == "") {
            // Tell the user to provide a media file URL/path.
            Toast.makeText(MainActivity.this, "Please enter the RTSP URL/path", Toast.LENGTH_LONG).show();
            dialog.show();
            return;
        }
    }

    @Override
    protected void onRestart() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(flags);
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        bSleepThread = true;
        bStop = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mMediaPlayer.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
        mMediaPlayer.getVLCVout().removeCallback(this);
        mMediaPlayer.release();
        mLibVLC.release();
    }


    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    public void hideNavigationAndStatusbar(){
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                    {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility)
                        {
                            if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mVideoSarNum = sarNum;
        mVideoSarDen = sarDen;
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
        mVideoWidth = mVideoSurface.getWidth();  //Note: we do NOT want to use the entire display!
        mVideoHeight = mVideoSurface.getHeight();
        vlcVout.setWindowSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
    }

    //
    @Override
    protected void onResume() {
        super.onResume();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(flags);
        bStop = false;
        playback();
        if (mThread != null && mThread.isAlive())
        {
            super.onResume();
            return;
        }

        mThread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {

                        if (mMediaPlayer != null && !mMediaPlayer.isPlaying() && mLastChecked == 0 ) {
                            mLastChecked = System.currentTimeMillis();
                            continue;
                        }else if (mMediaPlayer != null && mMediaPlayer.isPlaying() && mLastChecked == 0 && mCurrentPostion >= mMediaPlayer.getTime()){
                            mLastChecked = System.currentTimeMillis();
                            continue;
                        }else if (mMediaPlayer != null && mMediaPlayer.isPlaying() && mCurrentPostion < mMediaPlayer.getTime()) {
                            mLastChecked = 0;
                        }

                        mCurrentPostion = mMediaPlayer.getTime();

                        Long now = System.currentTimeMillis();

                        if (mLastChecked > 0 && (mLastChecked + 60000) < now )
                        {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    // things to do on the main thread
                                    mLastChecked = 0;
                                    if (!dialog.isShowing()) {
                                        String msg;
                                        if(index == 1){
                                            msg = "connecting with primary IP Address...";
                                        }else{
                                            msg = "connecting with secondary IP Address...";
                                        }
                                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                                        playback();
                                    }
                                }
                            });
                        }
                        sleep(100);
                        while (bSleepThread)
                        {
                            sleep(100);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
//        mThread.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        checkKeys();
        if(!dialog.isShowing())
            dialog.show();
        return true;
    }

    private void checkKeys() {
        Long now = System.currentTimeMillis();
        if(now - numpad0Time < 500 &&
                now - numpad1Time < 500 &&
                now - numpad2Time < 500) {

            dialog.show();
        }
    }

    @Override
    public void onBackPressed() {
        bSleepThread = true;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mMediaPlayer.stop();
        mMediaPlayer.getVLCVout().detachViews();
        mMediaPlayer.getVLCVout().removeCallback(this);

        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        bSleepThread = true;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        onStop();
        if(media != null){
            media.release();
        }
        mMediaPlayer.stop();
        vlcVout.detachViews();
    }

    public static void playback(){
        pb.setVisibility(View.VISIBLE);
        mMediaPlayer.getVLCVout().addCallback((IVLCVout.Callback) context);

        vlcVout = mMediaPlayer.getVLCVout();
        if (!vlcVout.areViewsAttached()) {
            vlcVout.setVideoView(mVideoSurface);
            vlcVout.attachViews();
        }

        if(media != null){
            media.release();
        }

        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
        }

        String url_first = sp.getString("url_first",null);
        path_first = url_first;
        String url_second = sp.getString("url_second",null);
        path_second = url_second;

        if (!TextUtils.isEmpty(url_second) || path_first != null || !TextUtils.isEmpty(url_second) || path_second != null) {
            bSleepThread = true;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(index == 0){
                media = new Media(mLibVLC, Uri.parse(url_first));
            }else{
                media = new Media(mLibVLC, Uri.parse(url_second));
            }
            media.setHWDecoderEnabled(false, true);
            mMediaPlayer.setMedia(media);
            bSleepThread = false;

            mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
                @Override
                public void onEvent(MediaPlayer.Event event) {
                    switch (event.type) {
                        case MediaPlayer.Event.Buffering:
                            Log.d(TAG, "onEvent: Buffering");
                            break;
                        case MediaPlayer.Event.EncounteredError:
                            Log.d(TAG, "onEvent: EncounteredError");
                            break;
                        case MediaPlayer.Event.EndReached:
                            Log.d(TAG, "onEvent: EndReached");
                            break;
                        case MediaPlayer.Event.ESAdded:
                            Log.d(TAG, "onEvent: ESAdded");
                            break;
                        case MediaPlayer.Event.ESDeleted:
                            Log.d(TAG, "onEvent: ESDeleted");
                            break;
                        case MediaPlayer.Event.MediaChanged:
                            Log.d(TAG, "onEvent: MediaChanged");
                            break;
                        case MediaPlayer.Event.Opening:
                            Log.d(TAG, "onEvent: Opening");
                            break;
                        case MediaPlayer.Event.PausableChanged:
                            Log.d(TAG, "onEvent: PausableChanged");
                            break;
                        case MediaPlayer.Event.Paused:
                            Log.d(TAG, "onEvent: Paused");
                            break;
                        case MediaPlayer.Event.Playing:
                            mReconnectCnt = 0;
                            if(!bStop){
                                Toast.makeText(context, "Connect Success!", Toast.LENGTH_LONG).show();
                            }
                            pb.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "onEvent: Playing");
                            break;
                        case MediaPlayer.Event.PositionChanged:
                            Log.d(TAG, "onEvent: PositionChanged");
                            break;
                        case MediaPlayer.Event.SeekableChanged:
                            Log.d(TAG, "onEvent: SeekableChanged");
                            break;
                        case MediaPlayer.Event.Stopped:
                            if (mLastChecked == 0){
                                mLastChecked = System.currentTimeMillis();
                            }
                            if(mLastChecked != 0 && mLastChecked + 60000 < System.currentTimeMillis()) {
                                mLastChecked = 0;
                                index = (index + 1) % 2;
                                if (!dialog.isShowing()) {
                                    String msg;
                                    if(index == 0){
                                        msg = "connecting with primary IP Address...";
                                    }else{
                                        msg = "connecting with secondary IP Address...";
                                    }

                                    if(!bStop){
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }

                            playback();
                            pb.setVisibility(View.VISIBLE);
                            Log.d(TAG, "onEvent: Stopped");
                            break;
                        case MediaPlayer.Event.TimeChanged:
                            Log.d(TAG, "onEvent: TimeChanged");
                            break;
                        case MediaPlayer.Event.Vout:
                            Log.d(TAG, "onEvent: Vout");
                            break;
                    }
                }
            });
            mMediaPlayer.play();
        }

    }
}
