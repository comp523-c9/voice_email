package com.cloudnine.emailclerk;

import android.app.Activity;
import android.content.Context;
import com.google.api.services.gmail.Gmail;

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
    private static int INITIAL_FETCH_NUMBER = 6;
    private static int SUBSEQUENT_FETCH_NUMBER = 10;

    //private String userEmail;
    //private String userName;
    //private Gmail service; //TODO is this still needed? Are any of these??

    /**
     * The list of currently loaded emails
     * @see Email
     * @see EmailController
     */
    public List<Email> emails;

    /**
     * The currently focused email
     */
    private int counter;

    /**
     * Holds the state of the list emails loop
     */
    private String[] listingState = {"READ","SKIP", "DELETE"};

    /**
     * Accumulates the message body
     */
    private String messageBody;
    private int fetchNumber; //The number of times new emails are fetched (50 at a time)

    /**
     * @todo What the heck is this?
     */
    private boolean sent;
    private boolean add;
    private boolean readingState;

    /**
     * Create the StateController
     * @param mainActivity Reference to the MainActivity core class of the app
     * @param context @todo what is Context? Why do we need it?
     * @param activity @todo what is an Activity? Why do we need it?
     * @param service Service object for accessing the Gmail API; needed by EmailController
     * @see MainActivity
     * @see Context
     * @see Activity
     * @see Gmail
     * @see EmailController
     */
    StateController(MainActivity mainActivity, Context context, Activity activity, Gmail service)
    {
        this.master = mainActivity;
        //this.mService = service; TODO remove maybe
        this.counter = -1;
        messageBody = "";
        fetchNumber = 0;
        emails = new ArrayList<Email>();

        emailController = new EmailController(this, service);
        voiceController = new VoiceController(context, activity, this);
        //settings = new SettingsController();

        emailController.getNewEmails(INITIAL_FETCH_NUMBER);
    }

    /**
     * Callback function which is called when emails are retrieved from the EmailController
     * @see EmailController
     */
    public void onEmailsRetrieved()
    {
        readNextEmail();
    }

    /**
     * Read the next email in the list out loud and provide the user with options
     */
    private void readNextEmail()
    {
        counter++;
        readingState = false;
//        if (counter == emails.size()) {
//            voiceController.textToSpeech("You are out of emails. Please restart the app");
//            return;
        if (counter >= emails.size() - 5) {
            emailController.fetchNewEmails(emails, SUBSEQUENT_FETCH_NUMBER);
            fetchNumber++;
        }
        Email curEmail = emails.get(counter);
        String output = "New email from " + curEmail.getSenderName() + " with the subject " + curEmail.getSubject() + ". Would you like to read, repeat, skip or delete?";
        String[] possibleInputs = new String[4];
        possibleInputs[0] = "SKIP";
        possibleInputs[1] = "DELETE";
        possibleInputs[2] = "READ";
        possibleInputs[3] = "REPEAT";

        if (sent) {
            VoiceController.textToSpeechQueue(output);
            sent = false;
        } else {
            VoiceController.textToSpeech(output);
        }

        voiceController.startListening(possibleInputs);
    }

    /**
     * Delete the current email
     */
    public void onCommandDelete()
    {
        emailController.deleteEmail(emails.get(counter).getID());
        readNextEmail();
    }

    /**
     * Read the message body of the current email
     */
    public void onCommandRead()
    {
        VoiceController.textToSpeech(emails.get(counter).getMessage() + " Would you like to reply, repeat, skip or delete?");
        String[] possibleInputs = new String[4];
        possibleInputs[0] = "SKIP";
        possibleInputs[1] = "DELETE";
        possibleInputs[2] = "REPLY";
        possibleInputs[3] = "REPEAT";
        readingState = true;
        voiceController.startListening(possibleInputs);
    }

    /**
     * Skip/Do Nothing with the current email
     */
    public void onCommandSkip() { readNextEmail(); }

    public void onCommandRepeat() {
        if(!readingState) {
            counter--;
            readNextEmail();
        }
        else{
            onCommandRead();
        }

    }
    /**
     * Compose a new email as a reply to the current one
     */
    public void onCommandReply()
    {
        VoiceController.textToSpeech("Please state your desired message.");
        String[] possibleInputs = new String[0];
        voiceController.startListening(possibleInputs);
    }

    /**
     * I am not sure what this does. @todo help?
     * @param message A string of some sort?
     */
    public void onReplyAnswered(String message)
    {
        if(add)
        {
            this.messageBody = messageBody + " " + message;
            add = false;
        } else
        {
            this.messageBody = message;
        }

        VoiceController.textToSpeech("Your message was recorded as: " + messageBody + " Would you like to skip, change, continue, or send?");
        String[] possibleInputs = new String[4];
        possibleInputs[0] = "SEND";
        possibleInputs[1] = "CHANGE";
        possibleInputs[2] = "SKIP";
        possibleInputs[3] = "CONTINUE";
        voiceController.startListening(possibleInputs);
    }

    /**
     * Send the email? Reply? @todo this is very unclear
     */
    public void onCommandSend()
    {
        Email curEmail = emails.get(counter);
        emailController.sendEmail(curEmail.getSenderAddress(), curEmail.getReceiverAddress(), "Re: " + curEmail.getSubject(), messageBody, curEmail);
        VoiceController.textToSpeech("The Email was sent");
        sent = true;
        readNextEmail();
    }

    /**
     * Change the email being composed
     */
    public void onCommandChange() { onCommandReply(); }

    /**
     * Not quite sure what this does
     */
    public void onCommandContinue()
    {
        VoiceController.textToSpeech("Please continue your message");
        add = true;
        String[] possibleInputs = new String[0];
        voiceController.startListening(possibleInputs);
    }

    /**
     * Clean up anything needed to safely destroy the app
     */
    public void onDestroy()
    {
        VoiceController.textToSpeech("");
        voiceController.stopListening();
    }
}

//class EmailIterator<T extends Email> implements Iterator<T>
//{
//    private T[] storedEmails;
//    private EmailController controller;
//    private int pointer;
//
//    private final int MARGIN = 10;
//    private final int BUFFER = 50;
//
//    public EmailIterator(EmailController controller)
//    {
//        this.storedEmails = (T[]) new Object[BUFFER];
//        this.controller = controller;
//        this.pointer = 0;
//    }
//
//    public boolean hasNext()
//    {
//        return pointer < BUFFER - 1;
//    }
//
//    public T next()
//    {
//        if(!hasNext())
//        {
//            throw new NoSuchElementException();
//        }
//        else
//        {
//            if(BUFFER - pointer < MARGIN)
//            {
//                controller.getNewEmails(10);
//            }
//
//            pointer++;
//            return storedEmails[pointer];
//        }
//    }
//}