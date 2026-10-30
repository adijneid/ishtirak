package org.example.ishtirak.server.controller;

import org.example.ishtirak.server.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

class TariffIntegrationTest extends IntegrationTestBase {

    @Test
    void createAndList() {
        createTariff("2026-08", 48241);
        JsonNode list = get("/api/tariffs", adminToken).body();
        assertEquals(1, list.size());
        assertEquals("2026-08", list.get(0).get("billingMonth").asString());
        assertFalse(list.get(0).get("locked").asBoolean());
        assertEquals("2026-08-01", jdbc.queryForObject("SELECT billing_month::text FROM tariff_month", String.class));
    }

    @Test
    void oneTariffPerMonth() {
        int id = createTariff("2026-08", 48241);
        assertError(post("/api/tariffs", adminToken, tariffJson("2026-08", 1)), 409, "DUPLICATE_MONTH");
        createTariff("2026-06", 41973);
        assertError(put("/api/tariffs/" + id, adminToken, tariffJson("2026-06", 1)), 409, "DUPLICATE_MONTH");
    }

    @Test
    void pricesMustBePositive() {
        assertError(post("/api/tariffs", adminToken, tariffJson("2026-08", 0)), 400, "VALIDATION_ERROR");
        assertError(post("/api/tariffs", adminToken,
                "{\"billingMonth\":\"2026-08\",\"pricePerKwh\":1,\"fee5a\":-1,\"fee10a\":1,\"feePerExtra5a\":1,\"ruralUpliftPct\":10}"),
                400, "VALIDATION_ERROR");
    }

    @Test
    void upliftMustBeBetween0And100() {
        String base = "{\"billingMonth\":\"2026-08\",\"pricePerKwh\":1,\"fee5a\":1,\"fee10a\":1,\"feePerExtra5a\":1,\"ruralUpliftPct\":";
        assertError(post("/api/tariffs", adminToken, base + "100.5}"), 400, "VALIDATION_ERROR");
        assertError(post("/api/tariffs", adminToken, base + "-1}"), 400, "VALIDATION_ERROR");
        assertEquals(201, post("/api/tariffs", adminToken, base + "0}").status());
    }

    @Test
    void editIsAllowedUntilTheMonthIsInvoicedThenLocked() {
        int tariff = createTariff("2026-06", 40000);
        assertEquals(200, put("/api/tariffs/" + tariff, adminToken, tariffJson("2026-06", 41973)).status());

        int zone = createZone("Center", false);
        int sub = createSubscriber("A", "03 000 001", zone);
        int subscription = createSubscription(sub, "M-1", 5, "0", "2026-05-01");
        saveReading(adminToken, "2026-06", subscription, "10");
        runBilling("2026-06");

        assertError(put("/api/tariffs/" + tariff, adminToken, tariffJson("2026-06", 1)), 409, "TARIFF_LOCKED");
        assertTrue(get("/api/tariffs", adminToken).body().get(0).get("locked").asBoolean());
        assertError(put("/api/tariffs/999", adminToken, tariffJson("2026-06", 1)), 404, "NOT_FOUND");
    }
}
