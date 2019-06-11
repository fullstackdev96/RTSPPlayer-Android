package com.example.rtsph264streamplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
//import android.support.v7.app.AppCompatActivity;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rtsph264streamplayer.dialog.CustomDialog;

import io.vov.vitamio.Vitamio;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.utils.Log;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;

public class MainActivity extends Activity {

    public static String TAG = "MainActivity";
    long numpad0Time, numpad1Time ,numpad2Time;
    static String path;
    private static VideoView mVideoView;
    private static Context context;
    private Thread mThread;
    private static boolean bSleepThread;

    private long mLastChecked;
    private static long mCurrentPostion;

    static SharedPreferences sp ;
    static ProgressBar pb;
    static CustomDialog dialog;
    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private int currentApiVersion;


    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Vitamio.isInitialized(getApplicationContext()))
            return;

        currentApiVersion = android.os.Build.VERSION.SDK_INT;
        hideNavigationAndStatusbar();


        context = this;
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        setContentView(R.layout.activity_main);
        pb = (ProgressBar) findViewById(R.id.progressbar);
//        hideProgress();

        mVideoView = (VideoView) findViewById(R.id.surface_view);
        dialog = new CustomDialog(MainActivity.this);

        String url = sp.getString("url",null);
        path = url;
        mLastChecked = 0;
        bSleepThread = false;
        if (path == null || path == "") {
            // Tell the user to provide a media file URL/path.
            Toast.makeText(MainActivity.this, "Please enter the RTSP URL/path", Toast.LENGTH_LONG).show();
            dialog.show();
            return;
        } else {
            /*
             * Alternatively,for streaming media you can use
             * mVideoView.setVideoURI(Uri.parse(URLstring));
             */
            pb.setVisibility(View.VISIBLE);
            pb.animate();
            mVideoView.setMediaController(new MediaController(this));
            mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, 0);
            mVideoView.requestFocus();
//            mVideoView.setBufferSize(1024*50);
//            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                @Override
//                public void onPrepared(MediaPlayer mediaPlayer) {
//                    // optional need Vitamio 4.0
//                    pb.setVisibility(View.INVISIBLE);
//                    Toast.makeText(context, "Connect success!", Toast.LENGTH_LONG).show();
//                    mediaPlayer.setPlaybackSpeed(1.0f);
//                }
//            });
        }

    }

    @Override
    protected void onResume() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(flags);
        startPlay();
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
                        if (mVideoView != null && !mVideoView.isPlaying() && !mVideoView.isBuffering() && mLastChecked == 0 ) {
                            mLastChecked = System.currentTimeMillis();
                        }else if (mVideoView != null && mVideoView.isPlaying() && mLastChecked == 0 && mCurrentPostion >= mVideoView.getCurrentPosition()){
                            mLastChecked = System.currentTimeMillis();
                        }else if (mVideoView != null && (mVideoView.isPlaying() || mVideoView.isBuffering()) && mCurrentPostion < mVideoView.getCurrentPosition()) {
                            mLastChecked = 0;
                        }

                        mCurrentPostion = mVideoView.getCurrentPosition();

                        Long now = System.currentTimeMillis();
                        if (mLastChecked > 0 && (mLastChecked + 60000) < now )
                        {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    // things to do on the main thread
                                    mLastChecked = 0;
                                    if (!dialog.isShowing()) {
                                        sp.edit().putString("url", null).apply();
                                        dialog.show();
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
        mThread.start();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        bSleepThread = true;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mVideoView.stopPlayback();
        finish();
    }

    @Override
    protected  void onStop()
    {
        bSleepThread = true;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(flags);

        super.onStart();
    }


    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onRestart() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(flags);
        super.onRestart();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_1:
                numpad0Time = System.currentTimeMillis();
                break;
            case KeyEvent.KEYCODE_2:
                numpad1Time = System.currentTimeMillis();
                break;
            case KeyEvent.KEYCODE_3:
                numpad2Time = System.currentTimeMillis();
                break;
        }

        checkKeys();

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

    public static void startPlay() {
        pb.setVisibility(View.VISIBLE);
        //        ProgressAsyncTask.execute((Runnable) context);
        String url = sp.getString("url",null);
        path = url;
        if (!TextUtils.isEmpty(url) || path != null) {
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    // optional need Vitamio 4.0
                    pb.setVisibility(View.INVISIBLE);
                    pb.animate();
                    Toast.makeText(context, "Connect success!", Toast.LENGTH_SHORT).show();
                    mediaPlayer.setPlaybackSpeed(1.0f);
                }
            });
//            mVideoView.seton
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (!dialog.isShowing())
                        startPlay();
                    return true;
                }
            });
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!dialog.isShowing())
                        startPlay();
                }
            });
            mVideoView.setBufferSize(1024*50);
            mCurrentPostion = 0 ;

            bSleepThread = true;

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mVideoView.stopPlayback();
            mVideoView.setVideoPath(url);
            bSleepThread = false;
        }
    }

    public void openVideo() {
        mVideoView.setVideoPath(path);
    }

    public void hideNavigationAndStatusbar(){
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().getDecorView().setSystemUiVisibility(flags);
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {

            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
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

    public static Dialog alertDialog(String msg){
        Dialog dialog = new Dialog(context);

        dialog.setContentView(R.layout.dialog_alert);
        TextView txt_alert = (TextView) dialog.findViewById(R.id.txt_alert);
        txt_alert.setText(msg);
        return dialog;
    }

}
