module com.prog3.progettoprog320232024 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    //requires javafx.web;
    requires javafx.base;
    requires json.simple;

    requires org.controlsfx.controls;
 // requires com.dlsc.formsfx;
 // requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires de.jensd.fx.glyphs.fontawesome;
    //requires com.almasb.fxgl.all;
  //  requires org.w3c.dom.events.Event;

    exports com.prog3.progettoprog320232024.client.view;
    opens com.prog3.progettoprog320232024.client.view to javafx.fxml;

    exports com.prog3.progettoprog320232024.client.controller;
    opens com.prog3.progettoprog320232024.client.controller to javafx.fxml;

    exports com.prog3.progettoprog320232024.client.model;
    opens com.prog3.progettoprog320232024.client.model to javafx.fxml;

    exports com.prog3.progettoprog320232024.server.view;
    opens com.prog3.progettoprog320232024.server.view to javafx.fxml;

    exports com.prog3.progettoprog320232024.server.controller;
    opens com.prog3.progettoprog320232024.server.controller to javafx.fxml;

    exports com.prog3.progettoprog320232024.server.model;
    opens com.prog3.progettoprog320232024.server.model to javafx.fxml;
}