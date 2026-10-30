package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubscriberRequest(
        @NotBlank
        @Size(max = 80)
        String fullName,
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9][0-9 -]{5,18}[0-9]$",
                message = "must be a phone number, e.g. 03 123456 or +961 3 123456")
        String phone,
        @Size(max = 150)
        String address,
        @NotNull(message = "choose a zone")
        Integer zoneId
) {
}
