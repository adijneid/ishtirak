package org.example.ishtirak.server.mapper;

import org.example.ishtirak.common.dto.InvoiceResponse;
import org.example.ishtirak.common.model.InvoiceStatus;
import org.example.ishtirak.server.model.Invoice;
import org.example.ishtirak.server.model.MeterReading;
import org.example.ishtirak.server.model.Subscriber;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;

@Component
public class InvoiceMapper {
    /** paid comes from the payment table: totals and status are computed, never stored. */
    public InvoiceResponse toResponse(Invoice invoice, BigDecimal paid) {
        MeterReading reading = invoice.getReading();
        Subscriber subscriber = reading.getSubscription().getSubscriber();
        BigDecimal balance = invoice.isVoided() ? BigDecimal.ZERO : invoice.getAmountDue().subtract(paid);
        return new InvoiceResponse(
                invoice.getId(),
                subscriber.getId(),
                subscriber.getFullName(),
                subscriber.getZone().getName(),
                YearMonth.from(reading.getBillingMonth()),
                reading.getSubscription().getMeterSerial(),
                invoice.getConsumptionKwh(),
                invoice.getAmountDue(),
                paid,
                balance,
                statusOf(invoice, paid),
                invoice.getIssuedAt(),
                invoice.getVoidReason()
        );
    }

    public static InvoiceStatus statusOf(Invoice invoice, BigDecimal paid) {
        if (invoice.isVoided()) {
            return InvoiceStatus.VOID;
        }
        if (paid.compareTo(invoice.getAmountDue()) >= 0) {
            return InvoiceStatus.PAID;
        }
        return paid.signum() > 0 ? InvoiceStatus.PARTIAL : InvoiceStatus.UNPAID;
    }
}
