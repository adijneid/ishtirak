package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.ZoneRequest;
import org.example.ishtirak.common.dto.ZoneResponse;
import org.example.ishtirak.server.exception.ConflictException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.mapper.ZoneMapper;
import org.example.ishtirak.server.model.Zone;
import org.example.ishtirak.server.repository.SubscriberRepository;
import org.example.ishtirak.server.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZoneService {
    private final ZoneRepository repository;
    private final SubscriberRepository subscriberRepository;
    private final ZoneMapper mapper;

    public ZoneService(ZoneRepository repository, SubscriberRepository subscriberRepository, ZoneMapper mapper) {
        this.repository = repository;
        this.subscriberRepository = subscriberRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(zone -> mapper.toResponse(zone, subscriberRepository.countByZoneId(zone.getId())))
                .toList();
    }

    @Transactional
    public ZoneResponse create(ZoneRequest request) {
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("DUPLICATE_NAME", "A zone named '" + request.name().trim() + "' already exists");
        }
        Zone zone = repository.save(mapper.toEntity(request));
        return mapper.toResponse(zone, 0);
    }

    @Transactional
    public ZoneResponse update(Integer id, ZoneRequest request) {
        Zone zone = findEntity(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ConflictException("DUPLICATE_NAME", "A zone named '" + request.name().trim() + "' already exists");
        }
        // issued invoices keep their snapshot amounts even if the rural flag changes
        mapper.updateEntity(zone, request);
        return mapper.toResponse(repository.save(zone), subscriberRepository.countByZoneId(id));
    }

    @Transactional
    public void delete(Integer id) {
        Zone zone = findEntity(id);
        if (subscriberRepository.existsByZoneId(id)) {
            throw new ConflictException("ZONE_IN_USE", "The zone still has subscribers and cannot be deleted");
        }
        repository.delete(zone);
    }

    /**
     * Helpers
     */
    public Zone findEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + id));
    }
}
