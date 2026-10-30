package org.example.ishtirak.client;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.example.ishtirak.common.dto.DashboardResponse;
import org.example.ishtirak.common.dto.ZoneCollection;

import java.time.YearMonth;

/** V2 Dashboard: month totals and collections per zone. */
public class DashboardController {
    @FXML
    private ComboBox<YearMonth> monthBox;
    @FXML
    private Button refreshButton;
    @FXML
    private Label billedLabel;
    @FXML
    private Label collectedLabel;
    @FXML
    private Label outstandingLabel;
    @FXML
    private Label rateLabel;
    @FXML
    private Label missingLabel;
    @FXML
    private BarChart<String, Number> zoneChart;

    @FXML
    private void initialize() {
        Ui.monthPicker(monthBox);
        monthBox.valueProperty().addListener((obs, o, n) -> handleRefresh());
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        YearMonth month = monthBox.getValue();
        Ui.run(() -> ApiClient.get().getDashboard(month), this::show, null, refreshButton);
    }

    private void show(DashboardResponse d) {
        billedLabel.setText(Ui.lbp(d.billed()));
        collectedLabel.setText(Ui.lbp(d.collected()));
        outstandingLabel.setText(Ui.lbp(d.outstanding()));
        rateLabel.setText(d.collectionRate().toPlainString() + " %");
        missingLabel.setText(String.valueOf(d.missingReadings()));

        XYChart.Series<String, Number> billed = new XYChart.Series<>();
        billed.setName("Billed");
        XYChart.Series<String, Number> collected = new XYChart.Series<>();
        collected.setName("Collected");
        for (ZoneCollection zone : d.zones()) {
            billed.getData().add(new XYChart.Data<>(zone.zone(), zone.billed()));
            collected.getData().add(new XYChart.Data<>(zone.zone(), zone.collected()));
        }
        zoneChart.getData().setAll(billed, collected);
        zoneChart.setTitle("Collections per zone, " + Ui.month(d.month()) + " (LBP)");
    }
}
