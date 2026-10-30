package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.InvoiceResponse;
import org.example.ishtirak.common.dto.PaymentHistoryItem;
import org.example.ishtirak.common.dto.VoidInvoiceRequest;
import org.example.ishtirak.common.model.InvoiceStatus;
import org.example.ishtirak.server.exception.BadRequestException;
import org.example.ishtirak.server.exception.ConflictException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.mapper.InvoiceMapper;
import org.example.ishtirak.server.model.Invoice;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.PaymentRepository;
import org.example.ishtirak.server.repository.SubscriberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {
    private final InvoiceRepository repository;
    private final PaymentRepository paymentRepository;
    private final SubscriberRepository subscriberRepository;
    private final InvoiceMapper mapper;

    public InvoiceService(InvoiceRepository repository, PaymentRepository paymentRepository,
                          SubscriberRepository subscriberRepository, InvoiceMapper mapper) {
        this.repository = repository;
        this.paymentRepository = paymentRepository;
        this.subscriberRepository = subscriberRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> findAll(YearMonth month, Integer subscriberId, String status) {
        InvoiceStatus statusFilter = parseStatus(status);
        List<Invoice> invoices;
        if (subscriberId != null) {
            if (!subscriberRepository.existsById(subscriberId)) {
                throw new ResourceNotFoundException("Subscriber not found: " + subscriberId);
            }
            invoices = repository.findDetailedBySubscriber(subscriberId);
        } else if (month != null) {
            invoices = repository.findDetailedByMonth(month.atDay(1));
        } else {
            invoices = repository.findAllDetailed();
        }
        Map<Integer, BigDecimal> paid = paidByInvoice(invoices);
        return invoices.stream()
                .filter(i -> month == null || YearMonth.from(i.getReading().getBillingMonth()).equals(month))
                .map(i -> mapper.toResponse(i, paid.getOrDefault(i.getId(), BigDecimal.ZERO)))
                .filter(r -> statusFilter == null || r.status() == statusFilter)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryItem> findPayments(Integer invoiceId) {
        if (!repository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found: " + invoiceId);
        }
        return paymentRepository.findByInvoiceIdWithUser(invoiceId).stream()
                .map(p -> new PaymentHistoryItem(p.getId(), invoiceId, p.getReceiptNo(), p.getAmount(),
                        p.getMethod(), p.getPaidAt(), p.getReceivedBy().getFullName()))
                .toList();
    }

    @Transactional
    public InvoiceResponse voidInvoice(Integer id, VoidInvoiceRequest request) {
        Invoice invoice = repository.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        if (invoice.isVoided()) {
            throw new ConflictException("INVOICE_VOID", "The invoice is already void");
        }
        if (paymentRepository.existsByInvoiceId(id)) {
            throw new ConflictException("INVOICE_HAS_PAYMENTS", "The invoice has payments and cannot be voided");
        }
        invoice.setVoided(true);
        invoice.setVoidReason(request.reason().trim());
        repository.saveAndFlush(invoice);
        return toResponse(id);
    }

    /**
     * Helpers
     */
    public InvoiceResponse toResponse(Integer id) {
        Invoice invoice = repository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return mapper.toResponse(invoice, paymentRepository.sumByInvoiceId(id));
    }

    private Map<Integer, BigDecimal> paidByInvoice(List<Invoice> invoices) {
        Map<Integer, BigDecimal> paid = new HashMap<>();
        if (invoices.isEmpty()) {
            return paid;
        }
        for (Object[] row : paymentRepository.sumsByInvoiceIds(invoices.stream().map(Invoice::getId).toList())) {
            paid.put((Integer) row[0], (BigDecimal) row[1]);
        }
        return paid;
    }

    private InvoiceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return InvoiceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status", "must be UNPAID, PARTIAL, PAID or VOID");
        }
    }
}
