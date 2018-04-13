package com.cloudnine.emailclerk;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
    private static int tts_progress_value;
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
        toolbar = (Toolbar) findViewById(R.id.mCustomToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ttsSpeedBar();
        skipReadSwitch();
    }

    public void ttsSpeedBar (){
        tts_seekbar = (SeekBar) findViewById(R.id.tts_speedbar);
        tts_speedtext = (TextView) findViewById(R.id.tts_speedbartext);
        tts_seekbar.setProgress(tts_progress_value);
        tts_speedtext.setText(String.valueOf(tts_seekbar.getProgress() * 10) + "%");
        tts_seekbar.setMax(10);


        tts_seekbar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {


                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tts_progress_value = progress;
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
                 }
                 else if (isChecked == false){
                     skip_read = false;
                 }
             }
         });
    }

}