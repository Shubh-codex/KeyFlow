package com.keyflow.product.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update. A null field means "leave unchanged".
 * An empty or whitespace-only description clears it.
 */
public record UpdateProductRequest(
        @Size(max = 200, message = "name must be at most 200 characters")
        @Pattern(regexp = ".*\\S.*", message = "name must not be blank")
        String name,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description) {

    @AssertTrue(message = "at least one of name or description must be provided")
    public boolean isAnyFieldPresent() {
        return name != null || description != null;
    }
}