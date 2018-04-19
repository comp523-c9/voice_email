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
 * @author Alec Schleicher
 */

public class EmailController {

    private com.google.api.services.gmail.Gmail mService;
    StateController stateController;

    /** EmailController keeps a one-time reference to all Labels (w/ their name and id, etc)
     *  If expanded upon, Label could be made its own class and this functionality could be moved
     *  to StateController. With the current scope of the project, it makes sense to keep it all
     *  in EmailController
     */
    List<Label> allLabels = new ArrayList<>();

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
    public void getNewEmails(int num, boolean unreadOnly) {
        String[] params = new String[2];
        params[0] = Integer.toString(num);
        params[1] = "false"; // False flag used in AsyncGetEmails - false means it's not a new batch
        new AsyncGetEmails(unreadOnly).execute(params);
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
    public void sendEmail(Email email, String messageBody, boolean replyAll) {
        new AsyncReplyToEmail(email, replyAll).execute(messageBody);
    }

    public void saveEmail(Email email) {
        new AsyncMoveEmail(email.getID(), email.getLabelList()).execute();
    }

    public void fetchNewEmails(List<Email> emails, int fetchNum, boolean unreadOnly) {
        int earliestDate = 0;
        int latestDate = 0;
        for (Email email: emails) {
            int date =Integer.parseInt(convertDate(email.getDate()));
            if (date < earliestDate || earliestDate == 0) {
                earliestDate = date;
            }
            if (date > latestDate) {
                latestDate = date;
            }
        }

        new AsyncGetEmails(Integer.toString(latestDate), Integer.toString(earliestDate), unreadOnly).execute(Integer.toString(fetchNum), "true");
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

    /** Helper method that pulls out a name, if there, from a recipient String **/
    public String getNameFromRecipient(String recipient) {

        String name = "";

        if (recipient.contains("<")) {
            name = recipient.trim().substring(0, recipient.indexOf("<")-1);
        }
        return name;
    }

    /** Helper method that pulls out the email address from a recipient String **/
    public String getAddressFromRecipient(String recipient) {
        if (recipient.contains("<")) {
            return recipient.substring((recipient.indexOf("<")+1), recipient.indexOf(">"));
        } else {
            return recipient.trim();
        }
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
        boolean unreadOnly;

        /** Default constructor that doesn't need email dates, used by
         * @getNewEmails() **/
        AsyncGetEmails(boolean unreadOnly) {
            this.unreadOnly = unreadOnly;
            mLastError = null;
        }

        /** Overloaded constructor that takes in dates. This constructor is used by
         * @fetchNewEmails() **/
        AsyncGetEmails(String startDate, String endDate, boolean unreadOnly) {
            mLastError = null;
            this.startDate = startDate;
            this.endDate = endDate;
            this.unreadOnly = unreadOnly;
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

            /** Create list to store email objects... **/
            List<Email> emailList = new ArrayList<Email>();

            /** Initialize batch object (for batch API request) **/
            BatchRequest batch = mService.batch();

            /** Get emails from the inbox only **/
            List<String> labels = new ArrayList<String>();
            labels.add("INBOX");

            /** If only getting unread emails, add to label **/
            if(unreadOnly) {
                labels.add("UNREAD");
            }

            ListMessagesResponse listResponse;

            ListLabelsResponse listLabelsReponse = mService.users().labels().list("me").execute();
            allLabels = listLabelsReponse.getLabels();

            if (getNewBatch.equals("false")) { // If not first batch...
                listResponse = mService.users().messages().list("me").setLabelIds(labels).setMaxResults(new Long(num)).execute();
            } else { // Get a more specific set of emails using the input dates
                startDate = Integer.toString(Integer.parseInt(startDate) + 1); // Add 1 second because 'after:' is inclusive
                endDate = Integer.toString(Integer.parseInt(endDate) + 1); // Subtract 1 second from endDate as well...
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
                String from = "";
                String to = "";
                String ccRecipients = "";
                String deliveredTo = "";
                String subject = "";
                String date = "";

                List<MessagePartHeader> headers = curMessage.getPayload().getHeaders();
                for(MessagePartHeader header : headers){
                    String name = header.getName();
                    if(name.equals("From")) {
                        from = header.getValue();
                    } else if (name.equals("To")) {
                        to = header.getValue();
                    } else if (name.equals("Cc")) {
                        ccRecipients = header.getValue();
                    } else if (name.equals("Delivered-To")) {
                        deliveredTo = header.getValue();
                    } else if (name.equals("Subject")) {
                        subject = header.getValue();
                    } else if (name.equals("Date")) {
                        date = header.getValue();
                    }
                }

                List<String> toList = new ArrayList<>();
                if (to.contains(",")) {
                        toList = Arrays.asList(to.split(","));
                } else {
                    toList.add(to);
                }

                List<String> ccList = new ArrayList<>();
                if (!ccRecipients.equals("")) {
                    if (ccRecipients.contains(",")) {
                        ccList = Arrays.asList(ccRecipients.split(","));
                    } else {
                        ccList.add(ccRecipients);
                    }
                }

                /** A complex way to parse MIME Emails and get the message body or any attachments
                 *  If it's just a text/plain email, just grab the body and be done. Else, recursively
                 *  loop to find attachments or plain text
                 * **/
                String messageBody = "";

                if (curMessage.getPayload().getMimeType().equals("text/plain")) {
                    String message = StringUtils.newStringUtf8(Base64.decodeBase64(curMessage.getPayload().getBody().getData().trim().toString()));
                    messageBody = message.replaceAll("(\r\n|\n\r|\n|\r)", "");
                } else {
                    try {
                        List<String> messageParts = new ArrayList<String>();
                        messageParts = lookForMessage(messageParts, curMessage.getPayload().getParts());

                        for (int y=0; y<messageParts.size(); y++) {
                            messageBody += messageParts.get(y) + " ";
                        }
                    } catch (Exception e) {

                    }
                }
                
                if (messageBody.equals("")) {
                    messageBody = curMessage.getSnippet();
                    if (messageBody.equals("")) {
                        messageBody = "There is no message body";
                    }
                }

                /** Lastly, get the email labels **/
                List<String> labelList = curMessage.getLabelIds();

                /** After all the email info we want is retrieved, create a new
                 * @Email object and add it to emailList **/
                emailList.add(new Email(id, threadId, from, toList, ccList, deliveredTo, subject, messageBody, date, labelList));

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
                        message = message.replaceAll("(\r\n|\n\r|\n|\r)", "").trim();
                        messageParts.add(message + ".");
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
                if (stateController.emails.size()==stateController.INITIAL_FETCH_NUMBER) {
                    stateController.onEmailsRetrieved();
                }
            }
        }
    }

    /** AsyncReplyToEmail is called by
     * @sendEmail() with a specific constructor
     * **/
    private class AsyncReplyToEmail extends AsyncTask<String, Void, Void> {

        Exception mLastError;
        Email email;
        boolean replyAll;

        AsyncReplyToEmail(Email email, boolean replyAll) {
            this.email = email;
            this.replyAll = replyAll;
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
            String messageBody = params[0];

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            try {
                /** Instantiate mimemessage and set from address **/
                MimeMessage mimeMessage = new MimeMessage(session);
                mimeMessage.setFrom(new InternetAddress(this.email.getDeliveredTo())); // Set from

                /** If it's a normal reply... **/
                if (!replyAll) {
                    /** Get the address and possible name of the sender (you) **/
                    String address = getAddressFromRecipient(email.getFrom());
                    String name = getNameFromRecipient(email.getFrom());

                    /** Add it as a recipient **/
                    if (name.equals("")) {
                        mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(address));
                    } else {
                        mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(address, name));
                    }
                } else { /** Else if it's a reply all... **/

                    List<InternetAddress> toRecipients= new ArrayList<>();

                    /** First add the original sender as a recipient **/
                    String address = getAddressFromRecipient(email.getFrom());
                    String name = getNameFromRecipient(email.getFrom());
                    if (name.equals("")) {
                        toRecipients.add(new InternetAddress(address));
                    } else {
                        toRecipients.add(new InternetAddress(address, name));
                    }

                    /** then loop through other 'TO' recipients and add all of them except your own...**/
                    String ownAddress = getAddressFromRecipient(email.getDeliveredTo());
                    for (int i=0; i<email.getTo().size(); i++) {
                        String toAddress = getAddressFromRecipient(email.getTo().get(i));
                        String toName = getNameFromRecipient(email.getTo().get(i));

                        /** If 'TO' recipient is not yourself... **/
                        if (!toAddress.equals(ownAddress)) {
                            /** Then add it to the list
                             * If the name is not there, only pass in the address **/
                            if (name.equals("")) {
                                toRecipients.add(new InternetAddress(toAddress));
                            } else {
                                toRecipients.add(new InternetAddress(toAddress, toName));
                            }
                        }
                    }

                    /** Finally, convert toRecipients List into an Array and add it to the MimeMessage **/
                    InternetAddress[] toArray = new InternetAddress[toRecipients.size()];
                    for (int i=0; i<toArray.length; i++) {
                        toArray[i] = toRecipients.get(i);
                    }
                    mimeMessage.addRecipients(javax.mail.Message.RecipientType.TO, toArray);

                    /** Loop through 'Cc' recipients **/
                    InternetAddress[] ccRecipients = new InternetAddress[email.getCc().size()];
                    for (int i=0; i<email.getCc().size(); i++) {
                        String ccAddress = getAddressFromRecipient(email.getCc().get(i));
                        String ccName = getNameFromRecipient(email.getCc().get(i));

                        /** If the name is not there, only pass in the address **/
                        if (ccName.equals("")) {
                            ccRecipients[i] = new InternetAddress(ccAddress);
                        } else {
                            ccRecipients[i] = new InternetAddress(ccAddress, ccName);
                        }
                    }
                    mimeMessage.addRecipients(javax.mail.Message.RecipientType.CC, ccRecipients);

                }

                /** Set subject with Re: if not already there **/
                String subject = email.getSubject();
                if (subject.substring(0, 4).toUpperCase().contains("RE")) {
                    mimeMessage.setSubject(subject);
                } else {
                    mimeMessage.setSubject("Re: " + subject);
                }

                /** Set message body... **/
                mimeMessage.setText(messageBody);
                mimeMessage.setHeader("In-Reply-To", email.getID());
                mimeMessage.setHeader("References", email.getID());

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                mimeMessage.writeTo(buffer);
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

    private class AsyncMoveEmail extends AsyncTask<Void, Void, Void> {

        String messageId;
        List<String> labelList;
        Exception mLastError;

        AsyncMoveEmail(String messageId, List<String> labelList) {
            this.messageId = messageId;
            this.labelList = labelList;
            mLastError = null;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {

                /** Create a list of labels ids to add and remove **/
                List<String> labelIdsToAdd = new ArrayList<>();
                List<String> labelIdsToRemove = new ArrayList<>();

                /** Add the label id of the Email Clerk label **/
                for (int i=0; i<allLabels.size(); i++) {
                    if (allLabels.get(i).getName().equals("Email Clerk")) {
                        labelIdsToAdd.add(allLabels.get(i).getId());
                    }
                }

                /** If an Email Clerk label hasn't been found, make one and search again for the Id **/
                if (labelIdsToAdd.size() == 0) {
                    Label label = new Label().setName("Email Clerk").setLabelListVisibility("labelShow").setMessageListVisibility("show");
                    mService.users().labels().create("me", label).execute();

                    allLabels = mService.users().labels().list("me").execute().getLabels();

                    for (int i=0; i<allLabels.size(); i++) {
                        if (allLabels.get(i).getName().equals("Email Clerk")) {
                            labelIdsToAdd.add(allLabels.get(i).getId());
                        }
                    }
                }

                /** Populate labelIdsToRemove by checking for names in labelsList **/
                for (int i=0; i<labelList.size(); i++) {
                    for (int j=0; j<allLabels.size(); j++) {
                        if (allLabels.get(j).getName().equals(labelList.get(i))) {
                            labelIdsToRemove.add(allLabels.get(j).getId());
                        }
                    }
                }

                /** Remove SENT label if going to be removed **/
                for (int i=0; i<labelIdsToRemove.size(); i++) {
                    if (labelIdsToRemove.get(i).equals("SENT")) {
                        labelIdsToRemove.remove(i);
                    }
                }

                ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelIdsToAdd).setRemoveLabelIds(labelIdsToRemove);
                mService.users().messages().modify("me", messageId, mods).execute();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
            return null;
        }
    }
}