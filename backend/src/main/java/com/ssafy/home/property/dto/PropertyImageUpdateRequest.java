package com.ssafy.home.property.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PropertyImageUpdateRequest(
        @NotNull @Min(0) Integer sortOrder
) {}
