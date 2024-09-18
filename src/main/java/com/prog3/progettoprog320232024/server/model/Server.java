package com.prog3.progettoprog320232024.server.model;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import com.prog3.progettoprog320232024.client.model.Client;
import com.prog3.progettoprog320232024.client.model.Email;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Server {
    ArrayList<Action> actions;
    ArrayList<Client> clients;
    SimpleStringProperty log; //attività e eventi che avvengono durante l'esecuzione del server, si aggiorna grazie a updateLog
    private boolean running = true;

    //public ArrayList<Action> getActions() {return actions;}
    private final String[] VisualName = {"ricevute", "inviate"};

    public ArrayList<Client> getClients() {
        return clients;
    }
    public SimpleStringProperty logProperty() {
        return log;
    }

    public Server() {
        clients = new ArrayList<>();
        actions = new ArrayList<>();
        log = new SimpleStringProperty("");
    }

    //Aggiungo un nuovo client al server
    public void addClient(Client c) {
        if (c != null)
            clients.add(c);
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    //Controlla la stringa e ritorna, se uguali, uno dei due stati della mail (received o sent)
    public static Email.EmailState stringToEmailState(String s) {
        if (s.compareTo("ricevute") == 0) {
            return Email.EmailState.RECEIVED;
        }
        if (s.compareTo("inviate") == 0) {
            return Email.EmailState.SENT;
        } else return null;
    }

    //Aggiorna il log con la stringa passata come parametro
    public void updateLog(String s) {
        Platform.runLater(() -> log.setValue(log.getValue() + s));
    }

    //Aggiorna la lista delle ACTION e la aggiunge anche al log del server
    public synchronized void add(Action Request) {
        actions.add(Request);
        updateLog(Request.toString() + '\n');
    }

    public boolean isRunning() {
        return running;
    }

    //Legge il JSON del client, contenente tutte le informazioni del client
    public void readFromJSONFile() {
        clients = new ArrayList<>();
        System.out.println("Lettura Client dati...");
        JSONParser jsonParser = new JSONParser();//Usato per la lettura del json
        try (FileReader reader = new FileReader("src/main/resources/com/prog3/progettoprog320232024/server/data/clients.json")) {
            Object obj = jsonParser.parse(reader);//Analizza il file JSON e restituisce un Object rappresentante la struttura gerarchica del file
            JSONArray clientsList = (JSONArray) obj;
            for (Object o : clientsList) { //Itero su ogni cliente dell'array JSON
                JSONObject jsonClient = (JSONObject) o; //Salvo il client in JSONClient
                String clientString = jsonClient.toString();//Salvo il client in una stringa
                String[] x = clientString.split("\"");
                Client client = new Client(x[1]);
                clients.add(client);//Dopo aver ottenuto il client lo salvo nella lista dei clients
                parseClientObject((JSONObject) o, client); //Implementato sotto, estrae le mail del client e le salva
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Utilizzato da readFromJsonFile scritto sopra. Prende tutte le informazioni di un client e aggiunge la sua mail letti dal file JSION.
    //I due parametri sono diversi perchè il primo è in JSON, il secondo è il client normale
    private void parseClientObject(JSONObject clientJson, Client clientObject) {

        // Ottiene Lista dati dal JSON corrispondente all'indirizzo del client
        JSONArray List = (JSONArray) clientJson.get(clientObject.getAddress());
        for (int i = 0; i < List.size(); i++) {
            // Ottiene un oggetto JSON rappresentante una sezione
            JSONObject sectionObj = (JSONObject) List.get(i);
            //Ottiene l'array di email dalla sezione corrispondente
            JSONArray emailList = (JSONArray) sectionObj.get(VisualName[i]);

            for (Object o : emailList) {
                // Ottiene un oggetto JSON rappresentante una email
                JSONObject emailObj = (JSONObject) o;

                // Estrae le informazioni dalla email
                String sender = (String) emailObj.get("sender");
                String recipient = (String) emailObj.get("recipient");
                String subject = (String) emailObj.get("subject");
                String text = (String) emailObj.get("text");
                String dateTime = (String) emailObj.get("dateTime");
                long ID = (Long) emailObj.get("ID");

                boolean read = (boolean) emailObj.get("read");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                Email.EmailState emailState = stringToEmailState(VisualName[i]);

                // Crea un oggetto Email con le informazioni estratte

                Email email = new Email(sender, recipient, subject, text, emailState, LocalDateTime.parse(dateTime, formatter), ID);

                email.setRead(read);
                // Aggiunge la email alla lista appropriata nel client
                switch (emailState) {
                    case RECEIVED -> clientObject.inboxProperty().add(email);
                    case SENT -> clientObject.sentProperty().add(email);
                }
            }
        }
    }

    //Salvo le informazioni del client nel JSON
    public synchronized void saveClientsToJSON() {
        System.out.println("Salvataggio dati server...");
        JSONArray array = new JSONArray();

        // Itera attraverso tutti i client
        for (Client client : clients) {

            JSONObject clients = new JSONObject();// Rappresenta i dati del client
            JSONArray arrayOfsection = new JSONArray();// Contiene le sezioni (inbox, sent) del client

            JSONObject section; // Rappresenta una sezione (inbox o sent) del client
            JSONArray arrayOfEmail; // Contiene le email in una sezione
            JSONObject emailDetails;// Rappresenta i dettagli di una email

            // SimpleListProperty delle email in arrivo e inviate del client
            SimpleListProperty<Email>[] lists = new SimpleListProperty[]{client.inboxProperty(), client.sentProperty()};
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            int i = 0;

            for (SimpleListProperty<Email> list : lists) {

                arrayOfEmail = new JSONArray();

                for (Email email : list) {
                    emailDetails = new JSONObject();
                    emailDetails.put("sender", email.getSender());
                    emailDetails.put("recipient", email.getRecipient());
                    emailDetails.put("subject", email.getSubject());
                    emailDetails.put("text", email.getText());
                    emailDetails.put("dateTime", email.getDateTime().format(formatter));
                    emailDetails.put("ID", email.getID());
                    emailDetails.put("read", email.isRead());
                    arrayOfEmail.add(emailDetails);
                }
                section = new JSONObject();
                section.put(VisualName[i], arrayOfEmail);
                arrayOfsection.add(section);
                clients.put(client.getAddress(), arrayOfsection);
                i++;
            }

            array.add(clients);

            try {//Inserisco l'array rappresentate il client nel json
                File file = new File("src/main/resources/com/prog3/progettoprog320232024/server/data/clients.json");
                PrintWriter out = new PrintWriter(file);
                try {
                    out.write(array.toJSONString());
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}


