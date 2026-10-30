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

class PaymentIntegrationTest extends IntegrationTestBase {

    private int subscriber;
    private int invoice;
    private long amountDue;

    /** One 10 A invoice of 100 kWh in June 2026: 100 x 41,973 + 685,000 = 4,882,300. */
    private void setUpInvoice() {
        int zone = createZone("Center", false);
        subscriber = createSubscriber("A", "03 000 001", zone);
        int subscription = createSubscription(subscriber, "M-1", 10, "0", "2026-05-01");
        createTariff("2026-06", 41973);
        saveReading(collectorToken, "2026-06", subscription, "100");
        runBilling("2026-06");
        JsonNode inv = invoicesOf(subscriber).get(0);
        invoice = inv.get("id").asInt();
        amountDue = inv.get("amountDue").asLong();
        assertEquals(4882300, amountDue);
    }

    private Response pay(String token, long amount, String method) {
        return post("/api/invoices/" + invoice + "/payments", token, "{\"amount\":" + amount + ",\"method\":\"" + method + "\"}");
    }

    @Test
    void cashAndTransferAreAcceptedAndBalanceIsUpdated() {
        setUpInvoice();
        Response cash = pay(collectorToken, 1000000, "CASH");
        assertEquals(201, cash.status());
        assertEquals(3882300, cash.body().get("newBalance").asLong());
        assertTrue(cash.body().get("receiptNo").asString().matches("RC\\d{6}-\\d{6}"));
        assertEquals("PARTIAL", invoicesOf(subscriber).get(0).get("status").asString());

        Response transfer = pay(adminToken, 3882300, "TRANSFER");
        assertEquals(201, transfer.status());
        assertEquals(0, transfer.body().get("newBalance").asLong());
        assertNotEquals(cash.body().get("receiptNo").asString(), transfer.body().get("receiptNo").asString());

        JsonNode inv = invoicesOf(subscriber).get(0);
        assertEquals("PAID", inv.get("status").asString());
        assertEquals(0, inv.get("balance").asLong());
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM payment", Integer.class));
        assertEquals(2, get("/api/invoices/" + invoice + "/payments", collectorToken).body().size());
    }

    @Test
    void paymentCannotExceedTheBalance() {
        setUpInvoice();
        assertError(pay(collectorToken, amountDue + 1, "CASH"), 409, "OVERPAYMENT");
        pay(collectorToken, amountDue - 10, "CASH");
        assertError(pay(collectorToken, 11, "CASH"), 409, "OVERPAYMENT");
        assertEquals(201, pay(collectorToken, 10, "CASH").status());
    }

    @Test
    void amountMustBePositiveWholeAndMethodValid() {
        setUpInvoice();
        assertError(pay(collectorToken, 0, "CASH"), 400, "VALIDATION_ERROR");
        assertError(pay(collectorToken, -5, "CASH"), 400, "VALIDATION_ERROR");
        assertError(pay(collectorToken, 10, "CHEQUE"), 400, "VALIDATION_ERROR");
        assertError(post("/api/invoices/" + invoice + "/payments", collectorToken, "{\"amount\":10.5,\"method\":\"CASH\"}"),
                400, "VALIDATION_ERROR");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM payment", Integer.class));
    }

    @Test
    void voidInvoiceCannotBePaidAndUnknownInvoiceIs404() {
        setUpInvoice();
        assertEquals(200, post("/api/invoices/" + invoice + "/void", adminToken, "{\"reason\":\"duplicate meter\"}").status());
        assertError(pay(collectorToken, 1000, "CASH"), 409, "INVOICE_VOID");
        assertError(post("/api/invoices/999/payments", collectorToken, "{\"amount\":1,\"method\":\"CASH\"}"), 404, "NOT_FOUND");
    }

    @Test
    void voidRules() {
        setUpInvoice();
        assertError(post("/api/invoices/" + invoice + "/void", adminToken, "{\"reason\":\"\"}"), 400, "VALIDATION_ERROR");
        pay(collectorToken, 1000, "CASH");
        assertError(post("/api/invoices/" + invoice + "/void", adminToken, "{\"reason\":\"x\"}"), 409, "INVOICE_HAS_PAYMENTS");
        assertError(post("/api/invoices/999/void", adminToken, "{\"reason\":\"x\"}"), 404, "NOT_FOUND");
    }

    @Test
    void voidingTwiceIsRejected() {
        setUpInvoice();
        post("/api/invoices/" + invoice + "/void", adminToken, "{\"reason\":\"x\"}");
        assertError(post("/api/invoices/" + invoice + "/void", adminToken, "{\"reason\":\"x\"}"), 409, "INVOICE_VOID");
    }

    @Test
    void twoSimultaneousFullPaymentsOnlyOneSucceeds() throws Exception {
        setUpInvoice();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Integer> payAll = () -> pay(collectorToken, amountDue, "CASH").status();
        List<Integer> statuses = pool.invokeAll(List.of(payAll, payAll)).stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).sorted().toList();
        pool.shutdown();
        assertEquals(List.of(201, 409), statuses);
        assertEquals(0, new BigDecimal(amountDue).compareTo(
                jdbc.queryForObject("SELECT SUM(amount) FROM payment", BigDecimal.class)));
    }
}
