package com.dailyrupi.expense;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.dailyrupi.common.ApiException;
import com.dailyrupi.expense.ExpenseDtos.DeletedExpense;
import com.dailyrupi.expense.ExpenseDtos.ExpenseChanges;
import com.dailyrupi.expense.ExpenseDtos.ExpensePage;
import com.dailyrupi.expense.ExpenseDtos.ExpenseRequest;
import com.dailyrupi.expense.ExpenseDtos.ExpenseResponse;
import com.dailyrupi.expense.ExpenseDtos.ExpenseSummary;
import com.dailyrupi.expense.ExpenseDtos.PaymentMethodOption;
import com.dailyrupi.masterdata.Category;
import com.dailyrupi.masterdata.CategoryRepository;
import com.dailyrupi.masterdata.Item;
import com.dailyrupi.masterdata.ItemRepository;
import com.dailyrupi.masterdata.SubCategory;
import com.dailyrupi.masterdata.SubCategoryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    /** Tolerance for the browser clock running slightly ahead of the server. */
    private static final long CLOCK_SKEW_MINUTES = 1;

    /** How far each changes window overlaps the previous one; see {@link ExpenseChanges}. */
    private static final long SYNC_OVERLAP_SECONDS = 60;

    private final ExpenseRepository expenses;
    private final ExpenseDeletionRepository deletions;
    private final PaymentMethodRepository paymentMethods;
    private final CategoryRepository categories;
    private final SubCategoryRepository subCategories;
    private final ItemRepository items;

    public ExpenseService(ExpenseRepository expenses, ExpenseDeletionRepository deletions,
            PaymentMethodRepository paymentMethods, CategoryRepository categories,
            SubCategoryRepository subCategories, ItemRepository items) {
        this.expenses = expenses;
        this.deletions = deletions;
        this.paymentMethods = paymentMethods;
        this.categories = categories;
        this.subCategories = subCategories;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodOption> paymentMethodOptions() {
        return paymentMethods.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(p -> new PaymentMethodOption(p.getId(), p.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpensePage list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Expense> result = expenses.findAll(PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("spentAt"), Sort.Order.desc("id"))));
        Lookup lookup = new Lookup();
        return new ExpensePage(result.getContent().stream().map(lookup::toResponse).toList(),
                safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ExpenseChanges changes(LocalDateTime since) {
        LocalDateTime nextSince = LocalDateTime.now().minusSeconds(SYNC_OVERLAP_SECONDS);
        Lookup lookup = new Lookup();
        if (since == null) {
            List<ExpenseResponse> all = expenses.findAll(Sort.by(Sort.Order.asc("id"))).stream()
                    .map(lookup::toResponse).toList();
            return new ExpenseChanges(nextSince, all, List.of());
        }
        List<ExpenseResponse> changed = expenses.findByUpdatedAtGreaterThanEqualOrderByUpdatedAtAscIdAsc(since)
                .stream().map(lookup::toResponse).toList();
        List<DeletedExpense> deleted = deletions.findByDeletedAtGreaterThanEqual(since).stream()
                .map(d -> new DeletedExpense(d.getExpenseId(), d.getClientId())).toList();
        return new ExpenseChanges(nextSince, changed, deleted);
    }

    /** Future-dated expenses cannot exist, so each total runs to the start of tomorrow. */
    @Transactional(readOnly = true)
    public ExpenseSummary summary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return new ExpenseSummary(today, weekStart, monthStart,
                expenses.sumBetween(today.atStartOfDay(), end),
                expenses.sumBetween(weekStart.atStartOfDay(), end),
                expenses.sumBetween(monthStart.atStartOfDay(), end));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        String clientId = request.clientId() == null ? null : request.clientId().toLowerCase();
        if (clientId != null) {
            // A retry of a create that already reached the server: answer with what was saved.
            var saved = expenses.findByClientId(clientId);
            if (saved.isPresent()) {
                return new Lookup().toResponse(saved.get());
            }
            if (deletions.existsByClientId(clientId)) {
                throw new ApiException(HttpStatus.GONE, "DELETED", "This expense was deleted");
            }
        }
        requireNotFuture(request.spentAt());
        requireUsableItem(request.itemId());
        requireUsablePaymentMethod(request.paymentMethodId());

        Expense expense = new Expense();
        expense.setClientId(clientId);
        apply(expense, request);
        return new Lookup().toResponse(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = expenses.findById(id).orElseThrow(ExpenseService::notFound);
        requireNotFuture(request.spentAt());
        // An old expense may keep an item or payment method that has since been
        // made inactive; only a newly chosen one has to be active.
        if (!request.itemId().equals(expense.getItemId())) {
            requireUsableItem(request.itemId());
        }
        if (!request.paymentMethodId().equals(expense.getPaymentMethodId())) {
            requireUsablePaymentMethod(request.paymentMethodId());
        }
        apply(expense, request);
        return new Lookup().toResponse(expense);
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = expenses.findById(id).orElseThrow(ExpenseService::notFound);
        deletions.save(new ExpenseDeletion(expense));
        expenses.delete(expense);
    }

    private static void apply(Expense expense, ExpenseRequest request) {
        expense.setItemId(request.itemId());
        expense.setPaymentMethodId(request.paymentMethodId());
        expense.setAmount(request.amount());
        expense.setSpentAt(request.spentAt());
        String note = request.note() == null ? null : request.note().trim();
        expense.setNote(note == null || note.isEmpty() ? null : note);
    }

    private static void requireNotFuture(LocalDateTime spentAt) {
        if (spentAt.isAfter(LocalDateTime.now().plusMinutes(CLOCK_SKEW_MINUTES))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FUTURE_DATE",
                    "Date and time cannot be in the future");
        }
    }

    /** The item, its sub category and its category must all exist and be active. */
    private void requireUsableItem(Long itemId) {
        Item item = items.findById(itemId).orElseThrow(() -> invalid("Item not found"));
        SubCategory subCategory = subCategories.findById(item.getSubCategoryId())
                .orElseThrow(() -> invalid("Item not found"));
        Category category = categories.findById(subCategory.getCategoryId())
                .orElseThrow(() -> invalid("Item not found"));
        if (!item.isActive() || !subCategory.isActive() || !category.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INACTIVE_ITEM",
                    "This item is inactive and cannot be used for new expenses");
        }
    }

    private void requireUsablePaymentMethod(Long paymentMethodId) {
        PaymentMethod method = paymentMethods.findById(paymentMethodId)
                .orElseThrow(() -> invalid("Payment method not found"));
        if (!method.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INACTIVE_PAYMENT_METHOD",
                    "This payment method is inactive");
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REFERENCE", message);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Expense not found");
    }

    /** Master data is small (a few hundred rows), so names are resolved from in-memory maps. */
    private final class Lookup {

        private final Map<Long, Item> itemsById = byId(items.findAll(), Item::getId);
        private final Map<Long, SubCategory> subCategoriesById = byId(subCategories.findAll(), SubCategory::getId);
        private final Map<Long, Category> categoriesById = byId(categories.findAll(), Category::getId);
        private final Map<Long, PaymentMethod> paymentMethodsById = byId(paymentMethods.findAll(),
                PaymentMethod::getId);

        ExpenseResponse toResponse(Expense e) {
            Item item = itemsById.get(e.getItemId());
            SubCategory subCategory = item == null ? null : subCategoriesById.get(item.getSubCategoryId());
            Category category = subCategory == null ? null : categoriesById.get(subCategory.getCategoryId());
            PaymentMethod method = paymentMethodsById.get(e.getPaymentMethodId());
            return new ExpenseResponse(e.getId(), e.getAmount(), e.getSpentAt(), e.getNote(),
                    category == null ? null : category.getId(), category == null ? null : category.getName(),
                    subCategory == null ? null : subCategory.getId(),
                    subCategory == null ? null : subCategory.getName(),
                    e.getItemId(), item == null ? null : item.getName(),
                    e.getPaymentMethodId(), method == null ? null : method.getName(),
                    e.getClientId(), e.getUpdatedAt());
        }

        private static <T> Map<Long, T> byId(List<T> rows, Function<T, Long> id) {
            return rows.stream().collect(Collectors.toMap(id, Function.identity()));
        }
    }
}
