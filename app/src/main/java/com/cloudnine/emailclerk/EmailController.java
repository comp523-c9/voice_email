package com.cloudnine.emailclerk;

import android.os.AsyncTask;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


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

import java.io.ByteArrayOutputStream;

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

    public void sendEmail(Email email, String bodyText) {
        List<String> paramsList = new ArrayList<String>();
        paramsList.add(email.getID());
        paramsList.add(email.getReceiverAddress());
        paramsList.add(email.getReceiverName());
        paramsList.add(email.getSenderAddress());
        paramsList.add(email.getSenderName());
        paramsList.add(email.getSubject());
        paramsList.add(bodyText);

        String[] params = new String[paramsList.size()];
        params = paramsList.toArray(params);

        new AsyncSendEmail().execute(params);
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
            JsonBatchCallback<com.google.api.services.gmail.model.Message> callback = new JsonBatchCallback<com.google.api.services.gmail.model.Message>() {
                public void onSuccess(com.google.api.services.gmail.model.Message message, HttpHeaders responseHeaders) {
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
                com.google.api.services.gmail.model.Message curMessage = messages.get(i);
                String id = curMessage.getId();
                String threadId = curMessage.getThreadId();
                String receiver = "";
                String sender = "";
                String subject = "";


                List<MessagePartHeader> headers = curMessage.getPayload().getHeaders();
                for(MessagePartHeader header:headers){
                    if (subject == "" || sender == "" || receiver == "") {
                        String name = header.getName();
                        if(name.equals("Subject")) {
                            subject = header.getValue();
                        } else if (name.equals("From")) {
                            sender = header.getValue();
                        } else if (name.equals("To")) {
                            receiver = header.getValue();
                        }
                    }
                }

                String receiverAddress = receiver.substring((receiver.indexOf("<")+1), receiver.indexOf(">"));
                String receiverName = receiver.substring(0, receiver.indexOf("<")-1);
                String senderAddress = sender.substring((sender.indexOf("<")+1), sender.indexOf(">"));
                String senderName = sender.substring(0, sender.indexOf("<")-1);

                String messageBody = curMessage.getSnippet();
                //String messageBody = StringUtils.newStringUtf8(Base64.decodeBase64(curMessage.getPayload().getParts().get(0).getBody().getData().trim().toString())); //TODO
                //messageBody = messageBody.replaceAll("(\r\n|\n\r|\n|\r)", "");

                emailList.add(new Email(id, threadId, receiverAddress, receiverName, senderAddress, senderName, subject, messageBody));
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
                asyncSendEmail(params);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }

        private void asyncSendEmail(String[] params) {
            String id = params[0];
            String fromAddress = params[2];
            String fromName = params[3];
            String toAddress = params[4];
            String toName = params[5];
            String subject = params[6];
            String bodyText = params[7];

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            try {
                MimeMessage email = new MimeMessage(session);
                email.setFrom(new InternetAddress(fromAddress, fromName));
                email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toAddress, toName));
                if (subject.substring(0, 3).equals("RE:") || subject.substring(0, 3).equals("Re:")) {
                    email.setSubject(subject);
                } else {
                    subject = "Re: " + subject;
                    email.setSubject(subject);
                }
                email.setText(bodyText);
                email.setHeader("In-Reply-To", id);
                email.setHeader("References", id);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                email.writeTo(buffer);
                byte[] bytes = buffer.toByteArray();
                String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
                com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
                message.setRaw(encodedEmail);
                mService.users().messages().send("me", message).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }


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
