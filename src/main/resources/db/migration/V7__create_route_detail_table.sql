CREATE TABLE route_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT,
    customer_id BIGINT,
    sequence_number INT NOT NULL,
    FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);
