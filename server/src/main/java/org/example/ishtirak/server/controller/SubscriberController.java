package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.PageResponse;
import org.example.ishtirak.common.dto.SubscriberDetailResponse;
import org.example.ishtirak.common.dto.SubscriberRequest;
import org.example.ishtirak.common.dto.SubscriberResponse;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.SubscriberService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscribers")
public class SubscriberController {
    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @GetMapping
    public PageResponse<SubscriberResponse> getAll(@RequestParam(required = false) Integer zoneId,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String search,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return subscriberService.findAll(zoneId, status, search, page, size);
    }

    @GetMapping("/{id}")
    public SubscriberDetailResponse getById(@PathVariable Integer id) {
        return subscriberService.findById(id);
    }

    @AdminOnly
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriberResponse create(
            @Valid
            @RequestBody
            SubscriberRequest request) {
        return subscriberService.create(request);
    }

    @AdminOnly
    @PutMapping("/{id}")
    public SubscriberResponse update(@PathVariable Integer id,
                                     @Valid
                                     @RequestBody
                                     SubscriberRequest request) {
        return subscriberService.update(id, request);
    }

    @AdminOnly
    @PatchMapping("/{id}/close")
    public SubscriberResponse close(@PathVariable Integer id) {
        return subscriberService.close(id);
    }
}
