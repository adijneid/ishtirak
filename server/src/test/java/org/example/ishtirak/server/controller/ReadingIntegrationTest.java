package org.example.ishtirak.server.controller;

import org.example.ishtirak.server.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReadingIntegrationTest extends IntegrationTestBase {

    private int zone;
    private int subscription;

    private void setUpMeter() {
        zone = createZone("Center", false);
        int sub = createSubscriber("A", "03 000 001", zone);
        subscription = createSubscription(sub, "M-1", 10, "100.0", "2026-05-01");
    }

    @Test
    void sheetShowsInitialReadingFirstThenThePreviousReading() {
        setUpMeter();
        JsonNode may = get("/api/readings/sheet?month=2026-05&zoneId=" + zone, collectorToken).body();
        assertEquals(1, may.size());
        assertEquals(0, new BigDecimal("100.0").compareTo(may.get(0).get("previousReading").decimalValue()));
        assertTrue(may.get(0).get("currentReading").isNull());

        assertEquals(1, saveReading(collectorToken, "2026-05", subscription, "150.5").body().get("saved").asInt());
        JsonNode june = get("/api/readings/sheet?month=2026-06&zoneId=" + zone, collectorToken).body();
        assertEquals(0, new BigDecimal("150.5").compareTo(june.get(0).get("previousReading").decimalValue()));
        // stored in PostgreSQL with the collector as reader
        assertEquals("collector", jdbc.queryForObject(
                "SELECT u.username FROM meter_reading r JOIN app_user u ON u.user_id = r.read_by", String.class));
    }

    @Test
    void sheetRequiresMonthAndAnExistingZone() {
        setUpMeter();
        assertError(get("/api/readings/sheet?zoneId=" + zone, collectorToken), 400, "VALIDATION_ERROR");
        assertError(get("/api/readings/sheet?month=2026-05", collectorToken), 400, "VALIDATION_ERROR");
        assertError(get("/api/readings/sheet?month=2026-05&zoneId=999", collectorToken), 404, "NOT_FOUND");
    }

    @Test
    void readingBelowPreviousIsRejected() {
        setUpMeter();
        JsonNode result = saveReading(collectorToken, "2026-05", subscription, "99.9").body();
        assertEquals(0, result.get("saved").asInt());
        assertEquals("READING_BELOW_PREVIOUS", result.get("rejected").get(0).get("code").asString());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM meter_reading", Integer.class));
    }

    @Test
    void futureMonthIsRejected() {
        setUpMeter();
        String next = YearMonth.now().plusMonths(1).toString();
        assertError(saveReading(collectorToken, next, subscription, "200"), 400, "VALIDATION_ERROR");
        assertError(post("/api/readings/batch", collectorToken, "{\"entries\":[]}"), 400, "VALIDATION_ERROR");
    }

    @Test
    void inactiveSubscriptionIsRejected() {
        setUpMeter();
        // before the start date
        assertEquals("INACTIVE", saveReading(collectorToken, "2026-04", subscription, "120").body()
                .get("rejected").get(0).get("code").asString());
        // after the end date
        patch("/api/subscriptions/" + subscription + "/end", adminToken, "{\"endDate\":\"2026-05-31\"}");
        assertEquals("INACTIVE", saveReading(collectorToken, "2026-06", subscription, "120").body()
                .get("rejected").get(0).get("code").asString());
        assertEquals("NOT_FOUND", saveReading(collectorToken, "2026-06", 999, "120").body()
                .get("rejected").get(0).get("code").asString());
    }

    @Test
    void readingCanBeCorrectedUntilInvoicedThenIsLocked() {
        setUpMeter();
        saveReading(collectorToken, "2026-06", subscription, "150");
        assertEquals(1, saveReading(collectorToken, "2026-06", subscription, "160").body().get("saved").asInt());
        assertEquals(0, new BigDecimal("160.0").compareTo(
                jdbc.queryForObject("SELECT reading_value FROM meter_reading", BigDecimal.class)));

        createTariff("2026-06", 48241);
        runBilling("2026-06");
        JsonNode locked = saveReading(collectorToken, "2026-06", subscription, "170").body();
        assertEquals("READING_LOCKED", locked.get("rejected").get(0).get("code").asString());
        assertEquals(0, new BigDecimal("160.0").compareTo(
                jdbc.queryForObject("SELECT reading_value FROM meter_reading", BigDecimal.class)));
        assertTrue(get("/api/readings/sheet?month=2026-06&zoneId=" + zone, collectorToken).body().get(0).get("invoiced").asBoolean());
    }

    @Test
    void highConsumptionIsAWarningNotARejection() {
        setUpMeter();
        saveReading(collectorToken, "2026-05", subscription, "200");   // 100 kWh
        saveReading(collectorToken, "2026-06", subscription, "300");   // 100 kWh
        JsonNode normal = saveReading(collectorToken, "2026-07", subscription, "400").body();   // 100 kWh
        assertEquals(0, normal.get("warnings").size());
        JsonNode high = saveReading(collectorToken, "2026-08", subscription, "750").body();     // 350 kWh > 3 x 100
        assertEquals(1, high.get("saved").asInt());
        assertEquals("HIGH_CONSUMPTION", high.get("warnings").get(0).get("code").asString());
    }

    @Test
    void validRowsAreSavedEvenIfOthersFail() {
        setUpMeter();
        int sub2 = createSubscriber("B", "03 000 002", zone);
        int second = createSubscription(sub2, "M-2", 5, "0", "2026-05-01");
        JsonNode result = post("/api/readings/batch", collectorToken, "{\"month\":\"2026-05\",\"entries\":["
                + "{\"subscriptionId\":" + subscription + ",\"value\":50},"
                + "{\"subscriptionId\":" + second + ",\"value\":40},"
                + "{\"subscriptionId\":" + second + ",\"value\":-3}]}").body();
        assertEquals(1, result.get("saved").asInt());
        assertEquals(2, result.get("rejected").size());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM meter_reading", Integer.class));
    }
}
