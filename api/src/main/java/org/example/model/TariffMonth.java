package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tariff_month")
@Getter
@Setter
public class TariffMonth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tariff_id")
    private Long tariffId;

    @NotNull
    @Column(name = "billing_month", nullable = false, unique = true)
    private LocalDate billingMonth;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 0)
    @Column(name = "price_per_kwh", nullable = false, precision = 12, scale = 0)
    private BigDecimal pricePerKwh;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 0)
    @Column(name = "fee_5a", nullable = false, precision = 12, scale = 0)
    private BigDecimal fee5a;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 0)
    @Column(name = "fee_10a", nullable = false, precision = 12, scale = 0)
    private BigDecimal fee10a;

    @NotNull
    @Positive
    @Digits(integer = 12, fraction = 0)
    @Column(name = "fee_per_extra_5a", nullable = false, precision = 12, scale = 0)
    private BigDecimal feePerExtra5a;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Digits(integer = 3, fraction = 1)
    @Column(name = "rural_uplift_pct", nullable = false, precision = 4, scale = 1)
    private BigDecimal ruralUpliftPct = new BigDecimal("10.0");
    

}
