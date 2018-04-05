package com.cloudnine.emailclerk;

/**
 * Created by alecs on 4/4/2018.
 */

public class StateController {

    private com.google.api.services.gmail.Gmail mService;
    protected EmailController emailControler;

    StateController(com.google.api.services.gmail.Gmail mService) {
        this.mService = mService;

        emailControler = new EmailController(mService);
    }
}
