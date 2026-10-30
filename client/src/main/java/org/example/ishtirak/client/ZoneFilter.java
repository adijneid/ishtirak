package org.example.ishtirak.client;

import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.example.ishtirak.common.dto.ZoneResponse;

import java.util.ArrayList;
import java.util.List;

/** Zone ComboBox with an optional "all zones" entry (represented by a zone with a null id). */
public final class ZoneFilter {
    private static final ZoneResponse ALL = new ZoneResponse(null, "", false, 0);

    private ZoneFilter() {
    }

    public static void install(ComboBox<ZoneResponse> box, String allLabel) {
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(ZoneResponse zone) {
                if (zone == null) {
                    return "";
                }
                return zone.id() == null ? allLabel : zone.name() + (zone.isRural() ? " (rural)" : "");
            }

            @Override
            public ZoneResponse fromString(String s) {
                return null;
            }
        });
    }

    public static void fill(ComboBox<ZoneResponse> box, List<ZoneResponse> zones, boolean withAll) {
        List<ZoneResponse> items = new ArrayList<>();
        if (withAll) {
            items.add(ALL);
        }
        items.addAll(zones);
        Integer previous = idOf(box.getValue());
        box.getItems().setAll(items);
        box.setValue(items.stream().filter(z -> java.util.Objects.equals(z.id(), previous)).findFirst()
                .orElse(items.isEmpty() ? null : items.get(0)));
    }

    public static Integer idOf(ZoneResponse zone) {
        return zone == null ? null : zone.id();
    }

    public static ZoneResponse byId(ComboBox<ZoneResponse> box, Integer id) {
        return box.getItems().stream().filter(z -> java.util.Objects.equals(z.id(), id)).findFirst().orElse(null);
    }
}
