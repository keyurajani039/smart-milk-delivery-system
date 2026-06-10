CREATE TABLE delivery_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    started_at DATETIME NOT NULL,
    ended_at DATETIME,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    loaded_milk_quantity DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
