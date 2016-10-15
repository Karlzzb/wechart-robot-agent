package com.karl.wechatrobot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private Intent intent;
    private Button forwardButton;
    private Intent settingIntent;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("server start", "yes");
    }

    public void onMyButtonClick(View view) {
        if (settingIntent == null) {
            settingIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        }
        startActivity(settingIntent);
    }

}
