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

class BillingIntegrationTest extends IntegrationTestBase {

    private int center;
    private int ehden;
    private int georges;
    private int antoine;
    private int georgesMeter;
    private int antoineMeter;

    /** Same data as the demo seed, August 2026 tariff. */
    private void setUpAugust() {
        center = createZone("Zgharta Center", false);
        ehden = createZone("Ehden", true);
        georges = createSubscriber("Georges Haddad", "03 111 201", center);
        antoine = createSubscriber("Antoine Karam", "71 555 605", ehden);
        georgesMeter = createSubscription(georges, "MTR-ZG-0001", 5, "1200.0", "2026-07-01");
        antoineMeter = createSubscription(antoine, "MTR-EH-0002", 20, "2500.0", "2026-07-01");
        saveReading(collectorToken, "2026-07", georgesMeter, "1262.0");
        saveReading(collectorToken, "2026-07", antoineMeter, "2800.0");
        saveReading(collectorToken, "2026-08", georgesMeter, "1330.0");
        saveReading(collectorToken, "2026-08", antoineMeter, "3120.0");
        createTariff("2026-08", 48241);
    }

    @Test
    void runCreatesInvoicesWithTheMinistryFormula() {
        setUpAugust();
        Response run = runBilling("2026-08");
        assertEquals(200, run.status());
        assertEquals(2, run.body().get("created").asInt());
        assertEquals(0, new BigDecimal("21931220").compareTo(run.body().get("totalBilled").decimalValue()));

        // 68 kWh non-rural 5 A, and 320 kWh rural 20 A
        assertEquals(0, new BigDecimal("3665388").compareTo(invoicesOf(georges).get(0).get("amountDue").decimalValue()));
        JsonNode antoineInvoice = invoicesOf(antoine).get(0);
        assertEquals(0, new BigDecimal("18265832").compareTo(antoineInvoice.get("amountDue").decimalValue()));
        assertEquals(0, new BigDecimal("320.0").compareTo(antoineInvoice.get("kwh").decimalValue()));
        assertEquals("UNPAID", antoineInvoice.get("status").asString());
        assertEquals(0, new BigDecimal("18265832").compareTo(
                jdbc.queryForObject("SELECT amount_due FROM invoice WHERE invoice_id = ?", BigDecimal.class,
                        antoineInvoice.get("id").asInt())));
    }

    @Test
    void runningTheSameMonthTwiceCreatesNoDuplicates() {
        setUpAugust();
        assertEquals(2, runBilling("2026-08").body().get("created").asInt());
        JsonNode second = runBilling("2026-08").body();
        assertEquals(0, second.get("created").asInt());
        assertEquals(2, second.get("skipped").asInt());
        assertEquals(0, second.get("totalBilled").decimalValue().signum());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM invoice", Integer.class));
    }

    @Test
    void partialUniqueIndexBlocksADuplicateEvenOutsideTheService() {
        setUpAugust();
        runBilling("2026-08");
        Integer readingId = jdbc.queryForObject("SELECT reading_id FROM invoice LIMIT 1", Integer.class);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO invoice (reading_id, consumption_kwh, amount_due) VALUES (?, 1, 1)", readingId));
    }

    @Test
    void missingTariffIs404AndMonthIsRequired() {
        setUpAugust();
        assertError(runBilling("2026-07"), 404, "TARIFF_NOT_FOUND");
        assertError(post("/api/billing/runs", adminToken, "{}"), 400, "VALIDATION_ERROR");
    }

    @Test
    void subscriptionsWithoutAReadingAreListedAsMissing() {
        setUpAugust();
        int nadine = createSubscriber("Nadine Fenianos", "81 666 706", ehden);
        createSubscription(nadine, "MTR-AR-0001", 5, "95.0", "2026-07-01");
        JsonNode run = runBilling("2026-08").body();
        assertEquals(2, run.get("created").asInt());
        assertEquals(1, run.get("missingReadings").size());
        assertEquals("MTR-AR-0001", run.get("missingReadings").get(0).get("meterSerial").asString());
    }

    @Test
    void voidedInvoiceIsReissuedOnTheNextRunAndKeepsItsSnapshot() {
        setUpAugust();
        runBilling("2026-08");
        int invoice = invoicesOf(georges).get(0).get("id").asInt();
        Response voided = post("/api/invoices/" + invoice + "/void", adminToken, "{\"reason\":\"wrong reading\"}");
        assertEquals("VOID", voided.body().get("status").asString());

        // the reading is unlocked again, the operator fixes it and re-runs
        assertEquals(1, saveReading(collectorToken, "2026-08", georgesMeter, "1300.0").body().get("saved").asInt());
        assertEquals(1, runBilling("2026-08").body().get("created").asInt());
        JsonNode list = invoicesOf(georges);
        assertEquals(2, list.size());
        // 38 kWh x 48,241 = 1,833,158 + 385,000
        assertTrue(list.findValues("amountDue").stream().anyMatch(v -> v.decimalValue().compareTo(new BigDecimal("2218158")) == 0));
        assertEquals("wrong reading", jdbc.queryForObject("SELECT void_reason FROM invoice WHERE invoice_id = ?", String.class, invoice));
    }

    @Test
    void ruralFlagChangeDoesNotAlterIssuedInvoices() {
        setUpAugust();
        runBilling("2026-08");
        put("/api/zones/" + ehden, adminToken, "{\"name\":\"Ehden\",\"isRural\":false}");
        assertEquals(0, new BigDecimal("18265832").compareTo(invoicesOf(antoine).get(0).get("amountDue").decimalValue()));
    }

    @Test
    void invoiceFiltersAndErrors() {
        setUpAugust();
        runBilling("2026-08");
        assertEquals(2, get("/api/invoices?month=2026-08", collectorToken).body().size());
        assertEquals(0, get("/api/invoices?month=2026-07", collectorToken).body().size());
        assertEquals(2, get("/api/invoices?status=UNPAID", collectorToken).body().size());
        assertError(get("/api/invoices?status=LATE", collectorToken), 400, "VALIDATION_ERROR");
        assertError(get("/api/invoices?subscriberId=999", collectorToken), 404, "NOT_FOUND");
    }
}
