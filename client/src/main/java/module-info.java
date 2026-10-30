module org.example.ishtirak.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    requires org.example.ishtirak.common;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens org.example.ishtirak.client to javafx.fxml, javafx.base;
    exports org.example.ishtirak.client;
}
