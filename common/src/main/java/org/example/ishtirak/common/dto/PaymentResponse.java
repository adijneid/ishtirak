package org.example.ishtirak.common.dto;

import java.math.BigDecimal;

public record PaymentResponse(
        Integer paymentId,
        String receiptNo,
        BigDecimal newBalance
) {
}
