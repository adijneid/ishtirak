package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.ishtirak.common.dto.InvoiceResponse;
import org.example.ishtirak.common.dto.PaymentRequest;
import org.example.ishtirak.common.dto.SubscriberResponse;
import org.example.ishtirak.common.model.InvoiceStatus;

import java.math.BigDecimal;

/** V8 Payment counter: find a subscriber, pick an open invoice, record a full or partial payment. */
public class PaymentCounterController {
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private TableView<SubscriberResponse> subscriberTable;
    @FXML
    private TableColumn<SubscriberResponse, String> nameColumn;
    @FXML
    private TableColumn<SubscriberResponse, String> phoneColumn;
    @FXML
    private TableColumn<SubscriberResponse, BigDecimal> balanceColumn;
    @FXML
    private TableView<InvoiceResponse> invoiceTable;
    @FXML
    private TableColumn<InvoiceResponse, String> monthColumn;
    @FXML
    private TableColumn<InvoiceResponse, String> meterColumn;
    @FXML
    private TableColumn<InvoiceResponse, BigDecimal> dueColumn;
    @FXML
    private TableColumn<InvoiceResponse, BigDecimal> openColumn;
    @FXML
    private TableColumn<InvoiceResponse, String> statusColumn;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> methodBox;
    @FXML
    private Label amountError;
    @FXML
    private Label methodError;
    @FXML
    private Label messageLabel;
    @FXML
    private Label receiptLabel;
    @FXML
    private Button payButton;

    private FormErrors errors;

    @FXML
    private void initialize() {
        errors = new FormErrors(messageLabel).field("amount", amountError).field("method", methodError);
        Ui.column(nameColumn, SubscriberResponse::fullName);
        Ui.column(phoneColumn, SubscriberResponse::phone);
        Ui.figure(balanceColumn, SubscriberResponse::balance, Ui::lbp);
        Ui.column(monthColumn, i -> Ui.month(i.month()));
        Ui.column(meterColumn, InvoiceResponse::meterSerial);
        Ui.figure(dueColumn, InvoiceResponse::amountDue, Ui::number);
        Ui.figure(openColumn, InvoiceResponse::balance, Ui::number);
        Ui.column(statusColumn, i -> Ui.capitalize(i.status().name().toLowerCase()));
        methodBox.getItems().setAll("CASH", "TRANSFER");
        methodBox.setValue("CASH");
        payButton.setDisable(true);
        subscriberTable.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> loadInvoices(s));
        invoiceTable.getSelectionModel().selectedItemProperty().addListener((obs, o, i) -> {
            payButton.setDisable(i == null);
            amountField.setText(i == null ? "" : i.balance().toPlainString());
            errors.clear();
        });
    }

    @FXML
    private void handleSearch() {
        String search = searchField.getText();
        Ui.run(() -> ApiClient.get().getSubscribers(null, "ACTIVE", search, 0, 30),
                page -> {
                    subscriberTable.getItems().setAll(page.content());
                    invoiceTable.getItems().clear();
                }, null, searchButton);
    }

    private void loadInvoices(SubscriberResponse subscriber) {
        invoiceTable.getItems().clear();
        if (subscriber == null) {
            return;
        }
        Ui.run(() -> ApiClient.get().getInvoices(null, subscriber.id(), null), invoices -> invoiceTable.getItems().setAll(
                invoices.stream().filter(i -> i.status() == InvoiceStatus.UNPAID || i.status() == InvoiceStatus.PARTIAL).toList()),
                null);
    }

    @FXML
    private void handlePay() {
        InvoiceResponse invoice = invoiceTable.getSelectionModel().getSelectedItem();
        if (invoice == null) {
            return;
        }
        BigDecimal amount;
        try {
            amount = Ui.decimal(amountField.getText());
        } catch (NumberFormatException e) {
            errors.showField("amount", "Type the amount in LBP, e.g. 1,500,000");
            return;
        }
        PaymentRequest request = new PaymentRequest(amount, methodBox.getValue());
        SubscriberResponse subscriber = subscriberTable.getSelectionModel().getSelectedItem();
        Ui.run(() -> ApiClient.get().recordPayment(invoice.id(), request), payment -> {
            receiptLabel.setText("Receipt " + payment.receiptNo() + ", remaining on this invoice " + Ui.lbp(payment.newBalance()));
            loadInvoices(subscriber);
            handleSearchKeepSelection(subscriber);
        }, errors, payButton);
    }

    /** Refreshes balances in the subscriber list without losing the current selection. */
    private void handleSearchKeepSelection(SubscriberResponse subscriber) {
        String search = searchField.getText();
        Ui.run(() -> ApiClient.get().getSubscribers(null, "ACTIVE", search, 0, 30), page -> {
            subscriberTable.getItems().setAll(page.content());
            page.content().stream().filter(s -> s.id().equals(subscriber.id())).findFirst()
                    .ifPresent(s -> subscriberTable.getSelectionModel().select(s));
        }, null);
    }
}
