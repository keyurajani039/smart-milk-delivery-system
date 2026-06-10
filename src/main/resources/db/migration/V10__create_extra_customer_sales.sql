CREATE TABLE extra_customer_sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    sale_date DATE NOT NULL,
    quantity_liters DOUBLE NOT NULL,
    amount_collected DOUBLE NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    notes VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
