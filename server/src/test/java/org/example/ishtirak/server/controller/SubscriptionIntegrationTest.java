package org.example.ishtirak.server.controller;

import org.example.ishtirak.server.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionIntegrationTest extends IntegrationTestBase {

    private int subscriber;

    private int newSubscriber() {
        int zone = createZone("Center", false);
        return createSubscriber("A", "03 000 001", zone);
    }

    @Test
    void amperageMustBeFiveToSixty() {
        subscriber = newSubscriber();
        assertError(post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "M-1", 0, "0", "2026-05-01")), 400, "VALIDATION_ERROR");
        assertError(post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "M-1", 65, "0", "2026-05-01")), 400, "VALIDATION_ERROR");
        assertEquals(201, post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "M-1", 60, "0", "2026-05-01")).status());
    }

    @Test
    void amperageMustBeAMultipleOfFive() {
        subscriber = newSubscriber();
        Response r = post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "M-1", 12, "0", "2026-05-01"));
        assertError(r, 400, "VALIDATION_ERROR");
        assertEquals("amperage", r.body().get("fieldErrors").get(0).get("field").asString());
    }

    @Test
    void initialReadingCannotBeNegativeAndFieldsAreRequired() {
        subscriber = newSubscriber();
        assertError(post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "M-1", 10, "-1", "2026-05-01")), 400, "VALIDATION_ERROR");
        assertError(post("/api/subscriptions", adminToken, "{\"subscriberId\":" + subscriber + "}"), 400, "VALIDATION_ERROR");
    }

    @Test
    void meterSerialMustBeUnique() {
        subscriber = newSubscriber();
        createSubscription(subscriber, "MTR-1", 10, "0", "2026-05-01");
        assertError(post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "mtr-1", 10, "0", "2026-05-01")), 409, "DUPLICATE_METER");
    }

    @Test
    void subscriberMustExistAndBeActive() {
        assertError(post("/api/subscriptions", adminToken, subscriptionJson(999, "M-1", 10, "0", "2026-05-01")), 404, "NOT_FOUND");
        subscriber = newSubscriber();
        assertEquals(200, patch("/api/subscribers/" + subscriber + "/close", adminToken, null).status());
        assertError(post("/api/subscriptions", adminToken, subscriptionJson(subscriber, "M-1", 10, "0", "2026-05-01")), 409, "SUBSCRIBER_CLOSED");
    }

    @Test
    void endDateCannotBeBeforeStartOrLastReading() {
        subscriber = newSubscriber();
        int id = createSubscription(subscriber, "M-1", 10, "0", "2026-05-01");
        assertError(patch("/api/subscriptions/" + id + "/end", adminToken, "{\"endDate\":\"2026-04-30\"}"), 409, "INVALID_END_DATE");
        saveReading(adminToken, "2026-07", id, "50");
        assertError(patch("/api/subscriptions/" + id + "/end", adminToken, "{\"endDate\":\"2026-06-15\"}"), 409, "INVALID_END_DATE");
        assertError(patch("/api/subscriptions/" + id + "/end", adminToken, "{}"), 400, "VALIDATION_ERROR");

        Response ok = patch("/api/subscriptions/" + id + "/end", adminToken, "{\"endDate\":\"2026-07-31\"}");
        assertEquals(200, ok.status());
        assertFalse(ok.body().get("active").asBoolean());
        assertEquals("2026-07-31", jdbc.queryForObject("SELECT end_date::text FROM subscription WHERE subscription_id = ?", String.class, id));
    }

    @Test
    void amperageChangeIsSavedButBaseCannotChangeOnceReadingsExist() {
        subscriber = newSubscriber();
        int id = createSubscription(subscriber, "M-1", 10, "0", "2026-05-01");
        saveReading(adminToken, "2026-06", id, "50");
        Response changed = put("/api/subscriptions/" + id, adminToken, subscriptionJson(subscriber, "M-1", 20, "0", "2026-05-01"));
        assertEquals(200, changed.status());
        assertEquals(20, changed.body().get("amperage").asInt());
        assertError(put("/api/subscriptions/" + id, adminToken, subscriptionJson(subscriber, "M-1", 20, "5", "2026-05-01")),
                409, "SUBSCRIPTION_HAS_READINGS");
    }
}
