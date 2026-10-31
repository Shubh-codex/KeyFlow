package com.keyflow.product.service;

import com.keyflow.common.exception.ConflictException;
import com.keyflow.common.exception.ResourceNotFoundException;
import com.keyflow.product.dto.CreateProductRequest;
import com.keyflow.product.dto.UpdateProductRequest;
import com.keyflow.product.entity.Product;
import com.keyflow.product.entity.ProductStatus;
import com.keyflow.product.repository.ProductRepository;
import com.keyflow.seller.repository.SellerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    // Deterministic order; id breaks ties between identical timestamps.
    private static final Sort LIST_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;

    public ProductService(ProductRepository productRepository, SellerRepository sellerRepository) {
        this.productRepository = productRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public Product create(CreateProductRequest request) {
        if (!sellerRepository.existsById(request.sellerId())) {
            throw new ResourceNotFoundException("Seller not found.");
        }
        Product product = new Product(request.sellerId(), request.name().trim(),
                normalizeDescription(request.description()));
        // saveAndFlush so the database-generated createdAt is populated before we respond.
        return productRepository.saveAndFlush(product);
    }

    @Transactional(readOnly = true)
    public Product get(UUID id, UUID sellerId) {
        return findOwned(id, sellerId);
    }

    @Transactional(readOnly = true)
    public Page<Product> list(UUID sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, LIST_SORT);
        return productRepository.findBySellerId(sellerId, pageable);
    }

    @Transactional
    public Product update(UUID id, UUID sellerId, UpdateProductRequest request) {
        Product product = findOwned(id, sellerId);
        if (product.getStatus() == ProductStatus.ARCHIVED) {
            throw new ConflictException("PRODUCT_ARCHIVED", "Archived products cannot be modified.");
        }
        String name = request.name() != null ? request.name().trim() : product.getName();
        String description = request.description() != null
                ? normalizeDescription(request.description())
                : product.getDescription();
        product.changeDetails(name, description);
        return product; // persisted by dirty checking when the transaction commits
    }

    @Transactional
    public Product archive(UUID id, UUID sellerId) {
        Product product = findOwned(id, sellerId);
        if (product.getStatus() == ProductStatus.ARCHIVED) {
            throw new ConflictException("INVALID_STATUS_TRANSITION", "Product is already archived.");
        }
        product.archive();
        return product;
    }

    // Same 404 whether the product is missing or belongs to another seller.
    private Product findOwned(UUID id, UUID sellerId) {
        return productRepository.findByIdAndSellerId(id, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}