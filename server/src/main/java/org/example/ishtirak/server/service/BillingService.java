package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.BillingRunResponse;
import org.example.ishtirak.common.dto.MissingReading;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.mapper.TariffMapper;
import org.example.ishtirak.server.model.Invoice;
import org.example.ishtirak.server.model.MeterReading;
import org.example.ishtirak.server.model.Subscription;
import org.example.ishtirak.server.model.TariffMonth;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.MeterReadingRepository;
import org.example.ishtirak.server.repository.SubscriptionRepository;
import org.example.ishtirak.server.repository.TariffMonthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BillingService {
    private final TariffMonthRepository tariffRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MeterReadingRepository readingRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReadingService readingService;
    private final TariffMapper tariffMapper;
    private final BillingCalculator calculator = new BillingCalculator();

    public BillingService(TariffMonthRepository tariffRepository, SubscriptionRepository subscriptionRepository,
                          MeterReadingRepository readingRepository, InvoiceRepository invoiceRepository,
                          ReadingService readingService, TariffMapper tariffMapper) {
        this.tariffRepository = tariffRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.readingRepository = readingRepository;
        this.invoiceRepository = invoiceRepository;
        this.readingService = readingService;
        this.tariffMapper = tariffMapper;
    }

    /**
     * Generates every invoice of a month in ONE transaction. Idempotent: readings that already have a
     * non-voided invoice are skipped (also enforced by the partial unique index uq_invoice_reading_not_voided).
     */
    @Transactional
    public BillingRunResponse generateInvoices(YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        // row lock on the tariff: a second run for the same month waits until this one commits
        TariffMonth tariff = tariffRepository.findForUpdateByBillingMonth(monthStart)
                .orElseThrow(() -> new ResourceNotFoundException("TARIFF_NOT_FOUND",
                        "No tariff has been entered for " + month));
        BillingCalculator.Rates rates = tariffMapper.toRates(tariff);

        int created = 0;
        int skipped = 0;
        BigDecimal totalBilled = BigDecimal.ZERO;
        List<MissingReading> missing = new ArrayList<>();

        for (Subscription subscription : subscriptionRepository.findActiveInMonth(monthStart, month.atEndOfMonth())) {
            Optional<MeterReading> reading = readingRepository.findBySubscriptionIdAndBillingMonth(subscription.getId(), monthStart);
            if (reading.isEmpty()) {
                missing.add(new MissingReading(subscription.getId(), subscription.getSubscriber().getFullName(),
                        subscription.getMeterSerial(), subscription.getSubscriber().getZone().getName()));
                continue;
            }
            if (invoiceRepository.existsByReadingIdAndVoidedFalse(reading.get().getId())) {
                skipped++;
                continue;
            }
            BigDecimal previous = readingService.previousReading(subscription, monthStart);
            BillingCalculator.Result result = calculator.calculate(
                    previous,
                    reading.get().getReadingValue(),
                    subscription.getAmperage(),
                    subscription.getSubscriber().getZone().isRural(),
                    rates);

            Invoice invoice = new Invoice();
            invoice.setReading(reading.get());
            invoice.setConsumptionKwh(result.consumptionKwh());
            invoice.setAmountDue(result.amountDue());
            invoice.setIssuedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            created++;
            totalBilled = totalBilled.add(result.amountDue());
        }
        return new BillingRunResponse(month, created, skipped, missing, totalBilled);
    }
}
