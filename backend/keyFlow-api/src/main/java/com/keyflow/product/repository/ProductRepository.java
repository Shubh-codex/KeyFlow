package com.keyflow.product.repository;

import com.keyflow.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Seller-scoped listing; always paged so results are bounded.
    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);

    // Ownership-aware lookup, so services never fetch another seller's product by ID alone.
    Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);
}