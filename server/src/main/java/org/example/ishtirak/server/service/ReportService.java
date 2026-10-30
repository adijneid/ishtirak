package org.example.ishtirak.server.service;

import org.example.ishtirak.common.dto.ArrearsRow;
import org.example.ishtirak.common.dto.DashboardResponse;
import org.example.ishtirak.common.dto.ZoneCollection;
import org.example.ishtirak.server.exception.ResourceNotFoundException;
import org.example.ishtirak.server.repository.InvoiceRepository;
import org.example.ishtirak.server.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
public class ReportService {
    private final InvoiceRepository invoiceRepository;
    private final ZoneRepository zoneRepository;

    public ReportService(InvoiceRepository invoiceRepository, ZoneRepository zoneRepository) {
        this.invoiceRepository = invoiceRepository;
        this.zoneRepository = zoneRepository;
    }

    /** billed = non-voided invoices of the month, collected = payments on them, outstanding = billed - collected. */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard(YearMonth month) {
        List<ZoneCollection> zones = invoiceRepository.collectionsByZone(month.atDay(1)).stream()
                .map(row -> new ZoneCollection(((Number) row[0]).intValue(), (String) row[1],
                        (BigDecimal) row[2], (BigDecimal) row[3]))
                .toList();
        BigDecimal billed = zones.stream().map(ZoneCollection::billed).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collected = zones.stream().map(ZoneCollection::collected).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rate = billed.signum() == 0 ? BigDecimal.ZERO
                : collected.multiply(new BigDecimal("100")).divide(billed, 1, RoundingMode.HALF_UP);
        long missing = invoiceRepository.countMissingReadings(month.atDay(1), month.atEndOfMonth());
        return new DashboardResponse(month, billed, collected, billed.subtract(collected), rate, missing, zones);
    }

    @Transactional(readOnly = true)
    public List<ArrearsRow> arrears(Integer zoneId) {
        if (zoneId != null && !zoneRepository.existsById(zoneId)) {
            throw new ResourceNotFoundException("Zone not found: " + zoneId);
        }
        return invoiceRepository.arrears(zoneId == null ? 0 : zoneId).stream()
                .map(row -> new ArrearsRow(((Number) row[0]).intValue(), (String) row[1], (String) row[2],
                        (String) row[3], ((Number) row[4]).longValue(), (BigDecimal) row[5]))
                .toList();
    }
}
