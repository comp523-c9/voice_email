package com.cloudnine.emailclerk;

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

    /**
     * A simple enumeration to hold the current activity of the app
     */
    public enum MainState
    {
        /**
         * The app has just opened and needs to perform certain checks and setup activities
         */
        OPENED,

        /**
         * The app is listing emails in order
         * @see
         */
        LISTING,

        /**
         * The app is reading out a specific email
         */
        READING,

        /**
         * The app is composing a new email
         */
        COMPOSING
    }

    private MainState state;

    private com.google.api.services.gmail.Gmail mService;

    /**
     * Every Email currently fetched from the EmailController
     * @see Email
     * @see EmailController
     * @todo Alec, is this correct?
     */
    public List<Email> emails;

    /**
     * Generate a new StateController
     * @param mainActivity Reference to the mainActivity in case we need to do GUI related things (is this necessary?)
     * @param mService The Gmail mailbox object
     * @see MainActivity
     * @see com.google.api.services.gmail.Gmail
     */
    StateController(MainActivity mainActivity, com.google.api.services.gmail.Gmail mService)
    {
        this.master = mainActivity;
        this.mService = mService;
        this.state = MainState.OPENED;

        emailController = new EmailController(this, mService);
        voiceController = new VoiceController(master.getApplicationContext(), master);
        //settings = new SettingsController();

        /* THIS IS A TEST TO FETCH EMAILS WITH THE EMAIL CONTROLLER */
        //emailControler.getNewEmails();
        emailController.getNewEmails(1);
    }

    /**
     * Called when email is received
     */
    public void onEmailsRetrieved()
    {
        Email curEmail = emails.get(0);
        //String output = "Hey dude, you got a new email from " + curEmail.getSenderName() + " with the subject " + curEmail.getSubject();
        //voiceController.textToSpeech(output);
        //voiceController.startListening();
        this.userEmail = emails.get(0).getReceiverAddress();
        this.userName = emails.get(0).getSenderName();
        emailController.deleteEmail(curEmail.getThreadId());
    }

    /**
     * Continually get emails from the emails list and read them out
     * @param pointer Which email in the list to start with
     */
    public void startListing(int pointer)
    {
        //TODO, need some way to interrupt this on a SKIP command. I recommend having voiceController throw an Exception
        while(state == MainState.LISTING)
        {
            Email current = emails.get(pointer);
            try
            {

                VoiceController.textToSpeech(current.getSubject());
                VoiceController.textToSpeech("From " + current.getSenderName());
                voiceController.textToSpeech("On " + current.getFormattedDateTime());
            }
            catch(Exception e) { } //"Skip Exception" maybe?
            pointer++;
            String[] cmdBuffer = voiceController.getCommandBuffer();
            if(cmdBuffer.length > 0)
            {
                sendCommand(cmdBuffer[0], current);
            }
        }
    }

    /**
     * Begin reading every sentence in the passed email
     * @param current The email to read
     */
    public void startReading(Email current)
    {
        Iterable<String> lines = Arrays.asList(current.getMessage().split("\\."));
        Iterator<String> iter = lines.iterator();
        while(state == MainState.READING && iter.hasNext())
        {
            try
            {
                VoiceController.textToSpeech(iter.next());
            }
            catch(Exception e)
            {
                state = MainState.LISTING;
            }
            String[] cmdBuffer = voiceController.getCommandBuffer();
            if(cmdBuffer.length > 0)
            {
                sendCommand(cmdBuffer[0], current);
            }
        }
    }

    public void draftEmail(String recipient, String subject, Email email)
    {
        String body = "";
        while(!body.contains("FINISH"))
        {
            body += voiceController.question("");
        }
        if(!subject.contains("Re: ")) { subject = "Re: " + subject; }
        emailController.sendEmail(recipient, userEmail, subject, body, email);
    }

    /**
     * Process the command phrase
     * @param command The single word command phrase
     * @param current The potentially relevant current email
     */
    public void sendCommand(String command, Email current)
    {
        switch(state)
        {
            case OPENED:
                openCommand(command, current);
                break;
            case LISTING:
                listingCommand(command, current);
                break;
            case READING:
                readingCommand(command, current);
                break;
            case COMPOSING:
                //composingCommand(command); Need to rework the flow here
                break;
        }
    }

    private void openCommand(String command, Email current)
    {
        switch(command)
        {

        }
    }

    private void listingCommand(String command, Email current)
    {
        switch(command)
        {
            case "SKIP":
                state = MainState.LISTING;
                break;
            case "DELETE":
                emailController.deleteEmail(current.getThreadId());
                break;
            case "READ":
                state = MainState.READING;
                startReading(current);
                break;
            case "COMPOSE":
                //email.composeNew();
                state = MainState.COMPOSING;
                break;
            default:
                VoiceController.textToSpeech("Command not recognized");
                break;
        }
    }

    private void readingCommand(String command, Email current)
    {
        switch(command)
        {
            case "SKIP":
                state = MainState.LISTING;
                break;
            case "REPLY ALL":
                if(voiceController.question("Are you sure you want to reply all?") == "YES")
                {
                    state = MainState.COMPOSING;
                    //draftEmail(current.getSenderAddress(), current); //TODO We need a way to get multiple senders
                }
                else { state = MainState.READING; }
                break;
            case "REPLY":
                state = MainState.COMPOSING;
                state = MainState.COMPOSING;
                draftEmail(current.getSenderAddress(), current); //TODO We need a way to get multiple senders
                break;
            case "FORWARD":
                String recipient = voiceController.question("To whom would you like to forward this email?");
                while(voiceController.question("Do you want to forward this email to " + recipient + "?") != "YES")
                    recipient = voiceController.question("To whom would you like to forward this email?");
                emailController.sendEmail(recipient, emailController.ME, "Fwd: " + current.getSubject(), current.getSenderEmail());
                break;
            case "DELETE":
                if(voiceController.question("Are you sure you want to delete?") == "YES")
                {
                    emailController.deleteEmail(current.getThreadId());
                    state = MainState.LISTING;
                }
                break;
            default:
                VoiceController.textToSpeech("Command not recognized");
                break;
        }
    }

    private void composingCommand(String command, String body, String recipient)
    {
        if(command == "FINISH")
        {
            VoiceController.textToSpeech(body);
            if(voiceController.question("Would you like to send this email?") == "YES")
            {
                emailController.sendEmail(recipient, userEmail, "subject", body);
                state = MainState.LISTING;
            }
            else { }
        }
        else
        {
            body += command; //I will probably rework this entirely
        }
    }
}