package com.dailyrupi.budget;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    public record BudgetRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero")
            @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places") BigDecimal amount) {
    }

    /**
     * One category's line for the month. {@code budget} and {@code remaining} are null when
     * no budget is set; {@code remaining} is negative when the category is over budget.
     */
    public record BudgetLine(Long categoryId, String categoryName, boolean categoryActive,
            BigDecimal budget, BigDecimal spent, BigDecimal remaining) {
    }

    /**
     * {@code month} is yyyy-MM. {@code totalSpent} is everything spent in the month;
     * {@code unbudgetedSpent} is the part of it in categories that have no budget.
     */
    public record MonthBudget(String month, BigDecimal totalBudget, BigDecimal totalSpent,
            BigDecimal unbudgetedSpent, List<BudgetLine> lines) {
    }

    public record CopyResult(int copied, MonthBudget month) {
    }
}
