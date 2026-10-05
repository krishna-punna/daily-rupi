package com.dailyrupi.expense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByActiveTrueOrderBySortOrderAscNameAsc();
}
