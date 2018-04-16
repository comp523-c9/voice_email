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
    public static VoiceController voiceController;
    private EmailController emailController;
    private String userEmail;
    private String userName;
    private com.google.api.services.gmail.Gmail mService;
    public List<Email> emails;
    private int counter;
    private String[] listingState = {"READ","SKIP", "DELETE"};
    private String messageBody;
    private int fetchNumber; //The number of times new emails are fetched (50 at a time)
    private boolean read;
    private boolean add;
    public final int STARTING_EMAIL_NUM = 50;
    public final int EMAILS_TO_FETCH_NUM = 10;

    StateController(MainActivity mainActivity, Context context, Activity activity, com.google.api.services.gmail.Gmail mService)
    {
        this.master = mainActivity;
        this.mService = mService;
        this.counter = -1;
        messageBody = "";
        fetchNumber = 0;
        emails = new ArrayList<Email>();

        emailController = new EmailController(this, mService);
        voiceController = new VoiceController(context, activity, this, listingState);
        //settings = new SettingsController();

        /* THIS IS A TEST TO FETCH EMAILS WITH THE EMAIL CONTROLLER */
        //emailControler.getNewEmails();
        emailController.getNewEmails(STARTING_EMAIL_NUM);
    }

    /**
     * Called when email is received
     */
    public void onEmailsRetrieved()
    {
        readNextEmail();
        this.userEmail = emails.get(0).getReceiverAddress();
        this.userName = emails.get(0).getSenderName();
    }

    public void readNextEmail() {
        counter++;
        if (counter == emails.size()) {
            voiceController.textToSpeech("You are out of emails. Please restart the app");
            return;
        } else if (counter == emails.size() - 5) {
            if (fetchNumber == 0) {
                emailController.fetchNewEmails(emails.get(0), emails.get(emails.size() - 1));
                counter--;
            } else {
                emailController.fetchNewEmails(emails.get(STARTING_EMAIL_NUM - 1 + ((fetchNumber - 1) * EMAILS_TO_FETCH_NUM)), emails.get(emails.size() - 1));
                counter--;
            }

            fetchNumber++;
        } else {
            Email curEmail = emails.get(counter);
            String output = "New email from " + curEmail.getSenderName() + " with the subject " + curEmail.getSubject() + ". Would you like to read, skip or delete?";
            String[] possibleInputs = new String[3];
            possibleInputs[0] = "SKIP";
            possibleInputs[1] = "DELETE";
            possibleInputs[2] = "READ";
            if(read){
                voiceController.textToSpeech(output, true);
                read = false;
            }
            else {
                voiceController.textToSpeech(output);
            }
            voiceController.startListening(possibleInputs);
        }
    }

    public void onCommandDelete() {
        emailController.deleteEmail(emails.get(counter).getID());
        voiceController.textToSpeech("The Email was deleted");
        read = true;
        readNextEmail();
    }
    public void onCommandRead() {
        voiceController.textToSpeech(emails.get(counter).getMessage() + " Would you like to skip, delete, or reply?");
        String[] possibleInputs = new String[3];
        possibleInputs[0] = "SKIP";
        possibleInputs[1] = "DELETE";
        possibleInputs[2] = "REPLY";
        voiceController.startListening(possibleInputs);
    }
    public void onCommandSkip() {
        readNextEmail();
    }
    public void onCommandReply() {
        voiceController.textToSpeech("Please state your desired message.");
        String[] possibleInputs = new String[0];
        voiceController.startListening(possibleInputs);
    }

    public void onReplyAnswered(String message) {
        if(add){
            this.messageBody = messageBody + " " + message;
            add = false;
        }
        else {
            this.messageBody = message;
        }
        voiceController.textToSpeech("Your message was recorded as: " + messageBody + " Would you like to skip, change, continue, or send?");
        String[] possibleInputs = new String[4];
        possibleInputs[0] = "SEND";
        possibleInputs[1] = "CHANGE";
        possibleInputs[2] = "SKIP";
        possibleInputs[3] = "CONTINUE";
        voiceController.startListening(possibleInputs);
    }

    public void onCommandSend() {
        Email curEmail = emails.get(counter);
        emailController.sendEmail(curEmail.getSenderAddress(), curEmail.getReceiverAddress(), "Re: " + curEmail.getSubject(), messageBody, curEmail);
        voiceController.textToSpeech("The Email was sent");
        read = true;
        readNextEmail();
    }

    public void onCommandChange() {
        onCommandReply();
    }

    public void onCommandContinue() {
        voiceController.textToSpeech("Please continue your message");
        add = true;
        String[] possibleInputs = new String[0];
        voiceController.startListening(possibleInputs);
    }
}