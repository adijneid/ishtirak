package org.example.ishtirak.server.controller;

import jakarta.validation.Valid;
import org.example.ishtirak.common.dto.ZoneRequest;
import org.example.ishtirak.common.dto.ZoneResponse;
import org.example.ishtirak.server.security.AdminOnly;
import org.example.ishtirak.server.service.ZoneService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
public class ZoneController {
    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    /** Readable by collectors too: they pick a zone on the readings screen (V5). */
    @GetMapping
    public List<ZoneResponse> findAll() {
        return zoneService.findAll();
    }

    @AdminOnly
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse create(
            @Valid
            @RequestBody
            ZoneRequest request) {
        return zoneService.create(request);
    }

    @AdminOnly
    @PutMapping("/{id}")
    public ZoneResponse update(@PathVariable Integer id,
                               @Valid
                               @RequestBody
                               ZoneRequest request) {
        return zoneService.update(id, request);
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        zoneService.delete(id);
    }
}
