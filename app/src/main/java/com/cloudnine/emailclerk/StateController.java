package com.cloudnine.emailclerk;

import android.app.Activity;
import android.content.Context;

import java.util.*;

public class StateController {

    private MainActivity master;
    //private SettingsController settings;
    private VoiceController voiceController;
    private EmailController emailController;
    private String[] dummyStringList;

    public enum MainState { OPENED, LISTING, READING, COMPOSING }
    public MainState state;

    private com.google.api.services.gmail.Gmail mService;
    public List<Email> emails;
    private int iterator;
    StateController(MainActivity mainActivity, Context context, Activity activity, com.google.api.services.gmail.Gmail mService) {
        master = mainActivity;
        this.mService = mService;
        this.master = master;
        this.state = MainState.OPENED;
        iterator = 0;
        emailController = new EmailController(this, mService);
        voiceController = new VoiceController(context, activity, this, dummyStringList );
        //settings = new SettingsController();

        /** THIS IS A TEST TO FETCH EMAILS WITH THE EMAIL CONTROLLER **/
        //emailControler.getNewEmails();
        emailController.getNewEmails(1);
    }

    public void onEmailsRetrieved() {
        Email curEmail = emails.get(0);
        voiceController.textToSpeech(curEmail.getSenderName() + curEmail.getSubject());
        voiceController.startListening();
       // emailController.sendEmail(curEmail, "this is the message body of a reply");
    }
    public void onCommandRead(){
        voiceController.textToSpeech(emails.get(0).getMessage());
    }
    public void onCommandSkip(){
        iterator++;
//        Email curEmail = emails.get(iterator);
        voiceController.textToSpeech("skipped email");
//        voiceController.textToSpeech(curEmail.getSenderName() + curEmail.getSubject());
        voiceController.startListening();

    }


//    public void sendCommand(String command)
//    {
//        switch(state)
//        {
//            case OPENED:
//                openCommand(command);
//                break;
//            case LISTING:
//                listingCommand(command);
//                break;
//            case READING:
//                readingCommand(command);
//                break;
//            case COMPOSING:
//                composingCommand(command);
//                break;
//        }
//    }

    private void openCommand(String command)
    {
        switch(command)
        {

        }
    }

    private void listingCommand(String command)
    {
        switch(command)
        {
            case "SKIP":
                //email.skipCurrent();
                break;
            case "DELETE":
                //email.deleteCurrent();
                break;
            case "READ":
                //email.readCurrent();
                state = MainState.READING;
                break;
            case "COMPOSE":
                //email.composeNew();
                state = MainState.COMPOSING;
                break;
            default:
                //voice.commandNotRecognized();
                break;
        }
    }

//    private void readingCommand(String command)
//    {
//        switch(command)
//        {
//            case "SKIP":
//                email.skipCurrent();
//                state = MainState.LISTING;
//                break;
//            case "REPLY ALL":
//                if(voice.question("Are you sure you want to reply all?")) {
//                    email.replyAll();
//                    state = MainState.COMPOSING;
//                }
//                else { state = MainState.READING; }
//                break;
//            case "REPLY":
//                email.reply();
//                state = MainState.COMPOSING;
//                break;
//            case "FORWARD":
//                email.forward();
//                break;
//            case "DELETE":
//                email.deleteCurrent();
//                state = MainState.LISTING;
//                break;
//            default:
//                voice.commandNotRecognized();
//                break;
//        }
//    }
//
//    private void composingCommand(String command)
//    {
//        if(command == "FINISH")
//        {
//            email.readCurrentComposition();
//            if(voice.question("Would you like to send this email?"))
//            {
//                email.send();
//                state = MainState.LISTING;
//            }
//            else
//            {
//                email.scrap();
//            }
//        }
//        else
//        {
//            email.append(command);
//        }
//    }
}
