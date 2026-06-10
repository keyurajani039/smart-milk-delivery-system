CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT,
    month INT NOT NULL,
    year INT NOT NULL,
    amount DOUBLE NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    payment_type VARCHAR(50),
    payment_date DATE,
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);
