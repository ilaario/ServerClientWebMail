package com.prog3.progettoprog320232024.client.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.prog3.progettoprog320232024.client.model.Client;
import com.prog3.progettoprog320232024.client.controller.ClientController;
import com.prog3.progettoprog320232024.client.controller.MessageController;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javafx.scene.image.Image;

public class ClientView extends Application {

    private static String clientEmail;
    private static final List<String> validEmails = Arrays.asList(
            "Pierluigi.boscaglia@gmail.com", "Dario.bonfiglio@gmail.com", "Michele.cascione@gmail.com"
    ); //Indirizzi email supportati



    public static void main(String[] args) {
        if (args.length > 0) {
            clientEmail = args[0];  //Legge l'indirizzo email dalla riga di comando
        }
        launch(args);
    }

    //Si occupa di creare e configurare la finestra principale dell'applicazione (Stage) in base all'email fornita.
    //Se l'email non è valida o non è fornita, l'applicazione visualizza un messaggio di errore e si chiude.
    @Override
    public void start(Stage primaryStage) throws IOException {
        if (clientEmail != null && validEmails.contains(clientEmail)) {
            // Crea il client con l'indirizzo email specificato
            Client client = new Client(clientEmail);
            createClientWindow(client, "Mail Sender - " + clientEmail);
        } else {
            System.out.println("Indirizzo email non riconosciuto o non fornito.");
            if (!validEmails.isEmpty()) {
                System.out.println("Indirizzi email disponibili:");
                for (String email : validEmails) {
                    System.out.println("- " + email);
                }
            } else {
                System.out.println("Nessun indirizzo email disponibile.");
            }
            System.out.println("Passare l'indirizzo email come parametro tramite args!");
            System.exit(0);
        }
    }

/*
 Se l'email è valida, il metodo createClientWindow viene invocato per configurare l'interfaccia utente.
 Questo include il caricamento del layout da un file FXML (definito in ClientView.fxml), l'inizializzazione del ClientController,
 e il binding delle proprietà del client al controller.
 La classe gestisce anche la creazione di una seconda finestra per i messaggi (definita in MessageView.fxml),
 che viene utilizzata per creare nuovi messaggi email. Questa finestra è gestita da un'istanza separata di MessageController.
*/
    private void createClientWindow(Client client, String title) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ClientView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        ClientController controller = fxmlLoader.getController();

        controller.setClient(client);
        controller.setStatusBinding();
        controller.bindClientProperties();

        FXMLLoader fxmlLoader1 = new FXMLLoader(getClass().getResource("MessageView.fxml"));
        Parent v = fxmlLoader1.load();
        MessageController controller1 = fxmlLoader1.getController();
        controller1.setClient(client);
        controller1.setClientController(controller);
        Scene scene1 = new Scene(v, 600, 400);
        Stage newStage = new Stage();

        newStage.setOnShown((event) -> controller1.bindMsg());
        newStage.setScene(scene1);

        newStage.setTitle("New Email");
        controller.setStage(newStage);

        Stage clientStage = new Stage();
        clientStage.setOnShown((event) -> controller.startPeriodicEmailDownloader());
        clientStage.setOnCloseRequest((windowEvent) -> controller.shutdownPeriodicEmailDownloader());
        clientStage.setTitle(title);
        clientStage.setScene(scene);

        clientStage.show();
    }
}
