package org.example.ishtirak.client;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class IshtirakApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        SceneManager.setStage(stage);
        SceneManager.switchScene("login-view.fxml");
        stage.setTitle("Ishtirak");
        stage.show();
    }
}
