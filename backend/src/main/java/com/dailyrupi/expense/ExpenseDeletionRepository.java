package com.dailyrupi.expense;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseDeletionRepository extends JpaRepository<ExpenseDeletion, Long> {

    boolean existsByClientId(String clientId);

    List<ExpenseDeletion> findByDeletedAtGreaterThanEqual(LocalDateTime since);
}
