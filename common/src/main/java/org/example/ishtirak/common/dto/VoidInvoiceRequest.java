package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidInvoiceRequest(
        @NotBlank
        @Size(max = 200)
        String reason
) {
}
