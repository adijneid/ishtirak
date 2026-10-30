package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Main window: side menu on the left, the selected view swapped into the centre of the BorderPane. */
public class MainController {
    private static MainController current;

    @FXML
    private VBox menu;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button subscribersButton;
    @FXML
    private Button readingsButton;
    @FXML
    private Button tariffsButton;
    @FXML
    private Button billingButton;
    @FXML
    private Button paymentsButton;
    @FXML
    private Button arrearsButton;
    @FXML
    private Button zonesButton;
    @FXML
    private Label userLabel;
    @FXML
    private StackPane content;
    @FXML
    private HBox connectionBanner;
    @FXML
    private ProgressIndicator busyIndicator;

    private int running;
    private Runnable retryAction;

    public static MainController current() {
        return current;
    }

    static void forget() {
        current = null;
    }

    @FXML
    private void initialize() {
        current = this;
        userLabel.setText(SessionContext.getFullName() + "\n" + (SessionContext.isAdmin() ? "Administrator" : "Collector"));
        // menu items the role cannot use are hidden (proposal, section 4)
        if (!SessionContext.isAdmin()) {
            for (Button adminOnly : List.of(dashboardButton, tariffsButton, billingButton, arrearsButton, zonesButton)) {
                adminOnly.setVisible(false);
                adminOnly.setManaged(false);
            }
        }
        hideConnectionBanner();
        busyIndicator.setVisible(false);
        if (SessionContext.isAdmin()) {
            handleDashboard();
        } else {
            handleReadings();
        }
    }

    public <C> C show(String fxml, Button menuButton, Consumer<C> init) {
        FXMLLoader loader = SceneManager.loader(fxml);
        Parent view = SceneManager.load(loader);
        C controller = loader.getController();
        for (Node table : view.lookupAll(".table-view")) {
            ((TableView<?>) table).setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        }
        content.getChildren().setAll(view);
        for (Node node : menu.getChildren()) {
            node.getStyleClass().remove("selected");
        }
        if (menuButton != null) {
            menuButton.getStyleClass().add("selected");
        }
        if (init != null) {
            init.accept(controller);
        }
        return controller;
    }

    public void showSubscriberDetails(int subscriberId) {
        this.<SubscriberDetailsController>show("subscriber-details-view.fxml", subscribersButton, c -> c.load(subscriberId));
    }

    public void showSubscribers() {
        handleSubscribers();
    }

    void busy(boolean on) {
        running += on ? 1 : -1;
        busyIndicator.setVisible(running > 0);
    }

    void showConnectionBanner(Runnable retry) {
        retryAction = retry;
        connectionBanner.setVisible(true);
        connectionBanner.setManaged(true);
    }

    void hideConnectionBanner() {
        connectionBanner.setVisible(false);
        connectionBanner.setManaged(false);
    }

    @FXML
    private void handleRetry() {
        hideConnectionBanner();
        if (retryAction != null) {
            retryAction.run();
        }
    }

    @FXML
    private void handleDashboard() {
        show("dashboard-view.fxml", dashboardButton, null);
    }

    @FXML
    private void handleSubscribers() {
        show("subscribers-view.fxml", subscribersButton, null);
    }

    @FXML
    private void handleReadings() {
        show("readings-view.fxml", readingsButton, null);
    }

    @FXML
    private void handleTariffs() {
        show("tariffs-view.fxml", tariffsButton, null);
    }

    @FXML
    private void handleBilling() {
        show("billing-run-view.fxml", billingButton, null);
    }

    @FXML
    private void handlePayments() {
        show("payment-counter-view.fxml", paymentsButton, null);
    }

    @FXML
    private void handleArrears() {
        show("arrears-view.fxml", arrearsButton, null);
    }

    @FXML
    private void handleZones() {
        show("zones-view.fxml", zonesButton, null);
    }

    @FXML
    private void handleLogout() {
        if (Ui.confirm("Log out", "Close your session on this computer?", "Log out")) {
            Ui.backToLogin("You are logged out.");
        }
    }
}
