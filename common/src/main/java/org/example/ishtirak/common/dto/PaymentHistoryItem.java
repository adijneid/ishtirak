package org.example.ishtirak.common.dto;

import org.example.ishtirak.common.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentHistoryItem(
        Integer paymentId,
        Integer invoiceId,
        String receiptNo,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime paidAt,
        String receivedBy
) {
}
