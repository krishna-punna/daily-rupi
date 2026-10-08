-- Sync support for the Android app (app-rupi).
-- client_id: an id the phone makes for each new expense, so a create retried after a
-- dropped connection is saved once. Expenses made on the web have none.
-- updated_at: when the expense last changed, so the app can fetch only what changed.
-- expense_deletions: deleted expenses leave a row here, so the app can drop them too.

ALTER TABLE expenses ADD COLUMN client_id VARCHAR(36) NULL;
ALTER TABLE expenses ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
UPDATE expenses SET updated_at = created_at;
ALTER TABLE expenses ADD CONSTRAINT uk_expenses_client_id UNIQUE (client_id);
CREATE INDEX idx_expenses_updated_at ON expenses (updated_at);

CREATE TABLE expense_deletions (
    expense_id BIGINT      NOT NULL PRIMARY KEY,
    client_id  VARCHAR(36) NULL,
    deleted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_expense_deletions_deleted_at ON expense_deletions (deleted_at);
