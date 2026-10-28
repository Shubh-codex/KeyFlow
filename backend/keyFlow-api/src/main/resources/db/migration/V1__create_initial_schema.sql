
CREATE TABLE sellers (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL REFERENCES sellers(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_product_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE inventory_databases (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL REFERENCES sellers(id),
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE digital_keys (
    id UUID PRIMARY KEY,
    inventory_database_id UUID NOT NULL
        REFERENCES inventory_databases(id),
    product_id UUID NOT NULL REFERENCES products(id),
    encrypted_key TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_digital_key_status
        CHECK (
            status IN (
                'AVAILABLE',
                'RESERVED',
                'DELIVERED',
                'DISABLED'
            )
        )
);

CREATE INDEX idx_products_seller_id
    ON products(seller_id);

CREATE INDEX idx_inventory_seller_id
    ON inventory_databases(seller_id);

CREATE INDEX idx_digital_keys_allocation
    ON digital_keys(inventory_database_id, status);
