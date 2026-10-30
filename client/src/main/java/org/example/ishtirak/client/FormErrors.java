package org.example.ishtirak.client;

import javafx.scene.control.Label;
import org.example.ishtirak.common.dto.FieldErrorResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shows a 400 answer's field messages under the matching inputs of a form. */
public class FormErrors {
    private final Label general;
    private final Map<String, Label> fields = new HashMap<>();

    public FormErrors(Label general) {
        this.general = general;
    }

    public FormErrors field(String name, Label label) {
        fields.put(name, label);
        return this;
    }

    public void clear() {
        if (general != null) {
            general.setText("");
        }
        fields.values().forEach(l -> l.setText(""));
    }

    public void show(List<FieldErrorResponse> errors, String message) {
        clear();
        StringBuilder unmatched = new StringBuilder();
        for (FieldErrorResponse error : errors) {
            Label label = fields.get(error.field());
            if (label != null) {
                label.setText(Ui.capitalize(error.message()));
            } else {
                unmatched.append(error.field()).append(" ").append(error.message()).append(". ");
            }
        }
        if (general != null) {
            general.setText(unmatched.length() > 0 ? unmatched.toString().trim() : message);
        }
    }

    /** A check done on the client before calling the server (e.g. a number that does not parse). */
    public void showField(String name, String message) {
        clear();
        Label label = fields.get(name);
        if (label != null) {
            label.setText(message);
        } else if (general != null) {
            general.setText(message);
        }
    }

    public void message(String text) {
        if (general != null) {
            general.setText(text);
        }
    }
}
