package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscription")
@Getter
@Setter
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscription_id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Subscriber subscriber;

    @NotBlank
    @Size(max = 30)
    @Column(name = "meter_serial", nullable = false, unique = true, length = 30)
    private String meter_serial;

    @NotNull
    @Min(5)
    @Max(60)
    @Column(name = "amperage", nullable = false)
    private Short amperage;

    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 9, fraction = 1)
    @Column(name = "initial_reading", nullable = false, precision = 10, scale = 1)
    private BigDecimal initialReading;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
