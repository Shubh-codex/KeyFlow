package com.keyflow.schema;

import com.keyflow.product.entity.Product;
import com.keyflow.product.repository.ProductRepository;
import com.keyflow.seller.entity.Seller;
import com.keyflow.seller.repository.SellerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the V2 migration on a throwaway PostgreSQL: a digital key's product and
 * inventory database must belong to the same seller, enforced by the database itself.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class SchemaIntegrityPostgresTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    SellerRepository sellerRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from digital_keys");
        jdbc.update("delete from inventory_databases");
        jdbc.update("delete from products");
        jdbc.update("delete from sellers");
    }

    private UUID newSeller(String email) {
        return sellerRepository.saveAndFlush(new Seller(email, "Test Seller")).getId();
    }

    private UUID newProduct(UUID sellerId) {
        return productRepository.saveAndFlush(new Product(sellerId, "Key product", null)).getId();
    }

    private UUID newInventoryDatabase(UUID sellerId) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into inventory_databases (id, seller_id, name) values (?, ?, ?)",
                id, sellerId, "Inventory");
        return id;
    }

    private void insertKey(UUID inventoryId, UUID productId, UUID sellerId) {
        jdbc.update("insert into digital_keys (id, inventory_database_id, product_id, seller_id, encrypted_key) "
                        + "values (?, ?, ?, ?, ?)",
                UUID.randomUUID(), inventoryId, productId, sellerId, "ciphertext");
    }

    @Test
    void keyWhoseProductAndInventoryShareASellerIsAccepted() {
        UUID seller = newSeller("a@example.com");

        insertKey(newInventoryDatabase(seller), newProduct(seller), seller);

        Integer count = jdbc.queryForObject("select count(*) from digital_keys", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void keyForAnotherSellersProductIsRejected() {
        UUID sellerA = newSeller("a@example.com");
        UUID sellerB = newSeller("b@example.com");
        UUID inventoryOfA = newInventoryDatabase(sellerA);
        UUID productOfB = newProduct(sellerB);

        assertThrows(DataIntegrityViolationException.class,
                () -> insertKey(inventoryOfA, productOfB, sellerA));
    }

    @Test
    void keyForAnotherSellersInventoryDatabaseIsRejected() {
        UUID sellerA = newSeller("a@example.com");
        UUID sellerB = newSeller("b@example.com");
        UUID productOfA = newProduct(sellerA);
        UUID inventoryOfB = newInventoryDatabase(sellerB);

        assertThrows(DataIntegrityViolationException.class,
                () -> insertKey(inventoryOfB, productOfA, sellerA));
    }

    @Test
    void keyWithoutSellerIdIsRejected() {
        UUID seller = newSeller("a@example.com");
        UUID inventory = newInventoryDatabase(seller);
        UUID product = newProduct(seller);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into digital_keys (id, inventory_database_id, product_id, encrypted_key) "
                                + "values (?, ?, ?, ?)",
                        UUID.randomUUID(), inventory, product, "ciphertext"));
    }
}