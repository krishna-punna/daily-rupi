package com.dailyrupi.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    boolean existsByItemId(Long itemId);

    Optional<Expense> findByClientId(String clientId);

    /** Expenses created or changed at or after {@code since}, oldest change first. */
    List<Expense> findByUpdatedAtGreaterThanEqualOrderByUpdatedAtAscIdAsc(LocalDateTime since);

    /** Expenses from {@code from} (inclusive) to {@code to} (exclusive). */
    List<Expense> findBySpentAtGreaterThanEqualAndSpentAtLessThan(LocalDateTime from, LocalDateTime to);

    /** Total spent from {@code from} (inclusive) to {@code to} (exclusive); zero when nothing was spent. */
    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.spentAt >= :from and e.spentAt < :to")
    BigDecimal sumBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
