package com.cloudnine.emailclerk;

public class Email {

    private String id;
    private String threadId;
    private String receiverAddress;
    private String receiverName;
    private String senderAddress;
    private String senderName;
    private String subject;
    private String message;
    private String date;

    // Constructor
    Email(String id, String threadId, String receiverAddress, String receiverName, String senderAddress, String senderName, String subject, String message, String date)
    {
        this.id = id;
        this.threadId = threadId;
        this.receiverAddress = receiverAddress;
        this.receiverName = receiverName;
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.subject = subject;
        this.message = message;
        this.date = date;
    }

    // Getters
    public String getID() { return id; }

    public String getThreadId() { return threadId; }

    public String getReceiverAddress() { return receiverAddress; }

    public String getReceiverName() { return receiverName; }

    public String getSenderAddress() { return senderAddress; }

    public String getSenderName() {
        return senderName;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() { return date; }
}

