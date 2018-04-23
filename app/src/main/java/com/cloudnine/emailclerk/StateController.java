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
    private VoiceController voiceController;
    private EmailController emailController;
    public static int INITIAL_FETCH_NUMBER = 20;
    public static int SUBSEQUENT_FETCH_NUMBER = 50;

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

    /**
     * The number of times new emails are fetched (50 at a time)
     */
    private int fetchNumber;

    /**
     * Checking if tts should be queued up instead of flushing
     */
    private boolean queueTextToSpeech;

    /**
     * Checking if we want to reply or reply all
     */
    private boolean replyAll;

    /**
     * Append previous message for continue method
     */
    private boolean continueMessage;

    /**
     * for repeat method.  Either repeat subject/sender or actual message.
     */
    private boolean readingState;

    /**
     * Create the StateController
     * @param mainActivity Reference to the MainActivity core class of the app
     * @param context @todo what is Context? Why do we need it? for VC
     * @param activity @todo what is an Activity? Why do we need it? for VC
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
        this.counter = -1;
        messageBody = "";
        fetchNumber = 0;
        emails = new ArrayList<Email>();

        emailController = new EmailController(this, service);
        voiceController = new VoiceController(context, activity, this);
        emailController.getNewEmails(INITIAL_FETCH_NUMBER, false);
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
        if (counter == emails.size()) {
            voiceController.textToSpeech("You are out of emails. Please restart the app");
            return;
        if (counter >= emails.size() - 5) {
            emailController.fetchNewEmails(emails, SUBSEQUENT_FETCH_NUMBER, SettingsController.getSkipRead());
        }
        Email curEmail = emails.get(counter);
        String output = "New email from " + emailController.getNameFromRecipient(curEmail.getFrom()) + " with the subject " + curEmail.getSubject() + ". Would you like to read, repeat, skip, save, or delete?";
        String[] possibleInputs = new String[5];
        possibleInputs[0] = "SKIP";
        possibleInputs[1] = "DELETE";
        possibleInputs[2] = "READ";
        possibleInputs[3] = "SAVE";
        possibleInputs[4] = "REPEAT";

        //if queueTTS is true, do not cut off the last tts call. (mainly "email sent" or "email deleted")
        if (queueTextToSpeech) {
            VoiceController.textToSpeechQueue(output);
            queueTextToSpeech = false;
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
     * Save the current email
     */
    public void onCommandSave()
    {
        emailController.saveEmail(emails.get(counter));
        voiceController.textToSpeech("The email was saved");
        queueTextToSpeech = true;
        readNextEmail();
    }

    /**
     * Read the message body of the current email
     */
    public void onCommandRead()
    {
        VoiceController.textToSpeech(emails.get(counter).getMessage() + " Would you like to reply, repeat, skip, save, or delete?" +
                "if you would like to reply all, say everyone");
        String[] possibleInputs = new String[6];
        possibleInputs[0] = "SKIP";
        possibleInputs[1] = "DELETE";
        possibleInputs[2] = "REPLY";
        possibleInputs[3] = "REPEAT";
        possibleInputs[4] = "EVERYONE";
        possibleInputs[5] = "SAVE";
        readingState = true;
        voiceController.startListening(possibleInputs);
    }

    /**
     * Skip/Do Nothing with the current email
     */
    public void onCommandSkip() { readNextEmail(); }

    /**
     *Repeat what was said on either readNextEmail or onCommandRead.
     */
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
        replyAll = false;
        VoiceController.textToSpeech("Please state your desired message.");
        String[] possibleInputs = new String[0];
        voiceController.startListening(possibleInputs);
    }

    /**
     * As onCommandReply, but replies to all senders
     */
    public void onCommandReplyAll()
    {
        VoiceController.textToSpeech("Please state your desired message.");
        String[] possibleInputs = new String[0];
        voiceController.startListening(possibleInputs);
    }

    /**
     * Receives a reply from VC once VC assumes that the reply was completed, then repeats the reply recorded.
     * User then decides to skip, change, continue, or send the reply.
     * @param message Reply recorded on VoiceController
     */
    public void onReplyAnswered(String message)
    {
        if(continueMessage)
        {
            this.messageBody = messageBody + " " + message;
            continueMessage = false;
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
     * Send the recorded reply.  4/18 - currently saves the reply to a draft.
     */
    public void onCommandSend()
    {
        Email curEmail = emails.get(counter);
        emailController.sendEmail(curEmail, messageBody, replyAll);
        VoiceController.textToSpeech("The Email was sent");
        queueTextToSpeech = true;
        readNextEmail();
    }

    /**
     * Change the email being composed
     */
    public void onCommandChange() { onCommandReply(); }

    /**
     * Appends a new message to the previous reply.
     */
    public void onCommandContinue()
    {
        VoiceController.textToSpeech("Please continue your message");
        continueMessage = true;
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
