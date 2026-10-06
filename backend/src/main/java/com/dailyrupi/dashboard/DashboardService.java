package com.dailyrupi.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dailyrupi.dashboard.DashboardDtos.DayTotal;
import com.dailyrupi.dashboard.DashboardDtos.ItemTotal;
import com.dailyrupi.dashboard.DashboardDtos.MonthDashboard;
import com.dailyrupi.dashboard.DashboardDtos.MonthTotal;
import com.dailyrupi.dashboard.DashboardDtos.PaymentMethodTotal;
import com.dailyrupi.expense.Expense;
import com.dailyrupi.expense.ExpenseRepository;
import com.dailyrupi.expense.PaymentMethod;
import com.dailyrupi.expense.PaymentMethodRepository;
import com.dailyrupi.masterdata.Category;
import com.dailyrupi.masterdata.CategoryRepository;
import com.dailyrupi.masterdata.Item;
import com.dailyrupi.masterdata.ItemRepository;
import com.dailyrupi.masterdata.SubCategory;
import com.dailyrupi.masterdata.SubCategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final int RECENT_MONTHS = 6;
    private static final int TOP_ITEMS = 5;

    private final ExpenseRepository expenses;
    private final PaymentMethodRepository paymentMethods;
    private final CategoryRepository categories;
    private final SubCategoryRepository subCategories;
    private final ItemRepository items;

    public DashboardService(ExpenseRepository expenses, PaymentMethodRepository paymentMethods,
            CategoryRepository categories, SubCategoryRepository subCategories, ItemRepository items) {
        this.expenses = expenses;
        this.paymentMethods = paymentMethods;
        this.categories = categories;
        this.subCategories = subCategories;
        this.items = items;
    }

    /** One person's month is at most a few hundred expenses, so they are totalled in memory. */
    @Transactional(readOnly = true)
    public MonthDashboard month(YearMonth month) {
        LocalDate first = month.atDay(1);
        LocalDate today = LocalDate.now();
        LocalDate last = month.atEndOfMonth().isBefore(today) ? month.atEndOfMonth() : today;
        int daysCovered = last.isBefore(first) ? 0 : last.getDayOfMonth();

        List<Expense> rows = expenses.findBySpentAtGreaterThanEqualAndSpentAtLessThan(
                first.atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        BigDecimal total = sum(rows);

        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        for (int day = 1; day <= daysCovered; day++) {
            byDay.put(month.atDay(day), BigDecimal.ZERO);
        }
        for (Expense e : rows) {
            byDay.computeIfPresent(e.getSpentAt().toLocalDate(), (d, t) -> t.add(e.getAmount()));
        }
        List<DayTotal> days = byDay.entrySet().stream()
                .map(d -> new DayTotal(d.getKey(), d.getValue())).toList();

        YearMonth previous = month.minusMonths(1);
        int previousDays = Math.min(daysCovered, previous.lengthOfMonth());
        BigDecimal previousSamePeriod = expenses.sumBetween(previous.atDay(1).atStartOfDay(),
                previous.atDay(1).plusDays(previousDays).atStartOfDay());
        BigDecimal previousMonthTotal = expenses.sumBetween(previous.atDay(1).atStartOfDay(),
                month.atDay(1).atStartOfDay());

        List<MonthTotal> recentMonths = new ArrayList<>();
        for (int back = RECENT_MONTHS - 1; back >= 0; back--) {
            YearMonth m = month.minusMonths(back);
            BigDecimal monthTotal = back == 0 ? total : back == 1 ? previousMonthTotal
                    : expenses.sumBetween(m.atDay(1).atStartOfDay(), m.plusMonths(1).atDay(1).atStartOfDay());
            recentMonths.add(new MonthTotal(m.toString(), monthTotal));
        }

        BigDecimal dailyAverage = daysCovered == 0 ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(daysCovered), 2, RoundingMode.HALF_UP);
        return new MonthDashboard(month.toString(), total, rows.size(), dailyAverage,
                previousSamePeriod, previousMonthTotal, days, recentMonths,
                byPaymentMethod(rows), topItems(rows));
    }

    private List<PaymentMethodTotal> byPaymentMethod(List<Expense> rows) {
        Map<Long, PaymentMethod> methods = byId(paymentMethods.findAll(), PaymentMethod::getId);
        return rows.stream()
                .collect(Collectors.groupingBy(Expense::getPaymentMethodId))
                .entrySet().stream()
                .map(g -> {
                    PaymentMethod method = methods.get(g.getKey());
                    return new PaymentMethodTotal(method == null ? "Unknown" : method.getName(),
                            sum(g.getValue()), g.getValue().size());
                })
                .sorted(Comparator.comparing(PaymentMethodTotal::total).reversed()
                        .thenComparing(PaymentMethodTotal::name))
                .toList();
    }

    private List<ItemTotal> topItems(List<Expense> rows) {
        Map<Long, Item> itemsById = byId(items.findAll(), Item::getId);
        Map<Long, SubCategory> subCategoriesById = byId(subCategories.findAll(), SubCategory::getId);
        Map<Long, Category> categoriesById = byId(categories.findAll(), Category::getId);
        return rows.stream()
                .collect(Collectors.groupingBy(Expense::getItemId))
                .entrySet().stream()
                .map(g -> {
                    Item item = itemsById.get(g.getKey());
                    SubCategory sub = item == null ? null : subCategoriesById.get(item.getSubCategoryId());
                    Category category = sub == null ? null : categoriesById.get(sub.getCategoryId());
                    return new ItemTotal(g.getKey(), item == null ? "Unknown" : item.getName(),
                            category == null ? null : category.getName(), sum(g.getValue()), g.getValue().size());
                })
                .sorted(Comparator.comparing(ItemTotal::total).reversed().thenComparing(ItemTotal::itemName))
                .limit(TOP_ITEMS)
                .toList();
    }

    private static BigDecimal sum(List<Expense> rows) {
        return rows.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> Map<Long, T> byId(List<T> rows, Function<T, Long> id) {
        return rows.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
