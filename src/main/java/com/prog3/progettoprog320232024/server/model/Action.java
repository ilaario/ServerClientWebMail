package com.prog3.progettoprog320232024.server.model;

import java.io.Serializable;
import com.prog3.progettoprog320232024.client.model.Client;


public class Action implements Serializable{ //implementa serializable così da permettere lo scambio di dati server-client

    public enum Tasks{
        INVIA_EMAIL,
        ELIMINA_EMAIL,
        OTTIENI_EMAIL,
        VISUALIZZA_EMAIL
    }

    String sender; //mittente
    String recipient; //destinatario
    Tasks task; //una delle 4 task da poter richiedere al server
    private boolean success; //successo dell'operazione

    public void setSuccessfully(boolean success) {
        this.success = success;
    }

    public Action(Client sender, String receiverAddress, Tasks task) {
        this.sender = sender.getAddress();
        this.recipient = receiverAddress;
        this.task = task;
    }
    public String getSender() {
        return sender;
    }

    public Tasks getTask() {
        return task;
    }

    public String getRecipient() {
        return recipient;
    }


    //report azioni client-server
    @Override
    public String toString() {
        return success
                ? ">> OK: Action -> Sender='" + sender + "', Recipient='" + recipient + "', Task=" + task
                : ">> ERRORE: Action -> Sender='" + sender + "', Recipient='" + recipient + "', Task=" + task;
    }
}
