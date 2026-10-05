package com.dailyrupi.expense;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    boolean existsByItemId(Long itemId);
}
