package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull
        @Positive
        @Digits(integer = 14, fraction = 0)
        BigDecimal amount,
        @NotBlank
        @Pattern(regexp = "CASH|TRANSFER", message = "must be CASH or TRANSFER")
        String method
) {
}
