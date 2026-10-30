package org.example.ishtirak.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Same role as in the course sample: one stage, scenes loaded from FXML files of this package. */
public class SceneManager {

    private static Stage stage;

    public static void setStage(Stage primaryStage) {
        stage = primaryStage;
    }

    public static Stage getStage() {
        return stage;
    }

    public static void switchScene(String fxml) throws IOException {
        Scene scene = new Scene(loader(fxml).load());
        scene.getStylesheets().add(SceneManager.class.getResource("ishtirak.css").toExternalForm());
        stage.setScene(scene);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    /** Loads a view that the main window places in its centre; returns the loader to reach the controller. */
    public static FXMLLoader loader(String fxml) {
        return new FXMLLoader(SceneManager.class.getResource(fxml));
    }

    public static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load view " + loader.getLocation(), e);
        }
    }
}
