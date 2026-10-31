package com.keyflow.product.repository;

import com.keyflow.product.entity.Product;
import com.keyflow.product.entity.ProductStatus;
import com.keyflow.seller.entity.Seller;
import com.keyflow.seller.repository.SellerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real Flyway migration and real queries against a throwaway PostgreSQL 17 container.
 * Deliberately not @Transactional: PostgreSQL's CURRENT_TIMESTAMP is fixed per transaction,
 * so rows saved in one transaction would share a timestamp and ordering could not be tested.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ProductRepositoryPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    SellerRepository sellerRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JdbcTemplate jdbc;

    private static final Sort LIST_SORT = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from digital_keys");
        jdbc.update("delete from inventory_databases");
        jdbc.update("delete from products");
        jdbc.update("delete from sellers");
    }

    private Seller newSeller(String email) {
        return sellerRepository.saveAndFlush(new Seller(email, "Test Seller"));
    }

    @Test
    void savedProductGetsDatabaseDefaultsAndStoresStatusAsString() {
        Seller seller = newSeller("a@example.com");

        Product saved = productRepository.saveAndFlush(new Product(seller.getId(), "Key", null));

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(ProductStatus.DRAFT, saved.getStatus());
        assertNull(saved.getDescription());
        String raw = jdbc.queryForObject("select status from products where id = ?", String.class, saved.getId());
        assertEquals("DRAFT", raw);
    }

    @Test
    void findByIdAndSellerIdReturnsNothingForAnotherSeller() {
        Seller owner = newSeller("owner@example.com");
        Seller other = newSeller("other@example.com");
        Product product = productRepository.saveAndFlush(new Product(owner.getId(), "Key", null));

        assertTrue(productRepository.findByIdAndSellerId(product.getId(), owner.getId()).isPresent());
        assertTrue(productRepository.findByIdAndSellerId(product.getId(), other.getId()).isEmpty());
    }

    @Test
    void listIsScopedToSellerAndOrderedNewestFirst() throws InterruptedException {
        Seller seller = newSeller("a@example.com");
        Seller other = newSeller("b@example.com");
        Product first = productRepository.saveAndFlush(new Product(seller.getId(), "first", null));
        Thread.sleep(10);
        Product second = productRepository.saveAndFlush(new Product(seller.getId(), "second", null));
        productRepository.saveAndFlush(new Product(other.getId(), "not mine", null));

        Page<Product> page = productRepository.findBySellerId(seller.getId(), PageRequest.of(0, 10, LIST_SORT));

        List<UUID> ids = page.getContent().stream().map(Product::getId).toList();
        assertEquals(List.of(second.getId(), first.getId()), ids);
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void pagingSplitsResultsAndReportsTotals() throws InterruptedException {
        Seller seller = newSeller("a@example.com");
        for (int i = 0; i < 3; i++) {
            productRepository.saveAndFlush(new Product(seller.getId(), "p" + i, null));
            Thread.sleep(10);
        }

        Page<Product> page0 = productRepository.findBySellerId(seller.getId(), PageRequest.of(0, 2, LIST_SORT));
        Page<Product> page1 = productRepository.findBySellerId(seller.getId(), PageRequest.of(1, 2, LIST_SORT));

        assertEquals(2, page0.getContent().size());
        assertEquals(1, page1.getContent().size());
        assertEquals(3, page0.getTotalElements());
        assertEquals(2, page0.getTotalPages());
    }

    @Test
    void duplicateSellerEmailIsRejectedByUniqueConstraint() {
        newSeller("dup@example.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> sellerRepository.saveAndFlush(new Seller("dup@example.com", "Another")));
    }

    @Test
    void productForNonexistentSellerIsRejectedByForeignKey() {
        assertThrows(DataIntegrityViolationException.class,
                () -> productRepository.saveAndFlush(new Product(UUID.randomUUID(), "Orphan", null)));
    }

    @Test
    void invalidStatusIsRejectedByCheckConstraint() {
        Seller seller = newSeller("a@example.com");

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into products (id, seller_id, name, status) values (?, ?, ?, ?)",
                        UUID.randomUUID(), seller.getId(), "Bad", "BOGUS"));
    }
}