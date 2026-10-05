package com.dailyrupi.budget;

import com.dailyrupi.budget.BudgetDtos.BudgetRequest;
import com.dailyrupi.budget.BudgetDtos.CopyResult;
import com.dailyrupi.budget.BudgetDtos.MonthBudget;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Needs the USER role, enforced by the /api/** rule in SecurityConfig. Months are yyyy-MM. */
@RestController
@RequestMapping("/api/budgets/{month}")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public MonthBudget month(@PathVariable String month) {
        return service.month(BudgetService.parseMonth(month));
    }

    @PutMapping("/categories/{categoryId}")
    public MonthBudget set(@PathVariable String month, @PathVariable Long categoryId,
            @Valid @RequestBody BudgetRequest body) {
        return service.set(BudgetService.parseMonth(month), categoryId, body.amount());
    }

    @DeleteMapping("/categories/{categoryId}")
    public MonthBudget remove(@PathVariable String month, @PathVariable Long categoryId) {
        return service.remove(BudgetService.parseMonth(month), categoryId);
    }

    @PostMapping("/copy-previous")
    public CopyResult copyFromPreviousMonth(@PathVariable String month) {
        return service.copyFromPreviousMonth(BudgetService.parseMonth(month));
    }
}
