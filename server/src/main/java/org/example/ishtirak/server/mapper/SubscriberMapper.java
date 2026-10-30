package org.example.ishtirak.server.mapper;

import org.example.ishtirak.common.dto.SubscriberRequest;
import org.example.ishtirak.common.dto.SubscriberResponse;
import org.example.ishtirak.server.model.Subscriber;
import org.example.ishtirak.server.model.Zone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SubscriberMapper {
    public Subscriber toEntity(SubscriberRequest request, Zone zone) {
        Subscriber subscriber = new Subscriber();
        updateEntity(subscriber, request, zone);
        return subscriber;
    }

    public void updateEntity(Subscriber subscriber, SubscriberRequest request, Zone zone) {
        subscriber.setFullName(request.fullName().trim());
        subscriber.setPhone(request.phone().trim());
        subscriber.setAddress(request.address() == null || request.address().isBlank() ? null : request.address().trim());
        subscriber.setZone(zone);
    }

    public SubscriberResponse toResponse(Subscriber subscriber, BigDecimal balance) {
        Zone zone = subscriber.getZone();
        return new SubscriberResponse(
                subscriber.getId(),
                subscriber.getFullName(),
                subscriber.getPhone(),
                subscriber.getAddress(),
                zone != null ? zone.getId() : null,
                zone != null ? zone.getName() : null,
                subscriber.getStatus(),
                subscriber.getCreatedAt(),
                balance
        );
    }
}
