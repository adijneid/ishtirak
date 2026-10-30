package org.example.ishtirak.server.repository;

import jakarta.persistence.LockModeType;
import org.example.ishtirak.server.model.TariffMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffMonthRepository extends JpaRepository<TariffMonth, Integer> {
    List<TariffMonth> findAllByOrderByBillingMonthDesc();

    boolean existsByBillingMonth(LocalDate billingMonth);

    boolean existsByBillingMonthAndIdNot(LocalDate billingMonth, Integer id);

    /** SELECT ... FOR UPDATE: two billing runs for the same month are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TariffMonth t where t.billingMonth = :month")
    Optional<TariffMonth> findForUpdateByBillingMonth(@Param("month") LocalDate month);
}
