package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @NotBlank
    @Size(max = 20)
    @Column(name = "receipt_no", nullable = false, unique = true, updatable = false, length = 20)
    private String receiptNo;

    @NotNull
    @Positive
    @Digits(integer = 14, fraction = 0)
    @Column(name = "amount", nullable = false, updatable = false, precision = 14, scale = 0)
    private BigDecimal amount;                  // LBP

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, updatable = false, length = 10)
    private PaymentMethod method;

    @CreationTimestamp
    @Column(name = "paid_at", nullable = false, updatable = false)
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by", nullable = false, updatable = false)
    private AppUser receivedBy;
}
