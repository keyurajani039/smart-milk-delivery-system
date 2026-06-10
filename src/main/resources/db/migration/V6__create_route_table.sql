CREATE TABLE routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_date DATE NOT NULL,
    user_id BIGINT,
    total_distance DOUBLE NOT NULL,
    total_customers INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
