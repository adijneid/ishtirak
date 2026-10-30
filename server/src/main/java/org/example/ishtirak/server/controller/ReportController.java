package org.example.ishtirak.server.controller;

import org.example.ishtirak.common.dto.ArrearsRow;
import org.example.ishtirak.common.dto.DashboardResponse;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@AdminOnly
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam YearMonth month) {
        return reportService.dashboard(month);
    }

    @GetMapping("/arrears")
    public List<ArrearsRow> arrears(@RequestParam(required = false) Integer zoneId) {
        return reportService.arrears(zoneId);
    }
}
