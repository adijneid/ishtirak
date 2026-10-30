package org.example.ishtirak.server.service;

import jakarta.persistence.criteria.Predicate;
import org.example.ishtirak.common.dto.PageResponse;
import org.example.ishtirak.common.dto.SubscriberDetailResponse;
import org.example.ishtirak.common.dto.SubscriberRequest;
import org.example.ishtirak.common.dto.SubscriberResponse;
import org.example.ishtirak.common.model.SubscriberStatus;
import org.example.ishtirak.server.exception.BadRequestException;
import org.example.ishtirak.server.exception.ConflictException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.mapper.SubscriberMapper;
import org.example.ishtirak.server.mapper.SubscriptionMapper;
import org.example.ishtirak.server.model.Subscriber;
import org.example.ishtirak.server.model.Zone;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.SubscriberRepository;
import org.example.ishtirak.server.repository.SubscriptionRepository;
import org.example.ishtirak.server.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubscriberService {
    private final SubscriberRepository repository;
    private final ZoneRepository zoneRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriberMapper mapper;
    private final SubscriptionMapper subscriptionMapper;

    public SubscriberService(SubscriberRepository repository, ZoneRepository zoneRepository,
                             SubscriptionRepository subscriptionRepository, InvoiceRepository invoiceRepository,
                             SubscriberMapper mapper, SubscriptionMapper subscriptionMapper) {
        this.repository = repository;
        this.zoneRepository = zoneRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.mapper = mapper;
        this.subscriptionMapper = subscriptionMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<SubscriberResponse> findAll(Integer zoneId, String status, String search, int page, int size) {
        SubscriberStatus statusFilter = parseStatus(status);
        if (zoneId != null && !zoneRepository.existsById(zoneId)) {
            throw new ResourceNotFoundException("Zone not found: " + zoneId);
        }
        if (page < 0) {
            throw new BadRequestException("page", "must be 0 or more");
        }
        if (size < 1 || size > 500) {
            throw new BadRequestException("size", "must be between 1 and 500");
        }

        Specification<Subscriber> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zone").get("id"), zoneId));
            }
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(root.get("phone"), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Subscriber> result = repository.findAll(spec, PageRequest.of(page, size, Sort.by("fullName", "id")));
        Map<Integer, BigDecimal> balances = balancesOf(result.getContent().stream().map(Subscriber::getId).toList());
        List<SubscriberResponse> content = result.getContent().stream()
                .map(s -> mapper.toResponse(s, balances.getOrDefault(s.getId(), BigDecimal.ZERO)))
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public SubscriberDetailResponse findById(Integer id) {
        Subscriber subscriber = findEntity(id);
        return new SubscriberDetailResponse(
                mapper.toResponse(subscriber, balanceOf(id)),
                subscriptionRepository.findBySubscriberIdOrderByStartDateAsc(id).stream()
                        .map(subscriptionMapper::toResponse)
                        .toList());
    }

    @Transactional
    public SubscriberResponse create(SubscriberRequest request) {
        Zone zone = findZone(request.zoneId());
        if (repository.existsByPhone(request.phone().trim())) {
            throw new ConflictException("DUPLICATE_PHONE", "Another subscriber already uses phone " + request.phone().trim());
        }
        Subscriber subscriber = repository.save(mapper.toEntity(request, zone));
        return mapper.toResponse(subscriber, BigDecimal.ZERO);
    }

    @Transactional
    public SubscriberResponse update(Integer id, SubscriberRequest request) {
        Subscriber subscriber = findEntity(id);
        Zone zone = findZone(request.zoneId());
        if (repository.existsByPhoneAndIdNot(request.phone().trim(), id)) {
            throw new ConflictException("DUPLICATE_PHONE", "Another subscriber already uses phone " + request.phone().trim());
        }
        mapper.updateEntity(subscriber, request, zone);
        return mapper.toResponse(repository.save(subscriber), balanceOf(id));
    }

    @Transactional
    public SubscriberResponse close(Integer id) {
        Subscriber subscriber = findEntity(id);
        if (subscriptionRepository.existsActiveForSubscriber(id, LocalDate.now())) {
            throw new ConflictException("HAS_OPEN_ITEMS", "End the subscriber's active subscription first");
        }
        BigDecimal balance = balanceOf(id);
        if (balance.signum() > 0) {
            throw new ConflictException("HAS_OPEN_ITEMS", "The subscriber still owes LBP " + balance.toPlainString());
        }
        subscriber.setStatus(SubscriberStatus.CLOSED);
        return mapper.toResponse(repository.save(subscriber), balance);
    }

    /**
     * Helpers
     */
    public Subscriber findEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found: " + id));
    }

    public BigDecimal balanceOf(Integer subscriberId) {
        return balancesOf(List.of(subscriberId)).getOrDefault(subscriberId, BigDecimal.ZERO);
    }

    private Map<Integer, BigDecimal> balancesOf(List<Integer> ids) {
        Map<Integer, BigDecimal> balances = new HashMap<>();
        if (ids.isEmpty()) {
            return balances;
        }
        for (Object[] row : invoiceRepository.balancesForSubscribers(ids)) {
            balances.put(((Number) row[0]).intValue(), (BigDecimal) row[1]);
        }
        return balances;
    }

    private Zone findZone(Integer zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + zoneId));
    }

    private SubscriberStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SubscriberStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status", "must be ACTIVE or CLOSED");
        }
    }
}
