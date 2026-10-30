package org.example.ishtirak.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZoneRequest(
        @NotBlank
        @Size(max = 60)
        String name,
        Boolean isRural
) {
}
