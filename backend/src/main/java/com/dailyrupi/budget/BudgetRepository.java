package com.dailyrupi.budget;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByMonth(LocalDate month);

    Optional<Budget> findByCategoryIdAndMonth(Long categoryId, LocalDate month);

    /** Total spent per category between {@code from} (inclusive) and {@code to} (exclusive). */
    @Query("""
            select new com.dailyrupi.budget.CategorySpend(s.categoryId, sum(e.amount))
            from Expense e, Item i, SubCategory s
            where e.itemId = i.id and i.subCategoryId = s.id
              and e.spentAt >= :from and e.spentAt < :to
            group by s.categoryId
            """)
    List<CategorySpend> spendByCategory(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
