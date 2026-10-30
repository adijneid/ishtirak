package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.ReadingBatchRequest;
import org.example.ishtirak.common.dto.ReadingBatchResponse;
import org.example.ishtirak.common.dto.ReadingSheetRow;
import org.example.ishtirak.server.service.ReadingService;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

/** Admin and Collector. */
@RestController
@RequestMapping("/api/readings")
public class ReadingController {
    private final ReadingService readingService;

    public ReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    /** month uses the format yyyy-MM, e.g. 2026-10. */
    @GetMapping("/sheet")
    public List<ReadingSheetRow> getSheet(@RequestParam YearMonth month,
                                          @RequestParam Integer zoneId) {
        return readingService.getSheet(month, zoneId);
    }

    @PostMapping("/batch")
    public ReadingBatchResponse saveBatch(
            @Valid
            @RequestBody
            ReadingBatchRequest request) {
        return readingService.saveBatch(request);
    }
}
