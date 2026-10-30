package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.SubscriptionEndRequest;
import org.example.ishtirak.common.dto.SubscriptionRequest;
import org.example.ishtirak.common.dto.SubscriptionResponse;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@AdminOnly
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/{id}")
    public SubscriptionResponse getById(@PathVariable Integer id) {
        return subscriptionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(
            @Valid
            @RequestBody
            SubscriptionRequest request) {
        return subscriptionService.create(request);
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable Integer id,
                                       @Valid
                                       @RequestBody
                                       SubscriptionRequest request) {
        return subscriptionService.update(id, request);
    }

    @PatchMapping("/{id}/end")
    public SubscriptionResponse end(@PathVariable Integer id,
                                    @Valid
                                    @RequestBody
                                    SubscriptionEndRequest request) {
        return subscriptionService.end(id, request);
    }
}
