package org.example.ishtirak.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_reading",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"subscription_id", "billing_month"}
                )})
@Getter
@Setter
public class MeterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /** Always the first day of the month. */
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "reading_value", nullable = false, precision = 10, scale = 1)
    private BigDecimal readingValue;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "read_by", nullable = false)
    private AppUser readBy;
}
