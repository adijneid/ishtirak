package org.example.ishtirak.server.controller;

import org.example.ishtirak.server.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

class SubscriberIntegrationTest extends IntegrationTestBase {

    @Test
    void createIsStoredAndReadBack() {
        int zone = createZone("Center", false);
        int id = createSubscriber("Rita Khoury", "70 222 302", zone);
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM subscriber WHERE subscriber_id = ?", String.class, id));
        JsonNode detail = get("/api/subscribers/" + id, collectorToken).body();
        assertEquals("Rita Khoury", detail.get("subscriber").get("fullName").asString());
        assertEquals("Center", detail.get("subscriber").get("zoneName").asString());
        assertEquals(0, detail.get("subscriber").get("balance").decimalValue().signum());
    }

    @Test
    void requiredFieldsAndPhoneFormat() {
        int zone = createZone("Center", false);
        Response r = post("/api/subscribers", adminToken, "{\"fullName\":\"\",\"phone\":\"abc\",\"zoneId\":" + zone + "}");
        assertError(r, 400, "VALIDATION_ERROR");
        assertEquals(2, r.body().get("fieldErrors").size());
        assertError(post("/api/subscribers", adminToken, "{\"fullName\":\"A\",\"phone\":\"03 123456\"}"), 400, "VALIDATION_ERROR");
    }

    @Test
    void duplicatePhoneIsRejected() {
        int zone = createZone("Center", false);
        createSubscriber("A", "03 123 456", zone);
        assertError(post("/api/subscribers", adminToken,
                "{\"fullName\":\"B\",\"phone\":\"03 123 456\",\"zoneId\":" + zone + "}"), 409, "DUPLICATE_PHONE");
        int b = createSubscriber("B", "03 999 999", zone);
        assertError(put("/api/subscribers/" + b, adminToken,
                "{\"fullName\":\"B\",\"phone\":\"03 123 456\",\"zoneId\":" + zone + "}"), 409, "DUPLICATE_PHONE");
    }

    @Test
    void unknownZoneIsRejected() {
        assertError(post("/api/subscribers", adminToken,
                "{\"fullName\":\"A\",\"phone\":\"03 123 456\",\"zoneId\":999}"), 404, "NOT_FOUND");
        assertError(get("/api/subscribers?zoneId=999", adminToken), 404, "NOT_FOUND");
        assertError(get("/api/subscribers/999", adminToken), 404, "NOT_FOUND");
    }

    @Test
    void filtersSearchAndPaging() {
        int center = createZone("Center", false);
        int ehden = createZone("Ehden", true);
        createSubscriber("Georges Haddad", "03 111 201", center);
        createSubscriber("Rita Khoury", "70 222 302", center);
        createSubscriber("Maya Douaihy", "03 444 504", ehden);

        assertEquals(2, get("/api/subscribers?zoneId=" + center, adminToken).body().get("totalElements").asInt());
        assertEquals("Rita Khoury", get("/api/subscribers?search=KHOURY", adminToken).body()
                .get("content").get(0).get("fullName").asString());
        assertEquals(1, get("/api/subscribers?search=444", adminToken).body().get("totalElements").asInt());
        JsonNode page = get("/api/subscribers?size=2&page=1", adminToken).body();
        assertEquals(1, page.get("content").size());
        assertEquals(2, page.get("totalPages").asInt());
        assertError(get("/api/subscribers?status=GONE", adminToken), 400, "VALIDATION_ERROR");
    }

    @Test
    void cannotCloseWithAnActiveSubscription() {
        int zone = createZone("Center", false);
        int sub = createSubscriber("A", "03 000 001", zone);
        createSubscription(sub, "M-1", 5, "0", "2026-05-01");
        assertError(patch("/api/subscribers/" + sub + "/close", adminToken, null), 409, "HAS_OPEN_ITEMS");
    }

    @Test
    void cannotCloseWithAnOutstandingBalanceThenClosesOncePaid() {
        int zone = createZone("Center", false);
        int sub = createSubscriber("A", "03 000 001", zone);
        int subscription = createSubscription(sub, "M-1", 5, "0", "2026-05-01");
        createTariff("2026-06", 48241);
        saveReading(adminToken, "2026-06", subscription, "10");
        runBilling("2026-06");
        assertEquals(200, patch("/api/subscriptions/" + subscription + "/end", adminToken, "{\"endDate\":\"2026-06-30\"}").status());

        assertError(patch("/api/subscribers/" + sub + "/close", adminToken, null), 409, "HAS_OPEN_ITEMS");

        JsonNode invoice = invoicesOf(sub).get(0);
        post("/api/invoices/" + invoice.get("id").asInt() + "/payments", adminToken,
                "{\"amount\":" + invoice.get("balance").asLong() + ",\"method\":\"CASH\"}");
        Response closed = patch("/api/subscribers/" + sub + "/close", adminToken, null);
        assertEquals(200, closed.status());
        assertEquals("CLOSED", closed.body().get("status").asString());
        assertEquals("CLOSED", jdbc.queryForObject("SELECT status FROM subscriber WHERE subscriber_id = ?", String.class, sub));
    }
}
