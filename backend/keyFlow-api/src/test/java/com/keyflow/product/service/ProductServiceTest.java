package com.keyflow.product.service;

import com.keyflow.common.exception.ResourceNotFoundException;
import com.keyflow.product.dto.CreateProductRequest;
import com.keyflow.product.entity.Product;
import com.keyflow.product.entity.ProductStatus;
import com.keyflow.product.repository.ProductRepository;
import com.keyflow.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    SellerRepository sellerRepository;

    @InjectMocks
    ProductService service;

    @Test
    void createsDraftProductWithTrimmedFields() {
        UUID sellerId = UUID.randomUUID();
        when(sellerRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new CreateProductRequest(sellerId, "  Game Key  ", "  desc  "));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(captor.capture());
        Product saved = captor.getValue();
        assertEquals(sellerId, saved.getSellerId());
        assertEquals("Game Key", saved.getName());
        assertEquals("desc", saved.getDescription());
        assertEquals(ProductStatus.DRAFT, saved.getStatus());
    }

    @Test
    void blankDescriptionIsStoredAsNull() {
        UUID sellerId = UUID.randomUUID();
        when(sellerRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.create(new CreateProductRequest(sellerId, "Name", "   "));

        assertNull(saved.getDescription());
    }

    @Test
    void unknownSellerThrowsNotFoundAndSavesNothing() {
        UUID sellerId = UUID.randomUUID();
        when(sellerRepository.existsById(sellerId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new CreateProductRequest(sellerId, "Name", null)));

        verify(productRepository, never()).saveAndFlush(any());
    }
}