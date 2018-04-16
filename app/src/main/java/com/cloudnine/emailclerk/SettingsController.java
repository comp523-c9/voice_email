package com.cloudnine.emailclerk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudnine.emailclerk.R;


public class SettingsController extends AppCompatActivity {
    Toolbar toolbar;
    private static SeekBar tts_seekbar;
    private static TextView tts_speedtext;
    private static Switch readSwitch;
    private static int tts_progress_value = 10;
    private static boolean skip_read;



    public static int getTTSSpeed (){
        return tts_progress_value;
    }

    public static boolean getSkipRead(){
        return skip_read;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
        SharedPreferences settings2 = getSharedPreferences(MainActivity.PREFS_NAME,0);
        toolbar = (Toolbar) findViewById(R.id.mCustomToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tts_progress_value=settings.getInt("speed",tts_progress_value);
        skip_read=settings.getBoolean("skipread",skip_read);
        ttsSpeedBar();
        skipReadSwitch();
    }

    public void ttsSpeedBar (){
        tts_seekbar = (SeekBar) findViewById(R.id.tts_speedbar);
        tts_speedtext = (TextView) findViewById(R.id.tts_speedbartext);
        tts_seekbar.setProgress(tts_progress_value);
        tts_speedtext.setText(String.valueOf(tts_seekbar.getProgress() * 10) + "%");
        tts_seekbar.setMax(20);


        tts_seekbar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {


                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tts_progress_value = progress;
                        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt("speed",tts_progress_value);
                        editor.commit();

                        tts_speedtext.setText(String.valueOf(progress * 10) + "%");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        tts_speedtext.setText(String.valueOf(tts_progress_value * 10) + "%");
                    }
                }
        );
    }
    public void skipReadSwitch () {
         readSwitch = (Switch) findViewById(R.id.skip_read_switch);
         readSwitch.setChecked(skip_read);
         readSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 if (isChecked == true){
                     skip_read = true;
                     SharedPreferences settings2 = getSharedPreferences(MainActivity.PREFS_NAME,0);
                     SharedPreferences.Editor editor = settings2.edit();
                     editor.putBoolean("skipread",skip_read);
                     editor.commit();
                 }
                 else if (isChecked == false){
                     skip_read = false;
                     SharedPreferences settings2 = getSharedPreferences(MainActivity.PREFS_NAME,0);
                     SharedPreferences.Editor editor = settings2.edit();
                     editor.putBoolean("skipread",skip_read);
                     editor.commit();
                 }
             }
         });
    }

}