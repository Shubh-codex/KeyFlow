package com.keyflow.product.service;

import com.keyflow.common.exception.ResourceNotFoundException;
import com.keyflow.product.dto.CreateProductRequest;
import com.keyflow.product.entity.Product;
import com.keyflow.product.repository.ProductRepository;
import com.keyflow.seller.repository.SellerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

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
        String description = request.description() == null || request.description().isBlank()
                ? null
                : request.description().trim();
        Product product = new Product(request.sellerId(), request.name().trim(), description);
        // saveAndFlush so the database-generated createdAt is populated before we respond.
        return productRepository.saveAndFlush(product);
    }
}