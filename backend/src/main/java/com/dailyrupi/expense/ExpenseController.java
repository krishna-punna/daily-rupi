package com.dailyrupi.expense;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.dailyrupi.expense.ExpenseDtos.ExpenseChanges;
import com.dailyrupi.expense.ExpenseDtos.ExpensePage;
import com.dailyrupi.expense.ExpenseDtos.ExpenseRequest;
import com.dailyrupi.expense.ExpenseDtos.ExpenseResponse;
import com.dailyrupi.expense.ExpenseDtos.ExpenseSummary;
import com.dailyrupi.expense.ExpenseDtos.PaymentMethodOption;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Needs the USER role, enforced by the /api/** rule in SecurityConfig. */
@RestController
@RequestMapping("/api")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodOption> paymentMethods() {
        return service.paymentMethodOptions();
    }

    /** Newest first; {@code from} and {@code to} (both inclusive, optional) limit it to those days. */
    @GetMapping("/expenses")
    public ExpensePage list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(page, size, from, to);
    }

    /**
     * For the Android app's sync: everything when {@code since} is left out, otherwise only what changed.
     * With no {@code since}, {@code from} returns only expenses spent on or after that day, so the phone can
     * start with just the last week.
     */
    @GetMapping("/expenses/changes")
    public ExpenseChanges changes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        return service.changes(since, from);
    }

    @GetMapping("/expenses/summary")
    public ExpenseSummary summary() {
        return service.summary();
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest body) {
        return service.create(body);
    }

    @PutMapping("/expenses/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
