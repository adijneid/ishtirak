package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.ReadingBatchRequest;
import org.example.ishtirak.common.dto.ReadingBatchResponse;
import org.example.ishtirak.common.dto.ReadingEntry;
import org.example.ishtirak.common.dto.ReadingIssue;
import org.example.ishtirak.common.dto.ReadingSheetRow;
import org.example.ishtirak.server.exception.BadRequestException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.model.AppUser;
import org.example.ishtirak.server.model.MeterReading;
import org.example.ishtirak.server.model.Subscription;
import org.example.ishtirak.server.repository.AppUserRepository;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.MeterReadingRepository;
import org.example.ishtirak.server.repository.SubscriptionRepository;
import org.example.ishtirak.server.repository.ZoneRepository;
import org.example.ishtirak.server.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReadingService {
    /** HIGH_CONSUMPTION warning above this multiple of the 3-month average (proposal). */
    private static final BigDecimal HIGH_CONSUMPTION_FACTOR = new BigDecimal("3");
    private static final int AVERAGE_MONTHS = 3;

    private final MeterReadingRepository repository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ZoneRepository zoneRepository;
    private final AppUserRepository userRepository;

    public ReadingService(MeterReadingRepository repository, SubscriptionRepository subscriptionRepository,
                          InvoiceRepository invoiceRepository, ZoneRepository zoneRepository,
                          AppUserRepository userRepository) {
        this.repository = repository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.zoneRepository = zoneRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ReadingSheetRow> getSheet(YearMonth month, Integer zoneId) {
        if (!zoneRepository.existsById(zoneId)) {
            throw new ResourceNotFoundException("Zone not found: " + zoneId);
        }
        LocalDate monthStart = month.atDay(1);
        return subscriptionRepository.findActiveInMonthByZone(zoneId, monthStart, month.atEndOfMonth()).stream()
                .map(s -> {
                    Optional<MeterReading> current = repository.findBySubscriptionIdAndBillingMonth(s.getId(), monthStart);
                    return new ReadingSheetRow(
                            s.getId(),
                            s.getSubscriber().getId(),
                            s.getSubscriber().getFullName(),
                            s.getMeterSerial(),
                            s.getAmperage(),
                            previousReading(s, monthStart),
                            current.map(MeterReading::getReadingValue).orElse(null),
                            current.map(r -> invoiceRepository.existsByReadingIdAndVoidedFalse(r.getId())).orElse(false));
                })
                .toList();
    }

    /**
     * Saves a zone's readings. Every row is checked on its own: valid rows are saved even if others are rejected.
     */
    @Transactional
    public ReadingBatchResponse saveBatch(ReadingBatchRequest request) {
        YearMonth month = request.month();
        if (month.isAfter(YearMonth.now())) {
            throw new BadRequestException("month", "cannot be in the future");
        }
        LocalDate monthStart = month.atDay(1);
        AppUser reader = userRepository.getReferenceById(CurrentUser.get().id());

        int saved = 0;
        List<ReadingIssue> rejected = new ArrayList<>();
        List<ReadingIssue> warnings = new ArrayList<>();

        for (ReadingEntry entry : request.entries()) {
            Integer subscriptionId = entry.subscriptionId();
            if (subscriptionId == null) {
                rejected.add(new ReadingIssue(null, "VALIDATION_ERROR", "subscriptionId is required"));
                continue;
            }
            Optional<Subscription> found = subscriptionRepository.findById(subscriptionId);
            if (found.isEmpty()) {
                rejected.add(new ReadingIssue(subscriptionId, "NOT_FOUND", "Subscription not found"));
                continue;
            }
            Subscription subscription = found.get();
            if (!subscription.isActiveIn(month)) {
                rejected.add(new ReadingIssue(subscriptionId, "INACTIVE", "The subscription is not active in " + month));
                continue;
            }
            BigDecimal value = entry.value();
            if (value == null || value.signum() < 0 || value.stripTrailingZeros().scale() > 1
                    || value.precision() - value.scale() > 9) {
                rejected.add(new ReadingIssue(subscriptionId, "VALIDATION_ERROR",
                        "The reading must be a number of at most one decimal and not negative"));
                continue;
            }
            BigDecimal previous = previousReading(subscription, monthStart);
            if (value.compareTo(previous) < 0) {
                rejected.add(new ReadingIssue(subscriptionId, "READING_BELOW_PREVIOUS",
                        "The reading " + value.toPlainString() + " is below the previous one (" + previous.toPlainString() + ")"));
                continue;
            }
            Optional<MeterReading> next = repository
                    .findTopBySubscriptionIdAndBillingMonthGreaterThanOrderByBillingMonthAsc(subscriptionId, monthStart);
            if (next.isPresent() && value.compareTo(next.get().getReadingValue()) > 0) {
                rejected.add(new ReadingIssue(subscriptionId, "READING_ABOVE_NEXT",
                        "The reading is above the one already saved for " + YearMonth.from(next.get().getBillingMonth())));
                continue;
            }

            Optional<MeterReading> existing = repository.findBySubscriptionIdAndBillingMonth(subscriptionId, monthStart);
            if (existing.isPresent() && invoiceRepository.existsByReadingIdAndVoidedFalse(existing.get().getId())) {
                if (existing.get().getReadingValue().compareTo(value) != 0) {
                    rejected.add(new ReadingIssue(subscriptionId, "READING_LOCKED",
                            "The reading is already invoiced and cannot change"));
                }
                continue; // same value as the invoiced one: nothing to do
            }

            MeterReading reading = existing.orElseGet(MeterReading::new);
            reading.setSubscription(subscription);
            reading.setBillingMonth(monthStart);
            reading.setReadingValue(value.setScale(1, RoundingMode.UNNECESSARY));
            reading.setReadAt(LocalDateTime.now());
            reading.setReadBy(reader);
            repository.save(reading);
            saved++;

            BigDecimal average = averageConsumption(subscription, monthStart);
            BigDecimal consumption = value.subtract(previous);
            if (average != null && average.signum() > 0
                    && consumption.compareTo(average.multiply(HIGH_CONSUMPTION_FACTOR)) > 0) {
                warnings.add(new ReadingIssue(subscriptionId, "HIGH_CONSUMPTION",
                        consumption.toPlainString() + " kWh is more than 3 times the recent average ("
                                + average.setScale(1, RoundingMode.HALF_UP).toPlainString() + " kWh)"));
            }
        }
        return new ReadingBatchResponse(saved, rejected, warnings);
    }

    /**
     * Helpers
     */
    /** Latest reading before the month, or the subscription's initial reading for its first month. */
    public BigDecimal previousReading(Subscription subscription, LocalDate monthStart) {
        return repository
                .findTopBySubscriptionIdAndBillingMonthLessThanOrderByBillingMonthDesc(subscription.getId(), monthStart)
                .map(MeterReading::getReadingValue)
                .orElse(subscription.getInitialReading());
    }

    /** Average consumption of the (up to) 3 months before this one; null when there is no history yet. */
    private BigDecimal averageConsumption(Subscription subscription, LocalDate monthStart) {
        List<MeterReading> history = repository
                .findBySubscriptionIdAndBillingMonthLessThanOrderByBillingMonthAsc(subscription.getId(), monthStart);
        if (history.isEmpty()) {
            return null;
        }
        List<BigDecimal> consumptions = new ArrayList<>();
        BigDecimal before = subscription.getInitialReading();
        for (MeterReading reading : history) {
            consumptions.add(reading.getReadingValue().subtract(before));
            before = reading.getReadingValue();
        }
        List<BigDecimal> recent = consumptions.subList(Math.max(0, consumptions.size() - AVERAGE_MONTHS), consumptions.size());
        BigDecimal total = recent.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(recent.size()), 4, RoundingMode.HALF_UP);
    }
}
