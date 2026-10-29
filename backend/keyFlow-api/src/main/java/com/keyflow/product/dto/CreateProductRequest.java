package com.keyflow.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProductRequest(
        @NotNull(message = "sellerId is required")
        UUID sellerId,

        @NotBlank(message = "name must not be blank")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description) {
}