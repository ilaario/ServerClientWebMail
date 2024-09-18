package com.prog3.progettoprog320232024.client.controller;

import com.prog3.progettoprog320232024.client.model.Email;
import com.prog3.progettoprog320232024.client.model.SerializableEmail;
import com.prog3.progettoprog320232024.client.model.Client;
import com.prog3.progettoprog320232024.server.model.Action;
import com.prog3.progettoprog320232024.server.controller.ServerController;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.util.Pair;


import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;



public class ClientController {

    @FXML
    private Label indentTo;
    @FXML
    private Label indentOggetto;
    @FXML
    private Label indentData;
    @FXML
    private FontAwesomeIconView inboxIcon;
    @FXML
    private Button rispondiButton;
    @FXML
    private Button rispondiTButton;
    @FXML
    private Button inoltraButton;
    @FXML
    private Button eliminaButton;
    @FXML
    private FontAwesomeIconView statusIcon;
    @FXML
    private Label accountNameLabel;
    @FXML
    private ImageView accountImageView;
    @FXML
    private Label dataLabel;
    @FXML
    private Label oggettoLabel;
    @FXML
    private Label destinatarioLabel;
    @FXML
    private Label mittenteLabel;
    @FXML
    private ListView<Email> inboxListView;
    @FXML
    private TextArea emailTextArea;
    @FXML
    private Label statusLabel;


    private Client client;
    private Socket socket; //utilizzato per stabilire una connessione client-server
    protected final ReentrantLock reentrantLock = new ReentrantLock(); //per la sincronizzazione dell'accesso a risorse condivise ofc
    ObjectOutputStream objectOutputStream; //utilizzato per inviare dati al server
    ObjectInputStream objectInputStream; //utilizzato per ricevere dati dal server
    private ScheduledExecutorService scheduledExEmailDownloader; //utilizzato per scaricare periodicamente le email dal server.
    //Task che viene eseguita a intervalli regolari di 5 secondi come implementato nel metodo startPeriodicEmailDownloader.
    private Stage newEmailStage; //è utilizzato per visualizzare una nuova finestra che permette all'utente di comporre e inviare nuove email. Controllato dal MessageController


    public Button getRispondiButton(){return rispondiButton;}
    public Button getInoltraButton(){return inoltraButton;}
    public Button getEliminaButton(){return eliminaButton;}
    public Button getRispondiTButton(){return rispondiTButton;}
    public FontAwesomeIconView getInboxIcon(){return inboxIcon;}
    public Label getDestinatarioLabel(){return destinatarioLabel;}
    public Label getOggettoLabel(){return oggettoLabel;}
    public Label getDataLabel(){return dataLabel;}
    public Label getMittenteLabel(){return mittenteLabel;}
    public Label getIndentTo(){return indentTo;}
    public Label getIndentOggetto(){return indentOggetto;}
    public Label getIndentData(){return indentData;}
    public TextArea getEmailTextArea(){return emailTextArea;}
    public void setStage(Stage newEmailStage) {
        this.newEmailStage = newEmailStage;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        if (this.client != null)
            throw new IllegalStateException("Il client può essere inizializzato solo una volta");
        this.client = client;
    }

    //Associazione dell'etichetta di stato in basso a sinistra della GUI
    // che ci consente di vedere se il Client è connesso al Server
    public Label getStatusLabel() {
        return statusLabel;
    }

    public FontAwesomeIconView getStatusIcon(){ return statusIcon; }

    public void setStatusBinding() {
        this.getStatusLabel().textProperty().bind(client.stateProperty());
        client.stateProperty().addListener((obs, oldState, newState) -> {
            Pair<String, Color> iconAndColor = getIconAndColorForState(newState); //metodo scritto qua sotto per stato della connessione client-server
            String iconName = iconAndColor.getKey();
            Color color = iconAndColor.getValue();

            this.getStatusIcon().setGlyphName(iconName);
            this.getStatusIcon().setFill(color); // Imposta il colore
        });
        //this.getStatusIcon().setIcon(client.stateProperty());
    }//binding dell'etichetta di stato

    private Pair<String, Color> getIconAndColorForState(String state) {
        switch (state) {
            case "Connected":
                return new Pair<>("CHECK_CIRCLE", Color.web("#0ceb20"));
            case "Reconnecting":
                return new Pair<>("TIMES_CIRCLE", Color.web("#eb0e0e"));
            default:
                return new Pair<>("QUESTION_CIRCLE", Color.BLACK);
        }
    }

    public Label getAccountLabel() {
        return accountNameLabel;
    }

    public ImageView getAccountImageView() {
        return accountImageView;
    }

    public void bindClientProperties() { //setto il client e la sua immagine profilo
        this.getAccountLabel().textProperty().bind(client.addressProperty());
        String mail = (client.addressProperty()).get();
        Image image = loadImageForClient(mail);
        double width = this.getAccountImageView().getFitWidth();
        double height = this.getAccountImageView().getFitHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(width, height) / 2;
        Circle clip = new Circle(centerX, centerY, radius);
        this.getAccountImageView().setClip(clip);
        this.getAccountImageView().setImage(image);

    }

    public void handleDelete(ActionEvent actionEvent) { //richiama il metodo sincronizzato deleteSelectedEmail
        try {
            deleteSelectedEmail();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleForward(ActionEvent actionEvent) { //inoltro mail
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Si prega di selezionare un'e-mail!");
                a.show();
            });
            return;
        }
        try {
            if (client.selectedEmail != null) {
                client.newEmail = new Email(client.getID() + 1);
                client.newEmail.setSender(client.addressProperty().get());
                client.newEmail.setSubject(client.selectedEmail.getSubject());
                client.newEmail.setText(client.selectedEmail.getText());
                newEmailStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void handleReply(ActionEvent actionEvent) { //Rispondi
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Si prega di seleziona un'e-mail!");
                a.show();
            });
            return;
        }
        try {
            if (client.selectedEmail != null) {
                client.newEmail = new Email(client.getID() + 1);
                client.newEmail.setSender(client.addressProperty().get());
                client.newEmail.setRecipient(client.selectedEmail.getSender());
                client.newEmail.setSubject("Re: " + client.selectedEmail.getSubject());
                newEmailStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleReplyAll(ActionEvent actionEvent) {//Rispondi a tutti
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Si prega di seleziona un'e-mail!");
                a.show();
            });
            return;
        }
        try {
            if (client.selectedEmail != null) {
                client.newEmail = new Email(client.getID() + 1);
                client.newEmail.setSender(client.addressProperty().get());
                String[] recipient = client.selectedEmail.getRecipient().split(","); //per controllare ogni mail del replyAll
                StringBuilder parameter = new StringBuilder();
                for (int i = 0; i < recipient.length; i++) {
                    String receiver = recipient[i].strip();//rimuove spazi bianchi dall'inizio e dalla fine di una stringa.
                    if (client.getAddress().equals(receiver))
                        parameter.append(client.selectedEmail.getSender());
                    else
                        parameter.append(receiver);
                    if (i != recipient.length - 1)
                        parameter.append(",");
                }
                client.newEmail.setRecipient(parameter.toString());
                client.newEmail.setSubject("Re: " + client.selectedEmail.getSubject());
                newEmailStage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void bindMailToView(Email email) { //per visualizzare la mail nella parte di destra del ClientView con i relativi dati (oggetto,mittente ...)
        try {
            Platform.runLater(() -> {

                mittenteLabel.textProperty().bind(email.senderProperty());
                destinatarioLabel.textProperty().bind(email.recipientProperty());
                oggettoLabel.textProperty().bind(email.subjectProperty());
                emailTextArea.textProperty().bind(email.textProperty());

                LocalDateTime dateTime = email.getDateTime();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                String formattedDateTime = dateTime.format(formatter);
                dataLabel.setText(formattedDateTime);

            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void handleWriteEmail() throws IOException {//Scrittura di una nuova mail dal tasto quello con il +
        try {
            client.newEmail = new Email(client.getID() + 1);
            client.newEmail.setSender(client.addressProperty().get());
            newEmailStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setAllVisible (){
        this.getInboxIcon().setVisible(false);
        this.getRispondiButton().setVisible(true);
        this.getEliminaButton().setVisible(true);
        this.getInoltraButton().setVisible(true);
        this.getRispondiTButton().setVisible(true);
        this.getDestinatarioLabel().setVisible(true);
        this.getMittenteLabel().setVisible(true);
        this.getOggettoLabel().setVisible(true);
        this.getDataLabel().setVisible(true);
        this.getEmailTextArea().setVisible(true);
        this.getIndentTo().setVisible(true);
        this.getIndentData().setVisible(true);
        this.getIndentOggetto().setVisible(true);
    }

    public void setAllInvisible (){
        this.getInboxIcon().setVisible(true);
        this.getRispondiButton().setVisible(false);
        this.getEliminaButton().setVisible(false);
        this.getInoltraButton().setVisible(false);
        this.getRispondiTButton().setVisible(false);
        this.getMittenteLabel().setVisible(false);
        this.getDestinatarioLabel().setVisible(false);
        this.getOggettoLabel().setVisible(false);
        this.getDataLabel().setVisible(false);
        this.getEmailTextArea().setVisible(false);
        this.getIndentData().setVisible(false);
        this.getIndentTo().setVisible(false);
        this.getIndentOggetto().setVisible(false);
    }


    /*
    Metodo richiamato quando si clicca sulla ListView a destra della schermata del controller. Se la lista è vuota rendo i bottoni invisibili e ritorno.
    ALTRIMENTI. Controllo se la mail selezionata non è stata letta. In tal caso inizio un nuovo thread che permette di comunicare col server e visualizzare
    la mail.
     */
    @FXML
    private void onInboxListViewClick(MouseEvent event) {

        if (inboxListView.getSelectionModel().getSelectedItems().size() <= 0) {  //controlla se è stata effettauata una selezione
            setAllInvisible();
            return;
        }

        Email email = inboxListView.getSelectionModel().getSelectedItems().get(0);
        client.selectedEmail = email;
        bindMailToView(email);
        setAllVisible();

        if (!email.isRead()) { //Verifica se l'email selezionata non è stata letta
            new Thread(() -> {
                try {
                    getNewSocket();
                    setSocketSuccess();
                    sendActionToServer(new Action(client, null, Action.Tasks.VISUALIZZA_EMAIL));//Chiedo al server di leggere la mail
                    sendEmailToServer(new SerializableEmail(client.selectedEmail));
                    ServerController.ServerResponse response = waitForResponse();
                    if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                        email.setRead(true); //se la richiesta è andata a buon fine setto l'attributo di mail letta a true
                    }
                } catch (IOException exception) {
                    setSocketFailure();
                } catch (ClassNotFoundException e) {
                    System.out.println("Impossibile leggere");
                } finally {
                    closeConnectionToServer(); //chiudo la connessione col server
                }
            }).start();

        }

    }

    /*
    Separa l'email selezionata dalla vista con unbind per crearne una vuota associandola alla lista del client
     */
    private void resetSelectedEmail() {
        client.selectedEmail = new Email(client.getID() + 1);
        Platform.runLater(() -> {
            mittenteLabel.textProperty().unbind();
            destinatarioLabel.textProperty().unbind();
            oggettoLabel.textProperty().unbind();
            emailTextArea.textProperty().unbind();
            dataLabel.textProperty().unbind();
        });
    }
/*
    private void resetFromButtons() { //resetta i testi della schermata a destra contenente oggetto destinatario e data

        Platform.runLater(() -> {
            mittenteLabel.textProperty().unbind();
            mittenteLabel.setText("From");
            destinatarioLabel.textProperty().unbind();
            destinatarioLabel.setText("Destinatario");
            oggettoLabel.textProperty().unbind();
            oggettoLabel.setText("Oggetto");
            emailTextArea.textProperty().unbind();
            emailTextArea.setText("");
            dataLabel.textProperty().unbind();
            dataLabel.setText("Data");
        });
    }
*/
    private Image loadImageForClient(String nomeClient) { //immagini per il profilo client
        String imagePath = "/com/prog3/progettoprog320232024/client/view/img/" + nomeClient + ".jpeg"; // Assicurati che il percorso sia corretto
        InputStream is = getClass().getResourceAsStream(imagePath);
        if (is == null) {
            // Gestisci il caso in cui la risorsa non è trovata
            return null; // o un'immagine predefinita
        }
        return new Image(is);
    }

        private void showNextEmail(int indexOfDeletedEmail) { //viene utilizzato dal metodo per l'eliminazione di una mail. Permette di aggiornare la vista dopo la cancellazione
        if (inboxListView.getItems().size() == 1) { //Caso in cui eliminiamo l'ultima mail dalla lista (non ce ne sono più quindi si disabilitano i bottoni)
            setAllInvisible();                          //RICORDA: Aggiorna la view grande a destra
            return;
        }

        if (indexOfDeletedEmail == 0) { //nel caso in cui si elimini la prima mail della lista semplicemente leghiamo la successiva alla vista
            inboxListView.getSelectionModel().select(1);
            client.selectedEmail = inboxListView.getItems().get(1);
            bindMailToView(client.selectedEmail);
        }

        if (indexOfDeletedEmail > 0 && indexOfDeletedEmail - 1 < inboxListView.getItems().size()) { //funziona allo stesso modo del caso ==0 ma il successivo si lega
            inboxListView.getSelectionModel().select(indexOfDeletedEmail - 1);                   //usando indexofdeletedemail -1
            client.selectedEmail = inboxListView.getItems().get(indexOfDeletedEmail - 1);
            bindMailToView(client.selectedEmail);

        }
    }



    private void deleteSelectedEmail() { //Clicco sul pulsante elimina (il metodo crea un thread con robe del socket)
        if (client.selectedEmail == null || client.selectedEmail.getSender().equals("")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Si prega di selezionare un'e-mail");
                a.show();
            });
            return;
        }
        new Thread(() -> { //Creo un thread per la connessione col server. Invio l'azione delete email
            synchronized (reentrantLock) {
                ServerController.ServerResponse response = null;
                if (client.selectedEmail != null) {
                    try {
                        getNewSocket();
                        setSocketSuccess();
                        sendActionToServer(new Action(client, null, Action.Tasks.ELIMINA_EMAIL)); //ACTION
                        SerializableEmail emailToBeDeleted = new SerializableEmail(client.selectedEmail); //Invio la mail da eliminare
                        sendEmailToServer(emailToBeDeleted);
                        response = waitForResponse();
                    } catch (IOException socketException) {
                        setSocketFailure();
                    } catch (ClassNotFoundException e) {
                        System.out.println("Errore in lettura (ClassNotFound)");
                    } finally {
                        closeConnectionToServer(); //Posso quindi chiudere la connessione col server
                    }
                    if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                        int indexOfEmail = client.whereIs(client.selectedEmail).indexOf(client.selectedEmail);
                        loadAllFromServer();
                        resetSelectedEmail();
                        showNextEmail(indexOfEmail); //Aggiorno la view a destra
                    } else {
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.ERROR, "Errore durante l'eliminazione di un'e-mail");
                            a.show();
                        });
                    }
                }
            }
        }).start();
    }

    public void loadAllFromServer() {//Carica nuove mail dal server e le aggiunge al Client.
        //Il thread effettua la connessione. invia l'action OTTIENIEMAIL. Se viene restituita dal server un ACTIONCOMPLETED allora tutto ok. Creo un arraylist che contiene
        //le mail inviate dal server: controllo ogni stato mail e se esiste già; in caso non esistesse viene aggiunta.
        //Nell'ultima parte si controlla se ci sono mail che non sono sul server ma sono ancora sul client, così da eliminare.
        new Thread(() -> {
            synchronized (reentrantLock) {
                try {
                    getNewSocket(); //socket per stabilire connessione col server
                    setSocketSuccess();
                } catch (IOException e) {
                    setSocketFailure();
                    return;
                }
                Action request = new Action(client, null, Action.Tasks.OTTIENI_EMAIL); //Mando l'ACTION per ottenere le mail dal server
                sendActionToServer(request);//mando la richiesta al server
                ServerController.ServerResponse response = null;
                try {
                    response = waitForResponse();
                } catch (Exception ex) {
                    System.out.println("Nessuna risposta");
                }
                if (response == ServerController.ServerResponse.ACTION_COMPLETED) {
                    ArrayList<Email> emailsFromServer = new ArrayList<>(); //Se l'azione è COMPLETED creo l'arraylist
                    try {
                        SerializableEmail serializableEmail;
                        while ((serializableEmail = (SerializableEmail) objectInputStream.readObject()) != null) {
                            Email serverEmail = new Email(serializableEmail);
                            emailsFromServer.add(serverEmail);
                        }
                    } catch (java.io.EOFException EOFException) {
                        System.out.println("Tutte le mail sono state recapitate");
                    } catch (ClassNotFoundException e) {
                        System.out.println("Errore in lettura (ClassNotFound)");
                    } catch (IOException ioException) {
                        System.out.println("Errore durante la lettura delle email in loadAllFromServer");
                    } finally {
                        closeConnectionToServer(); //Chiudo la connessione del socket
                    }

                    //Itero per le mail ricevute dal server
                    AtomicInteger newMails = new AtomicInteger(); //utilizzata per salvare le mail ricevute dal server non presenti in inbox o sent. Atomic usato per concorrenza
                    for (Email serverEmail : emailsFromServer) {
                        switch (serverEmail.getState()) {
                            case RECEIVED -> {
                                if (!client.hasSameIDInCollection(client.inboxProperty(), serverEmail)) {
                                    if (!serverEmail.isRead()) { //se la mail non è stata letta
                                        newMails.getAndIncrement(); //contatore usato per tenere traccia delle nuove mail ricevute, usato dopo per l'output a video della notifica
                                    }
                                    Platform.runLater(() -> client.inboxProperty().add(serverEmail));
                                }
                            }
                            case SENT -> {
                                if (!client.hasSameIDInCollection(client.sentProperty(), serverEmail))
                                    Platform.runLater(() -> client.sentProperty().add(serverEmail));
                            }
                        }
                    }
                    //Ordino le mail per data nel caso di inbox e sent
                    Platform.runLater(() -> {
                        client.inboxProperty().sort((email1, email2) ->
                                email2.getDateTime().compareTo(email1.getDateTime()));
                        client.sentProperty().sort((email1, email2) ->
                                email2.getDateTime().compareTo(email1.getDateTime()));
                    });

                    //Verifica se ci sono mail nel client non presenti sul server
                    Platform.runLater(() -> {
                        client.inboxProperty().removeIf(inboxEmail -> !containsID(emailsFromServer, inboxEmail, Email.EmailState.RECEIVED));
                        client.sentProperty().removeIf(sentEmail -> !containsID(emailsFromServer, sentEmail, Email.EmailState.SENT));
                    });

                    if (newMails.get() > 0) { //Notifica di nuove mail ricevute
                        Platform.runLater(() -> {
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "Hai ricevuto" + newMails + " e-mail!");
                            a.show();
                        });
                    }
                } else //ACTION non andata a buon fine
                {
                    System.out.println("il caricamento ha causato un problema");
                }
            }
        }).start();
    }

    //Controlla se un ID di email è già presente nella lista EmailsFromServer dello EmailState passato come parametro.
    private boolean containsID(ArrayList<Email> emailsFromServer, Email inboxEmail, Email.EmailState emailState) {
        for (Email email : emailsFromServer) {
            if (email.getState() == emailState && inboxEmail.getID() == email.getID()) return true;
        }
        return false;
    }

    //Creo una connessione socket e inizializzo gli input e output stream per lo scambio di dati tra server e client (input server-client, output client-server)
    protected void getNewSocket() throws IOException {
        socket = new Socket(InetAddress.getLocalHost(), 6969);
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    //Avvisa che la connessione è avvenuta con successo. Aggiorna lo state del client a connected e colora di verde la scritta dello status (connected)
     protected void setSocketSuccess() {
        Platform.runLater(() -> {
            client.stateProperty().setValue("Connected");
            this.getStatusLabel().setTextFill(Color.web("Green"));
        });
    }

    protected void setSocketFailure() {//come socket success ma in caso contrario. Colora di rosso la scritta "avviamento connessione"
        Platform.runLater(() -> {
            client.stateProperty().setValue("Reconnecting");
            this.getStatusLabel().setTextFill(Color.web("Red"));
        });
    }

    protected void closeConnectionToServer() { //Arresto la connessione chiudendo il socket e i relativi stream I/O
        if (socket != null && objectInputStream != null && objectOutputStream != null) {
            try {
                socket.close();
                objectOutputStream.close();
                objectInputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    //Task periodico eseguito ogni 5 secondi che richiama loadAllFromServer, per caricare tutte le mail dal server
    public void startPeriodicEmailDownloader() {
        if (scheduledExEmailDownloader != null) return;
        scheduledExEmailDownloader = Executors.newScheduledThreadPool(1);
        scheduledExEmailDownloader.scheduleAtFixedRate(new PeriodicEmailDownloader(), 0, 2, TimeUnit.SECONDS);
        //PeriodicEmailDownloader richiama a sua volta loadAllFromServer
    }

    //Interrompo l'esecuzione del task periodico di 5 secondi
    public void shutdownPeriodicEmailDownloader() {
        if (scheduledExEmailDownloader != null)
            scheduledExEmailDownloader.shutdown();
    }

    //Eseguito quando si clicca sulla casella "Ricevuti" che stampa appunto le mail ricevute
    public void emailsReceived(ActionEvent actionEvent) {
        client.selectedEmail = new Email(client.getID() + 1);
        if (client.inboxProperty().size() > 0) client.selectedEmail = client.inboxProperty().get(0);
        inboxListView.itemsProperty().bind(client.inboxProperty());
        setAllInvisible();
    }

    //Invocato quando si clicca il tasto per visualizzare le mail inviate
    public void emailsSent(ActionEvent actionEvent) {
        client.selectedEmail = new Email(client.getID() + 1);
        if (client.sentProperty().size() > 0) client.selectedEmail = client.sentProperty().get(0);
        inboxListView.itemsProperty().bind(client.sentProperty());
        setAllInvisible();
    }

    //Utilizzato da StartPeriodicEmailDownloader per eseguire l'aggiornamento delle mail (task 5 sec)
    class PeriodicEmailDownloader implements Runnable {
        public PeriodicEmailDownloader() {
        }

        @Override
        public void run() {
            loadAllFromServer();
        }
    }

    //Uso l'objectOutpuStream per inviare l'ACTION al server
    public void sendActionToServer(Action action) {
        if (objectOutputStream == null) return;
        try {
            objectOutputStream.writeObject(action);
            objectOutputStream.flush();
        } catch (Exception e) {
            System.out.println("Socket chiuso");
        }
    }

    //Invia una mail serializzata al server tramite l'objOutStream
    public void sendEmailToServer(SerializableEmail serializableEmail) {
        if (objectOutputStream == null) return;
        try {
            objectOutputStream.writeObject(serializableEmail);
            objectOutputStream.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //Attende la risposta del server e restituisce la risposta in questione grazie all'objectInputStream del socket
    public ServerController.ServerResponse waitForResponse() throws IOException, ClassNotFoundException {
        ServerController.ServerResponse response;
        response = (ServerController.ServerResponse) objectInputStream.readObject();
        return response;
    }
}
