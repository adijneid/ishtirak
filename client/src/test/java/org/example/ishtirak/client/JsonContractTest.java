package org.example.ishtirak.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.ishtirak.common.dto.ErrorResponse;
import org.example.ishtirak.common.dto.ReadingBatchRequest;
import org.example.ishtirak.common.dto.ReadingEntry;
import org.example.ishtirak.common.dto.TariffResponse;
import org.example.ishtirak.common.dto.ZoneRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The JSON the client writes and reads must match what the server expects and sends. */
class JsonContractTest {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void zoneRequestUsesTheProposalFieldNames() throws Exception {
        assertEquals("{\"name\":\"Miziara\",\"isRural\":true}", mapper.writeValueAsString(new ZoneRequest("Miziara", true)));
    }

    @Test
    void monthsTravelAsYearMonthText() throws Exception {
        String json = mapper.writeValueAsString(new ReadingBatchRequest(YearMonth.of(2026, 8),
                List.of(new ReadingEntry(7, new BigDecimal("620.5")))));
        assertEquals("{\"month\":\"2026-08\",\"entries\":[{\"subscriptionId\":7,\"value\":620.5}]}", json);
    }

    @Test
    void readsAServerResponseCopiedFromARealRun() throws Exception {
        TariffResponse t = mapper.readValue("{\"id\":3,\"billingMonth\":\"2026-08\",\"pricePerKwh\":48241,\"fee5a\":385000,"
                + "\"fee10a\":685000,\"feePerExtra5a\":300000,\"ruralUpliftPct\":10.0,\"locked\":false}", TariffResponse.class);
        assertEquals(YearMonth.of(2026, 8), t.billingMonth());
        assertEquals(0, new BigDecimal("48241").compareTo(t.pricePerKwh()));
    }

    @Test
    void readsTheCommonErrorShape() throws Exception {
        ErrorResponse e = mapper.readValue("{\"status\":409,\"code\":\"OVERPAYMENT\",\"message\":\"too much\","
                + "\"fieldErrors\":[],\"timestamp\":\"2026-10-27T11:49:54.825491211\"}", ErrorResponse.class);
        ApiException api = new ApiException(e);
        assertEquals(409, api.getStatus());
        assertEquals("OVERPAYMENT", api.getCode());
        assertFalse(api.isConnectionLost());
    }

    @Test
    void unreachableServerIsReportedAsConnectionLost() {
        ApiClient client = new ApiClient("http://localhost:1");
        ApiException e = assertThrows(ApiException.class, () -> client.login("admin", "admin123"));
        assertTrue(e.isConnectionLost());
        assertEquals("CONNECTION_LOST", e.getCode());
    }
}
