package org.example.ishtirak.server.mapper;

import org.example.ishtirak.common.dto.ZoneRequest;
import org.example.ishtirak.common.dto.ZoneResponse;
import org.example.ishtirak.server.model.Zone;
import org.springframework.stereotype.Component;

@Component
public class ZoneMapper {
    public Zone toEntity(ZoneRequest request) {
        Zone zone = new Zone();
        updateEntity(zone, request);
        return zone;
    }

    public void updateEntity(Zone zone, ZoneRequest request) {
        zone.setName(request.name().trim());
        zone.setRural(Boolean.TRUE.equals(request.isRural()));
    }

    public ZoneResponse toResponse(Zone zone, long subscriberCount) {
        return new ZoneResponse(zone.getId(), zone.getName(), zone.isRural(), subscriberCount);
    }
}
