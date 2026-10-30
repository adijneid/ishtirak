package org.example.ishtirak.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Entity
@Table(name = "subscription")
@Getter
@Setter
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    @Column(name = "meter_serial", nullable = false, unique = true, length = 30)
    private String meterSerial;

    @Column(nullable = false)
    private Short amperage;

    @Column(name = "initial_reading", nullable = false, precision = 10, scale = 1)
    private BigDecimal initialReading;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** A subscription is active in a month if it overlaps any day of that month. */
    public boolean isActiveIn(YearMonth month) {
        return !startDate.isAfter(month.atEndOfMonth())
                && (endDate == null || !endDate.isBefore(month.atDay(1)));
    }

    /** Active today: not ended, or ending in the future. */
    public boolean isActiveNow() {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }
}
