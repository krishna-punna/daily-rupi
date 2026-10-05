-- One spending limit per category per month.
-- budget_month is always the first day of the month it covers.
-- Deleting a category (only possible once it has no sub categories, so no expenses)
-- removes its budgets with it.

CREATE TABLE budgets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT         NOT NULL,
    budget_month DATE           NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    created_at   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_budgets_category_month UNIQUE (category_id, budget_month),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
);

CREATE INDEX idx_budgets_month ON budgets (budget_month);
