CREATE TABLE milk_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    price_per_liter DOUBLE NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL
);
