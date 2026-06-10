CREATE TABLE subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    duration_days INT NOT NULL,
    max_customers INT NOT NULL,
    features_json VARCHAR(1000)
);

CREATE TABLE user_device_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    device_id VARCHAR(255) NOT NULL,
    last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    token_hash VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert standard subscription plans
INSERT INTO subscription_plans (name, price, duration_days, max_customers, features_json) VALUES 
('Free Trial', 0.0, 67, 15, '{"features": ["basic_tracking", "telegram_bot", "qr_payment"]}'),
('Bronze Plan', 299.0, 30, 50, '{"features": ["basic_tracking", "telegram_bot", "qr_payment"]}'),
('Silver Plan', 599.0, 90, 150, '{"features": ["route_optimization", "telegram_bot", "pdf_invoices", "qr_payment"]}'),
('Gold Plan', 1999.0, 365, 9999, '{"features": ["route_optimization", "telegram_bot", "pdf_invoices", "qr_payment", "gujarati_ai_bot"]}');
