package com.cloudnine.emailclerk;

import java.util.*;
/**
 * Created by alecs on 4/4/2018.
 * @author Alec Schleicher
 */

public class Email {

    private String id;
    private String threadId;
    private String from;
    private List<String> to;
    private List<String> cc;
    private String deliveredTo;
    private String subject;
    private String message;
    private String date;
    private List<String> labelList;

    /** Constructor **/
    Email(String id, String threadId, String from, List<String> to, List<String> cc, String deliveredTo,
          String subject, String message, String date, List<String> labelList)
    {
        this.id = id;
        this.threadId = threadId;
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.deliveredTo = deliveredTo;
        this.subject = subject;
        this.message = message;
        this.date = date;
        this.labelList = labelList;
    }

    /** Getters **/
    public String getID() {
        return id;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getFrom() {
        return from;
    }

    public List<String> getTo() {
        return to;
    }

    public List<String> getCc() {
        return cc;
    }

    public String getDeliveredTo() {
        return deliveredTo;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public List<String> getLabelList() {
        return labelList;
    }
}