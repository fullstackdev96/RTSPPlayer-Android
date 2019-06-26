package com.example.rtsph264streamplayer.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.example.rtsph264streamplayer.MainActivity;
import com.example.rtsph264streamplayer.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class CustomDialog extends Dialog implements View.OnClickListener {

    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    public Activity c;
    public static EditText edit_first, edit_second;
    public Button btn_ok, btn_cancel;
    final SharedPreferences sp;
    Button btnFirst, btnSecond;
    ListViewDialog listdlg;
    static ArrayList<String> lists;
    Gson gson ;
    String json ;
    Type stringType ;
    SharedPreferences.Editor prefsEditor;
    private String prefName = "urls";

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
        edit_first = (EditText) findViewById(R.id.edit_ip_first);
        edit_second = (EditText) findViewById(R.id.edit_ip_second);

        btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        btnFirst = (Button) findViewById(R.id.btn_first);
        btnSecond = (Button) findViewById(R.id.btn_second);
        btnFirst.setOnClickListener(this);
        btnSecond.setOnClickListener(this);

        listdlg = new ListViewDialog(this.c);

        lists = new ArrayList<String>();
        gson = new Gson();
        stringType = new TypeToken<ArrayList<String>>() {}.getType();
        json = sp.getString("urls", new Gson().toJson(new ArrayList<>()));
        lists = gson.fromJson(json, stringType);

        Window window = this.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        String url_first = sp.getString("url_first",null);
        String url_second = sp.getString("url_second",null);

        if(url_first != null && !url_first.isEmpty()){
            edit_first.setText(url_first);
        }

        if(url_second != null && !url_second.isEmpty()){
            edit_second.setText(url_second);
        }

        wlp.gravity = Gravity.TOP;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        this.setCanceledOnTouchOutside(false);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_ok:
                String ipaddress_first = edit_first.getText().toString();
                String ipaddress_second = edit_second.getText().toString();
                if(!ipaddress_first.isEmpty()){
                    if(ipaddress_first.substring(0,7).equals("rtsp://")){
                        sp.edit().putString("url_first", ipaddress_first).apply();
                    }else{
                        showAlert("Invalid First RTSP STREAM URL");
                        return;
                    }
                }else{
                    showAlert("Please enter the First URL");
                    return;
                }

                if(!ipaddress_second.isEmpty()){
                    if(ipaddress_second.substring(0,7).equals("rtsp://")){
                        sp.edit().putString("url_second", ipaddress_second).apply();
                    }else{
                        showAlert("Invalid Second RTSP STREAM URL");
                        return;
                    }
                }else{
                    showAlert("Please enter the Second URL");
                    return;
                }

                if(lists != null && lists.size() > 8){
                    lists.remove(0);
                    lists.remove(1);
                }

                lists.add(ipaddress_first);
                lists.add(ipaddress_second);

                prefsEditor = sp.edit();
                json = gson.toJson(lists); //tasks is an ArrayList instance variable
                prefsEditor.putString("urls", json);
                prefsEditor.commit();

                listdlg.setChangedData(lists);

                MainActivity.index = 0;
                MainActivity.playback();
                dismiss();
                Log.d("Ipaddress_first" , ipaddress_first);
                Log.d("Ipaddress_second" , ipaddress_second);
                break;
            case R.id.btn_cancel:
                String url = sp.getString("url_first",null);
                if(url == null){
                    this.c.finish();
                }else{
                    this.dismiss();
                }

                break;
            case R.id.btn_first:
                if(checkUrls()){
                    listdlg.type = 1;
                    listdlg.show();
                }else{
                    showAlert("No URL Saved!");
                }
                break;
            case R.id.btn_second:
                if(checkUrls()) {
                    listdlg.type = 2;
                    listdlg.show();
                }else{
                    showAlert("No URL Saved!");
                }
                break;
        }
    }

    public boolean checkUrls(){
        json = sp.getString("urls", new Gson().toJson(new ArrayList<>()));
        lists = gson.fromJson(json, stringType);

        if(lists != null && lists.size() > 0){
            return true;
        }else{
            return false;
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

    public static void setText(String url, int type){
        if(type == 1){
            edit_first.setText(url);
        }else{
            edit_second.setText(url);
        }
    }
}
