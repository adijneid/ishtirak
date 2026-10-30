package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.BillingRunRequest;
import org.example.ishtirak.common.dto.BillingRunResponse;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.BillingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@AdminOnly
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /** 200 (not 201): a repeated run is allowed and may create nothing. */
    @PostMapping("/runs")
    public BillingRunResponse run(
            @Valid
            @RequestBody
            BillingRunRequest request) {
        return billingService.generateInvoices(request.month());
    }
}
