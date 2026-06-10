CREATE TABLE deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT,
    user_id BIGINT,
    delivery_date DATE NOT NULL,
    delivery_time DATETIME,
    milk_quantity DOUBLE NOT NULL,
    extra_milk DOUBLE NOT NULL,
    total_milk DOUBLE NOT NULL,
    delivery_status VARCHAR(50) NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
