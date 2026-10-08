package com.dailyrupi.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
            @Size(max = 255) String note,
            // Optional id made by the Android app (a UUID). A create with an id already saved returns
            // that expense instead of saving it twice. Ignored on update.
            @Pattern(regexp = "[0-9a-fA-F-]{36}", message = "must be a UUID") String clientId) {
    }

    public record ExpenseResponse(Long id, BigDecimal amount, LocalDateTime spentAt, String note,
            Long categoryId, String categoryName,
            Long subCategoryId, String subCategoryName,
            Long itemId, String itemName,
            Long paymentMethodId, String paymentMethodName,
            String clientId, LocalDateTime updatedAt) {
    }

    public record ExpensePage(List<ExpenseResponse> content, int page, int size, long totalElements) {
    }

    public record DeletedExpense(Long id, String clientId) {
    }

    /**
     * What changed since the time the app asked for, for the Android app's sync. Pass {@code nextSince}
     * back as {@code since} next time; it overlaps the previous window a little, so a change committed
     * while this one was being read is never missed, and the app applies repeats harmlessly.
     */
    public record ExpenseChanges(LocalDateTime nextSince, List<ExpenseResponse> expenses,
            List<DeletedExpense> deleted) {
    }

    public record PaymentMethodOption(Long id, String name) {
    }

    /** Totals up to now for today, the week starting Monday and the calendar month, by server local date. */
    public record ExpenseSummary(LocalDate date, LocalDate weekStart, LocalDate monthStart,
            BigDecimal today, BigDecimal week, BigDecimal month) {
    }
}
