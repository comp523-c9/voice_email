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

import java.util.ArrayList;
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

//    private int count = 1;

    public VoiceController(Context context, Activity activity, StateController stateController, String[] commandList)

    {
//        returnedText = (TextView) findViewById(R.id.textView1);
        this.context = context;
        this.activity = activity;
        this.stateController = stateController;
        this.commandList = commandList;

        speech = SpeechRecognizer.createSpeechRecognizer(context);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(context));
        speech.setRecognitionListener(this);
//        AudioManager amanager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        amanager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
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
        if(tts == null) {
            // Instantiate TTS Object
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    tts.setLanguage(Locale.US);
                    tts.speak(inputs, TextToSpeech.QUEUE_FLUSH, null);
                }
            });
        }
        else{
            tts.speak(input, TextToSpeech.QUEUE_FLUSH, null);

        }
    }

    public String getSpeechResult(){
        return partialResult;
    }

    public void startListening(String[] validCommands){
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
        speech.startListening(recognizerIntent);

    }
    private void stopListening(){
        speech.stopListening();
    }
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
        Log.i(LOG_TAG, "onPartialResults");
        ArrayList<String> matches = partials
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches) {
            text += result + "\n";
            singlePartialResult = result;
            //TODO logic for read and commands
//            if(singlePartialResult.toLowerCase().contains("read")){
//                stopListening();
//                stateController.onCommandRead();
//                singlePartialResult = "";
//            }
//            if(singlePartialResult.toLowerCase().contains("skip")){
//                speech.cancel();
//                singlePartialResult = "";
//                stateController.onCommandSkip();
//            }
            for(int i = 0; i < validCommands.length; i++)
                if(singlePartialResult.toUpperCase().contains(validCommands[i])){
                    MainActivity.returnedText.setText(singlePartialResult);
                    singlePartialResult = "";
                    //TODO call some method from state controller
                    if(validCommands[i].contains("READ")){
                        stateController.onCommandRead();
                    }
                    if(validCommands[i].contains("SKIP")){
                        stateController.onCommandSkip();
                    }
                    if(validCommands[i].contains("DELETE")){

                    }
                    if(validCommands[i].contains("REPLY")){

                    }
                    if(validCommands[i].contains("REDO")){

                    }
                    if(validCommands[i].contains("SEND")){

                    }
                }

        }

        partialResult = text;


//        count++;
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
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

        //returnedText.setText(text);
        partialResult = text;
        //MainActivity.returnedText.setText(text);
        partialResult = "";
    }
    @Override
    public void onRmsChanged(float rmsdB)
    {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
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