-- Guarantee that a digital key's product and inventory database belong to the same seller.
-- A composite foreign key can only target a unique column set, so the parent tables
-- get UNIQUE (id, seller_id), redundant with the primary key but required as a FK target.

ALTER TABLE products
    ADD CONSTRAINT uq_products_id_seller UNIQUE (id, seller_id);

ALTER TABLE inventory_databases
    ADD CONSTRAINT uq_inventory_databases_id_seller UNIQUE (id, seller_id);

-- Add seller_id, backfill from each key's product, then make it mandatory.
ALTER TABLE digital_keys
    ADD COLUMN seller_id UUID;

UPDATE digital_keys k
SET seller_id = p.seller_id
FROM products p
WHERE p.id = k.product_id;

ALTER TABLE digital_keys
    ALTER COLUMN seller_id SET NOT NULL;

-- Replace the two independent foreign keys with seller-aware composite ones.
ALTER TABLE digital_keys
    DROP CONSTRAINT digital_keys_product_id_fkey,
    DROP CONSTRAINT digital_keys_inventory_database_id_fkey;

ALTER TABLE digital_keys
    ADD CONSTRAINT fk_digital_keys_product_seller
        FOREIGN KEY (product_id, seller_id)
        REFERENCES products (id, seller_id),
    ADD CONSTRAINT fk_digital_keys_inventory_seller
        FOREIGN KEY (inventory_database_id, seller_id)
        REFERENCES inventory_databases (id, seller_id);