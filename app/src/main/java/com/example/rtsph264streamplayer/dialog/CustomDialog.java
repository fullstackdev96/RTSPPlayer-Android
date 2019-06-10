package com.example.rtsph264streamplayer.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.rtsph264streamplayer.MainActivity;
import com.example.rtsph264streamplayer.R;

public class CustomDialog extends Dialog implements View.OnClickListener {

    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    public Activity c;
    public Dialog d;
    public EditText edit;
    public Button btn_ok, btn_cancel;
    final SharedPreferences sp;
    private String regex = "rtsp://";

    public CustomDialog(Activity activity){
        super(activity);
        this.c = activity;
        sp = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(flags);
        setContentView(R.layout.dialog_setip);
        edit = (EditText) findViewById(R.id.edit_ip);
        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
//        this.setCancelable(false);


        String url = "rtsp://192.168.109.2:1935/vod/sample.mp4";
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_ok:
                String ipaddress = edit.getText().toString();
                if(!ipaddress.isEmpty()){
                    if(ipaddress.substring(0,7).equals("rtsp://")){
                        sp.edit().putString("url", ipaddress).apply();
                        MainActivity.startPlay();
                        this.dismiss();
                        Log.d("Ipaddress" , ipaddress);
                    }else{
                        showAlert("Invalid RTSP STREAM URL");
                    }
                }else{
                    showAlert("Please enter the URL");
                }
                break;
            case R.id.btn_cancel:
                String url = sp.getString("url",null);
                if(url == null){
                    this.c.finish();
                }else{
                    this.dismiss();
                }

                break;
        }
    }

    public void showAlert(String msg){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
        builder1.setMessage(msg);
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}
