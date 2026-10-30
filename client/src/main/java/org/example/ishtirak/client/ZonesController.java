package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.ishtirak.common.dto.ZoneRequest;
import org.example.ishtirak.common.dto.ZoneResponse;

/** V10 Zones: add, rename, delete, mark rural. */
public class ZonesController {
    @FXML
    private TableView<ZoneResponse> zoneTable;
    @FXML
    private TableColumn<ZoneResponse, String> nameColumn;
    @FXML
    private TableColumn<ZoneResponse, String> ruralColumn;
    @FXML
    private TableColumn<ZoneResponse, String> countColumn;
    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameError;
    @FXML
    private CheckBox ruralCheck;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button deleteButton;

    private FormErrors errors;
    private ZoneResponse editing;

    @FXML
    private void initialize() {
        errors = new FormErrors(messageLabel).field("name", nameError);
        Ui.column(nameColumn, ZoneResponse::name);
        Ui.column(ruralColumn, z -> z.isRural() ? "Rural (+ uplift)" : "Not rural");
        Ui.column(countColumn, ZoneResponse::subscriberCount);
        zoneTable.getSelectionModel().selectedItemProperty().addListener((obs, o, zone) -> edit(zone));
        handleNew();
        load();
    }

    private void load() {
        Ui.run(() -> ApiClient.get().getZones(), zones -> zoneTable.getItems().setAll(zones), errors);
    }

    private void edit(ZoneResponse zone) {
        errors.clear();
        editing = zone;
        if (zone == null) {
            return;
        }
        formTitle.setText("Edit zone");
        nameField.setText(zone.name());
        ruralCheck.setSelected(zone.isRural());
        deleteButton.setDisable(false);
    }

    @FXML
    private void handleNew() {
        editing = null;
        zoneTable.getSelectionModel().clearSelection();
        formTitle.setText("New zone");
        nameField.clear();
        ruralCheck.setSelected(false);
        deleteButton.setDisable(true);
        errors.clear();
    }

    @FXML
    private void handleSave() {
        ZoneRequest request = new ZoneRequest(nameField.getText(), ruralCheck.isSelected());
        ZoneResponse target = editing;
        Ui.run(() -> target == null ? ApiClient.get().createZone(request) : ApiClient.get().updateZone(target.id(), request),
                saved -> {
                    handleNew();
                    messageLabel.getStyleClass().setAll("success");
                    messageLabel.setText("Zone " + saved.name() + " saved.");
                    load();
                },
                errors, saveButton);
        messageLabel.getStyleClass().setAll("message");
    }

    @FXML
    private void handleDelete() {
        ZoneResponse target = editing;
        if (target == null || !Ui.confirm("Delete zone", "Delete the zone " + target.name() + "?", "Delete zone")) {
            return;
        }
        Ui.run(() -> {
            ApiClient.get().deleteZone(target.id());
            return target;
        }, deleted -> {
            handleNew();
            load();
        }, errors, deleteButton);
    }
}
