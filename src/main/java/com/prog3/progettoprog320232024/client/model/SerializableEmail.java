package com.prog3.progettoprog320232024.client.model;

import java.io.Serializable;
import java.time.LocalDateTime;
//Utile per l'invio delle mail al server e comoda per mantenere la logica della visualizzazione delle Email
//separata dalla logica di trasferimento in rete delle SerializableEmail
public class SerializableEmail implements Serializable {

    private final String sender;
    private final String recipient;
    private final String subject;
    private final String text;

    private final Email.EmailState state;
    private final LocalDateTime dateTime;
    private final long ID;
    private final boolean read;

    public boolean isRead() {
        return read;
    }

    public long getID() {
        return ID;
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public Email.EmailState getState() {
        return state;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public SerializableEmail(Email email) {
        this.sender = email.getSender().strip();
        this.recipient = email.getRecipient().strip();
        this.subject = email.getSubject();
        this.text = email.getText();
        this.state = email.getState();
        this.dateTime = email.getDateTime();
        this.ID = email.getID();
        this.read = email.isRead();
    }

}
