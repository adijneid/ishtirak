package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.SubscriptionEndRequest;
import org.example.ishtirak.common.dto.SubscriptionRequest;
import org.example.ishtirak.common.dto.SubscriptionResponse;
import org.example.ishtirak.common.model.SubscriberStatus;
import org.example.ishtirak.server.exception.BadRequestException;
import org.example.ishtirak.server.exception.ConflictException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.mapper.SubscriptionMapper;
import org.example.ishtirak.server.model.MeterReading;
import org.example.ishtirak.server.model.Subscriber;
import org.example.ishtirak.server.model.Subscription;
import org.example.ishtirak.server.repository.MeterReadingRepository;
import org.example.ishtirak.server.repository.SubscriberRepository;
import org.example.ishtirak.server.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SubscriptionService {
    private final SubscriptionRepository repository;
    private final SubscriberRepository subscriberRepository;
    private final MeterReadingRepository readingRepository;
    private final SubscriptionMapper mapper;

    public SubscriptionService(SubscriptionRepository repository, SubscriberRepository subscriberRepository,
                               MeterReadingRepository readingRepository, SubscriptionMapper mapper) {
        this.repository = repository;
        this.subscriberRepository = subscriberRepository;
        this.readingRepository = readingRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Integer id) {
        return mapper.toResponse(findEntity(id));
    }

    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        checkAmperage(request.amperage());
        Subscriber subscriber = subscriberRepository.findById(request.subscriberId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found: " + request.subscriberId()));
        if (subscriber.getStatus() != SubscriberStatus.ACTIVE) {
            throw new ConflictException("SUBSCRIBER_CLOSED", "The subscriber is closed");
        }
        if (repository.existsByMeterSerialIgnoreCase(request.meterSerial().trim())) {
            throw new ConflictException("DUPLICATE_METER", "Meter " + request.meterSerial().trim() + " is already registered");
        }
        return mapper.toResponse(repository.save(mapper.toEntity(request, subscriber)));
    }

    /**
     * Changes meter serial or amperage. Issued invoices keep their snapshot amounts, so a new amperage
     * applies from the next billing month that has not been invoiced yet.
     */
    @Transactional
    public SubscriptionResponse update(Integer id, SubscriptionRequest request) {
        checkAmperage(request.amperage());
        Subscription subscription = findEntity(id);
        if (!subscription.getSubscriber().getId().equals(request.subscriberId())) {
            throw new BadRequestException("subscriberId", "a subscription cannot be moved to another subscriber");
        }
        if (subscription.getSubscriber().getStatus() != SubscriberStatus.ACTIVE) {
            throw new ConflictException("SUBSCRIBER_CLOSED", "The subscriber is closed");
        }
        if (repository.existsByMeterSerialIgnoreCaseAndIdNot(request.meterSerial().trim(), id)) {
            throw new ConflictException("DUPLICATE_METER", "Meter " + request.meterSerial().trim() + " is already registered");
        }
        boolean baseChanged = subscription.getInitialReading().compareTo(request.initialReading()) != 0
                || !subscription.getStartDate().equals(request.startDate());
        if (baseChanged && readingRepository.existsBySubscriptionId(id)) {
            throw new ConflictException("SUBSCRIPTION_HAS_READINGS",
                    "Initial reading and start date cannot change once readings exist");
        }
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(request.startDate())) {
            throw new ConflictException("INVALID_END_DATE", "The start date would be after the end date");
        }
        subscription.setMeterSerial(request.meterSerial().trim());
        subscription.setAmperage(request.amperage().shortValue());
        subscription.setInitialReading(request.initialReading());
        subscription.setStartDate(request.startDate());
        return mapper.toResponse(repository.save(subscription));
    }

    @Transactional
    public SubscriptionResponse end(Integer id, SubscriptionEndRequest request) {
        Subscription subscription = findEntity(id);
        if (request.endDate().isBefore(subscription.getStartDate())) {
            throw new ConflictException("INVALID_END_DATE", "The end date is before the start date");
        }
        Optional<MeterReading> lastReading = readingRepository.findTopBySubscriptionIdOrderByBillingMonthDesc(id);
        if (lastReading.isPresent() && request.endDate().isBefore(lastReading.get().getBillingMonth())) {
            throw new ConflictException("INVALID_END_DATE",
                    "The end date is before the last reading (" + lastReading.get().getBillingMonth() + ")");
        }
        subscription.setEndDate(request.endDate());
        return mapper.toResponse(repository.save(subscription));
    }

    /**
     * Helpers
     */
    public Subscription findEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + id));
    }

    private void checkAmperage(Integer amperage) {
        if (amperage % 5 != 0) {
            throw new BadRequestException("amperage", "must be a multiple of 5");
        }
    }
}
