package com.prog3.progettoprog320232024.server.view;

import com.prog3.progettoprog320232024.server.controller.ServerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ServerView extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    //Starta la ServerView

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ServerView.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        ServerController controller = fxmlLoader.getController(); //Setto il controller della serverview
        stage.setOnCloseRequest((event) -> controller.stopServer());
        stage.setTitle("Server");
        stage.setScene(scene);
        stage.show();
    }
}
