package com.keyflow.product.service;

import com.keyflow.common.exception.ConflictException;
import com.keyflow.common.exception.ResourceNotFoundException;
import com.keyflow.product.dto.CreateProductRequest;
import com.keyflow.product.dto.UpdateProductRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private final UUID sellerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    // ---- create ----

    @Test
    void createsDraftProductWithTrimmedFields() {
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
        when(sellerRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.create(new CreateProductRequest(sellerId, "Name", "   "));

        assertNull(saved.getDescription());
    }

    @Test
    void unknownSellerThrowsNotFoundAndSavesNothing() {
        when(sellerRepository.existsById(sellerId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(new CreateProductRequest(sellerId, "Name", null)));

        verify(productRepository, never()).saveAndFlush(any());
    }

    // ---- get / list ----

    @Test
    void getReturnsOwnedProduct() {
        Product product = new Product(sellerId, "Name", null);
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        assertSame(product, service.get(productId, sellerId));
    }

    @Test
    void getThrowsNotFoundWhenProductIsNotOwnedByThatSeller() {
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(productId, sellerId));
    }

    @Test
    void listIsScopedToSellerWithFixedSort() {
        when(productRepository.findBySellerId(eq(sellerId), any(Pageable.class))).thenReturn(Page.empty());

        service.list(sellerId, 2, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findBySellerId(eq(sellerId), captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")), pageable.getSort());
    }

    // ---- update ----

    @Test
    void updateChangesOnlyProvidedFields() {
        Product product = new Product(sellerId, "Old", "Old desc");
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        service.update(productId, sellerId, new UpdateProductRequest("  New  ", null));

        assertEquals("New", product.getName());
        assertEquals("Old desc", product.getDescription());
    }

    @Test
    void updateWithBlankDescriptionClearsIt() {
        Product product = new Product(sellerId, "Name", "Old desc");
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        service.update(productId, sellerId, new UpdateProductRequest(null, "   "));

        assertEquals("Name", product.getName());
        assertNull(product.getDescription());
    }

    @Test
    void updatingArchivedProductThrowsConflict() {
        Product product = new Product(sellerId, "Name", null);
        product.archive();
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.update(productId, sellerId, new UpdateProductRequest("New", null)));

        assertEquals("PRODUCT_ARCHIVED", ex.getCode());
        assertEquals("Name", product.getName());
    }

    // ---- archive ----

    @Test
    void archiveMovesProductToArchived() {
        Product product = new Product(sellerId, "Name", null);
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        Product result = service.archive(productId, sellerId);

        assertEquals(ProductStatus.ARCHIVED, result.getStatus());
    }

    @Test
    void archivingTwiceThrowsInvalidTransition() {
        Product product = new Product(sellerId, "Name", null);
        product.archive();
        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.archive(productId, sellerId));

        assertEquals("INVALID_STATUS_TRANSITION", ex.getCode());
    }
}