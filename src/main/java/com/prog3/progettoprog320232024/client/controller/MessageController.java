package com.prog3.progettoprog320232024.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import com.prog3.progettoprog320232024.client.model.Client;
import com.prog3.progettoprog320232024.client.model.Email;
import com.prog3.progettoprog320232024.client.model.SerializableEmail;
import com.prog3.progettoprog320232024.server.model.Action;
import com.prog3.progettoprog320232024.server.controller.ServerController;
import javafx.stage.Stage;


import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageController { //Controller per la schermata dell'invio di una mail (destinatari, oggetto, testo, tasto invia)

    @FXML
    private TextField destinatarioTextField;

    @FXML
    private TextField oggettoTextField;

    @FXML
    private TextArea msgTextArea;

    private Client client;
    private ClientController clientController;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }

    public void bindMsg() {
        destinatarioTextField.textProperty().bindBidirectional(client.newEmail.recipientProperty());
        oggettoTextField.textProperty().bindBidirectional(client.newEmail.subjectProperty());
        msgTextArea.textProperty().bindBidirectional(client.newEmail.textProperty());
    }


    public void onSendButtonClicked(Event event){
        doNewMailOperation(event, new Action(client, client.newEmail.getRecipient(), Action.Tasks.INVIA_EMAIL));
    }

    public void doNewMailOperation(Event event,Action action){

        AtomicBoolean allGood = new AtomicBoolean(false);

        Thread t1 = new Thread(() -> {
            synchronized (clientController.reentrantLock) {
                try {
                    //Controllo il formato della mail prima dell'invio
                    if (action.getTask() == Action.Tasks.INVIA_EMAIL) {
                        String[] receiversTmp = action.getRecipient().split(",");

                        for (String s : receiversTmp) {
                            if (!Email.validateEmailAddress(s.trim())) { //Se l'email non è in un formato valido
                                Platform.runLater(() -> {
                                    Alert a = new Alert(Alert.AlertType.ERROR, "E-mail con formato errato");
                                    a.show();
                                });
                                return;
                            }
                        }
                    }

                    //Stabilisco una connessione e invio i dati al server
                    clientController.getNewSocket();
                    clientController.setSocketSuccess();
                    clientController.sendActionToServer(action); //ACTION=InviaMail in questo caso
                    clientController.sendEmailToServer(new SerializableEmail(client.newEmail)); //Invio la mail serializzabile al server
                    ServerController.ServerResponse response = clientController.waitForResponse(); //Attendo la risposta da parte del server


                    if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                        System.out.println("Email inviata correttamente a " + action.getRecipient());
                        allGood.set(true);
                    } else {
                        handleServerResponse(response); //Se l'azione non è stata completata gestito la risposta
                    }
                } catch (IOException socketException) {
                    clientController.setSocketFailure();
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.ERROR, "Impossibile comunicare con il server!");
                        a.show();
                    });
                } catch (ClassNotFoundException e) {
                    System.out.println("Impossibile leggere");
                } finally {
                    clientController.closeConnectionToServer(); //chiudo la connessione (socket e relativi stream)
                }
            }
        });
        t1.start();
        try {
            t1.join();//Assicura che le operazioni di t1 siano completate prima che il thread corrente proceda con il resto del codice
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        //Se la risposta da parte del server è ACTIONCOMPLETED settiamo allgood a true per segnare la buona uscita delle op, quindi viene chiusa la finestra
        if (allGood.get()) {
            resetNewEmailFields();
            clientController.loadAllFromServer();
            closeCurrentWindow(event);
        }
    }

    //dà un alert specifico in base alla risposta negativa ricevuta dal server
    private void handleServerResponse(ServerController.ServerResponse response) {
        Platform.runLater(() -> {
            Alert alert;
            switch (response) {
                case RECIPIENT_NOT_FOUND:
                    alert = new Alert(Alert.AlertType.ERROR, "Uno o più client selezionati non sono stati trovati!");
                    break;
                default:
                    alert = new Alert(Alert.AlertType.ERROR, "Errore nell'invio e-mail");
                    break;
            }
            alert.show();
        });
    }

    //Usato in doNewMailOperation (scritturanuovamail). Resetta i campi della schermata di invio mail così da non trovare i campi usati precedentemente
    private void resetNewEmailFields() {
        client.newEmail = new Email(client.getID() + 1);
        msgTextArea.textProperty().unbindBidirectional(client.newEmail.textProperty());
        destinatarioTextField.textProperty().unbindBidirectional(client.newEmail.recipientProperty());
        oggettoTextField.textProperty().unbindBidirectional(client.newEmail.subjectProperty());
    }

    //Chiude la finestra corrente, in questo caso quella per la scrittura di una nuova mail aka messageview
    private void closeCurrentWindow(javafx.event.Event event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
