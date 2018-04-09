package com.cloudnine.emailclerk;

import android.os.AsyncTask;

import javax.mail.BodyPart;
//import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.util.*;
import java.io.*;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.*;
import com.google.api.services.gmail.model.*;

import org.json.JSONObject;

/**
 * Created by alecs on 4/4/2018.
 */

public class EmailController {

    private com.google.api.services.gmail.Gmail mService;
    public List<Email> emails;
    volatile boolean finished = false;
    StateController stateController; // Reference to StateController

    protected final int GET_NEW_EMAILS = 1;
    private final int SEND_EMAIL     = 2;
    private final int DELETE_EMAIL   = 3;

    EmailController(StateController stateController, com.google.api.services.gmail.Gmail mService) {
        this.stateController = stateController;
        this.mService = mService;
    }

    public void getNewEmails(int num) {
        String[] params = new String[1];
        params[0] = Integer.toString(num);
        new AsyncGetEmails().execute(params);
    }

    public void deleteEmail(String threadId) {
        String[] params = new String[1];
        params[0] = threadId;
        new AsyncDeleteEmail().execute(params);
    }

    public void sendEmail(String to, String from, String subject, String bodyText) {
        List<String> paramsList = new ArrayList<String>();
        paramsList.add(to);
        paramsList.add(from);
        paramsList.add(subject);
        paramsList.add(bodyText);

        String[] params = new String[paramsList.size()];
        params = paramsList.toArray(params);

        //new AsyncSendEmail()
    }

    private class AsyncGetEmails extends AsyncTask<String, Void, List<Email>> {

        Exception mLastError;

        AsyncGetEmails() {
            mLastError = null;
        }

        @Override
        protected List<Email> doInBackground(String... params) {

            try {
                return asyncGetEmails(Integer.parseInt(params[0]));
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private List<Email> asyncGetEmails(int num) throws IOException {

            // Create list to store email objects...
            List<Email> emailList = new ArrayList<Email>();

            // Initialize batch object
            BatchRequest batch = mService.batch();

            ListMessagesResponse listResponse = mService.users().messages().list("me").setMaxResults(new Long(num)).execute();

            final List<com.google.api.services.gmail.model.Message> messages = new ArrayList<com.google.api.services.gmail.model.Message>();
            JsonBatchCallback<Message> callback = new JsonBatchCallback<com.google.api.services.gmail.model.Message>() {
                public void onSuccess(Message message, HttpHeaders responseHeaders) {
                    System.out.println("MessageThreadID:" + message.getThreadId());
                    System.out.println("MessageID:" + message.getId());
                    synchronized (messages) {
                        messages.add(message);
                    }
                }

                @Override
                public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders)
                        throws IOException {
                }
            };

            // Loop through the messages to get necessary info
            for (com.google.api.services.gmail.model.Message message : listResponse.getMessages()) {
                String id = message.getId();
                mService.users().messages().get("me", id).setFormat("full").queue(batch, callback);
            }

            batch.execute();

            for (int i=0; i<messages.size(); i++) {
                Message curMessage = messages.get(i);
                String id = curMessage.getId();
                String threadId = curMessage.getThreadId();
                String subject = "";
                String sender = "";


                List<MessagePartHeader> headers = curMessage.getPayload().getHeaders();
                for(MessagePartHeader header:headers){
                    if (subject == "" || sender == "") {
                        String name = header.getName();
                        if(name.equals("Subject")) {
                            subject = header.getValue();
                        } else if (name.equals("From")) {
                            sender = header.getValue();
                        }
                    }
                }

                String senderName = sender.substring(0, sender.indexOf("<")-1);
                String senderEmail = sender.substring((sender.indexOf("<")+1), sender.indexOf(">"));
                String messageBody = curMessage.getSnippet();
                //String messageBody = StringUtils.newStringUtf8(Base64.decodeBase64(curMessage.getPayload().getParts().get(0).getBody().getData().trim().toString())); //TODO
                messageBody = messageBody.replaceAll("(\r\n|\n\r|\n|\r)", "");

                emailList.add(new Email(id, threadId, subject, senderName, senderEmail, messageBody));
            }
            return emailList;
        }

        @Override
        protected void onPostExecute(List<Email> output) {
            if (output != null && output.size() != 0) {
                stateController.emails = output;
                stateController.onEmailsRetrieved();
            }
        }
    }

    private class AsyncSendEmail extends AsyncTask<String, Void, Void> {

        Exception mLastError;

        AsyncSendEmail() {
            mLastError = null;
        }

        @Override
        protected Void doInBackground(String... params) {




            try {

            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }
    }

    private class AsyncDeleteEmail extends AsyncTask<String, Void, Void> {

        Exception mLastError;

        AsyncDeleteEmail() {
            mLastError = null;
        }

        @Override
        protected Void doInBackground(String... threadId) {

            try {
                mService.users().threads().delete("me", threadId[0]).execute();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }
    }
}
