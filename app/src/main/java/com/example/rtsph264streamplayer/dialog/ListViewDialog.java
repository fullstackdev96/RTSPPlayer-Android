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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rtsph264streamplayer.MainActivity;
import com.example.rtsph264streamplayer.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class ListViewDialog extends Dialog implements View.OnClickListener {

    int type;
    static ArrayList<String> ary;
    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    public Activity c;
    ArrayList<String> lists ;
    Gson gson ;
    String json ;
    Type stringType ;
    SharedPreferences.Editor prefsEditor;
    MyAdapter adapter;
    ListView listView;

    final SharedPreferences sp;

    public ListViewDialog(Activity activity){
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

        setContentView(R.layout.dialog_selectip);
        lists = new ArrayList<String>();

        gson = new Gson();
        stringType = new TypeToken<ArrayList<String>>() {}.getType();
        json = sp.getString("urls", new Gson().toJson(new ArrayList<>()));
        lists = gson.fromJson(json, stringType);

        prefsEditor = sp.edit();
        json = gson.toJson(lists); //tasks is an ArrayList instance variable
        prefsEditor.putString("urls", json);
        prefsEditor.commit();

        adapter = new MyAdapter();
        listView = (ListView) findViewById(R.id.list_selectip);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CustomDialog.setText(lists.get(position).toString(), type);
                dismiss();
            }
        });

        this.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_ok:
                break;
        }
    }

    private class MyAdapter extends BaseAdapter {

        // override other abstract methods here

        @Override
        public int getCount() {
            return lists.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_string, container, false);
            }

            ((TextView) convertView.findViewById(R.id.item_txt))
                    .setText(lists.get(position).toString());

            return convertView;
        }

    }

    public void setChangedData(ArrayList<String> array){
        lists = array;
        if(adapter == null){
            return;
        }
        adapter.notifyDataSetChanged();
    }
}
