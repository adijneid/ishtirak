package org.example.ishtirak.common.dto;

import java.util.List;

public record SubscriberDetailResponse(
        SubscriberResponse subscriber,
        List<SubscriptionResponse> subscriptions
) {
}
