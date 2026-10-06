package com.dailyrupi.dashboard;

import com.dailyrupi.budget.BudgetService;
import com.dailyrupi.dashboard.DashboardDtos.MonthDashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Needs the USER role, enforced by the /api/** rule in SecurityConfig. Months are yyyy-MM.
 * Spending by category and budget vs spent come from /api/budgets/{month}, and the
 * today/week/month totals from /api/expenses/summary, so they are not repeated here.
 */
@RestController
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/dashboard/{month}")
    public MonthDashboard month(@PathVariable String month) {
        return service.month(BudgetService.parseMonth(month));
    }
}
