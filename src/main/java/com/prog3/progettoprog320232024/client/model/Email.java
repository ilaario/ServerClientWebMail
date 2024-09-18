package com.prog3.progettoprog320232024.client.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Email implements Comparable<Email> { //Email (rispetto alla serializable) utilizzata dal client grazie al binding del simplestringproperty.
                                                  //SerializableEmail utilizzata per l'invio dei dati al server
    public enum EmailState {
        RECEIVED,
        SENT,
    }

    private long ID;
    private final StringProperty sender; //mittente
    private final StringProperty recipient; //destinatario
    private final StringProperty subject; //oggetto
    private final StringProperty text; //testo
    private EmailState state;
    private final LocalDateTime dateTime;
    private boolean read = true;

    // Constructor
    public Email(String sender, String recipient, String subject, String text, EmailState state, LocalDateTime dateTime, long ID) {
        this.sender = new SimpleStringProperty(sender);
        this.recipient = new SimpleStringProperty(recipient);
        this.subject = new SimpleStringProperty(subject);
        this.text = new SimpleStringProperty(text);
        this.state = state;
        this.dateTime = dateTime;
        this.ID = ID;
        this.read = true;
    }

    public Email(SerializableEmail email) {
        this.sender = new SimpleStringProperty(email.getSender());
        this.recipient = new SimpleStringProperty(email.getRecipient());
        this.subject = new SimpleStringProperty(email.getSubject());
        this.text = new SimpleStringProperty(email.getText());
        this.state = email.getState();
        this.dateTime = email.getDateTime();
        ID = email.getID();
        read = email.isRead();
    }

    public Email(long ID) {
        this.sender = new SimpleStringProperty("");
        this.recipient = new SimpleStringProperty("");
        this.subject = new SimpleStringProperty("");
        this.text = new SimpleStringProperty("");
        dateTime = LocalDateTime.now();
        this.ID = ID;
    }
    public String getSender() {
        return sender.get();
    }

    public void setSender(String sender) {
        this.sender.set(sender);
    }

    public StringProperty senderProperty() {
        return sender;
    }

    public String getRecipient() {
        return recipient.get();
    }

    public void setRecipient(String recipient) {
        this.recipient.set(recipient);
    }

    public StringProperty recipientProperty() {
        return recipient;
    }

    public String getSubject() {
        return subject.get();
    }

    public void setSubject(String subject) {
        this.subject.set(subject);
    }

    public StringProperty subjectProperty() {
        return subject;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public StringProperty textProperty() {
        return text;
    }

    public long getID() {
        return ID;
    }

    public void setID(long id) {
        ID = id;
    }

    public EmailState getState() {
        return state;
    }

    public void setState(EmailState state) {
        this.state = state;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Email clone() {
        return new Email(this.getSender(), this.getRecipient(), this.getSubject(), this.getText(), this.getState(), this.getDateTime(), this.getID());
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return "From: " + getSender() + "\n" +
                "To: " + getRecipient() + "\n" +
                "Subject: " + getSubject() + '\n' +
                "Date: " + formatter.format(this.dateTime);
    }

    @Override
    public int compareTo(Email other) {
        int senderComparison = sender.getValue().compareTo(other.getSender());
        if (senderComparison != 0) return senderComparison; //se mittenti sono diversi
        return Long.compare(this.getID(), other.getID()); //se diversi, confronto gli ID delle due mail
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Email other = (Email) obj;
        return ID == other.ID && sender.getValue().equals(other.getSender());
    }

    private static final Pattern SINTASSI_CORRETTA = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validateEmailAddress(String emailStr) {//sintassi email rispettata ^
        Matcher matcher = SINTASSI_CORRETTA.matcher(emailStr);
        return matcher.find();
    }
}

