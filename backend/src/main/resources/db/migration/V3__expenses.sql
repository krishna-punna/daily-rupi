CREATE TABLE payment_methods (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(40) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 1000,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_payment_methods_name UNIQUE (name)
);

INSERT INTO payment_methods (name, sort_order) VALUES
    ('UPI', 10),
    ('Cash', 20),
    ('Debit Card', 30),
    ('Credit Card', 40),
    ('Net Banking', 50),
    ('Wallet', 60),
    ('Auto Debit', 70),
    ('Cheque', 80);

CREATE TABLE expenses (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id           BIGINT         NOT NULL,
    payment_method_id BIGINT         NOT NULL,
    amount            DECIMAL(12, 2) NOT NULL,
    spent_at          DATETIME(6)    NOT NULL,
    note              VARCHAR(255)   NULL,
    created_at        DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_expenses_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_expenses_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id)
);

CREATE INDEX idx_expenses_spent_at ON expenses (spent_at);
