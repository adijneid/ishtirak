package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.TariffRequest;
import org.example.ishtirak.common.dto.TariffResponse;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.TariffService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tariffs")
@AdminOnly
public class TariffController {
    private final TariffService tariffService;

    public TariffController(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @GetMapping
    public List<TariffResponse> findAll() {
        return tariffService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse create(
            @Valid
            @RequestBody
            TariffRequest request) {
        return tariffService.create(request);
    }

    @PutMapping("/{id}")
    public TariffResponse update(@PathVariable Integer id,
                                 @Valid
                                 @RequestBody
                                 TariffRequest request) {
        return tariffService.update(id, request);
    }
}
