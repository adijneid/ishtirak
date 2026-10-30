package org.example.ishtirak.common.dto;

import org.example.ishtirak.common.model.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record InvoiceResponse(
        Integer id,
        Integer subscriberId,
        String subscriber,
        String zone,
        YearMonth month,
        String meterSerial,
        BigDecimal kwh,
        BigDecimal amountDue,
        BigDecimal paid,
        BigDecimal balance,
        InvoiceStatus status,
        LocalDateTime issuedAt,
        String voidReason
) {
}
