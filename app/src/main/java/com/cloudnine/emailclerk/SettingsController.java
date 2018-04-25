package com.cloudnine.emailclerk;

import android.content.Context;
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




/** Declares the toolbar and widgets for this activity
 * @author Andrew Gill**/

public class SettingsController extends AppCompatActivity {
    Toolbar toolbar;
    private static SeekBar tts_seekbar;
    private static TextView tts_speedtext;
    private static Switch readSwitch;
    private static Switch sigSwitch;
    private static Switch hideCommandSwitch;
    private static int tts_progress_value = 10;
    private static boolean skip_read;
    private static boolean signature_added;
    private static boolean commands_hidden;
    private static Context context; //MainActivity's context


    public static float getSpeedFlt(Context context1){
        SharedPreferences settings = context1.getSharedPreferences(MainActivity.PREFS_NAME,0);
        float fltval =settings.getFloat("speedflt",10);
        return fltval/10;
    }

    public static boolean getSkipRead(){
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME,0);
        boolean skipRead = settings.getBoolean("skipread",false);
        return skipRead;
    }
    public static boolean getIncludeSig(){
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME,0);
        boolean sigIncluded = settings.getBoolean("sigadded",false);
        return sigIncluded;
    }
    public static boolean getSkipCommands(){
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME,0);
        boolean commandsSkipped = settings.getBoolean("skipcommands",false);
        return commandsSkipped;
    }
    /**Stores Boolean values in the SharedPreferences object.
     * **/

    private void storeBoolInPrefs(int switchCode){
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        switch(switchCode){
            case 0:
                editor.putBoolean("skipread",skip_read);
                break;
            case 1:
                editor.putBoolean("sigadded",signature_added);
                break;
            case 2:
                editor.putBoolean("skipcommands",commands_hidden);
                break;
        }
        editor.putBoolean("skipread",skip_read);
        editor.commit();
    }

    /**Stores float values in the SharedPreferences object.  This needs the parameter flt_prog
     * because the SeekBar outputs ints and these must be converted into floats (needed by
     * VoiceController) before storing.
     * **/
    private void storeFloatInPrefs(float flt_prog){
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("speed",tts_progress_value);
        editor.putFloat("speedflt",flt_prog);
        editor.commit();
    }

    /**Declares toolbar and back adds button for navigation to SettingsController's parent, MainActivity.
     * The SharedPreferences object stores variables as key-value pairs that persist across resets
     * of the app.  Here, these values are trying to be accessed from the object if they exist already.
     * **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        context = this.getApplicationContext();
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
        toolbar = (Toolbar) findViewById(R.id.mCustomToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tts_progress_value=settings.getInt("speed",tts_progress_value);
        skip_read=settings.getBoolean("skipread",skip_read);
        signature_added=settings.getBoolean("sigadded",signature_added);
        commands_hidden=settings.getBoolean("skipcommands",commands_hidden);
        ttsSpeedBar();
        skipReadSwitch();
        signatureSwitch();
        HideCommandSwitch();
    }
    /** This sets up a SeekBar widget and its change listeners.  Text-to-Speech values are registered
     * from 1 to 20 and printed as percents, with a default at 100%.
     * **/
    public void ttsSpeedBar (){
        tts_seekbar = (SeekBar) findViewById(R.id.tts_speedbar);
        tts_speedtext = (TextView) findViewById(R.id.tts_speedbartext);
        tts_seekbar.setProgress(tts_progress_value);
        tts_speedtext.setText(String.valueOf(tts_seekbar.getProgress() * 10) + "%");
        tts_seekbar.setMax(20);


        tts_seekbar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    /**Changes to the SeekBar are logged into the SharedPreferences object for later use.
                     * The default value of 10 (100%) is provided in the case that the SharedPreferences
                     * object has not logged any values prior.
                     * **/
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tts_progress_value = progress;
                        float flt_prog = (float)progress;
                        storeFloatInPrefs(flt_prog);
                        tts_speedtext.setText(String.valueOf(progress * 10) + "%");
                        //VoiceController.speed=flt_prog;

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

    /** This sets up a Switch widget tied to a boolean value.  This value is stored in the
     * SharedPreferences object for consistency across resets of the app.  It contains the default
     * value of false if SharedPreferences hasn't been accessed before.
     * **/
    public void skipReadSwitch () {
         readSwitch = (Switch) findViewById(R.id.skip_read_switch);
         readSwitch.setChecked(skip_read);
         readSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
             @Override
             public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 if (isChecked){
                     skip_read = true;
                     storeBoolInPrefs(0);
                 }
                 else {
                     skip_read = false;
                     storeBoolInPrefs(0);
                 }
             }
         });
    }
    public void signatureSwitch () {
        sigSwitch = (Switch) findViewById(R.id.signature_switch);
        sigSwitch.setChecked(signature_added);
        sigSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    signature_added = true;
                    storeBoolInPrefs(1);
                }
                else {
                    signature_added = false;
                    storeBoolInPrefs(1);
                }
            }
        });
    }
    public void HideCommandSwitch () {
        hideCommandSwitch = (Switch) findViewById(R.id.skip_command_switch);
        hideCommandSwitch.setChecked(commands_hidden);
        hideCommandSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    commands_hidden = true;
                    storeBoolInPrefs(2);
                }
                else {
                    commands_hidden = false;
                    storeBoolInPrefs(2);
                }
            }
        });
    }
}