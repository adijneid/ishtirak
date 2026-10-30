package org.example.ishtirak.server;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Starts the real server on a random port, connected to the PostgreSQL test database (ishtirak_test),
 * and calls it over HTTP with JSON exactly like the JavaFX client does.
 * Every test starts from empty business tables; the users of V2__seed_users.sql stay.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    @Value("${local.server.port}")
    protected int port;

    @Autowired
    protected JdbcTemplate jdbc;

    protected final HttpClient http = HttpClient.newHttpClient();
    protected final JsonMapper json = JsonMapper.builder().build();

    protected String adminToken;
    protected String collectorToken;

    protected record Response(int status, JsonNode body) {
        String code() {
            return body.get("code").asString();
        }
    }

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE payment, invoice, meter_reading, tariff_month, subscription, subscriber, zone "
                + "RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE app_user SET active = (username <> 'former')");
        adminToken = login("admin", "admin123");
        collectorToken = login("collector", "collector123");
    }

    // ---------- HTTP helpers ----------

    protected Response call(String method, String path, String token, Object body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json");
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            HttpRequest.BodyPublisher publisher = body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body instanceof String s ? s : json.writeValueAsString(body));
            HttpResponse<String> response = http.send(builder.method(method, publisher).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode node = response.body().isBlank() ? null : json.readTree(response.body());
            return new Response(response.statusCode(), node);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected Response get(String path, String token) {
        return call("GET", path, token, null);
    }

    protected Response post(String path, String token, Object body) {
        return call("POST", path, token, body);
    }

    protected Response put(String path, String token, Object body) {
        return call("PUT", path, token, body);
    }

    protected Response patch(String path, String token, Object body) {
        return call("PATCH", path, token, body);
    }

    protected Response delete(String path, String token) {
        return call("DELETE", path, token, null);
    }

    protected String login(String username, String password) {
        Response r = post("/api/auth/login", null, "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertEquals(200, r.status(), "login " + username);
        return r.body().get("token").asString();
    }

    /** Asserts status and error code, and that the body has the common error shape. */
    protected void assertError(Response r, int status, String code) {
        assertEquals(status, r.status(), () -> "unexpected body " + r.body());
        assertEquals(code, r.code(), () -> "unexpected body " + r.body());
        assertEquals(status, r.body().get("status").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(r.body().has("message"));
        org.junit.jupiter.api.Assertions.assertTrue(r.body().get("fieldErrors").isArray());
        org.junit.jupiter.api.Assertions.assertTrue(r.body().has("timestamp"));
    }

    // ---------- fixtures, all created through the API ----------

    protected int createZone(String name, boolean rural) {
        Response r = post("/api/zones", adminToken, "{\"name\":\"" + name + "\",\"isRural\":" + rural + "}");
        assertEquals(201, r.status(), () -> r.body().toString());
        return r.body().get("id").asInt();
    }

    protected int createSubscriber(String name, String phone, int zoneId) {
        Response r = post("/api/subscribers", adminToken,
                "{\"fullName\":\"" + name + "\",\"phone\":\"" + phone + "\",\"address\":\"Test\",\"zoneId\":" + zoneId + "}");
        assertEquals(201, r.status(), () -> r.body().toString());
        return r.body().get("id").asInt();
    }

    protected int createSubscription(int subscriberId, String meter, int amperage, String initial, String startDate) {
        Response r = post("/api/subscriptions", adminToken, subscriptionJson(subscriberId, meter, amperage, initial, startDate));
        assertEquals(201, r.status(), () -> r.body().toString());
        return r.body().get("id").asInt();
    }

    protected String subscriptionJson(int subscriberId, String meter, int amperage, String initial, String startDate) {
        return "{\"subscriberId\":" + subscriberId + ",\"meterSerial\":\"" + meter + "\",\"amperage\":" + amperage
                + ",\"initialReading\":" + initial + ",\"startDate\":\"" + startDate + "\"}";
    }

    /** August 2026 ministry values (LBP). */
    protected String tariffJson(String month, long price) {
        return "{\"billingMonth\":\"" + month + "\",\"pricePerKwh\":" + price
                + ",\"fee5a\":385000,\"fee10a\":685000,\"feePerExtra5a\":300000,\"ruralUpliftPct\":10}";
    }

    protected int createTariff(String month, long price) {
        Response r = post("/api/tariffs", adminToken, tariffJson(month, price));
        assertEquals(201, r.status(), () -> r.body().toString());
        return r.body().get("id").asInt();
    }

    protected Response saveReading(String token, String month, int subscriptionId, String value) {
        return post("/api/readings/batch", token,
                "{\"month\":\"" + month + "\",\"entries\":[{\"subscriptionId\":" + subscriptionId + ",\"value\":" + value + "}]}");
    }

    protected Response runBilling(String month) {
        return post("/api/billing/runs", adminToken, "{\"month\":\"" + month + "\"}");
    }

    protected JsonNode invoicesOf(int subscriberId) {
        return get("/api/invoices?subscriberId=" + subscriberId, adminToken).body();
    }
}
