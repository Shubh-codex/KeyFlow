package com.keyflow.product.controller;

import com.keyflow.common.response.PageResponse;
import com.keyflow.product.dto.CreateProductRequest;
import com.keyflow.product.dto.ProductResponse;
import com.keyflow.product.dto.UpdateProductRequest;
import com.keyflow.product.entity.Product;
import com.keyflow.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * DEVELOPMENT ONLY: sellerId arrives from the client until authentication exists (Phase 4).
 * It is not proof of identity; the authenticated seller will replace it.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        Product created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(created));
    }

    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam UUID sellerId,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(productService.list(sellerId, page, size), ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id, @RequestParam UUID sellerId) {
        return ProductResponse.from(productService.get(id, sellerId));
    }

    @PatchMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id,
                                  @RequestParam UUID sellerId,
                                  @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.from(productService.update(id, sellerId, request));
    }

    @PostMapping("/{id}/archive")
    public ProductResponse archive(@PathVariable UUID id, @RequestParam UUID sellerId) {
        return ProductResponse.from(productService.archive(id, sellerId));
    }
}