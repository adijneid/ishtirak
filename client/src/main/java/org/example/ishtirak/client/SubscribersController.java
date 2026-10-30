package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.ishtirak.common.dto.PageResponse;
import org.example.ishtirak.common.dto.SubscriberRequest;
import org.example.ishtirak.common.dto.SubscriberResponse;
import org.example.ishtirak.common.dto.ZoneResponse;
import org.example.ishtirak.common.model.SubscriberStatus;

import java.math.BigDecimal;

/** V3 Subscribers: searchable list; add, edit or close (admin). Collectors only read. */
public class SubscribersController {
    private static final int PAGE_SIZE = 25;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<ZoneResponse> zoneFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Button searchButton;
    @FXML
    private TableView<SubscriberResponse> subscriberTable;
    @FXML
    private TableColumn<SubscriberResponse, String> nameColumn;
    @FXML
    private TableColumn<SubscriberResponse, String> phoneColumn;
    @FXML
    private TableColumn<SubscriberResponse, String> zoneColumn;
    @FXML
    private TableColumn<SubscriberResponse, String> statusColumn;
    @FXML
    private TableColumn<SubscriberResponse, BigDecimal> balanceColumn;
    @FXML
    private Label pageLabel;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;

    @FXML
    private VBox formPanel;
    @FXML
    private Label formTitle;
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private ComboBox<ZoneResponse> zoneField;
    @FXML
    private Label nameError;
    @FXML
    private Label phoneError;
    @FXML
    private Label addressError;
    @FXML
    private Label zoneError;
    @FXML
    private Label messageLabel;
    @FXML
    private Button saveButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button detailsButton;

    private FormErrors errors;
    private SubscriberResponse editing;
    private int page;
    private int totalPages;

    @FXML
    private void initialize() {
        errors = new FormErrors(messageLabel)
                .field("fullName", nameError).field("phone", phoneError)
                .field("address", addressError).field("zoneId", zoneError);
        Ui.column(nameColumn, SubscriberResponse::fullName);
        Ui.column(phoneColumn, SubscriberResponse::phone);
        Ui.column(zoneColumn, SubscriberResponse::zoneName);
        Ui.column(statusColumn, s -> s.status() == SubscriberStatus.ACTIVE ? "Active" : "Closed");
        Ui.figure(balanceColumn, SubscriberResponse::balance, Ui::lbp);

        statusFilter.getItems().setAll("All statuses", "ACTIVE", "CLOSED");
        statusFilter.setValue("All statuses");
        ZoneFilter.install(zoneFilter, "All zones");
        ZoneFilter.install(zoneField, "");

        // collectors: read-only list (proposal V3), no details view (V4 is admin only)
        boolean admin = SessionContext.isAdmin();
        formPanel.setVisible(admin);
        formPanel.setManaged(admin);

        subscriberTable.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> edit(s));
        subscriberTable.setRowFactory(t -> {
            TableRow<SubscriberResponse> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (admin && e.getClickCount() == 2 && !row.isEmpty()) {
                    MainController.current().showSubscriberDetails(row.getItem().id());
                }
            });
            return row;
        });

        Ui.run(() -> ApiClient.get().getZones(), zones -> {
            ZoneFilter.fill(zoneFilter, zones, true);
            ZoneFilter.fill(zoneField, zones, false);
            zoneField.setValue(null);
        }, null);
        handleNew();
        handleSearch();
    }

    @FXML
    private void handleSearch() {
        page = 0;
        load();
    }

    private void load() {
        Integer zoneId = ZoneFilter.idOf(zoneFilter.getValue());
        String status = statusFilter.getValue() == null || statusFilter.getValue().startsWith("All") ? null : statusFilter.getValue();
        String search = searchField.getText();
        int requested = page;
        Ui.run(() -> ApiClient.get().getSubscribers(zoneId, status, search, requested, PAGE_SIZE),
                this::show, null, searchButton);
    }

    private void show(PageResponse<SubscriberResponse> result) {
        subscriberTable.getItems().setAll(result.content());
        totalPages = Math.max(1, result.totalPages());
        pageLabel.setText("Page " + (result.page() + 1) + " of " + totalPages + ", " + result.totalElements() + " subscribers");
        previousButton.setDisable(result.page() == 0);
        nextButton.setDisable(result.page() + 1 >= totalPages);
    }

    @FXML
    private void handlePrevious() {
        if (page > 0) {
            page--;
            load();
        }
    }

    @FXML
    private void handleNext() {
        if (page + 1 < totalPages) {
            page++;
            load();
        }
    }

    private void edit(SubscriberResponse s) {
        if (s == null) {
            return;
        }
        errors.clear();
        editing = s;
        formTitle.setText("Edit subscriber");
        nameField.setText(s.fullName());
        phoneField.setText(s.phone());
        addressField.setText(s.address() == null ? "" : s.address());
        zoneField.setValue(ZoneFilter.byId(zoneField, s.zoneId()));
        closeButton.setDisable(s.status() == SubscriberStatus.CLOSED);
        detailsButton.setDisable(false);
    }

    @FXML
    private void handleNew() {
        editing = null;
        subscriberTable.getSelectionModel().clearSelection();
        formTitle.setText("New subscriber");
        nameField.clear();
        phoneField.clear();
        addressField.clear();
        zoneField.setValue(null);
        closeButton.setDisable(true);
        detailsButton.setDisable(true);
        errors.clear();
    }

    @FXML
    private void handleSave() {
        SubscriberRequest request = new SubscriberRequest(nameField.getText(), phoneField.getText(),
                addressField.getText(), ZoneFilter.idOf(zoneField.getValue()));
        SubscriberResponse target = editing;
        Ui.run(() -> target == null ? ApiClient.get().createSubscriber(request)
                        : ApiClient.get().updateSubscriber(target.id(), request),
                saved -> {
                    handleNew();
                    load();
                    Ui.info("Subscriber saved", saved.fullName() + " is saved.");
                }, errors, saveButton);
    }

    @FXML
    private void handleClose() {
        SubscriberResponse target = editing;
        if (target == null || !Ui.confirm("Close subscriber",
                "Close " + target.fullName() + "? A closed subscriber cannot get new meters.", "Close subscriber")) {
            return;
        }
        Ui.run(() -> ApiClient.get().closeSubscriber(target.id()), closed -> {
            handleNew();
            load();
        }, errors, closeButton);
    }

    @FXML
    private void handleDetails() {
        if (editing != null) {
            MainController.current().showSubscriberDetails(editing.id());
        }
    }
}
