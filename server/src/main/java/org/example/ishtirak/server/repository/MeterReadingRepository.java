package org.example.ishtirak.server.repository;

import org.example.ishtirak.server.model.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MeterReadingRepository extends JpaRepository<MeterReading, Integer> {
    Optional<MeterReading> findBySubscriptionIdAndBillingMonth(Integer subscriptionId, LocalDate billingMonth);

    /** The previous reading: the latest one strictly before the month. */
    Optional<MeterReading> findTopBySubscriptionIdAndBillingMonthLessThanOrderByBillingMonthDesc(
            Integer subscriptionId, LocalDate billingMonth);

    /** The next reading: the earliest one strictly after the month. */
    Optional<MeterReading> findTopBySubscriptionIdAndBillingMonthGreaterThanOrderByBillingMonthAsc(
            Integer subscriptionId, LocalDate billingMonth);

    List<MeterReading> findBySubscriptionIdAndBillingMonthLessThanOrderByBillingMonthAsc(
            Integer subscriptionId, LocalDate billingMonth);

    Optional<MeterReading> findTopBySubscriptionIdOrderByBillingMonthDesc(Integer subscriptionId);

    boolean existsBySubscriptionId(Integer subscriptionId);
}
