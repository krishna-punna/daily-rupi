package com.dailyrupi.expense;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Left behind when an expense is deleted, so the Android app can drop its copy on the next sync. */
@Entity
@Table(name = "expense_deletions")
public class ExpenseDeletion {

    @Id
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;

    protected ExpenseDeletion() {
    }

    public ExpenseDeletion(Expense expense) {
        this.expenseId = expense.getId();
        this.clientId = expense.getClientId();
        this.deletedAt = LocalDateTime.now();
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public String getClientId() {
        return clientId;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
