package org.example.ishtirak.server.repository;

import org.example.ishtirak.server.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    boolean existsByMeterSerialIgnoreCase(String meterSerial);

    boolean existsByMeterSerialIgnoreCaseAndIdNot(String meterSerial, Integer id);

    List<Subscription> findBySubscriberIdOrderByStartDateAsc(Integer subscriberId);

    /** Subscriptions of a zone that overlap the month [monthStart, monthEnd]. */
    @Query("""
            select s from Subscription s
            join fetch s.subscriber sub
            where sub.zone.id = :zoneId
              and s.startDate <= :monthEnd
              and (s.endDate is null or s.endDate >= :monthStart)
            order by sub.fullName, s.meterSerial
            """)
    List<Subscription> findActiveInMonthByZone(@Param("zoneId") Integer zoneId,
                                               @Param("monthStart") LocalDate monthStart,
                                               @Param("monthEnd") LocalDate monthEnd);

    /** All subscriptions that overlap the month, with subscriber and zone loaded for the billing run. */
    @Query("""
            select s from Subscription s
            join fetch s.subscriber sub
            join fetch sub.zone
            where s.startDate <= :monthEnd
              and (s.endDate is null or s.endDate >= :monthStart)
            order by s.id
            """)
    List<Subscription> findActiveInMonth(@Param("monthStart") LocalDate monthStart,
                                         @Param("monthEnd") LocalDate monthEnd);

    @Query("""
            select count(s) > 0 from Subscription s
            where s.subscriber.id = :subscriberId
              and (s.endDate is null or s.endDate > :today)
            """)
    boolean existsActiveForSubscriber(@Param("subscriberId") Integer subscriberId,
                                      @Param("today") LocalDate today);
}
