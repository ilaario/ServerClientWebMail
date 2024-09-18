package com.prog3.progettoprog320232024.server.controller;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import com.prog3.progettoprog320232024.server.model.Server;
import com.prog3.progettoprog320232024.server.model.Action;
import com.prog3.progettoprog320232024.client.model.Client;
import com.prog3.progettoprog320232024.client.model.Email;
import com.prog3.progettoprog320232024.client.model.SerializableEmail;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerController {

    public enum ServerResponse {
        ACTION_COMPLETED,//Azione completata con successo
        RECIPIENT_NOT_FOUND,//Uno o più client selezionati non sono stati trovati
        UNKNOWN_ERROR,//Errore trovato con eccezione
        EMAIL_NOT_FOUND,//Email (esempio in una lista) non trovata
        CLIENT_NOT_FOUND,//Quando il client specificato è NULL
    }

    private Server server;
    private ServerSocket serverSocket;
    private ExecutorService executorService; //usato per newFixedThreadPool(10). Pool di thread utilizzati per catturare ed eseguire la connessione della richiesta del client
    private ScheduledExecutorService scheduledExecutorService; //Usato per l'aggiornamento dei dati del client nel JSON ogni 30 sec

    @FXML
    public Button startButton;
    @FXML
    public Button stopButton;
    @FXML
    public TextArea logTextArea;


    public void initialize() {
        if (this.server != null)
            throw new IllegalStateException("Server già avviato!");
        this.server = new Server();
        startButton.setDisable(true);
        logTextArea.textProperty().bind(server.logProperty());
        startServer();
    }


     //Funzioni per abilitare/disabilitare bottoni

    @FXML
    private void onStartButtonClick() {
        server.setRunning(true);
        startButton.setDisable(true);
        stopButton.setDisable(false);
        startServer();
    }

    @FXML
    private void onStopButtonClick() {
        server.setRunning(false);
        startButton.setDisable(false);
        stopButton.setDisable(true);
        stopServer();
    }

    //Viene arrestato il server spegnendo i pool usati per la connessione e per l'aggiornamento del JSON (executor e scheduledexecutor)
    //Inoltre vengono salvati i dati dei client nel JSON
    public void stopServer() {
        server.setRunning(false);
        executorService.shutdown();
        scheduledExecutorService.shutdown();
        server.saveClientsToJSON();
        try {
            serverSocket.close();
        } catch (IOException ioException) {
            System.out.println(">> | CHIUSURA !!! | ");
        }
        server.updateLog(">> | CHIUSURA !!! | " + '\n');
    }

    /**
    Ritorna un Client cercato tramite mail
     */
    private Client findClientByAddress(String address) {
        Client sender = null;
        ArrayList<Client> clients = server.getClients();
        int i = 0;
        while ((sender == null) && i < clients.size()) {
            if (clients.get(i).getAddress().equals(address)) sender = clients.get(i);
            i++;
        }
        return sender;
    }


    private void startServer() {
        try {
            serverSocket = new ServerSocket(6969);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        server.updateLog(">> | AVVIO !!! |" + '\n');
        server.readFromJSONFile();
        executorService = Executors.newFixedThreadPool(100);//Threadpool usata per eseguire la connessione con i client
        scheduledExecutorService = Executors.newScheduledThreadPool(1); //pool pianificato usato per salvare i dati nel JSON ogni 30 sec
        scheduledExecutorService.scheduleAtFixedRate(new SaveAllTask(), 30, 30, TimeUnit.SECONDS); //Salvataggio nel file JSON

        //Per permettere al client di usare la vista viene gestita la fase di connessione tramite un thread
        new Thread(() -> {
            while (server.isRunning()) {
                try {
                    Socket incomingRequestSocket = serverSocket.accept();
                    ServerTask st = new ServerTask(incomingRequestSocket);
                    executorService.execute(st);
                } catch (SocketException socketException) {
                    System.out.println("Chiusura del Socket");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    //Pulisce il log del server
    public void onClearButtonClicked(ActionEvent actionEvent) {
        server.logProperty().setValue("");
    }

    /**Classe che rappresenta l'attività che eseguirà il server inviata dal client tramite l'ObjectOutputStream.
    * Invierà una risposta al client
     */
    class ServerTask implements Runnable {
        Socket socketS;
        ObjectOutputStream objectOutputStream; //stream del socket da client a server
        ObjectInputStream objectInputStream; //da server a client
        boolean allInitialized = false; //indica se la servertask è stata inizializzata correttamente ed è pronta per lo scambio dati

        public ServerTask(Socket socketS) {
            this.socketS = socketS;
            try {
                objectOutputStream = new ObjectOutputStream(socketS.getOutputStream());
                objectInputStream = new ObjectInputStream(socketS.getInputStream());
                allInitialized = true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() { //Gestisce le varie richieste che sono state ricevute dal client
            if (!allInitialized) return;
            try {
                Action actionRequest = (Action) objectInputStream.readObject(); //Leggo la richiesta e gestisco i vari casi
                ServerResponse response;
                if (actionRequest.getTask() == Action.Tasks.INVIA_EMAIL) {
                    response = addEmailToReceiversInbox(actionRequest, objectInputStream);
                    sendResponse(response);//Invia la risposta
                    server.add(actionRequest);//Aggiunge la richiesta mandata al server alla lista delle action e aggiorna il log
                    server.saveClientsToJSON();//Salva i client e i relativi dati nel JSON
                } else if (actionRequest.getTask() == Action.Tasks.ELIMINA_EMAIL) {//per ogni ACTION si fanno le stesse cose di prima
                    response = deleteEmail(actionRequest, objectInputStream);
                    sendResponse(response);
                    server.add(actionRequest);
                    server.saveClientsToJSON();
                } else if (actionRequest.getTask() == Action.Tasks.VISUALIZZA_EMAIL) {
                    response = setEmailAsRead(actionRequest, objectInputStream);
                    sendResponse(response);
                    server.add(actionRequest);
                    server.saveClientsToJSON();
                } else if (actionRequest.getTask() == Action.Tasks.OTTIENI_EMAIL) {
                    //L'unico metodo nullo perché invia la risposta prima di inviare tutte le email
                    sendAllEmails(actionRequest);
                    server.add(actionRequest);
                }
            } catch (Exception ex) {
                synchronized (logTextArea) {
                    server.updateLog(" Errore nell'elaborazione della richiesta " + '\n');
                }
                ex.printStackTrace();
            } finally {
                try {
                    objectOutputStream.close(); //chiudo gli IO stream
                    objectInputStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        /**
         Invia la risposta al client tramite il socketOutputStream
         */
        private void sendResponse(ServerResponse response) {
            try {
                objectOutputStream.writeObject(response);
                objectOutputStream.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         Aggiunge la mail alla casella Inbox di uno più client destinatari e aggiorna la casella Sent del client mittente
         */
        ServerResponse addEmailToReceiversInbox(Action actionRequest, ObjectInputStream inStream) {//Instream = mail serializzabile, actionRequest = azione inviata dal client
            try {
                SerializableEmail serializableEmail = (SerializableEmail) inStream.readObject();
                Email sentEmail = new Email(serializableEmail);
                sentEmail.setRecipient(sentEmail.getRecipient().replaceAll(" ", "")); //setta il destinatario

                //client mittente
                Client sender = findClientByAddress(actionRequest.getSender());

                sentEmail.setState(Email.EmailState.SENT);
                String[] recipientsTmp = actionRequest.getRecipient().split(","); //splitta i vari destinatari
                ArrayList<Client> recipients = new ArrayList<>();

                //controlla se ci sono ricevitori null
                for (int i = 0; i < recipientsTmp.length; i++) {
                    Client client = findClientByAddress(recipientsTmp[i].strip());
                    if (recipientsTmp[i] == null || client == null) {
                        actionRequest.setSuccessfully(false);
                        return ServerResponse.RECIPIENT_NOT_FOUND; //ritorna la risposta di destinatari nulli
                    }
                    recipients.add(client);
                    recipientsTmp[i] = recipientsTmp[i].strip(); //aggiungo e splitto l'array dei destinatari della mail
                }

                for (int j = 0; j < recipientsTmp.length; j++) {
                    Client receiver = recipients.get(j);

                    Email inboxEmail = sentEmail.clone();
                    inboxEmail.setState(Email.EmailState.RECEIVED);
                    inboxEmail.setRead(false); //la mail non è ancora stata letta

                    inboxEmail.setID(receiver.getID() + 1);
                    receiver.inboxProperty().add(inboxEmail); //aggiungiamo la mail nella inboc del destinatario
                }
                sentEmail.setID(sender.getID() + 1);
                sender.sentProperty().add(sentEmail); //Aggiorno la lista delle mail INVIATE dal mittente
                actionRequest.setSuccessfully(true);
                return ServerResponse.ACTION_COMPLETED; //Tutto eseguito correttamente
            } catch (Exception ex) {
                ex.printStackTrace();
                actionRequest.setSuccessfully(false);
                return ServerResponse.UNKNOWN_ERROR;
            }
        }

        /**
         Elimina la mail specificata. Cerca il client che ha richiesto l'eliminazione e, dopo aver trovato la mail, la elimina dalla lista
         */
        ServerResponse deleteEmail(Action actionRequest, ObjectInputStream inStream) {//Action= azione inviata, inStream= email serializzabile
            try {
                SerializableEmail serializableEmail = (SerializableEmail) inStream.readObject();
                Email emailToBeDeleted = new Email(serializableEmail);

                //Cerco il client che ha eseguito la richiesta di eliminazione
                Client sender = findClientByAddress(actionRequest.getSender());
                if (sender == null) {
                    actionRequest.setSuccessfully(false);
                    return ServerResponse.CLIENT_NOT_FOUND; //sender nullo
                }
                SimpleListProperty<Email> list = sender.whereIs(emailToBeDeleted); //controlla se la mail si trova nelle Inbox o nelle Inviate (Sent)
                if (list == null) {
                    actionRequest.setSuccessfully(false);
                    return ServerResponse.EMAIL_NOT_FOUND;
                } else {
                    list.remove(emailToBeDeleted);//rimozione della mail trovata
                    actionRequest.setSuccessfully(true);
                    return ServerResponse.ACTION_COMPLETED; //Azione completata
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                actionRequest.setSuccessfully(false);
                return ServerResponse.UNKNOWN_ERROR;
            }

        }

        /**
        Eseguita quando si riceve l'ACTION OTTIENI_EMAIL. Aggiunge in una lista le mail Inviate e Ricevute e le spedisce tramite il socket al client
         */
        void sendAllEmails(Action actionRequest) {
            try {
                Client requestClient = findClientByAddress(actionRequest.getSender());

                if (requestClient == null) {
                    server.addClient(new Client(actionRequest.getSender()));
                    server.saveClientsToJSON();
                    sendResponse(ServerResponse.CLIENT_NOT_FOUND); //CLIENT NULL
                    actionRequest.setSuccessfully(false);
                    return;
                }
                // Salvo tutte le mail Inbox e Sent di un Client
                SimpleListProperty<Email> allEmails = new SimpleListProperty<>(FXCollections.observableArrayList());
                allEmails.addAll(requestClient.inboxProperty());
                allEmails.addAll(requestClient.sentProperty());

                sendResponse(ServerResponse.ACTION_COMPLETED);
                actionRequest.setSuccessfully(true);

                //Itero per tutte le email e le invio tramite il socket
                for (Email email : allEmails) {
                    SerializableEmail serializableEmail = new SerializableEmail(email);
                    objectOutputStream.writeObject(serializableEmail);
                    objectOutputStream.flush();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     Eseguita quando viene eseguita l'action VISUALIZZA_EMAIL. Controlla solo le mail di Inbox e setta l'attributo mail letta a true
     */
    private ServerResponse setEmailAsRead(Action actionRequest, ObjectInputStream objectInputStream) {
        try {////Action= azione inviata dal client.OIS= per leggere la mail serializzabile
            SerializableEmail serializableEmail = (SerializableEmail) objectInputStream.readObject();
            long id = serializableEmail.getID();

            Client sender = findClientByAddress(actionRequest.getSender());
            if (sender == null) {
                actionRequest.setSuccessfully(false);
                return ServerResponse.CLIENT_NOT_FOUND;//Client non trovato
            }

            //Controlliamo solo la posta in arrivo poiché un'e-mail non letta può essere solo lì
            Email email = sender.findEmailById(sender.inboxProperty(), id);
            if (email == null) {
                actionRequest.setSuccessfully(false);
                return ServerResponse.EMAIL_NOT_FOUND;
            }

            email.setRead(true);
            server.saveClientsToJSON();
            actionRequest.setSuccessfully(true);
            return ServerResponse.ACTION_COMPLETED;
        } catch (Exception ex) {
            ex.printStackTrace();
            actionRequest.setSuccessfully(false);
            return ServerResponse.UNKNOWN_ERROR;
        }
    }

    /**
     Usato dall'istruzione che aggiorna i dati ogni 30 sec. Quando usata come parametro esegue run, che salva i dati del client nel JSON
     */
    class SaveAllTask implements Runnable {
        public SaveAllTask() {
        }

        @Override
        public void run() {
            server.saveClientsToJSON();
        }
    }

}
