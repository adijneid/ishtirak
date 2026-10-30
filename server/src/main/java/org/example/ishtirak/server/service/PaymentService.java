package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.PaymentRequest;
import org.example.ishtirak.common.dto.PaymentResponse;
import org.example.ishtirak.common.model.PaymentMethod;
import org.example.ishtirak.server.exception.ConflictException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.model.Invoice;
import org.example.ishtirak.server.model.Payment;
import org.example.ishtirak.server.repository.AppUserRepository;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.PaymentRepository;
import org.example.ishtirak.server.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentService {
    private static final DateTimeFormatter RECEIPT_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final PaymentRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final AppUserRepository userRepository;

    public PaymentService(PaymentRepository repository, InvoiceRepository invoiceRepository,
                          AppUserRepository userRepository) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentResponse recordPayment(Integer invoiceId, PaymentRequest request) {
        // SELECT ... FOR UPDATE: a second payment on the same invoice waits, then sees the new balance
        Invoice invoice = invoiceRepository.findForUpdateById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        if (invoice.isVoided()) {
            throw new ConflictException("INVOICE_VOID", "The invoice is void");
        }
        BigDecimal balance = invoice.getAmountDue().subtract(repository.sumByInvoiceId(invoiceId));
        if (request.amount().compareTo(balance) > 0) {
            throw new ConflictException("OVERPAYMENT",
                    "The amount is more than the balance (LBP " + balance.toPlainString() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setMethod(PaymentMethod.valueOf(request.method()));
        payment.setPaidAt(now);
        payment.setReceivedBy(userRepository.getReferenceById(CurrentUser.get().id()));
        payment.setReceiptNo(String.format("RC%s-%06d", now.format(RECEIPT_MONTH), repository.nextReceiptNumber()));
        Payment saved = repository.save(payment);

        return new PaymentResponse(saved.getId(), saved.getReceiptNo(), balance.subtract(request.amount()));
    }
}
