package com.dailyrupi.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record DayTotal(LocalDate date, BigDecimal total) {
    }

    /** {@code month} is yyyy-MM. */
    public record MonthTotal(String month, BigDecimal total) {
    }

    public record PaymentMethodTotal(String name, BigDecimal total, long count) {
    }

    public record ItemTotal(Long itemId, String itemName, String categoryName, BigDecimal total, long count) {
    }

    /**
     * Stats for one month, by server local date. {@code days} runs from the first of the month
     * to today for the current month (future days cannot have spending), is the whole month for
     * a past month and is empty for a future one. {@code previousSamePeriod} is what was spent in
     * the same number of days at the start of the previous month, so a month in progress is
     * compared like for like. {@code recentMonths} is the six months ending with this one.
     */
    public record MonthDashboard(String month, BigDecimal total, long count, BigDecimal dailyAverage,
            BigDecimal previousSamePeriod, BigDecimal previousMonthTotal,
            List<DayTotal> days, List<MonthTotal> recentMonths,
            List<PaymentMethodTotal> paymentMethods, List<ItemTotal> topItems) {
    }
}
