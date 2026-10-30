package org.example.ishtirak.server.repository;

import org.example.ishtirak.server.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    boolean existsByInvoiceId(Integer invoiceId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoice.id = :invoiceId")
    BigDecimal sumByInvoiceId(@Param("invoiceId") Integer invoiceId);

    /** Rows: [invoice_id, total_paid]. */
    @Query("select p.invoice.id, sum(p.amount) from Payment p where p.invoice.id in :ids group by p.invoice.id")
    List<Object[]> sumsByInvoiceIds(@Param("ids") Collection<Integer> ids);

    @Query("select p from Payment p join fetch p.receivedBy where p.invoice.id = :invoiceId order by p.paidAt, p.id")
    List<Payment> findByInvoiceIdWithUser(@Param("invoiceId") Integer invoiceId);

    @Query(value = "SELECT nextval('receipt_no_seq')", nativeQuery = true)
    long nextReceiptNumber();
}
