package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.TariffRequest;
import org.example.ishtirak.common.dto.TariffResponse;
import org.example.ishtirak.server.exception.ConflictException;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.mapper.TariffMapper;
import org.example.ishtirak.server.model.TariffMonth;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.TariffMonthRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TariffService {
    private final TariffMonthRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final TariffMapper mapper;

    public TariffService(TariffMonthRepository repository, InvoiceRepository invoiceRepository, TariffMapper mapper) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> findAll() {
        return repository.findAllByOrderByBillingMonthDesc().stream()
                .map(t -> mapper.toResponse(t, isLocked(t.getBillingMonth())))
                .toList();
    }

    @Transactional
    public TariffResponse create(TariffRequest request) {
        LocalDate month = request.billingMonth().atDay(1);
        if (repository.existsByBillingMonth(month)) {
            throw new ConflictException("DUPLICATE_MONTH", "A tariff for " + request.billingMonth() + " already exists");
        }
        return mapper.toResponse(repository.save(mapper.toEntity(request)), false);
    }

    @Transactional
    public TariffResponse update(Integer id, TariffRequest request) {
        TariffMonth tariff = findEntity(id);
        if (isLocked(tariff.getBillingMonth())) {
            throw new ConflictException("TARIFF_LOCKED", "The month is already invoiced, its tariff can no longer change");
        }
        LocalDate newMonth = request.billingMonth().atDay(1);
        if (repository.existsByBillingMonthAndIdNot(newMonth, id)) {
            throw new ConflictException("DUPLICATE_MONTH", "A tariff for " + request.billingMonth() + " already exists");
        }
        if (!newMonth.equals(tariff.getBillingMonth()) && isLocked(newMonth)) {
            throw new ConflictException("TARIFF_LOCKED", request.billingMonth() + " is already invoiced");
        }
        mapper.updateEntity(tariff, request);
        return mapper.toResponse(repository.save(tariff), false);
    }

    /**
     * Helpers
     */
    public TariffMonth findEntity(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found: " + id));
    }

    private boolean isLocked(LocalDate month) {
        return invoiceRepository.existsActiveForMonth(month);
    }
}
