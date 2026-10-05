import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ItemNode {
  id: number;
  name: string;
  isDefault: boolean;
  active: boolean;
}

export interface SubCategoryNode extends ItemNode {
  items: ItemNode[];
}

export interface CategoryNode extends ItemNode {
  subCategories: SubCategoryNode[];
}

/** URL segment for each level of master data. */
export type Level = 'categories' | 'sub-categories' | 'items';

export interface PaymentMethodOption {
  id: number;
  name: string;
}

export interface ExpenseRequest {
  itemId: number;
  paymentMethodId: number;
  amount: number;
  /** Local date and time, yyyy-MM-ddTHH:mm */
  spentAt: string;
  note: string | null;
}

export interface Expense {
  id: number;
  amount: number;
  spentAt: string;
  note: string | null;
  categoryId: number | null;
  categoryName: string | null;
  subCategoryId: number | null;
  subCategoryName: string | null;
  itemId: number;
  itemName: string | null;
  paymentMethodId: number;
  paymentMethodName: string | null;
}

export interface ExpensePage {
  content: Expense[];
  page: number;
  size: number;
  totalElements: number;
}

/** Totals by the server's local date; the week starts on Monday. */
export interface ExpenseSummary {
  /** yyyy-MM-dd */
  date: string;
  weekStart: string;
  monthStart: string;
  today: number;
  week: number;
  month: number;
}

@Injectable({ providedIn: 'root' })
export class MasterDataApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/master-data';

  /** Active entries only unless includeInactive is set (the management screen). */
  tree(includeInactive: boolean): Promise<CategoryNode[]> {
    return firstValueFrom(
      this.http.get<CategoryNode[]>(this.base, { params: { includeInactive } }),
    );
  }

  addCategory(name: string): Promise<unknown> {
    return firstValueFrom(this.http.post(`${this.base}/categories`, { name }));
  }

  addSubCategory(categoryId: number, name: string): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`${this.base}/categories/${categoryId}/sub-categories`, { name }),
    );
  }

  addItem(subCategoryId: number, name: string): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`${this.base}/sub-categories/${subCategoryId}/items`, { name }),
    );
  }

  rename(level: Level, id: number, name: string): Promise<unknown> {
    return firstValueFrom(this.http.put(`${this.base}/${level}/${id}`, { name }));
  }

  setActive(level: Level, id: number, active: boolean): Promise<unknown> {
    return firstValueFrom(this.http.patch(`${this.base}/${level}/${id}/status`, { active }));
  }

  remove(level: Level, id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(`${this.base}/${level}/${id}`));
  }
}

@Injectable({ providedIn: 'root' })
export class ExpenseApi {
  private readonly http = inject(HttpClient);

  paymentMethods(): Promise<PaymentMethodOption[]> {
    return firstValueFrom(this.http.get<PaymentMethodOption[]>('/api/payment-methods'));
  }

  list(page: number, size: number): Promise<ExpensePage> {
    return firstValueFrom(this.http.get<ExpensePage>('/api/expenses', { params: { page, size } }));
  }

  summary(): Promise<ExpenseSummary> {
    return firstValueFrom(this.http.get<ExpenseSummary>('/api/expenses/summary'));
  }

  create(body: ExpenseRequest): Promise<Expense> {
    return firstValueFrom(this.http.post<Expense>('/api/expenses', body));
  }

  update(id: number, body: ExpenseRequest): Promise<Expense> {
    return firstValueFrom(this.http.put<Expense>(`/api/expenses/${id}`, body));
  }

  remove(id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/expenses/${id}`));
  }
}

export interface BudgetLine {
  categoryId: number;
  categoryName: string;
  categoryActive: boolean;
  /** Null when no budget is set for this category and month. */
  budget: number | null;
  spent: number;
  /** Negative when over budget; null when no budget is set. */
  remaining: number | null;
}

export interface MonthBudget {
  /** yyyy-MM */
  month: string;
  totalBudget: number;
  totalSpent: number;
  unbudgetedSpent: number;
  lines: BudgetLine[];
}

@Injectable({ providedIn: 'root' })
export class BudgetApi {
  private readonly http = inject(HttpClient);

  /** month is yyyy-MM */
  month(month: string): Promise<MonthBudget> {
    return firstValueFrom(this.http.get<MonthBudget>(`/api/budgets/${month}`));
  }

  set(month: string, categoryId: number, amount: number): Promise<MonthBudget> {
    return firstValueFrom(
      this.http.put<MonthBudget>(`/api/budgets/${month}/categories/${categoryId}`, { amount }),
    );
  }

  remove(month: string, categoryId: number): Promise<MonthBudget> {
    return firstValueFrom(
      this.http.delete<MonthBudget>(`/api/budgets/${month}/categories/${categoryId}`),
    );
  }

  copyFromPreviousMonth(month: string): Promise<{ copied: number; month: MonthBudget }> {
    return firstValueFrom(
      this.http.post<{ copied: number; month: MonthBudget }>(
        `/api/budgets/${month}/copy-previous`,
        null,
      ),
    );
  }
}

export interface DayTotal {
  /** yyyy-MM-dd */
  date: string;
  total: number;
}

export interface MonthTotal {
  /** yyyy-MM */
  month: string;
  total: number;
}

export interface PaymentMethodTotal {
  name: string;
  total: number;
  count: number;
}

export interface ItemTotal {
  itemId: number;
  itemName: string;
  categoryName: string | null;
  total: number;
  count: number;
}

/**
 * Stats for one month by the server's local date. days runs up to today for the current
 * month and is empty for a future one; previousSamePeriod covers the same number of days at
 * the start of the previous month; recentMonths is the six months ending with this one.
 */
export interface MonthDashboard {
  month: string;
  total: number;
  count: number;
  dailyAverage: number;
  previousSamePeriod: number;
  previousMonthTotal: number;
  days: DayTotal[];
  recentMonths: MonthTotal[];
  paymentMethods: PaymentMethodTotal[];
  topItems: ItemTotal[];
}

@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  /** month is yyyy-MM */
  month(month: string): Promise<MonthDashboard> {
    return firstValueFrom(this.http.get<MonthDashboard>(`/api/dashboard/${month}`));
  }
}
