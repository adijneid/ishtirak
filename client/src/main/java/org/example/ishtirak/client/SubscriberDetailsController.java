package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import org.example.ishtirak.common.dto.InvoiceResponse;
import org.example.ishtirak.common.dto.PaymentHistoryItem;
import org.example.ishtirak.common.dto.SubscriberDetailResponse;
import org.example.ishtirak.common.dto.SubscriberResponse;
import org.example.ishtirak.common.dto.SubscriptionRequest;
import org.example.ishtirak.common.dto.SubscriptionResponse;
import org.example.ishtirak.common.model.InvoiceStatus;
import org.example.ishtirak.common.model.SubscriberStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/** V4 Subscriber Details: profile, subscriptions (add meter, change amperage, end), invoices and payments. */
public class SubscriberDetailsController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label titleLabel;
    @FXML
    private Label phoneLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Label zoneLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label createdLabel;
    @FXML
    private Label balanceLabel;

    @FXML
    private TableView<SubscriptionResponse> subscriptionTable;
    @FXML
    private TableColumn<SubscriptionResponse, String> meterColumn;
    @FXML
    private TableColumn<SubscriptionResponse, String> ampColumn;
    @FXML
    private TableColumn<SubscriptionResponse, String> initialColumn;
    @FXML
    private TableColumn<SubscriptionResponse, String> startColumn;
    @FXML
    private TableColumn<SubscriptionResponse, String> endColumn;
    @FXML
    private TableColumn<SubscriptionResponse, String> activeColumn;
    @FXML
    private Label subscriptionFormTitle;
    @FXML
    private TextField meterField;
    @FXML
    private ComboBox<Integer> amperageBox;
    @FXML
    private TextField initialField;
    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private Label meterError;
    @FXML
    private Label amperageError;
    @FXML
    private Label initialError;
    @FXML
    private Label startError;
    @FXML
    private Label endError;
    @FXML
    private Label subscriptionMessage;
    @FXML
    private Button addMeterButton;
    @FXML
    private Button saveMeterButton;
    @FXML
    private Button endButton;

    @FXML
    private TableView<InvoiceResponse> invoiceTable;
    @FXML
    private TableColumn<InvoiceResponse, String> invMonthColumn;
    @FXML
    private TableColumn<InvoiceResponse, String> invMeterColumn;
    @FXML
    private TableColumn<InvoiceResponse, BigDecimal> invKwhColumn;
    @FXML
    private TableColumn<InvoiceResponse, BigDecimal> invAmountColumn;
    @FXML
    private TableColumn<InvoiceResponse, BigDecimal> invPaidColumn;
    @FXML
    private TableColumn<InvoiceResponse, BigDecimal> invBalanceColumn;
    @FXML
    private TableColumn<InvoiceResponse, String> invStatusColumn;
    @FXML
    private Button voidButton;
    @FXML
    private TableView<PaymentHistoryItem> paymentTable;
    @FXML
    private TableColumn<PaymentHistoryItem, String> payReceiptColumn;
    @FXML
    private TableColumn<PaymentHistoryItem, BigDecimal> payAmountColumn;
    @FXML
    private TableColumn<PaymentHistoryItem, String> payMethodColumn;
    @FXML
    private TableColumn<PaymentHistoryItem, String> payDateColumn;
    @FXML
    private TableColumn<PaymentHistoryItem, String> payByColumn;

    private FormErrors subscriptionErrors;
    private int subscriberId;
    private SubscriptionResponse selectedSubscription;

    @FXML
    private void initialize() {
        subscriptionErrors = new FormErrors(subscriptionMessage)
                .field("meterSerial", meterError).field("amperage", amperageError)
                .field("initialReading", initialError).field("startDate", startError)
                .field("endDate", endError);
        for (int a = 5; a <= 60; a += 5) {
            amperageBox.getItems().add(a);
        }
        Ui.column(meterColumn, SubscriptionResponse::meterSerial);
        Ui.column(ampColumn, s -> s.amperage() + " A");
        Ui.column(initialColumn, s -> Ui.kwh(s.initialReading()));
        Ui.column(startColumn, SubscriptionResponse::startDate);
        Ui.column(endColumn, s -> s.endDate() == null ? "" : s.endDate());
        Ui.column(activeColumn, s -> s.active() ? "Active" : "Ended");
        subscriptionTable.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> editSubscription(s));

        Ui.column(invMonthColumn, i -> Ui.month(i.month()));
        Ui.column(invMeterColumn, InvoiceResponse::meterSerial);
        Ui.figure(invKwhColumn, InvoiceResponse::kwh, Ui::kwh);
        Ui.figure(invAmountColumn, InvoiceResponse::amountDue, Ui::number);
        Ui.figure(invPaidColumn, InvoiceResponse::paid, Ui::number);
        Ui.figure(invBalanceColumn, InvoiceResponse::balance, Ui::number);
        Ui.column(invStatusColumn, i -> Ui.capitalize(i.status().name().toLowerCase()));
        invoiceTable.getSelectionModel().selectedItemProperty().addListener((obs, o, i) -> showPayments(i));

        Ui.column(payReceiptColumn, PaymentHistoryItem::receiptNo);
        Ui.figure(payAmountColumn, PaymentHistoryItem::amount, Ui::lbp);
        Ui.column(payMethodColumn, p -> Ui.capitalize(p.method().name().toLowerCase()));
        Ui.column(payDateColumn, p -> p.paidAt().format(DATE_TIME));
        Ui.column(payByColumn, PaymentHistoryItem::receivedBy);
        voidButton.setDisable(true);
        clearSubscriptionForm();
    }

    public void load(int id) {
        subscriberId = id;
        Ui.run(() -> ApiClient.get().getSubscriber(id), this::show, null);
        Ui.run(() -> ApiClient.get().getInvoices(null, id, null), invoices -> {
            invoiceTable.getItems().setAll(invoices);
            paymentTable.getItems().clear();
        }, null);
    }

    private void show(SubscriberDetailResponse detail) {
        SubscriberResponse s = detail.subscriber();
        titleLabel.setText(s.fullName());
        phoneLabel.setText(s.phone());
        addressLabel.setText(s.address() == null ? "-" : s.address());
        zoneLabel.setText(s.zoneName());
        statusLabel.setText(s.status() == SubscriberStatus.ACTIVE ? "Active" : "Closed");
        createdLabel.setText(s.createdAt().format(DATE_TIME));
        balanceLabel.setText(Ui.lbp(s.balance()));
        subscriptionTable.getItems().setAll(detail.subscriptions());
        boolean closed = s.status() == SubscriberStatus.CLOSED;
        addMeterButton.setDisable(closed);
    }

    // ---------------- subscriptions ----------------

    private void editSubscription(SubscriptionResponse s) {
        selectedSubscription = s;
        subscriptionErrors.clear();
        if (s == null) {
            return;
        }
        subscriptionFormTitle.setText("Meter " + s.meterSerial());
        meterField.setText(s.meterSerial());
        amperageBox.setValue(s.amperage());
        initialField.setText(s.initialReading().toPlainString());
        startPicker.setValue(s.startDate());
        endPicker.setValue(s.endDate() != null ? s.endDate() : LocalDate.now());
        saveMeterButton.setDisable(false);
        endButton.setDisable(false);
    }

    @FXML
    private void clearSubscriptionForm() {
        selectedSubscription = null;
        subscriptionTable.getSelectionModel().clearSelection();
        subscriptionFormTitle.setText("New meter");
        meterField.clear();
        amperageBox.setValue(5);
        initialField.setText("0");
        startPicker.setValue(LocalDate.now().withDayOfMonth(1));
        endPicker.setValue(LocalDate.now());
        saveMeterButton.setDisable(true);
        endButton.setDisable(true);
        subscriptionErrors.clear();
    }

    private SubscriptionRequest subscriptionRequest() {
        try {
            return new SubscriptionRequest(subscriberId, meterField.getText(), amperageBox.getValue(),
                    Ui.decimal(initialField.getText()), startPicker.getValue());
        } catch (NumberFormatException e) {
            subscriptionErrors.showField("initialReading", "Must be a number, e.g. 1250.5");
            return null;
        }
    }

    @FXML
    private void handleAddMeter() {
        SubscriptionRequest request = subscriptionRequest();
        if (request == null) {
            return;
        }
        Ui.run(() -> ApiClient.get().createSubscription(request), saved -> {
            clearSubscriptionForm();
            load(subscriberId);
        }, subscriptionErrors, addMeterButton);
    }

    /** Amperage change: issued invoices keep their amounts, the new value applies to the next billing run. */
    @FXML
    private void handleSaveMeter() {
        SubscriptionResponse target = selectedSubscription;
        SubscriptionRequest request = subscriptionRequest();
        if (target == null || request == null) {
            return;
        }
        Ui.run(() -> ApiClient.get().updateSubscription(target.id(), request), saved -> {
            clearSubscriptionForm();
            load(subscriberId);
            Ui.info("Meter saved", "Changes apply from the next month that is not yet invoiced.");
        }, subscriptionErrors, saveMeterButton);
    }

    @FXML
    private void handleEndSubscription() {
        SubscriptionResponse target = selectedSubscription;
        LocalDate endDate = endPicker.getValue();
        if (target == null || !Ui.confirm("End subscription",
                "End meter " + target.meterSerial() + " on " + endDate + "?", "End subscription")) {
            return;
        }
        Ui.run(() -> ApiClient.get().endSubscription(target.id(), endDate), saved -> {
            clearSubscriptionForm();
            load(subscriberId);
        }, subscriptionErrors, endButton);
    }

    // ---------------- invoices ----------------

    private void showPayments(InvoiceResponse invoice) {
        paymentTable.getItems().clear();
        voidButton.setDisable(invoice == null || invoice.status() == InvoiceStatus.VOID);
        if (invoice == null) {
            return;
        }
        Ui.run(() -> ApiClient.get().getPayments(invoice.id()), payments -> paymentTable.getItems().setAll(payments), null);
    }

    @FXML
    private void handleVoid() {
        InvoiceResponse invoice = invoiceTable.getSelectionModel().getSelectedItem();
        if (invoice == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(SceneManager.getStage());
        dialog.setTitle("Void invoice");
        dialog.setHeaderText("Void the invoice of " + Ui.month(invoice.month()) + " (" + Ui.lbp(invoice.amountDue()) + ")?");
        dialog.setContentText("Reason:");
        Optional<String> reason = dialog.showAndWait();
        if (reason.isEmpty()) {
            return;
        }
        Ui.run(() -> ApiClient.get().voidInvoice(invoice.id(), reason.get()), voided -> load(subscriberId), null, voidButton);
    }

    @FXML
    private void handleBack() {
        MainController.current().showSubscribers();
    }
}
