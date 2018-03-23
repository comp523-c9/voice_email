package com.example.quickstart;

public class Email {

    private String id;
    private String subject;
    private String senderName;
    private String senderEmail;
    private String message;

    // Constructor
    Email(String id, String subject, String senderName, String senderEmail, String message) {
        this.id = id;
        this.subject = subject;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.message = message;
    }

    // Getters and Setters
    public String getID() {
        return id;
    }

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
