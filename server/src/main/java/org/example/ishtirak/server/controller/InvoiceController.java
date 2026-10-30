package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.InvoiceResponse;
import org.example.ishtirak.common.dto.PaymentHistoryItem;
import org.example.ishtirak.common.dto.PaymentRequest;
import org.example.ishtirak.common.dto.PaymentResponse;
import org.example.ishtirak.common.dto.VoidInvoiceRequest;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.InvoiceService;
import org.example.ishtirak.server.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    public InvoiceController(InvoiceService invoiceService, PaymentService paymentService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<InvoiceResponse> getAll(@RequestParam(required = false) YearMonth month,
                                        @RequestParam(required = false) Integer subscriberId,
                                        @RequestParam(required = false) String status) {
        return invoiceService.findAll(month, subscriberId, status);
    }

    @AdminOnly
    @PostMapping("/{id}/void")
    public InvoiceResponse voidInvoice(@PathVariable Integer id,
                                       @Valid
                                       @RequestBody
                                       VoidInvoiceRequest request) {
        return invoiceService.voidInvoice(id, request);
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse recordPayment(@PathVariable Integer id,
                                         @Valid
                                         @RequestBody
                                         PaymentRequest request) {
        return paymentService.recordPayment(id, request);
    }

    /** Payments history of one invoice (V4 Subscriber Details). */
    @GetMapping("/{id}/payments")
    public List<PaymentHistoryItem> getPayments(@PathVariable Integer id) {
        return invoiceService.findPayments(id);
    }
}
