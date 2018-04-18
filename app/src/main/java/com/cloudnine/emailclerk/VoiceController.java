package com.cloudnine.emailclerk;

/**
 * Created by alecs on 4/5/2018.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class VoiceController implements
        RecognitionListener {

    private String LOG_TAG = "VoiceRecognitionActivity";
    private static final int REQUEST_RECORD_PERMISSION = 100; //for mic permission

    /**
     * for speech to text.
     */
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;

    private static Context context; //MainActivity's context
    private static TextToSpeech tts;
    private StateController stateController;
    private String[] validCommands; //Valid commands based on what state the app is currently on.

    //results from speech to text.
    public String partialResult = "";
    public String singlePartialResult = "";
//    private float[] micLevels;
//    private int counter = 0;


//    private int count = 1;

    /** Retrieves the TTS speed value from the persistent SharedPreferences object
     * @author Andrew Gill**/
    private static float getspeedFlt(){
        SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME,0);
        float data =settings.getFloat("speedflt",10);
        return data/10; // values from 1-20 from seekBar become 0.1-2.0 to work with tts.SpeechRate
    }

    public VoiceController(Context context, Activity activity, StateController stateController)

    {
        this.context = context;
        this.stateController = stateController;
//        micLevels = new float[5];
//        for(int i=0;i<5;i++){
//            micLevels[i] =0;
//        }

        //instantiate new SpeechRecognizer.
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(context));
        speech.setRecognitionListener(this);

        //set recognizerIntent with speech recognition, freeform model, and partial results active.
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        //request microphone permission
        ActivityCompat.requestPermissions
                (activity,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_PERMISSION);
    }


    /**
     * Instantiate TTS object if not available, then reads the input, IGNORING previous tts request.
     * @param input message to read out loud
     */
    public static void textToSpeech(String input) {
        final String inputs = input;
        final HashMap<String, String> params = new HashMap();

        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
        if(tts == null) {
            // Instantiate TTS Object
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    tts.setLanguage(Locale.US);
                    float spdflt = getspeedFlt();
                    tts.setSpeechRate(spdflt);
                    tts.speak(inputs, TextToSpeech.QUEUE_FLUSH, params);
                }
            });
        }
        else{
            float spdflt = getspeedFlt();
            tts.setSpeechRate(spdflt);
            tts.speak(input, TextToSpeech.QUEUE_FLUSH, params);

        }
    }
    /**
     * Instantiate TTS object if not available, then reads the input, AFTER the previous tts request has been completed.
     * @param input message to read out loud
     */
    public static void textToSpeechQueue(String input){
        final String inputs = input;
        final HashMap<String, String> params = new HashMap();
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
        if(tts == null) {
            // Instantiate TTS Object
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    tts.setLanguage(Locale.US);
                    float spdflt = getspeedFlt();
                    tts.setSpeechRate(spdflt);
                    tts.speak(inputs, TextToSpeech.QUEUE_ADD, params);
                }
            });
        }
        else{
            float spdflt = getspeedFlt();
            tts.setSpeechRate(spdflt);
            tts.speak(input, TextToSpeech.QUEUE_ADD, params);
        }
    }

    /**
     * Destroys current speech recognizer, then starts a new speech recognizer because cancel() does not end the process cleanly.
     * Then the speechRecognizer starts listening.
     * @param validCommands valid commands for speech to text to listen for.
     */
    public void startListening(String[] validCommands){
        this.validCommands = validCommands;
        speech.stopListening();
        speech.cancel();
        speech.destroy();
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        speech.setRecognitionListener(this);
//        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
//                "en");
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speech.startListening(recognizerIntent);

    }

    /**
     * Stops then destroys the speechRecognizer. Only to be called when onDestroy is called.
     */
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
        String errorMessage = getErrorText(errorCode);
        Log.d(LOG_TAG, "FAILED " + errorMessage);
        startListening(validCommands);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }


    /**
     * If one of the partial results comes out to be a valid command, calls the corresponding onCommmand method.
     * @param partials
     */
    @Override
    public void onPartialResults(Bundle partials) {
        String text = "";
        Log.i(LOG_TAG, "onPartialResults");
        ArrayList<String> matches = partials
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);


        for (String result : matches) {
            text += result + "\n";
            singlePartialResult = result;

            MainActivity.returnedText.setText(singlePartialResult);

            for (int i = 0; i < validCommands.length; i++)
                if (singlePartialResult.toUpperCase().contains(validCommands[i])) {
                    MainActivity.returnedText.setText(singlePartialResult);
                    singlePartialResult = "";
                    if (validCommands[i].toUpperCase().contains("READ")) {
                        stateController.onCommandRead();
                        break;
                    } else if (validCommands[i].toUpperCase().contains("SKIP")) {
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
                    } else if (validCommands[i].toUpperCase().contains("REPEAT")) {
                        stateController.onCommandRepeat();
                        break;
                    }



                }

        }
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    /**
     * If there is a valid command, then relisten for the commands.  Otherwise, call onReplyAnswered and pass in the reply composed.
     * @param results
     */
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
}