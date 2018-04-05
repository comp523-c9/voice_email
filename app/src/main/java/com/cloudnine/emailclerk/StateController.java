package com.cloudnine.emailclerk;

public class StateController
{
    private MainActivity master;
    private SettingsController settings;
    private VoiceController voice;

    public enum MainState { OPENED, LISTING, READING, COMPOSING }
    public MainState state;

    private com.google.api.services.gmail.Gmail mService;
    protected EmailController emailControler;

    StateController(com.google.api.services.gmail.Gmail mService) {
        this.mService = mService;
        this.master = master;
        this.state = MainState.OPENED;

        emailControler = new EmailController(mService);
        voice = new VoiceController(this);
        settings = new SettingsController();
    }

    public void sendCommand(String command)
    {
        switch(state)
        {
            case OPENED:
                openCommand(command);
                break;
            case LISTING:
                listingCommand(command);
                break;
            case READING:
                readingCommand(command);
                break;
            case COMPOSING:
                composingCommand(command);
                break;
        }
    }

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
                email.skipCurrent();
                break;
            case "DELETE":
                email.deleteCurrent();
                break;
            case "READ":
                email.readCurrent();
                state = MainState.READING
                break;
            case "COMPOSE":
                email.composeNew();
                state = MainState.COMPOSING;
                break;
            default:
                voice.commandNotRecognized();
                break;
        }
    }

    private void readingCommand(String command)
    {
        switch(command)
        {
            case "SKIP":
                email.skipCurrent();
                state = MainState.LISTING;
                break;
            case "REPLY ALL":
                if(voice.question("Are you sure you want to reply all?")) {
                    email.replyAll();
                    state = MainState.COMPOSING;
                }
                else { state = MainState.READING; }
                break;
            case "REPLY":
                email.reply();
                state = MainState.COMPOSING;
                break;
            case "FORWARD":
                email.forward();
                break;
            case "DELETE":
                email.deleteCurrent();
                state = MainState.LISTING;
                break;
            default:
                voice.commandNotRecognized();
                break;
        }
    }

    private void composingCommand(String command)
    {
        if(command == "FINISH")
        {
            email.readCurrentComposition();
            if(voice.question("Would you like to send this email?"))
            {
                email.send();
                state = MainState.LISTING;
            }
            else
            {
                email.scrap();
            }
        }
        else
        {
            email.append(command);
        }


>>>>>>> 52580bdff50ae95c7269970581befe3a2b90ad1f
    }
}
