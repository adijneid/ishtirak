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

class ReportIntegrationTest extends IntegrationTestBase {

    @Test
    void billedMinusCollectedEqualsOutstandingPerMonthAndZone() {
        int center = createZone("Center", false);
        int ehden = createZone("Ehden", true);
        int a = createSubscriber("A", "03 000 001", center);
        int b = createSubscriber("B", "03 000 002", ehden);
        int c = createSubscriber("C", "03 000 003", ehden);
        int ma = createSubscription(a, "M-A", 5, "0", "2026-05-01");
        int mb = createSubscription(b, "M-B", 10, "0", "2026-05-01");
        createSubscription(c, "M-C", 10, "0", "2026-05-01");   // no reading -> missing
        createTariff("2026-06", 41973);
        saveReading(collectorToken, "2026-06", ma, "10");
        saveReading(collectorToken, "2026-06", mb, "20");
        runBilling("2026-06");

        JsonNode invA = invoicesOf(a).get(0);
        post("/api/invoices/" + invA.get("id").asInt() + "/payments", collectorToken, "{\"amount\":100000,\"method\":\"CASH\"}");

        JsonNode d = get("/api/reports/dashboard?month=2026-06", adminToken).body();
        BigDecimal billed = d.get("billed").decimalValue();
        BigDecimal collected = d.get("collected").decimalValue();
        assertEquals(0, billed.subtract(collected).compareTo(d.get("outstanding").decimalValue()));
        // A: 10 x 41,973 + 385,000 = 804,730 ; B rural: 20 x 41,973 x 1.1 + 685,000 = 1,608,406
        assertEquals(0, new BigDecimal("2413136").compareTo(billed));
        assertEquals(0, new BigDecimal("100000").compareTo(collected));
        assertEquals(1, d.get("missingReadings").asLong());
        assertEquals(0, new BigDecimal("4.1").compareTo(d.get("collectionRate").decimalValue()));
        // the same totals, straight from SQL
        assertEquals(0, billed.compareTo(jdbc.queryForObject(
                "SELECT SUM(amount_due) FROM invoice WHERE voided = FALSE", BigDecimal.class)));

        JsonNode zones = d.get("zones");
        assertEquals(2, zones.size());
        BigDecimal zoneBilled = BigDecimal.ZERO;
        for (JsonNode z : zones) {
            zoneBilled = zoneBilled.add(z.get("billed").decimalValue());
        }
        assertEquals(0, billed.compareTo(zoneBilled));

        JsonNode arrears = get("/api/reports/arrears", adminToken).body();
        assertEquals(2, arrears.size());
        assertEquals("B", arrears.get(0).get("subscriber").asString());   // sorted by balance, largest first
        assertEquals(0, new BigDecimal("704730").compareTo(arrears.get(1).get("balance").decimalValue()));
        assertEquals(1, get("/api/reports/arrears?zoneId=" + center, adminToken).body().size());
        assertError(get("/api/reports/arrears?zoneId=999", adminToken), 404, "NOT_FOUND");
    }

    @Test
    void dashboardRequiresAMonth() {
        assertError(get("/api/reports/dashboard", adminToken), 400, "VALIDATION_ERROR");
        JsonNode empty = get("/api/reports/dashboard?month=2026-01", adminToken).body();
        assertEquals(0, empty.get("billed").decimalValue().signum());
        assertEquals(0, empty.get("collectionRate").decimalValue().signum());
    }
}
