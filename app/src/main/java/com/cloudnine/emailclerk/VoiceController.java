package com.cloudnine.emailclerk;

/**
 * Created by alecs on 4/5/2018.
 */

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.speech.tts.TextToSpeech;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class VoiceController implements
        RecognitionListener {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private TextView returnedText;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private ToggleButton button;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private static Context context;
    private Activity activity;
    public String partialResult = "testing";
    public String singlePartialResult = "";
    private static TextToSpeech tts;
    private StateController stateController;
    private String[] commandList;
    private String[] validCommands;
    private int iterator = 0;
    public boolean go;
    private int volume;
    public static float speed;
//    private float[] micLevels;
//    private int counter = 0;


//    private int count = 1;

    public VoiceController(Context context, Activity activity, StateController stateController, String[] commandList)

    {
//        returnedText = (TextView) findViewById(R.id.textView1);
        this.context = context;
        this.activity = activity;
        this.stateController = stateController;
        this.commandList = commandList;
        this.speed = 0.8f;
//        micLevels = new float[5];
//        for(int i=0;i<5;i++){
//            micLevels[i] =0;
//        }


        speech = SpeechRecognizer.createSpeechRecognizer(context);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(context));
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 20000);
        ActivityCompat.requestPermissions
                (activity,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_PERMISSION);
//        speech.startListening(recognizerIntent);

//        if(button.isChecked()==true){
//            speech.stopListening();
//        }
    }

    //maybe throw this to main?
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        testString = "pass3";
//        switch (requestCode) {
//            case REQUEST_RECORD_PERMISSION:
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    speech.startListening(recognizerIntent);
//                } else {
//                    Toast.makeText(context, "Permission Denied!", Toast
//                            .LENGTH_SHORT).show();
//                }
//        }
//    }

    public static void textToSpeech(String input) {
        final String inputs = input;
        final HashMap<String, String> params = new HashMap();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        if(tts == null) {
            // Instantiate TTS Object

            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    tts.setLanguage(Locale.US);
                    tts.setSpeechRate(speed);
                    tts.speak(inputs, TextToSpeech.QUEUE_FLUSH, params);
                }
            });
        }
        else{
            tts.speak(input, TextToSpeech.QUEUE_FLUSH, params);

        }
    }
    public static void textToSpeech(String input, boolean bool){
        final String inputs = input;
        final HashMap<String, String> params = new HashMap();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
        if(tts == null) {
            // Instantiate TTS Object
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    tts.setLanguage(Locale.US);
                    tts.setSpeechRate((float)0.8);
                    tts.speak(inputs, TextToSpeech.QUEUE_ADD, params);
                }
            });
        }
        else{
            tts.speak(input, TextToSpeech.QUEUE_ADD, params);

        }
    }

    public String getSpeechResult(){
        return partialResult;
    }

    public void startListening(String[] validCommands){
        volume = MainActivity.amanager.getStreamVolume(AudioManager.STREAM_MUSIC); // getting system volume into var for later un-muting
        MainActivity.amanager.setStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
        this.validCommands = validCommands;
        speech.stopListening();
        speech.cancel();
        speech.destroy();
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        speech.setRecognitionListener(this);
//        AudioManager amanager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        amanager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 20000);
        speech.startListening(recognizerIntent);

    }
    public void stopListening(){
        speech.stopListening();
        speech.cancel();
        speech.destroy();
    }
    //    public float getSensitivity(){
//        float micLevel = 0;
//        for(int i=0;i<5;i++){
//            micLevel += micLevels[i];
//        }
//        micLevel = micLevel/5;
//        return micLevel;
//    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int errorCode) {
//        MainActivity.amanager.setStreamVolume(AudioManager.STREAM_MUSIC, volume , 0);
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        startListening(validCommands);
        //MainActivity.returnedText.setText(errorMessage);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle partials) {
        String text = "";
        Log.i(LOG_TAG, "onPartialResults");
        ArrayList<String> matches = partials
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);


        for (String result : matches) {
            text += result + "\n";
            singlePartialResult = text;

            MainActivity.returnedText.setText(singlePartialResult);

            for (int i = 0; i < validCommands.length; i++)
                if (singlePartialResult.toUpperCase().contains(validCommands[i])) {
                    MainActivity.returnedText.setText(singlePartialResult);
                    singlePartialResult = "";
                    if (validCommands[i].toUpperCase().contains("READ")) {
                        stateController.onCommandRead();
                        break;
                    }
                    else if (validCommands[i].toUpperCase().contains("SKIP")) {
                        stateController.onCommandSkip();
                        break;
                    } else if (validCommands[i].toUpperCase().contains("DELETE")) {
                        stateController.onCommandDelete();
                        break;
                    } else if (validCommands[i].toUpperCase().contains("REPLY")) {
                        stateController.onCommandReply();
                        break;
                    } else if (validCommands[i].toUpperCase().contains("CHANGE")) {
                        stateController.onCommandChange();
                        break;
                    } else if (validCommands[i].toUpperCase().contains("SEND")) {
                        stateController.onCommandSend();
                        break;
                    } else if (validCommands[i].toUpperCase().contains("CONTINUE")) {
                        stateController.onCommandContinue();
                        break;
                    }

                }
        }

        partialResult = text;


//        count++;
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
        MainActivity.amanager.setStreamVolume(AudioManager.STREAM_MUSIC, volume , 0);
        go = true;
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches) {
            text += result + "\n";
            break;
        }
        if(validCommands.length!=0){
            startListening(validCommands);
        }
        else{
            partialResult = text;
            MainActivity.returnedText.setText(text);
            partialResult = "";
            stateController.onReplyAnswered(text);
        }
        //returnedText.setText(text);
        //MainActivity.returnedText.setText(text);

    }
    @Override
    public void onRmsChanged(float rmsdB)
    {
//        micLevels[counter] = rmsdB;
//        counter++;
//        if(counter >= 5){
//            counter = 0;
//        }
//        if(rmsdB>2) {
//            Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
//        }
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }
//
//    /**
//     * Ask the user a question with text to speech and wait until a speech to text response
//     * @param question The question to ask
//     * @return The user's answer (please switch to uppercase for consistency)
//     */
//    public String question(String question)
//    {
//        //TODO Implement this
//        return null;
//    }

//    /**
//     * Get all the commands the user has sent and clear the list
//     * @return List of uppercase string commands
//     */
//    public String[] getCommandBuffer()
//    {
//        return commandBuffer;
//    }
}