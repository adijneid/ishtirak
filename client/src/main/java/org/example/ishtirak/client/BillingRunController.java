package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.example.ishtirak.common.dto.BillingRunResponse;
import org.example.ishtirak.common.dto.MissingReading;
import org.example.ishtirak.common.dto.ReadingSheetRow;
import org.example.ishtirak.common.dto.ZoneResponse;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** V7 Billing run: preview missing readings, confirm, then show the run summary. */
public class BillingRunController {
    @FXML
    private ComboBox<YearMonth> monthBox;
    @FXML
    private Button checkButton;
    @FXML
    private Button runButton;
    @FXML
    private Label previewLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private TableView<MissingReading> missingTable;
    @FXML
    private TableColumn<MissingReading, String> zoneColumn;
    @FXML
    private TableColumn<MissingReading, String> subscriberColumn;
    @FXML
    private TableColumn<MissingReading, String> meterColumn;

    @FXML
    private void initialize() {
        Ui.monthPicker(monthBox);
        Ui.column(zoneColumn, MissingReading::zone);
        Ui.column(subscriberColumn, MissingReading::subscriber);
        Ui.column(meterColumn, MissingReading::meterSerial);
        monthBox.valueProperty().addListener((obs, o, n) -> {
            resultLabel.setText("");
            handleCheck();
        });
        handleCheck();
    }

    /** Reads every zone's sheet for the month to list meters without a reading before billing. */
    @FXML
    private void handleCheck() {
        YearMonth month = monthBox.getValue();
        Ui.run(() -> {
            List<MissingReading> missing = new ArrayList<>();
            int ready = 0;
            int invoiced = 0;
            for (ZoneResponse zone : ApiClient.get().getZones()) {
                for (ReadingSheetRow row : ApiClient.get().getReadingSheet(month, zone.id())) {
                    if (row.currentReading() == null) {
                        missing.add(new MissingReading(row.subscriptionId(), row.subscriber(), row.meterSerial(), zone.name()));
                    } else if (row.invoiced()) {
                        invoiced++;
                    } else {
                        ready++;
                    }
                }
            }
            return new Preview(missing, ready, invoiced);
        }, preview -> {
            missingTable.getItems().setAll(preview.missing());
            previewLabel.setText(preview.ready() + " readings ready to bill, " + preview.invoiced()
                    + " already invoiced, " + preview.missing().size() + " meters without a reading.");
        }, null, checkButton, runButton);
    }

    @FXML
    private void handleRun() {
        YearMonth month = monthBox.getValue();
        if (!Ui.confirm("Run billing", "Generate the invoices of " + Ui.month(month)
                + "? Readings that are already invoiced are skipped, and the month's tariff will be locked.", "Generate invoices")) {
            return;
        }
        Ui.run(() -> ApiClient.get().runBilling(month), this::showResult, null, runButton, checkButton);
    }

    private void showResult(BillingRunResponse r) {
        resultLabel.setText(r.created() + " invoices created for " + Ui.month(r.month()) + ", total " + Ui.lbp(r.totalBilled())
                + ". " + r.skipped() + " already invoiced and skipped. " + r.missingReadings().size() + " meters without a reading.");
        missingTable.getItems().setAll(r.missingReadings());
        previewLabel.setText("");
    }

    private record Preview(List<MissingReading> missing, int ready, int invoiced) {
    }
}
