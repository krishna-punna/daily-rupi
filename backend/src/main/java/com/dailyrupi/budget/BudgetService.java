package com.dailyrupi.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dailyrupi.budget.BudgetDtos.BudgetLine;
import com.dailyrupi.budget.BudgetDtos.CopyResult;
import com.dailyrupi.budget.BudgetDtos.MonthBudget;
import com.dailyrupi.common.ApiException;
import com.dailyrupi.masterdata.Category;
import com.dailyrupi.masterdata.CategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private static final YearMonth EARLIEST = YearMonth.of(2000, 1);
    private static final YearMonth LATEST = YearMonth.of(2100, 12);

    private final BudgetRepository budgets;
    private final CategoryRepository categories;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories) {
        this.budgets = budgets;
        this.categories = categories;
    }

    /** Parses yyyy-MM, refusing anything else with a 400 the screen can show. */
    public static YearMonth parseMonth(String raw) {
        try {
            YearMonth month = YearMonth.parse(raw);
            if (month.isBefore(EARLIEST) || month.isAfter(LATEST)) {
                throw badMonth();
            }
            return month;
        } catch (DateTimeParseException e) {
            throw badMonth();
        }
    }

    /**
     * Every active category, plus any inactive one that still has a budget or spending
     * this month, so nothing that counts toward the month disappears from view.
     */
    @Transactional(readOnly = true)
    public MonthBudget month(YearMonth month) {
        Map<Long, Budget> budgetByCategory = budgets.findByMonth(month.atDay(1)).stream()
                .collect(Collectors.toMap(Budget::getCategoryId, Function.identity()));
        Map<Long, BigDecimal> spentByCategory = budgets.spendByCategory(
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay()).stream()
                .collect(Collectors.toMap(CategorySpend::categoryId, CategorySpend::spent));

        List<BudgetLine> lines = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal unbudgetedSpent = BigDecimal.ZERO;
        for (Category category : categories.findAllByOrderBySortOrderAscNameAsc()) {
            Budget budget = budgetByCategory.get(category.getId());
            BigDecimal spent = spentByCategory.getOrDefault(category.getId(), BigDecimal.ZERO);
            if (!category.isActive() && budget == null && spent.signum() == 0) {
                continue;
            }
            totalSpent = totalSpent.add(spent);
            if (budget == null) {
                unbudgetedSpent = unbudgetedSpent.add(spent);
                lines.add(new BudgetLine(category.getId(), category.getName(), category.isActive(),
                        null, spent, null));
            } else {
                totalBudget = totalBudget.add(budget.getAmount());
                lines.add(new BudgetLine(category.getId(), category.getName(), category.isActive(),
                        budget.getAmount(), spent, budget.getAmount().subtract(spent)));
            }
        }
        return new MonthBudget(month.toString(), totalBudget, totalSpent, unbudgetedSpent, lines);
    }

    @Transactional
    public MonthBudget set(YearMonth month, Long categoryId, BigDecimal rawAmount) {
        // Validation already allows at most two decimals; this only normalises 600 to 600.00.
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.UNNECESSARY);
        Category category = categories.findById(categoryId).orElseThrow(BudgetService::categoryNotFound);
        Budget budget = budgets.findByCategoryIdAndMonth(categoryId, month.atDay(1)).orElse(null);
        if (budget == null) {
            // An existing budget on an inactive category may still be changed; a new one may not.
            if (!category.isActive()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INACTIVE_CATEGORY",
                        "This category is inactive. Make it active to give it a budget.");
            }
            budgets.save(new Budget(categoryId, month.atDay(1), amount));
        } else {
            budget.setAmount(amount);
        }
        return month(month);
    }

    @Transactional
    public MonthBudget remove(YearMonth month, Long categoryId) {
        Budget budget = budgets.findByCategoryIdAndMonth(categoryId, month.atDay(1))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND",
                        "No budget is set for this category and month"));
        budgets.delete(budget);
        return month(month);
    }

    /**
     * Copies last month's budgets into this month for active categories that have none yet.
     * Budgets already set this month are left as they are.
     */
    @Transactional
    public CopyResult copyFromPreviousMonth(YearMonth month) {
        Map<Long, Category> categoriesById = categories.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, Budget> existing = budgets.findByMonth(month.atDay(1)).stream()
                .collect(Collectors.toMap(Budget::getCategoryId, Function.identity()));
        int copied = 0;
        for (Budget previous : budgets.findByMonth(month.minusMonths(1).atDay(1))) {
            Category category = categoriesById.get(previous.getCategoryId());
            if (category == null || !category.isActive() || existing.containsKey(previous.getCategoryId())) {
                continue;
            }
            budgets.save(new Budget(previous.getCategoryId(), month.atDay(1), previous.getAmount()));
            copied++;
        }
        return new CopyResult(copied, month(month));
    }

    private static ApiException badMonth() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MONTH",
                "Month must look like 2026-10 and fall between 2000 and 2100");
    }

    private static ApiException categoryNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Category not found");
    }
}
