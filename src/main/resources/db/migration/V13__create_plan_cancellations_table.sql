CREATE TABLE plan_cancellations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    cancellation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500),
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);
