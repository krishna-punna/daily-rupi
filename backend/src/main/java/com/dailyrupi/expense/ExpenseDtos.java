package com.dailyrupi.expense;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record ExpenseRequest(
            @NotNull Long itemId,
            @NotNull Long paymentMethodId,
            @NotNull @DecimalMin(value = "0.01", message = "must be greater than zero")
            @Digits(integer = 10, fraction = 2, message = "must have at most 2 decimal places") BigDecimal amount,
            // Local date and time, for example 2026-10-05T20:41. Checked in the service: never in the future.
            @NotNull LocalDateTime spentAt,
            @Size(max = 255) String note) {
    }

    public record ExpenseResponse(Long id, BigDecimal amount, LocalDateTime spentAt, String note,
            Long categoryId, String categoryName,
            Long subCategoryId, String subCategoryName,
            Long itemId, String itemName,
            Long paymentMethodId, String paymentMethodName) {
    }

    public record ExpensePage(List<ExpenseResponse> content, int page, int size, long totalElements) {
    }

    public record PaymentMethodOption(Long id, String name) {
    }
}
