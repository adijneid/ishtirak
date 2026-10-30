package org.example.ishtirak.server.controller;

import org.example.ishtirak.server.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

class ZoneIntegrationTest extends IntegrationTestBase {

    @Test
    void createListUpdateDelete() {
        int id = createZone("Ehden", true);
        JsonNode list = get("/api/zones", adminToken).body();
        assertEquals(1, list.size());
        assertTrue(list.get(0).get("isRural").asBoolean());

        Response updated = put("/api/zones/" + id, adminToken, "{\"name\":\"Ehden Upper\",\"isRural\":false}");
        assertEquals(200, updated.status());
        assertEquals("Ehden Upper", updated.body().get("name").asString());
        assertEquals("Ehden Upper", jdbc.queryForObject("SELECT name FROM zone WHERE zone_id = ?", String.class, id));

        Response deleted = delete("/api/zones/" + id, adminToken);
        assertEquals(204, deleted.status());
        assertNull(deleted.body());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM zone", Integer.class));
    }

    @Test
    void nameIsRequiredAndUnique() {
        assertError(post("/api/zones", adminToken, "{\"name\":\" \"}"), 400, "VALIDATION_ERROR");
        createZone("Ardeh", true);
        assertError(post("/api/zones", adminToken, "{\"name\":\"ardeh\"}"), 409, "DUPLICATE_NAME");
        int other = createZone("Miziara", false);
        assertError(put("/api/zones/" + other, adminToken, "{\"name\":\"Ardeh\"}"), 409, "DUPLICATE_NAME");
    }

    @Test
    void unknownIdIs404() {
        assertError(put("/api/zones/999", adminToken, "{\"name\":\"X\"}"), 404, "NOT_FOUND");
        assertError(delete("/api/zones/999", adminToken), 404, "NOT_FOUND");
    }

    @Test
    void zoneWithSubscribersCannotBeDeleted() {
        int zone = createZone("Center", false);
        createSubscriber("A", "03 000 001", zone);
        assertError(delete("/api/zones/" + zone, adminToken), 409, "ZONE_IN_USE");
    }
}
