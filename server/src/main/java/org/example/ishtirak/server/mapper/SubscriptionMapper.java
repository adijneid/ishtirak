package org.example.ishtirak.server.mapper;

import org.example.ishtirak.common.dto.SubscriptionRequest;
import org.example.ishtirak.common.dto.SubscriptionResponse;
import org.example.ishtirak.server.model.Subscriber;
import org.example.ishtirak.server.model.Subscription;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {
    public Subscription toEntity(SubscriptionRequest request, Subscriber subscriber) {
        Subscription subscription = new Subscription();
        subscription.setSubscriber(subscriber);
        subscription.setMeterSerial(request.meterSerial().trim());
        subscription.setAmperage(request.amperage().shortValue());
        subscription.setInitialReading(request.initialReading());
        subscription.setStartDate(request.startDate());
        return subscription;
    }

    public SubscriptionResponse toResponse(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getSubscriber().getId(),
                subscription.getMeterSerial(),
                subscription.getAmperage(),
                subscription.getInitialReading(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.isActiveNow()
        );
    }
}
