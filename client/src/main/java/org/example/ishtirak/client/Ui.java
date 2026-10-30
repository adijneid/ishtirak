package org.example.ishtirak.client;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared helpers for the controllers: background service calls, error handling and formatting.
 * No business rule lives here; the server decides, the client only shows the answer.
 */
public final class Ui {
    private static final NumberFormat WHOLE = NumberFormat.getIntegerInstance(Locale.US);
    private static final NumberFormat ONE_DECIMAL = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    /** Plain-language explanations of the 409 business codes of the proposal. */
    private static final Map<String, String> CONFLICTS = Map.ofEntries(
            Map.entry("DUPLICATE_NAME", "Another zone already has this name."),
            Map.entry("ZONE_IN_USE", "This zone still has subscribers. Move or remove them before deleting the zone."),
            Map.entry("DUPLICATE_PHONE", "Another subscriber already uses this phone number."),
            Map.entry("HAS_OPEN_ITEMS", "The subscriber still has an active meter or an unpaid balance."),
            Map.entry("SUBSCRIBER_CLOSED", "The subscriber is closed, so no meter can be added or changed."),
            Map.entry("DUPLICATE_METER", "This meter serial number is already registered."),
            Map.entry("INVALID_END_DATE", "The end date must be on or after the start date and the last reading."),
            Map.entry("SUBSCRIPTION_HAS_READINGS", "Readings exist, so the initial reading and start date are fixed."),
            Map.entry("DUPLICATE_MONTH", "A tariff for this month already exists."),
            Map.entry("TARIFF_LOCKED", "Invoices were already issued with this tariff, so it can no longer change."),
            Map.entry("INVOICE_HAS_PAYMENTS", "Payments were recorded on this invoice, so it cannot be voided."),
            Map.entry("INVOICE_VOID", "This invoice is void."),
            Map.entry("OVERPAYMENT", "The amount is more than what is still owed on this invoice.")
    );

    static {
        ONE_DECIMAL.setMinimumFractionDigits(1);
        ONE_DECIMAL.setMaximumFractionDigits(1);
    }

    private Ui() {
    }

    // ---------------- background calls ----------------

    /**
     * Runs a service call off the JavaFX thread. The given nodes are disabled while it runs, a progress
     * indicator is shown, and errors are handled the same way everywhere (see handleError).
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, FormErrors form, Node... disable) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        for (Node node : disable) {
            node.setDisable(true);
        }
        MainController main = MainController.current();
        if (main != null) {
            main.busy(true);
        }
        Runnable finish = () -> {
            for (Node node : disable) {
                node.setDisable(false);
            }
            if (main != null) {
                main.busy(false);
            }
        };
        task.setOnSucceeded(e -> {
            finish.run();
            if (form != null) {
                form.clear();
            }
            if (main != null) {
                main.hideConnectionBanner();
            }
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            finish.run();
            handleError(task.getException(), form, () -> run(work, onSuccess, form, disable));
        });
        Thread thread = new Thread(task, "api-call");
        thread.setDaemon(true);
        thread.start();
    }

    /** 400 -> messages under the inputs; 409 -> explanation alert; 401 -> back to login; no connection -> banner. */
    public static void handleError(Throwable error, FormErrors form, Runnable retry) {
        if (!(error instanceof ApiException api)) {
            error.printStackTrace();
            alert(Alert.AlertType.ERROR, "Unexpected error", String.valueOf(error.getMessage()));
            return;
        }
        if (api.isConnectionLost()) {
            MainController main = MainController.current();
            if (main != null) {
                main.showConnectionBanner(retry);
            } else if (form != null) {
                form.message("The server at " + ApiClient.get().getBaseUrl() + " cannot be reached. Check that it is running.");
            } else {
                alert(Alert.AlertType.ERROR, "No connection", "The server cannot be reached.");
            }
            return;
        }
        if (LoginController.isLoginCall(api) && form != null) {
            form.message(api.getCode().equals("ACCOUNT_DISABLED")
                    ? "This account is disabled. Ask the administrator to enable it."
                    : "Wrong username or password.");
            return;
        }
        switch (api.getStatus()) {
            case 400 -> {
                if (form != null) {
                    form.show(api.getFieldErrors(), "Please correct the highlighted fields.");
                } else {
                    StringBuilder text = new StringBuilder();
                    api.getFieldErrors().forEach(f -> text.append(f.field()).append(" ").append(f.message()).append("\n"));
                    alert(Alert.AlertType.WARNING, "Check the values", text.length() == 0 ? api.getMessage() : text.toString());
                }
            }
            case 401 -> backToLogin("Your session has ended. Please log in again.");
            case 403 -> alert(Alert.AlertType.WARNING, "Not allowed", api.getMessage());
            case 404 -> alert(Alert.AlertType.WARNING, "Not found", api.getMessage());
            case 409 -> alert(Alert.AlertType.WARNING, "Action refused",
                    CONFLICTS.getOrDefault(api.getCode(), "") + "\n\n" + api.getMessage());
            default -> alert(Alert.AlertType.ERROR, "Server error",
                    "The server could not complete the request (" + api.getCode() + "). Try again, and contact the "
                            + "administrator if it keeps happening.");
        }
    }

    public static void backToLogin(String message) {
        SessionContext.clear();
        MainController.forget();
        LoginController.setPendingMessage(message);
        try {
            SceneManager.switchScene("login-view.fxml");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------- dialogs ----------------

    public static void alert(Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type, text, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.initOwner(SceneManager.getStage());
        alert.showAndWait();
    }

    public static void info(String title, String text) {
        alert(Alert.AlertType.INFORMATION, title, text);
    }

    /** Destructive or bulk actions always ask first. */
    public static boolean confirm(String title, String text, String actionLabel) {
        ButtonType action = new ButtonType(actionLabel, ButtonType.OK.getButtonData());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, action, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.initOwner(SceneManager.getStage());
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == action;
    }

    // ---------------- formatting and parsing ----------------

    public static String lbp(BigDecimal amount) {
        return amount == null ? "" : WHOLE.format(amount) + " LBP";
    }

    public static String number(BigDecimal value) {
        return value == null ? "" : WHOLE.format(value);
    }

    public static String kwh(BigDecimal value) {
        return value == null ? "" : ONE_DECIMAL.format(value);
    }

    public static String month(YearMonth month) {
        return month == null ? "" : month.format(MONTH);
    }

    public static String capitalize(String text) {
        return text == null || text.isEmpty() ? "" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Accepts "1,250,000" or "1250000"; returns null for an empty field. */
    public static BigDecimal decimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return new BigDecimal(text.trim().replace(",", "").replace(" ", ""));
    }

    // ---------------- reusable controls ----------------

    /** A month picker (two years back, up to next month) that shares its value with SessionContext. */
    public static void monthPicker(ComboBox<YearMonth> box) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth last = YearMonth.now().plusMonths(1);
        for (int i = 0; i < 27; i++) {
            months.add(last.minusMonths(i));
        }
        box.getItems().setAll(months);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth value) {
                return month(value);
            }

            @Override
            public YearMonth fromString(String text) {
                return null;
            }
        });
        YearMonth selected = SessionContext.getSelectedMonth();
        if (!months.contains(selected)) {
            box.getItems().add(selected);
        }
        box.setValue(selected);
        box.valueProperty().addListener((obs, old, value) -> SessionContext.setSelectedMonth(value));
    }

    /** Text column from a record accessor. */
    public static <S> void column(TableColumn<S, String> column, Function<S, Object> value) {
        column.setCellValueFactory(c -> {
            Object v = value.apply(c.getValue());
            return new SimpleObjectProperty<>(v == null ? "" : v.toString());
        });
    }

    /** Right-aligned figure column that still sorts by the number, not the text. */
    public static <S> void figure(TableColumn<S, BigDecimal> column, Function<S, BigDecimal> value,
                                  Function<BigDecimal, String> format) {
        column.setCellValueFactory(c -> new SimpleObjectProperty<>(value.apply(c.getValue())));
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : format.apply(item));
            }
        });
        column.getStyleClass().add("figure");
    }

    public static void later(Runnable runnable) {
        Platform.runLater(runnable);
    }
}
