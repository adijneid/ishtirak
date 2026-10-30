package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice")
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_id", nullable = false, updatable = false)
    private MeterReading reading;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 9, fraction = 1)
    @Column(name = "consumption_kwh", nullable = false, updatable = false, precision = 10, scale = 1)
    private BigDecimal consumptionKwh;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 14, fraction = 0)
    @Column(name = "amount_due", nullable = false, updatable = false, precision = 14, scale = 0)
    private BigDecimal amountDue;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "voided", nullable = false)
    private boolean voided = false;

    @Size(max = 200)
    @Column(name = "void_reason", length = 200)
    private String voidReason;
}