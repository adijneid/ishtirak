package org.example.ishtirak.common.dto;

import java.math.BigDecimal;

public record ArrearsRow(
        Integer subscriberId,
        String subscriber,
        String phone,
        String zone,
        long unpaidInvoices,
        BigDecimal balance
) {
}
