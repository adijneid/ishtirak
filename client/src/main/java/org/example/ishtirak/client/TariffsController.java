package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.example.ishtirak.common.dto.TariffRequest;
import org.example.ishtirak.common.dto.TariffResponse;

import java.math.BigDecimal;
import java.time.YearMonth;

/** V6 Tariffs: the ministry's monthly values; invoiced months are read-only. */
public class TariffsController {
    @FXML
    private TableView<TariffResponse> tariffTable;
    @FXML
    private TableColumn<TariffResponse, String> monthColumn;
    @FXML
    private TableColumn<TariffResponse, BigDecimal> priceColumn;
    @FXML
    private TableColumn<TariffResponse, BigDecimal> fee5Column;
    @FXML
    private TableColumn<TariffResponse, BigDecimal> fee10Column;
    @FXML
    private TableColumn<TariffResponse, BigDecimal> extraColumn;
    @FXML
    private TableColumn<TariffResponse, BigDecimal> upliftColumn;
    @FXML
    private TableColumn<TariffResponse, String> lockedColumn;

    @FXML
    private Label formTitle;
    @FXML
    private GridPane form;
    @FXML
    private ComboBox<YearMonth> monthBox;
    @FXML
    private TextField priceField;
    @FXML
    private TextField fee5Field;
    @FXML
    private TextField fee10Field;
    @FXML
    private TextField extraField;
    @FXML
    private TextField upliftField;
    @FXML
    private Label monthError;
    @FXML
    private Label priceError;
    @FXML
    private Label fee5Error;
    @FXML
    private Label fee10Error;
    @FXML
    private Label extraError;
    @FXML
    private Label upliftError;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;

    private FormErrors errors;
    private TariffResponse editing;

    @FXML
    private void initialize() {
        errors = new FormErrors(messageLabel)
                .field("billingMonth", monthError).field("pricePerKwh", priceError)
                .field("fee5a", fee5Error).field("fee10a", fee10Error)
                .field("feePerExtra5a", extraError).field("ruralUpliftPct", upliftError);
        Ui.monthPicker(monthBox);
        Ui.column(monthColumn, t -> Ui.month(t.billingMonth()));
        Ui.figure(priceColumn, TariffResponse::pricePerKwh, Ui::number);
        Ui.figure(fee5Column, TariffResponse::fee5a, Ui::number);
        Ui.figure(fee10Column, TariffResponse::fee10a, Ui::number);
        Ui.figure(extraColumn, TariffResponse::feePerExtra5a, Ui::number);
        Ui.figure(upliftColumn, TariffResponse::ruralUpliftPct, BigDecimal::toPlainString);
        Ui.column(lockedColumn, t -> t.locked() ? "Invoiced, locked" : "Editable");
        tariffTable.getSelectionModel().selectedItemProperty().addListener((obs, o, t) -> edit(t));
        handleNew();
        load();
    }

    private void load() {
        Ui.run(() -> ApiClient.get().getTariffs(), list -> tariffTable.getItems().setAll(list), errors);
    }

    private void edit(TariffResponse t) {
        if (t == null) {
            return;
        }
        errors.clear();
        editing = t;
        formTitle.setText(t.locked() ? "Tariff of " + Ui.month(t.billingMonth()) + " (locked)" : "Edit tariff");
        monthBox.setValue(t.billingMonth());
        priceField.setText(t.pricePerKwh().toPlainString());
        fee5Field.setText(t.fee5a().toPlainString());
        fee10Field.setText(t.fee10a().toPlainString());
        extraField.setText(t.feePerExtra5a().toPlainString());
        upliftField.setText(t.ruralUpliftPct().toPlainString());
        form.setDisable(t.locked());
        saveButton.setDisable(t.locked());
        if (t.locked()) {
            messageLabel.setText("Invoices were issued with this tariff, so it is read-only.");
        }
    }

    @FXML
    private void handleNew() {
        editing = null;
        tariffTable.getSelectionModel().clearSelection();
        formTitle.setText("New tariff");
        for (TextField f : new TextField[]{priceField, fee5Field, fee10Field, extraField}) {
            f.clear();
        }
        upliftField.setText("10");
        form.setDisable(false);
        saveButton.setDisable(false);
        errors.clear();
    }

    @FXML
    private void handleSave() {
        TariffRequest request;
        try {
            request = new TariffRequest(monthBox.getValue(),
                    Ui.decimal(priceField.getText()), Ui.decimal(fee5Field.getText()),
                    Ui.decimal(fee10Field.getText()), Ui.decimal(extraField.getText()),
                    Ui.decimal(upliftField.getText()));
        } catch (NumberFormatException e) {
            errors.message("Prices and uplift must be numbers.");
            return;
        }
        TariffResponse target = editing;
        Ui.run(() -> target == null ? ApiClient.get().createTariff(request) : ApiClient.get().updateTariff(target.id(), request),
                saved -> {
                    handleNew();
                    load();
                    messageLabel.setText("");
                    Ui.info("Tariff saved", "The tariff of " + Ui.month(saved.billingMonth()) + " is saved.");
                },
                errors, saveButton);
    }
}
