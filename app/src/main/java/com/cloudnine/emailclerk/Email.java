package com.cloudnine.emailclerk;

public class Email {

    private String id;
    private String threadId;
    private String subject;
    private String senderName;
    private String senderEmail;
    private String message;

    // Constructor
    Email(String id, String threadId, String subject, String senderName, String senderEmail, String message) {
        this.id = id;
        this.threadId = threadId;
        this.subject = subject;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.message = message;
    }

    // Getters and Setters
    public String getID() {
        return id;
    }

    public String getThreadId() { return threadId; }

    public String getSubject() {
        return subject;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getMessage() {
        return message;
    }

}
