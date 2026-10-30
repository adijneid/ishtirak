package org.example.ishtirak.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_id", nullable = false, updatable = false)
    private MeterReading reading;

    /** Snapshot, see proposal normalization rationale. */
    @Column(name = "consumption_kwh", nullable = false, updatable = false, precision = 10, scale = 1)
    private BigDecimal consumptionKwh;

    /** Snapshot, see proposal normalization rationale. */
    @Column(name = "amount_due", nullable = false, updatable = false, precision = 14, scale = 0)
    private BigDecimal amountDue;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean voided = false;

    @Column(name = "void_reason", length = 200)
    private String voidReason;
}
