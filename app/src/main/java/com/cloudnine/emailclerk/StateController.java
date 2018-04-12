package com.cloudnine.emailclerk;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.Voice;

import java.util.*;

/**
 * The core controller for the app; this is a singleton
 * that holds the state of the app and manages most
 * activities including that of the EmailController
 * and the VoiceController
 * @see EmailController
 * @see VoiceController
 *
 * @author Ben
 */
public class StateController
{

    private MainActivity master;
    //private SettingsController settings;
    private VoiceController voiceController;
    private EmailController emailController;
    private String userEmail;
    private String userName;
    private com.google.api.services.gmail.Gmail mService;
    public List<Email> emails;
    private int counter;
    private String[] listingState = {"READ","SKIP", "DELETE"};

    StateController(MainActivity mainActivity, Context context, Activity activity, com.google.api.services.gmail.Gmail mService)
    {
        this.master = mainActivity;
        this.mService = mService;
        this.counter = 0;

        emailController = new EmailController(this, mService);
        voiceController = new VoiceController(context, activity, this, listingState);
        //settings = new SettingsController();

        /* THIS IS A TEST TO FETCH EMAILS WITH THE EMAIL CONTROLLER */
        //emailControler.getNewEmails();
        emailController.getNewEmails(5);
    }

    /**
     * Called when email is received
     */
    public void onEmailsRetrieved()
    {
        Email curEmail = emails.get(0);
        String output = "New email from " + curEmail.getSenderName() + " with the subject " + curEmail.getSubject() + ". Would you like to read, skip or delete?";
        voiceController.textToSpeech(output);
        voiceController.startListening(listingState);
        this.userEmail = emails.get(0).getReceiverAddress();
        this.userName = emails.get(0).getSenderName();
    }

    public void readNextEmail() {
        counter++;
        Email curEmail = emails.get(counter);
        String output = "New email from " + curEmail.getSenderName() + " with the subject " + curEmail.getSubject() + ". Would you like to read, skip or delete?";
        String[] possibleInputs = new String[2];
        possibleInputs[0] = "SKIP";
        possibleInputs[1] = "DELETE";
        voiceController.textToSpeech(output);
        voiceController.startListening(possibleInputs);
    }

    public void onCommandDelete() {
        emailController.deleteEmail(emails.get(counter).getID());
        readNextEmail();
    }

    public void onCommandSkip() {
        readNextEmail();
    }
}