package com.cloudnine.emailclerk;

import android.os.AsyncTask;

import java.lang.Thread;
import java.util.*;
import java.io.*;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import android.util.Log;

import com.google.api.services.gmail.*;
import com.google.api.services.gmail.model.*;
/**
 * Created by alecs on 4/4/2018.
 */

public class EmailController {

    private com.google.api.services.gmail.Gmail mService;
    public List<Email> emails;
    volatile boolean finished = false;
    StateController stateController; // Reference to StateController

    EmailController(StateController stateController, com.google.api.services.gmail.Gmail mService) {
        this.stateController = stateController;
        this.mService = mService;
    }

    public void getNewEmails() {
        new AsyncGetEmails().execute();
    }

    //TODO THIS IS WHERE ALL OF THE GMAIL METHODS WILL GO
//    public static void readEmail() {
//        Email curEmail = emails.get(emailCounter);
//        String response = "New email from " + curEmail.getSenderName() + " with the subject " + curEmail.getSubject();
//        String question = "Would you like to reply, delete, skip or repeat?";
//        //mOutputText.setText(response);
//        emailCounter++; // Increment counter
//        //textToSpeech(response);
//        //textToSpeech(question);
//    }

    private class AsyncGetEmails extends AsyncTask<Void, Void, List<Email>> {

        Exception mLastError;

        AsyncGetEmails() {
            mLastError = null;
        }

        @Override
        protected List<Email> doInBackground(Void... params) {
//            if(android.os.Debug.isDebuggerConnected())
//                android.os.Debug.waitForDebugger();
            try {
                return asyncGetEmails();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private List<Email> asyncGetEmails() throws IOException {

            // Create list to store email objects...
            List<Email> emailList = new ArrayList<Email>();

            // Specify which headers should be fetched TODO make it so that it only needs 1 list, so 1 API call...
            List<String> subjectList = new ArrayList<String>();
            subjectList.add("Subject");
            List<String> senderList = new ArrayList<String>();
            senderList.add("From");

            ListMessagesResponse listResponse = mService.users().messages().list("me").setMaxResults(new Long(5)).execute();

            // Loop through the messages to get necessary info
            for (Message message : listResponse.getMessages()) {

                String id = message.getId();

                Message messageSubject = mService.users().messages().get("me", id).setFormat("metadata").setMetadataHeaders((subjectList)).execute();
                Message messageSender = mService.users().messages().get("me", id).setFormat("metadata").setMetadataHeaders(senderList).execute();

                String subject = messageSubject.getPayload().getHeaders().get(0).getValue();
                String senderFull = messageSender.getPayload().getHeaders().get(0).getValue();
                String senderName =  senderFull;//.substring(0, senderFull.indexOf("<")-1);
                String senderEmail = senderFull;//.substring(senderFull.indexOf("<")+1, senderFull.length()-1);
                String messageBody = ""; //TODO

                // Create email object
                Email email = new Email(message.getId(), subject, senderName, senderEmail, messageBody);
                emailList.add(email);
            }
            return emailList;
        }

        @Override
        protected void onPostExecute(List<Email> output) {
            if (output == null || output.size() == 0) {
                //TODO
            } else {
                stateController.emails = output;
                stateController.onEmailsRetrieved();
            }
        }
    }














}
