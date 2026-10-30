package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/** V1 Login. */
public class LoginController {
    private static String pendingMessage;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label usernameError;

    @FXML
    private Label passwordError;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    private FormErrors errors;

    public static void setPendingMessage(String message) {
        pendingMessage = message;
    }

    @FXML
    private void initialize() {
        errors = new FormErrors(messageLabel)
                .field("username", usernameError)
                .field("password", passwordError);
        if (pendingMessage != null) {
            messageLabel.setText(pendingMessage);
            pendingMessage = null;
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        Ui.run(() -> ApiClient.get().login(username, password),
                login -> {
                    SessionContext.start(login);
                    try {
                        SceneManager.switchScene("main-view.fxml");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                },
                errors, loginButton, usernameField, passwordField);
    }

    @FXML
    private void handleClear() {
        usernameField.clear();
        passwordField.clear();
        errors.clear();
    }

    // called by Ui for 401/403 on login
    static boolean isLoginCall(ApiException e) {
        return "INVALID_CREDENTIALS".equals(e.getCode()) || "ACCOUNT_DISABLED".equals(e.getCode());
    }
}
