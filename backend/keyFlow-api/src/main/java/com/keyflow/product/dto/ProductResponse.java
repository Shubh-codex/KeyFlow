package com.keyflow.product.dto;

import com.keyflow.product.entity.Product;
import com.keyflow.product.entity.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID sellerId,
        String name,
        String description,
        ProductStatus status,
        Instant createdAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt());
    }
}