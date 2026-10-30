package org.example.ishtirak.server.controller;

import org.example.ishtirak.server.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void loginReturnsTokenRoleAndFullName() {
        Response r = post("/api/auth/login", null, "{\"username\":\"admin\",\"password\":\"admin123\"}");
        assertEquals(200, r.status());
        assertFalse(r.body().get("token").asString().isBlank());
        assertEquals("ADMIN", r.body().get("role").asString());
        assertEquals("Operator Admin", r.body().get("fullName").asString());
    }

    @Test
    void bothFieldsAreRequired() {
        Response r = post("/api/auth/login", null, "{\"username\":\"\",\"password\":\"\"}");
        assertError(r, 400, "VALIDATION_ERROR");
        assertEquals(2, r.body().get("fieldErrors").size());
    }

    @Test
    void wrongPasswordAndUnknownUserAre401() {
        assertError(post("/api/auth/login", null, "{\"username\":\"admin\",\"password\":\"nope\"}"), 401, "INVALID_CREDENTIALS");
        assertError(post("/api/auth/login", null, "{\"username\":\"ghost\",\"password\":\"x\"}"), 401, "INVALID_CREDENTIALS");
    }

    @Test
    void disabledAccountIs403() {
        assertError(post("/api/auth/login", null, "{\"username\":\"former\",\"password\":\"disabled123\"}"), 403, "ACCOUNT_DISABLED");
    }

    @Test
    void disablingAnAccountBlocksItsExistingToken() {
        jdbc.update("UPDATE app_user SET active = FALSE WHERE username = 'collector'");
        assertError(get("/api/zones", collectorToken), 403, "ACCOUNT_DISABLED");
    }

    @Test
    void missingOrInvalidTokenIs401() {
        assertError(get("/api/zones", null), 401, "UNAUTHORIZED");
        assertError(get("/api/zones", "garbage"), 401, "INVALID_TOKEN");
        String tampered = adminToken.substring(0, adminToken.length() - 2) + "xx";
        assertError(get("/api/zones", tampered), 401, "INVALID_TOKEN");
    }

    @Test
    void collectorCanUseOnlyItsServices() {
        // allowed (proposal: login, subscriber reads, reading sheet, save readings, invoices, payments)
        int zone = createZone("Z", false);
        assertEquals(200, get("/api/subscribers", collectorToken).status());
        assertEquals(200, get("/api/readings/sheet?month=2026-08&zoneId=" + zone, collectorToken).status());
        assertEquals(200, post("/api/readings/batch", collectorToken, "{\"month\":\"2026-08\",\"entries\":[]}").status());
        assertEquals(200, get("/api/invoices", collectorToken).status());
        assertEquals(200, get("/api/zones", collectorToken).status());

        // admin only
        assertError(post("/api/zones", collectorToken, "{\"name\":\"X\"}"), 403, "FORBIDDEN");
        assertError(get("/api/tariffs", collectorToken), 403, "FORBIDDEN");
        assertError(post("/api/billing/runs", collectorToken, "{\"month\":\"2026-08\"}"), 403, "FORBIDDEN");
        assertError(get("/api/reports/dashboard?month=2026-08", collectorToken), 403, "FORBIDDEN");
        assertError(get("/api/reports/arrears", collectorToken), 403, "FORBIDDEN");
        assertError(post("/api/subscribers", collectorToken, "{}"), 403, "FORBIDDEN");
        assertError(post("/api/subscriptions", collectorToken, "{}"), 403, "FORBIDDEN");
        assertError(post("/api/invoices/1/void", collectorToken, "{\"reason\":\"x\"}"), 403, "FORBIDDEN");
    }
}
