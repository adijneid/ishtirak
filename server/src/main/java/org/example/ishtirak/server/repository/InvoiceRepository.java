package org.example.ishtirak.server.repository;

import jakarta.persistence.LockModeType;
import org.example.ishtirak.server.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    boolean existsByReadingIdAndVoidedFalse(Integer readingId);

    /** Tariff lock: the month has at least one non-voided invoice. */
    @Query("select count(i) > 0 from Invoice i where i.voided = false and i.reading.billingMonth = :month")
    boolean existsActiveForMonth(@Param("month") LocalDate month);

    /** SELECT ... FOR UPDATE on the invoice row, so two payments cannot exceed the balance together. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findForUpdateById(@Param("id") Integer id);

    @Query("""
            select i from Invoice i
            join fetch i.reading r
            join fetch r.subscription s
            join fetch s.subscriber sub
            join fetch sub.zone
            where i.id = :id
            """)
    Optional<Invoice> findDetailedById(@Param("id") Integer id);

    @Query("""
            select i from Invoice i
            join fetch i.reading r
            join fetch r.subscription s
            join fetch s.subscriber sub
            join fetch sub.zone
            where sub.id = :subscriberId
            order by r.billingMonth desc, i.id desc
            """)
    List<Invoice> findDetailedBySubscriber(@Param("subscriberId") Integer subscriberId);

    @Query("""
            select i from Invoice i
            join fetch i.reading r
            join fetch r.subscription s
            join fetch s.subscriber sub
            join fetch sub.zone
            where r.billingMonth = :month
            order by sub.fullName, i.id
            """)
    List<Invoice> findDetailedByMonth(@Param("month") LocalDate month);

    @Query("""
            select i from Invoice i
            join fetch i.reading r
            join fetch r.subscription s
            join fetch s.subscriber sub
            join fetch sub.zone
            order by r.billingMonth desc, sub.fullName, i.id
            """)
    List<Invoice> findAllDetailed();

    /** Balance per subscriber = sum over non-voided invoices of (amount_due - paid). Rows: [subscriber_id, balance]. */
    @Query(value = """
            SELECT sub.subscriber_id, COALESCE(SUM(i.amount_due - COALESCE(p.paid, 0)), 0)
            FROM invoice i
            JOIN meter_reading r ON r.reading_id = i.reading_id
            JOIN subscription sub ON sub.subscription_id = r.subscription_id
            LEFT JOIN (SELECT invoice_id, SUM(amount) AS paid FROM payment GROUP BY invoice_id) p
                   ON p.invoice_id = i.invoice_id
            WHERE i.voided = FALSE AND sub.subscriber_id IN (:ids)
            GROUP BY sub.subscriber_id
            """, nativeQuery = true)
    List<Object[]> balancesForSubscribers(@Param("ids") Collection<Integer> ids);

    /** Arrears report rows: [subscriber_id, full_name, phone, zone_name, unpaid_invoices, balance]. */
    @Query(value = """
            SELECT s.subscriber_id, s.full_name, s.phone, z.name,
                   COUNT(*) FILTER (WHERE i.amount_due - COALESCE(p.paid, 0) > 0),
                   SUM(i.amount_due - COALESCE(p.paid, 0)) AS balance
            FROM invoice i
            JOIN meter_reading r ON r.reading_id = i.reading_id
            JOIN subscription sub ON sub.subscription_id = r.subscription_id
            JOIN subscriber s ON s.subscriber_id = sub.subscriber_id
            JOIN zone z ON z.zone_id = s.zone_id
            LEFT JOIN (SELECT invoice_id, SUM(amount) AS paid FROM payment GROUP BY invoice_id) p
                   ON p.invoice_id = i.invoice_id
            WHERE i.voided = FALSE AND (:zoneId = 0 OR z.zone_id = :zoneId)
            GROUP BY s.subscriber_id, s.full_name, s.phone, z.name
            HAVING SUM(i.amount_due - COALESCE(p.paid, 0)) > 0
            ORDER BY balance DESC, s.full_name
            """, nativeQuery = true)
    List<Object[]> arrears(@Param("zoneId") int zoneId);

    /** Dashboard rows per zone for one month: [zone_id, zone_name, billed, collected]. */
    @Query(value = """
            SELECT z.zone_id, z.name,
                   COALESCE(SUM(i.amount_due), 0),
                   COALESCE(SUM(p.paid), 0)
            FROM zone z
            LEFT JOIN subscriber s ON s.zone_id = z.zone_id
            LEFT JOIN subscription sub ON sub.subscriber_id = s.subscriber_id
            LEFT JOIN meter_reading r ON r.subscription_id = sub.subscription_id AND r.billing_month = :month
            LEFT JOIN invoice i ON i.reading_id = r.reading_id AND i.voided = FALSE
            LEFT JOIN (SELECT invoice_id, SUM(amount) AS paid FROM payment GROUP BY invoice_id) p
                   ON p.invoice_id = i.invoice_id
            GROUP BY z.zone_id, z.name
            ORDER BY z.name
            """, nativeQuery = true)
    List<Object[]> collectionsByZone(@Param("month") LocalDate month);

    /** Subscriptions active in the month that have no reading for it yet. */
    @Query(value = """
            SELECT COUNT(*) FROM subscription sub
            WHERE sub.start_date <= :monthEnd
              AND (sub.end_date IS NULL OR sub.end_date >= :monthStart)
              AND NOT EXISTS (SELECT 1 FROM meter_reading r
                              WHERE r.subscription_id = sub.subscription_id AND r.billing_month = :monthStart)
            """, nativeQuery = true)
    long countMissingReadings(@Param("monthStart") LocalDate monthStart, @Param("monthEnd") LocalDate monthEnd);
}
