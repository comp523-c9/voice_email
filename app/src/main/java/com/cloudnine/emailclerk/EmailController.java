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


import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.services.gmail.model.*;

import java.io.ByteArrayOutputStream;

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

    public void getNewEmails(int num) {
        String[] params = new String[2];
        params[0] = Integer.toString(num);
        params[1] = "false";
        new AsyncGetEmails().execute(params);
    }

    public void deleteEmail(String threadId) {
        String[] params = new String[1];
        params[0] = threadId;
        new AsyncDeleteEmail().execute(params);
    }

    /** sendEmail() is overloaded. This is the 'compose' one **/
    public void sendEmail(String toAddress, String fromAddress, String subject, String messageBody) {
        List<String> paramsList = new ArrayList<String>();
        paramsList.add(toAddress);
        paramsList.add(fromAddress);
        paramsList.add(subject);
        paramsList.add(messageBody);

        String[] params = new String[paramsList.size()];
        params = paramsList.toArray(params);

        new AsyncComposeEmail().execute(params);
    }

    /** This is the reply one **/
    public void sendEmail(String toAddress, String fromAddress, String subject, String messageBody, Email email) {
        List<String> paramsList = new ArrayList<String>();
        paramsList.add(toAddress);
        paramsList.add(email.getSenderName());
        paramsList.add(fromAddress);
        paramsList.add(email.getReceiverName());
        paramsList.add(subject);
        paramsList.add(messageBody);
        paramsList.add(email.getID());

        String[] params = new String[paramsList.size()];
        params = paramsList.toArray(params);

        new AsyncReplyToEmail().execute(params);
    }

    public void fetchNewEmails(Email startEmail, Email endEmail) {
        String startDate = convertDate(startEmail.getDate());
        String endDate = convertDate(endEmail.getDate());
        new AsyncGetEmails(startDate, endDate).execute(Integer.toString(stateController.EMAILS_TO_FETCH_NUM), "true");
    }

    /**
     * @param inputDate
     * @return convertDate in Epoch time for use in fetchNewEmails()
     **/
    private String convertDate(String inputDate) {
        String convertedDate = "";
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss ZZZZZ");

        try {
            Date date = df.parse(inputDate.substring(5));
            convertedDate = Long.toString(date.getTime());
        } catch(Exception e) {
            Exception b = e;
        }
        return convertedDate.substring(0, convertedDate.length() - 3); // Chop off the last 3 digits to convert to seconds
    }

    private class AsyncGetEmails extends AsyncTask<String, Void, List<Email>> {

        Exception mLastError;
        String startDate;
        String endDate;

        AsyncGetEmails() {
            mLastError = null;
        }
        AsyncGetEmails(String startDate, String endDate) {
            mLastError = null;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        protected List<Email> doInBackground(String... params) {

            try {
                return asyncGetEmails(Integer.parseInt(params[0]), params[1]);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private List<Email> asyncGetEmails(int num, String getNewBatch) throws IOException {

            // Create list to store email objects...
            List<Email> emailList = new ArrayList<Email>();

            // Initialize batch object
            BatchRequest batch = mService.batch();

            List<String> labels = new ArrayList<String>();
            labels.add("INBOX");
            ListMessagesResponse listResponse;

            if (getNewBatch.equals("false")) {
                listResponse = mService.users().messages().list("me").setLabelIds(labels).setMaxResults(new Long(num)).execute();
            } else {
                startDate = Integer.toString(Integer.parseInt(startDate) + 2); // Add 1 second because 'after:' is inclusive
                String query = "before:" + endDate + " || after:" + startDate;
                listResponse = mService.users().messages().list("me").setLabelIds(labels).setMaxResults(new Long(num))
                        .setQ(query).execute();
            }

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
                public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
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
                String date = "";


                List<MessagePartHeader> headers = curMessage.getPayload().getHeaders();
                for(MessagePartHeader header:headers){
                    if (subject == "" || sender == "" || receiver == "" || date == "") {
                        String name = header.getName();
                        if(name.equals("Subject")) {
                            subject = header.getValue();
                        } else if (name.equals("From")) {
                            sender = header.getValue();
                        } else if (name.equals("To")) {
                            receiver = header.getValue();
                        } else if (name.equals("Date")) {
                            date = header.getValue();
                        }
                    }
                }

                String receiverAddress;
                String receiverName;
                String senderAddress;
                String senderName;

                if (receiver.indexOf("<") > 0) {
                    receiverAddress = receiver.substring((receiver.indexOf("<")+1), receiver.indexOf(">"));
                    receiverName = receiver.substring(0, receiver.indexOf("<")-1);
                } else {
                    receiverAddress = receiver;
                    receiverName = "";
                }
                if (sender.indexOf("<") > 0) {
                    senderAddress = sender.substring((sender.indexOf("<")+1), sender.indexOf(">"));
                    senderName = sender.substring(0, sender.indexOf("<")-1);
                } else {
                    senderAddress = sender;
                    senderName = "";
                }

                //String messageBody = curMessage.getSnippet();
//                String messageBody = StringUtils.newStringUtf8(Base64.decodeBase64(curMessage.getPayload().getParts().get(0).getBody().getData().trim().toString())); //TODO
//                messageBody = messageBody.replaceAll("(\r\n|\n\r|\n|\r)", "");
                String messageBody = "There is no message body";
                if (!curMessage.getPayload().getMimeType().equals("text/html")) {
                    try {
                        messageBody = StringUtils.newStringUtf8(Base64.decodeBase64(curMessage.getPayload().getParts().get(0).getBody().getData().trim().toString())); //TODO
                        messageBody = messageBody.replaceAll("(\r\n|\n\r|\n|\r)", "");
                    } catch(Exception e) {

                    }
                }



                emailList.add(new Email(id, threadId, receiverAddress, receiverName, senderAddress, senderName, subject, messageBody, date));
            }
            return emailList;
        }

        @Override
        protected void onPostExecute(List<Email> output) {
            if (output != null && output.size() != 0) {
                stateController.emails.addAll(output);
                if (stateController.emails.size()==stateController.STARTING_EMAIL_NUM) {
                    stateController.onEmailsRetrieved();
                }
            }
        }
    }

    private class AsyncReplyToEmail extends AsyncTask<String, Void, Void> {

        Exception mLastError;

        AsyncReplyToEmail() {
            mLastError = null;
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                asyncReplyToEmail(params);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }

        private void asyncReplyToEmail(String[] params) {
            String toAddress = params[0];
            String toName = params[1];
            String fromAddress = params[2];
            String fromName = params[3];
            String subject = params[4];
            String messageBody = params[5];
            String id = params[6];

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            try {
                MimeMessage email = new MimeMessage(session);
                email.setFrom(new InternetAddress(fromAddress, fromName));
                email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toAddress, toName));
                email.setSubject(subject);
                email.setText(messageBody);
                email.setHeader("In-Reply-To", id);
                email.setHeader("References", id);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                email.writeTo(buffer);
                byte[] bytes = buffer.toByteArray();
                String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
                com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
                message.setRaw(encodedEmail);
                Draft draft = new Draft();
                draft.setMessage(message);
                mService.users().drafts().create("me", draft).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    private class AsyncComposeEmail extends AsyncTask<String, Void, Void> {

        Exception mLastError;

        AsyncComposeEmail() {
            mLastError = null;
        }

        @Override
        protected Void doInBackground(String... params) {

            try {
                asyncComposeEmail(params);
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }

        private void asyncComposeEmail(String[] params) {
            String toAddress = params[0];
            String fromAddress = params[1];
            String subject = params[2];
            String messageBody = params[3];;

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            try {
                MimeMessage email = new MimeMessage(session);
                email.setFrom(new InternetAddress(fromAddress)); //TODO also add fromName
                email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toAddress)); //TODO also add toName
                email.setSubject(subject);
                email.setText(messageBody);

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                email.writeTo(buffer);
                byte[] bytes = buffer.toByteArray();
                String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
                com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
                message.setRaw(encodedEmail);
                Draft draft = new Draft();
                draft.setMessage(message);
                mService.users().drafts().create("me", draft).execute();
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
                mService.users().messages().trash("me", threadId[0]).execute();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }
    }
}