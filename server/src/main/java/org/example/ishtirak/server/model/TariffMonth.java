package org.example.ishtirak.server.model;

import jakarta.persistence.*;
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
    private Integer id;

    /** Always the first day of the month. */
    @Column(name = "billing_month", nullable = false, unique = true)
    private LocalDate billingMonth;

    @Column(name = "price_per_kwh", nullable = false, precision = 12, scale = 0)
    private BigDecimal pricePerKwh;

    @Column(name = "fee_5a", nullable = false, precision = 12, scale = 0)
    private BigDecimal fee5a;

    @Column(name = "fee_10a", nullable = false, precision = 12, scale = 0)
    private BigDecimal fee10a;

    @Column(name = "fee_per_extra_5a", nullable = false, precision = 12, scale = 0)
    private BigDecimal feePerExtra5a;

    @Column(name = "rural_uplift_pct", nullable = false, precision = 4, scale = 1)
    private BigDecimal ruralUpliftPct = new BigDecimal("10.0");
}
