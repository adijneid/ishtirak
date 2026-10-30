package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.ishtirak.common.dto.ArrearsRow;
import org.example.ishtirak.common.dto.ZoneResponse;

import java.math.BigDecimal;

/** V9 Arrears: subscribers with an unpaid balance, largest first. */
public class ArrearsController {
    @FXML
    private ComboBox<ZoneResponse> zoneBox;
    @FXML
    private Button refreshButton;
    @FXML
    private TableView<ArrearsRow> arrearsTable;
    @FXML
    private TableColumn<ArrearsRow, String> nameColumn;
    @FXML
    private TableColumn<ArrearsRow, String> phoneColumn;
    @FXML
    private TableColumn<ArrearsRow, String> zoneColumn;
    @FXML
    private TableColumn<ArrearsRow, String> unpaidColumn;
    @FXML
    private TableColumn<ArrearsRow, BigDecimal> balanceColumn;
    @FXML
    private Label totalLabel;

    @FXML
    private void initialize() {
        Ui.column(nameColumn, ArrearsRow::subscriber);
        Ui.column(phoneColumn, ArrearsRow::phone);
        Ui.column(zoneColumn, ArrearsRow::zone);
        Ui.column(unpaidColumn, ArrearsRow::unpaidInvoices);
        Ui.figure(balanceColumn, ArrearsRow::balance, Ui::lbp);
        ZoneFilter.install(zoneBox, "All zones");
        zoneBox.valueProperty().addListener((obs, o, n) -> handleRefresh());
        Ui.run(() -> ApiClient.get().getZones(), zones -> ZoneFilter.fill(zoneBox, zones, true), null);
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        Integer zoneId = ZoneFilter.idOf(zoneBox.getValue());
        Ui.run(() -> ApiClient.get().getArrears(zoneId), rows -> {
            arrearsTable.getItems().setAll(rows);
            BigDecimal total = rows.stream().map(ArrearsRow::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalLabel.setText(rows.size() + " subscribers owe " + Ui.lbp(total));
        }, null, refreshButton);
    }
}
