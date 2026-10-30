package org.example.ishtirak.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.ishtirak.common.dto.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * The only class that talks to the server: JSON over HTTP with the DTOs of the common module.
 * Every controller uses this one shared instance. Calls are blocking, so controllers run them in a
 * background javafx.concurrent.Task (see Ui.run).
 */
public class ApiClient {
    private static final ApiClient INSTANCE =
            new ApiClient(System.getProperty("ishtirak.api.url", "http://localhost:8080"));

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static ApiClient get() {
        return INSTANCE;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // ---------------- auth ----------------

    public LoginResponse login(String username, String password) {
        return send("POST", "/api/auth/login", new LoginRequest(username, password), new TypeReference<>() {}, false);
    }

    // ---------------- zones ----------------

    public List<ZoneResponse> getZones() {
        return send("GET", "/api/zones", null, new TypeReference<>() {});
    }

    public ZoneResponse createZone(ZoneRequest request) {
        return send("POST", "/api/zones", request, new TypeReference<>() {});
    }

    public ZoneResponse updateZone(int id, ZoneRequest request) {
        return send("PUT", "/api/zones/" + id, request, new TypeReference<>() {});
    }

    public void deleteZone(int id) {
        send("DELETE", "/api/zones/" + id, null, new TypeReference<Void>() {});
    }

    // ---------------- subscribers ----------------

    public PageResponse<SubscriberResponse> getSubscribers(Integer zoneId, String status, String search, int page, int size) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("zoneId", zoneId);
        query.put("status", status);
        query.put("search", search);
        query.put("page", page);
        query.put("size", size);
        return send("GET", "/api/subscribers" + query(query), null, new TypeReference<>() {});
    }

    public SubscriberDetailResponse getSubscriber(int id) {
        return send("GET", "/api/subscribers/" + id, null, new TypeReference<>() {});
    }

    public SubscriberResponse createSubscriber(SubscriberRequest request) {
        return send("POST", "/api/subscribers", request, new TypeReference<>() {});
    }

    public SubscriberResponse updateSubscriber(int id, SubscriberRequest request) {
        return send("PUT", "/api/subscribers/" + id, request, new TypeReference<>() {});
    }

    public SubscriberResponse closeSubscriber(int id) {
        return send("PATCH", "/api/subscribers/" + id + "/close", null, new TypeReference<>() {});
    }

    // ---------------- subscriptions ----------------

    public SubscriptionResponse createSubscription(SubscriptionRequest request) {
        return send("POST", "/api/subscriptions", request, new TypeReference<>() {});
    }

    public SubscriptionResponse updateSubscription(int id, SubscriptionRequest request) {
        return send("PUT", "/api/subscriptions/" + id, request, new TypeReference<>() {});
    }

    public SubscriptionResponse endSubscription(int id, LocalDate endDate) {
        return send("PATCH", "/api/subscriptions/" + id + "/end", new SubscriptionEndRequest(endDate), new TypeReference<>() {});
    }

    // ---------------- tariffs ----------------

    public List<TariffResponse> getTariffs() {
        return send("GET", "/api/tariffs", null, new TypeReference<>() {});
    }

    public TariffResponse createTariff(TariffRequest request) {
        return send("POST", "/api/tariffs", request, new TypeReference<>() {});
    }

    public TariffResponse updateTariff(int id, TariffRequest request) {
        return send("PUT", "/api/tariffs/" + id, request, new TypeReference<>() {});
    }

    // ---------------- readings and billing ----------------

    public List<ReadingSheetRow> getReadingSheet(YearMonth month, int zoneId) {
        return send("GET", "/api/readings/sheet" + query(Map.of("month", month, "zoneId", zoneId)), null,
                new TypeReference<>() {});
    }

    public ReadingBatchResponse saveReadings(YearMonth month, List<ReadingEntry> entries) {
        return send("POST", "/api/readings/batch", new ReadingBatchRequest(month, entries), new TypeReference<>() {});
    }

    public BillingRunResponse runBilling(YearMonth month) {
        return send("POST", "/api/billing/runs", new BillingRunRequest(month), new TypeReference<>() {});
    }

    // ---------------- invoices and payments ----------------

    public List<InvoiceResponse> getInvoices(YearMonth month, Integer subscriberId, String status) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("month", month);
        query.put("subscriberId", subscriberId);
        query.put("status", status);
        return send("GET", "/api/invoices" + query(query), null, new TypeReference<>() {});
    }

    public InvoiceResponse voidInvoice(int id, String reason) {
        return send("POST", "/api/invoices/" + id + "/void", new VoidInvoiceRequest(reason), new TypeReference<>() {});
    }

    public PaymentResponse recordPayment(int invoiceId, PaymentRequest request) {
        return send("POST", "/api/invoices/" + invoiceId + "/payments", request, new TypeReference<>() {});
    }

    public List<PaymentHistoryItem> getPayments(int invoiceId) {
        return send("GET", "/api/invoices/" + invoiceId + "/payments", null, new TypeReference<>() {});
    }

    // ---------------- reports ----------------

    public DashboardResponse getDashboard(YearMonth month) {
        return send("GET", "/api/reports/dashboard" + query(Map.of("month", month)), null, new TypeReference<>() {});
    }

    public List<ArrearsRow> getArrears(Integer zoneId) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("zoneId", zoneId);
        return send("GET", "/api/reports/arrears" + query(query), null, new TypeReference<>() {});
    }

    // ---------------- plumbing ----------------

    private <T> T send(String method, String path, Object body, TypeReference<T> type) {
        return send(method, path, body, type, true);
    }

    private <T> T send(String method, String path, Object body, TypeReference<T> type, boolean authenticated) {
        HttpResponse<String> response;
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            if (authenticated && SessionContext.getToken() != null) {
                request.header("Authorization", "Bearer " + SessionContext.getToken());
            }
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw ApiException.connectionLost(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.connectionLost(e);
        }

        try {
            if (response.statusCode() >= 400) {
                throw new ApiException(readError(response));
            }
            if (response.body() == null || response.body().isBlank()) {
                return null;   // 204 No Content
            }
            return mapper.readValue(response.body(), type);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected answer from the server: " + response.body(), e);
        }
    }

    private ErrorResponse readError(HttpResponse<String> response) {
        try {
            return mapper.readValue(response.body(), ErrorResponse.class);
        } catch (IOException e) {
            // not our JSON error shape (e.g. a proxy page): keep the status, describe it plainly
            return new ErrorResponse(response.statusCode(), "HTTP_" + response.statusCode(),
                    "The server answered with status " + response.statusCode(), List.of(), LocalDateTime.now());
        }
    }

    private static String query(Map<String, Object> params) {
        StringJoiner joiner = new StringJoiner("&", "?", "");
        joiner.setEmptyValue("");
        params.forEach((key, value) -> {
            if (value != null && !value.toString().isBlank()) {
                joiner.add(key + "=" + URLEncoder.encode(value.toString(), StandardCharsets.UTF_8));
            }
        });
        return joiner.toString();
    }
}
