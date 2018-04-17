package com.cloudnine.emailclerk;

import android.os.AsyncTask;
import javax.mail.Session;
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
    StateController stateController;

    /** Constructor takes a reference to post
     * @StateController and the Gmail service object to use in API calls
     * **/
    EmailController(StateController stateController, com.google.api.services.gmail.Gmail mService) {
        this.stateController = stateController;
        this.mService = mService;
    }

    /** Gets first batch of emails (Gets 'num' emails
     * This is used for the first batch of emails only
     * Later batches are fetched with fetchNewEmails
     * **/
    public void getNewEmails(int num) {
        String[] params = new String[2];
        params[0] = Integer.toString(num);
        params[1] = "false"; // False flag used in AsyncGetEmails - false means it's not a new batch
        new AsyncGetEmails().execute(params);
    }

    /** Moves the email with the specified threadID into the trash folder **/
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

    public void fetchNewEmails(List<Email> emails, int fetchNum) {
        int earliestDate = 0;
        int latestDate = 0;
        for (Email email: emails) {
            int date =Integer.parseInt(convertDate(email.getDate()));
            if (date < earliestDate || earliestDate == 0) {
                earliestDate = date;
            } else if (date > latestDate) {
                latestDate = date;
            }
        }

        new AsyncGetEmails(Integer.toString(earliestDate), Integer.toString(latestDate)).execute(Integer.toString(fetchNum), "true");
    }

    /** Helper method to convert a generic date to Epoch time
     * Epoch time is used in a query when fetching emails in fetchNewEmails()
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

    /** Above are the simplified methods that StateController calls
     * ===========================================================================================================================
     * ===========================================================================================================================
     * ===========================================================================================================================
     *  Below are the ASYNCHRONOUS tasks that the above methods use. Most of the more specific work is done here
     * **/


    /** AsyncGetEmails is called by
     * @getNewEmails() and
     * @fetchNewEmails()
     * **/
    private class AsyncGetEmails extends AsyncTask<String, Void, List<Email>> {

        Exception mLastError;
        String startDate;
        String endDate;

        /** Default constructor that doesn't need email dates, used by
         * @getNewEmails() **/
        AsyncGetEmails() {
            mLastError = null;
        }

        /** Overloaded constructor that takes in dates. This constructor is used by
         * @fetchNewEmails() **/
        AsyncGetEmails(String startDate, String endDate) {
            mLastError = null;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        /** The only actual asynchronous part of the AsyncTask **/
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

            // Initialize batch object (for batch API request)
            BatchRequest batch = mService.batch();

            List<String> labels = new ArrayList<String>();
            labels.add("INBOX");
            ListMessagesResponse listResponse;


            if (getNewBatch.equals("false")) { // If not first batch...
                listResponse = mService.users().messages().list("me").setLabelIds(labels).setMaxResults(new Long(num)).execute();
            } else { // Get a more specific set of emails using the input dates
                startDate = Integer.toString(Integer.parseInt(startDate) + 1); // Add 1 second because 'after:' is inclusive
                String query = "before:" + endDate + " || after:" + startDate;
                listResponse = mService.users().messages().list("me").setLabelIds(labels).setMaxResults(new Long(num))
                        .setQ(query).execute();
            }

            final List<com.google.api.services.gmail.model.Message> messages = new ArrayList<Message>();

            /** This is the callback for the batch request.
             * After the request is completed, either onSuccess() or onFailure() is called
             * **/
            JsonBatchCallback<com.google.api.services.gmail.model.Message> callback = new JsonBatchCallback<com.google.api.services.gmail.model.Message>() {
                public void onSuccess(com.google.api.services.gmail.model.Message message, HttpHeaders responseHeaders) {
                    synchronized (messages) {
                        messages.add(message);
                    }
                }

                @Override
                public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) { }
            };

            // Loop through the messages to get necessary info
            for (com.google.api.services.gmail.model.Message message : listResponse.getMessages()) {
                String id = message.getId();
                mService.users().messages().get("me", id).setFormat("full").queue(batch, callback);
            }

            /** Execute the batch request **/
            batch.execute();

            /** Loop through raw emails in messages and pick out info we want... **/
            for (int i=0; i<messages.size(); i++) {
                com.google.api.services.gmail.model.Message curMessage = messages.get(i);

                /** Get Id and threadId **/
                String id = curMessage.getId();
                String threadId = curMessage.getThreadId();

                /** Get receiver and sender address (and optionally their names as well)
                 *  Get the email subject
                 *  Get the date the email was received
                 *  Info is retrieved by looking for specific headers in the email
                 * **/
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

                /** Receiver and Sender is of the form:
                 * John Smith <johnsmith@example.com> **/
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

                List<String> messageParts = new ArrayList<String>();
                messageParts = lookForMessage(messageParts, curMessage.getPayload().getParts());

                String messageBody = "";
                for (int y=0; y<messageParts.size(); y++) {
                    messageBody += messageParts.get(y);
                }
                
                if (messageBody.equals("")) {
                    messageBody = curMessage.getSnippet();
                    if (messageBody.equals("")) {
                        messageBody = "There is no message body";
                    }
                }

                /** TODO Get the email body depending on what type of email it is... **/

                //String messageBody = StringUtils.newStringUtf8(Base64.decodeBase64(curMessage.getPayload().getParts().get(0).getBody().getData().trim().toString())); //TODO
                //messageBody = messageBody.replaceAll("(\r\n|\n\r|\n|\r)", "");

                /** After all the email info we want is retrieved, create a new
                 * @Email object and add it to emailList **/
                emailList.add(new Email(id, threadId, receiverAddress, receiverName, senderAddress, senderName, subject, messageBody, date));
            }

            /** After all the emails are looped through, move on to
             * @onPostExecute() **/
            return emailList;
        }

        private List<String> lookForMessage(List<String> messageParts, List<MessagePart> parts) {

            String messageBody = "";

            for (int x=0; x<parts.size(); x++) {

                String name = parts.get(x).getMimeType();

                /** Look for attachments and add them to the message if they exist **/
                if (parts.get(x).getMimeType().contains("application")) {
                    try {
                        messageParts.add("The email contains one file with the name " + parts.get(x).getFilename());
                    } catch (Exception e){}
                }

                /** Look for images and add them to the message if they exist **/
                if (parts.get(x).getMimeType().contains("image")) {
                    try {
                        messageParts.add("The email contains an image with the name " + parts.get(x).getFilename());
                    } catch (Exception e){}
                }

                /** Look for basic text and add it to the message if it exists **/
                if (parts.get(x).getMimeType().contains("text/plain")) {
                    try {
                        String message = StringUtils.newStringUtf8(Base64.decodeBase64(parts.get(x).getBody().getData().trim().toString()));
                        message = message.replaceAll("(\r\n|\n\r|\n|\r)", "");
                        messageParts.add(message);
                    } catch (Exception e){}
                }

                /** If message contains a multipart, recursively call this method again to dig further for messages **/
                if (parts.get(x).getMimeType().contains(("multipart"))) {
                    lookForMessage(messageParts, parts.get(x).getParts());
                }
            }

            return messageParts;
        }

        @Override
        protected void onPostExecute(List<Email> output) {
            if (output != null && output.size() != 0) {
                stateController.emails.addAll(output);
                stateController.onEmailsRetrieved();
            }
        }
    }

    /** AsyncReplyToEmail is called by
     * @sendEmail() with a specific constructor
     * **/
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

    /** AsyncReplyToEmail is called by
     * @sendEmail() with a specific constructor
     * **/
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

    /** AsyncDeleteEmail is called by
     * @deleteEmail()
     * **/
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