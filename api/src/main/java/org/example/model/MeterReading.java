package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meter_reading",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_reading_subscription_month",
                columnNames = {"subscription_id", "billing_month"}))
@Getter
@Setter
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Long readingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @NotNull
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 9, fraction = 1)
    @Column(name = "reading_value", nullable = false, precision = 10, scale = 1)
    private BigDecimal readingValue;

    @NotNull
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "read_by", nullable = false)
    private AppUser readBy;
}