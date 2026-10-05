package com.dailyrupi.budget;

import java.math.BigDecimal;

/** Row of {@link BudgetRepository#spendByCategory}. */
public record CategorySpend(Long categoryId, BigDecimal spent) {
}
