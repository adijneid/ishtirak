package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import org.example.ishtirak.common.dto.ReadingBatchResponse;
import org.example.ishtirak.common.dto.ReadingEntry;
import org.example.ishtirak.common.dto.ReadingIssue;
import org.example.ishtirak.common.dto.ReadingSheetRow;
import org.example.ishtirak.common.dto.ZoneResponse;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V5 Monthly readings: a whole zone on one screen. Rows turn red when rejected, amber on a
 * consumption warning; invoiced rows are locked. The server validates every row.
 */
public class ReadingsController {
    @FXML
    private ComboBox<YearMonth> monthBox;
    @FXML
    private ComboBox<ZoneResponse> zoneBox;
    @FXML
    private Button loadButton;
    @FXML
    private Button saveButton;
    @FXML
    private TableView<ReadingRow> readingTable;
    @FXML
    private TableColumn<ReadingRow, String> subscriberColumn;
    @FXML
    private TableColumn<ReadingRow, String> meterColumn;
    @FXML
    private TableColumn<ReadingRow, String> ampColumn;
    @FXML
    private TableColumn<ReadingRow, String> previousColumn;
    @FXML
    private TableColumn<ReadingRow, String> currentColumn;
    @FXML
    private TableColumn<ReadingRow, String> noteColumn;
    @FXML
    private Label summaryLabel;

    @FXML
    private void initialize() {
        Ui.monthPicker(monthBox);
        ZoneFilter.install(zoneBox, "");
        Ui.column(subscriberColumn, r -> r.getSheet().subscriber());
        Ui.column(meterColumn, r -> r.getSheet().meterSerial());
        Ui.column(ampColumn, r -> r.getSheet().amperage() + " A");
        Ui.column(previousColumn, r -> Ui.kwh(r.getSheet().previousReading()));
        previousColumn.getStyleClass().add("figure");

        currentColumn.setCellValueFactory(c -> c.getValue().inputProperty());
        currentColumn.setCellFactory(c -> new TextFieldTableCell<>(new javafx.util.converter.DefaultStringConverter()) {
            @Override
            public void startEdit() {
                ReadingRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null && !row.getSheet().invoiced()) {
                    super.startEdit();
                }
            }
        });
        currentColumn.setOnEditCommit(e -> {
            e.getRowValue().inputProperty().set(e.getNewValue());
            e.getRowValue().mark(ReadingRow.State.NORMAL, "");
        });
        currentColumn.getStyleClass().add("figure");
        noteColumn.setCellValueFactory(c -> c.getValue().noteProperty());

        readingTable.setEditable(true);
        readingTable.setRowFactory(t -> new TableRow<>() {
            private final javafx.beans.value.ChangeListener<ReadingRow.State> listener = (obs, o, n) -> paint(n);

            @Override
            protected void updateItem(ReadingRow item, boolean empty) {
                if (getItem() != null) {
                    getItem().stateProperty().removeListener(listener);
                }
                super.updateItem(item, empty);
                if (item == null || empty) {
                    paint(null);
                } else {
                    item.stateProperty().addListener(listener);
                    paint(item.stateProperty().get());
                }
            }

            private void paint(ReadingRow.State state) {
                getStyleClass().removeAll("row-rejected", "row-warning", "row-locked");
                if (state == ReadingRow.State.REJECTED) {
                    getStyleClass().add("row-rejected");
                } else if (state == ReadingRow.State.WARNING) {
                    getStyleClass().add("row-warning");
                } else if (state == ReadingRow.State.LOCKED) {
                    getStyleClass().add("row-locked");
                }
            }
        });

        monthBox.valueProperty().addListener((obs, o, n) -> handleLoad());
        zoneBox.valueProperty().addListener((obs, o, n) -> {
            if (n != null && o != null) {
                handleLoad();
            }
        });
        Ui.run(() -> ApiClient.get().getZones(), zones -> {
            ZoneFilter.fill(zoneBox, zones, false);
            if (!zones.isEmpty()) {
                handleLoad();
            }
        }, null);
    }

    @FXML
    private void handleLoad() {
        load(Map.of(), Map.of(), false);
    }

    /** Reloads the sheet from the server, then re-applies the last save's results and rejected inputs. */
    private void load(Map<Integer, ReadingIssue[]> issues, Map<Integer, String> keepInput, boolean afterSave) {
        YearMonth month = monthBox.getValue();
        ZoneResponse zone = zoneBox.getValue();
        if (zone == null) {
            summaryLabel.setText("Choose a zone.");
            return;
        }
        Ui.run(() -> ApiClient.get().getReadingSheet(month, zone.id()), sheet -> {
            List<ReadingRow> rows = new ArrayList<>();
            for (ReadingSheetRow line : sheet) {
                ReadingRow row = new ReadingRow(line);
                if (keepInput.containsKey(line.subscriptionId())) {
                    row.inputProperty().set(keepInput.get(line.subscriptionId()));
                }
                ReadingIssue[] found = issues.get(line.subscriptionId());
                if (found != null) {
                    if (found[0] != null) {
                        row.mark(ReadingRow.State.REJECTED, found[0].message());
                    } else if (found[1] != null) {
                        row.mark(ReadingRow.State.WARNING, found[1].message());
                    }
                }
                rows.add(row);
            }
            readingTable.getItems().setAll(rows);
            if (!afterSave) {
                long done = sheet.stream().filter(r -> r.currentReading() != null).count();
                summaryLabel.setText(done + " of " + sheet.size() + " meters read for " + Ui.month(month) + ".");
            }
        }, null, loadButton, saveButton);
    }

    @FXML
    private void handleSave() {
        readingTable.edit(-1, null);
        YearMonth month = monthBox.getValue();
        List<ReadingEntry> entries = new ArrayList<>();
        Map<Integer, String> typed = new HashMap<>();
        for (ReadingRow row : readingTable.getItems()) {
            if (row.getSheet().invoiced() || !row.isChanged()) {
                continue;
            }
            Integer id = row.getSheet().subscriptionId();
            typed.put(id, row.inputProperty().get());
            try {
                entries.add(new ReadingEntry(id, Ui.decimal(row.inputProperty().get())));
            } catch (NumberFormatException e) {
                row.mark(ReadingRow.State.REJECTED, "Not a number");
            }
        }
        if (entries.isEmpty()) {
            summaryLabel.setText("Nothing new to save. Type the new readings in the Current column.");
            return;
        }
        Ui.run(() -> ApiClient.get().saveReadings(month, entries), result -> afterSave(result, typed), null, saveButton, loadButton);
    }

    private void afterSave(ReadingBatchResponse result, Map<Integer, String> typed) {
        Map<Integer, ReadingIssue[]> issues = new HashMap<>();
        Map<Integer, String> keep = new HashMap<>();
        for (ReadingIssue r : result.rejected()) {
            issues.computeIfAbsent(r.subscriptionId(), k -> new ReadingIssue[2])[0] = r;
            if (typed.containsKey(r.subscriptionId())) {
                keep.put(r.subscriptionId(), typed.get(r.subscriptionId()));
            }
        }
        for (ReadingIssue w : result.warnings()) {
            issues.computeIfAbsent(w.subscriptionId(), k -> new ReadingIssue[2])[1] = w;
        }
        summaryLabel.setText(result.saved() + " saved, " + result.rejected().size() + " rejected, "
                + result.warnings().size() + " warnings. Red rows were not saved; amber rows were saved but look unusual.");
        load(issues, keep, true);
    }
}
