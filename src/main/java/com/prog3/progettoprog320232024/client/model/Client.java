package com.prog3.progettoprog320232024.client.model;

import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

public class Client {

    private final SimpleStringProperty address; //SimpleStringProperty: utilizzato in javaFX per rendere una stringa osservabile
    private final SimpleListProperty<Email> inbox; //Idem per liste
    private final SimpleListProperty<Email> sent; //email inviate
    private final SimpleStringProperty state;
    public Email selectedEmail; //Email selezionata dalla inbox su cui svolgere le varie operazioni
    public Email newEmail;  //La mail da inviare (contiene per esempio il destinatario della mail che stiamo inviando)

    public Client(String address) {
        this.address = new SimpleStringProperty(address);
        this.inbox = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.sent = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.state = new SimpleStringProperty("");
    }

    public SimpleStringProperty addressProperty() {
        return address;
    }

    public SimpleListProperty<Email> inboxProperty() {
        return inbox;
    }

    public SimpleStringProperty stateProperty() {
        return state;
    }

    public String getAddress() {
        return address.get();
    }

    public SimpleListProperty<Email> sentProperty() {
        return sent;
    }

    public SimpleListProperty<Email> whereIs(Email email) { //per vedere se la mail specificata si trova nelle inbox o nelle inviate,
        SimpleListProperty<Email> ret = null;               //ritorna la lista inbox o inviate
        if (inboxProperty().contains(email))
            ret = inboxProperty();
        if (sentProperty().contains(email))
            ret = sentProperty();
        return ret;
    }

    public long getID() { //restituisce l'ID massimo tra tutte le mail (inbox e inviate)
        long max = 0;     //permette di generare un ID univoco per una nuova mail
        SimpleListProperty<Email> allEmails = new SimpleListProperty<>(FXCollections.observableArrayList());
        allEmails.addAll(inboxProperty());
        allEmails.addAll(sentProperty());
        for (Email email : allEmails) {
            if (email.getID() > max) max = email.getID();
        }
        return max;
    }

    //Controlla se in una lista c'è la mail con l'id specificato
    public Email findEmailById(SimpleListProperty<Email> emailList, long id) {
        for (Email email : emailList) {
            if (email.getID() == id) {
                return email;
            }
        }
        return null;
    }

    // verifica se un'istanza di Email specificata ha un ID che corrisponde all'ID di qualsiasi email all'interno di una data lista
    //utilizzato per controllare la presenza di duplicati di un'email in base al suo ID all'interno di una lista di email.
    public boolean hasSameIDInCollection(SimpleListProperty<Email> list, Email email) {
        for (Email iteratedEmail : list) {
            if (iteratedEmail.getID() == email.getID()) {
                return true;
            }
        }
        return false;
    }
}
