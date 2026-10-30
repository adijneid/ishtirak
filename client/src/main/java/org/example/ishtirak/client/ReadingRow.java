package org.example.ishtirak.client;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.example.ishtirak.common.dto.ReadingSheetRow;

/** One editable line of the reading grid: the server's sheet row plus what the collector typed. */
public class ReadingRow {
    public enum State { NORMAL, SAVED, WARNING, REJECTED, LOCKED }

    private final ReadingSheetRow sheet;
    private final StringProperty input = new SimpleStringProperty("");
    private final StringProperty note = new SimpleStringProperty("");
    private final ObjectProperty<State> state = new SimpleObjectProperty<>(State.NORMAL);

    public ReadingRow(ReadingSheetRow sheet) {
        this.sheet = sheet;
        if (sheet.currentReading() != null) {
            input.set(sheet.currentReading().toPlainString());
        }
        if (sheet.invoiced()) {
            state.set(State.LOCKED);
            note.set("Invoiced, locked");
        }
    }

    public ReadingSheetRow getSheet() {
        return sheet;
    }

    public StringProperty inputProperty() {
        return input;
    }

    public StringProperty noteProperty() {
        return note;
    }

    public ObjectProperty<State> stateProperty() {
        return state;
    }

    /** Typed something different from what the server has. */
    public boolean isChanged() {
        String text = input.get() == null ? "" : input.get().trim();
        if (text.isEmpty()) {
            return false;
        }
        if (sheet.currentReading() == null) {
            return true;
        }
        try {
            return Ui.decimal(text).compareTo(sheet.currentReading()) != 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public void mark(State newState, String message) {
        state.set(newState);
        note.set(message);
    }
}
