package org.example.ishtirak.server.repository;

import org.example.ishtirak.server.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SubscriberRepository extends JpaRepository<Subscriber, Integer>, JpaSpecificationExecutor<Subscriber> {
    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Integer id);

    boolean existsByZoneId(Integer zoneId);

    long countByZoneId(Integer zoneId);
}
