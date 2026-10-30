package org.example.ishtirak.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.ishtirak.common.model.SubscriberStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriber")
@Getter
@Setter
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscriber_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "full_name", nullable = false, length = 80)
    private String fullName;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 150)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubscriberStatus status = SubscriberStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
