import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  CategoryNode,
  Expense,
  ExpenseApi,
  ExpenseSummary,
  ItemNode,
  MasterDataApi,
  PaymentMethodOption,
  SubCategoryNode,
} from '../core/api';
import { messageOf } from '../core/api-error.interceptor';

/** Current local date and time in the format a datetime-local input uses. */
function nowLocal(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
}

@Component({
  selector: 'app-expenses-page',
  imports: [FormsModule, CurrencyPipe, DatePipe],
  styles: `
    .summary {
      position: sticky;
      top: 0;
      z-index: 10;
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 8px;
      margin: -16px -16px 16px;
      padding: 12px 16px;
      background: var(--bg);
      border-bottom: 1px solid var(--line);
    }

    .stat {
      min-width: 0;
      padding: 10px 12px;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: var(--radius);
    }

    .stat span,
    .stat small {
      display: block;
      color: var(--muted);
      font-size: 0.8rem;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .stat strong {
      display: block;
      font-size: 1.25rem;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    @media (max-width: 480px) {
      .summary {
        gap: 6px;
        padding: 8px 16px;
      }

      .stat {
        padding: 8px;
      }

      .stat strong {
        font-size: 1rem;
      }

      .stat small {
        display: none;
      }
    }
  `,
  template: `
    <section class="summary" aria-label="Spending summary">
      <div class="stat">
        <span>Today</span>
        <strong>{{ summary?.today ?? 0 | currency: 'INR' : 'symbol' : digits(summary?.today) }}</strong>
        <small>{{ summary?.date | date: 'EEE d MMM' }}</small>
      </div>
      <div class="stat">
        <span>This week</span>
        <strong>{{ summary?.week ?? 0 | currency: 'INR' : 'symbol' : digits(summary?.week) }}</strong>
        <small>Since {{ summary?.weekStart | date: 'EEE d MMM' }}</small>
      </div>
      <div class="stat">
        <span>This month</span>
        <strong>{{ summary?.month ?? 0 | currency: 'INR' : 'symbol' : digits(summary?.month) }}</strong>
        <small>{{ summary?.monthStart | date: 'MMMM yyyy' }}</small>
      </div>
    </section>

    <section class="card">
      <h1>{{ editing ? 'Edit expense' : 'Add expense' }}</h1>

      <form class="grid" (ngSubmit)="submit()">
        <label>
          Category
          <select
            name="category"
            [ngModel]="categoryId"
            (ngModelChange)="onCategoryChange($event)"
          >
            <option [ngValue]="null">Select a category</option>
            @for (c of categories; track c.id) {
              <option [ngValue]="c.id">{{ c.name }}</option>
            }
          </select>
        </label>

        <label>
          Sub category
          <select
            name="subCategory"
            [ngModel]="subCategoryId"
            (ngModelChange)="onSubCategoryChange($event)"
            [disabled]="categoryId === null"
          >
            <option [ngValue]="null">Select a sub category</option>
            @for (s of subCategories(); track s.id) {
              <option [ngValue]="s.id">{{ s.name }}</option>
            }
          </select>
        </label>

        <label>
          Item
          <select name="item" [(ngModel)]="itemId" [disabled]="subCategoryId === null">
            <option [ngValue]="null">Select an item</option>
            @for (i of items(); track i.id) {
              <option [ngValue]="i.id">{{ i.name }}</option>
            }
          </select>
        </label>

        <label>
          Amount (₹)
          <input
            name="amount"
            type="number"
            [(ngModel)]="amount"
            min="0.01"
            step="0.01"
            inputmode="decimal"
            placeholder="0.00"
          />
        </label>

        <label>
          Date and time
          <input
            name="spentAt"
            type="datetime-local"
            [(ngModel)]="spentAt"
            [max]="maxDateTime"
            (focus)="maxDateTime = now()"
          />
        </label>

        <label>
          Paid by
          <select name="paymentMethod" [(ngModel)]="paymentMethodId">
            @for (p of paymentMethods; track p.id) {
              <option [ngValue]="p.id">{{ p.name }}</option>
            }
          </select>
        </label>

        <label class="wide">
          Note (optional)
          <input name="note" [(ngModel)]="note" maxlength="255" />
        </label>

        @if (keptItem) {
          <p class="hint wide">
            This expense uses <strong>{{ keptItem.label }}</strong>, which is now inactive. Leave
            the dropdowns as they are to keep it, or pick an active item to replace it.
          </p>
        }
        @if (error) {
          <p class="error wide" role="alert">{{ error }}</p>
        }

        <div class="actions wide">
          <button type="submit" [disabled]="busy">{{ editing ? 'Save changes' : 'Add expense' }}</button>
          @if (editing) {
            <button type="button" class="ghost" (click)="resetForm()">Cancel</button>
          }
        </div>
      </form>
    </section>

    <section class="card">
      <h2>Recent expenses</h2>
      @if (expenses.length === 0) {
        <p class="hint">No expenses yet. Add your first one above.</p>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date and time</th>
                <th>Item</th>
                <th>Category</th>
                <th>Paid by</th>
                <th class="num">Amount</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (e of expenses; track e.id) {
                <tr>
                  <td>{{ e.spentAt | date: 'dd MMM yyyy, h:mm a' }}</td>
                  <td>
                    {{ e.itemName }}
                    @if (e.note) {
                      <small>{{ e.note }}</small>
                    }
                  </td>
                  <td>{{ e.categoryName }} › {{ e.subCategoryName }}</td>
                  <td>{{ e.paymentMethodName }}</td>
                  <td class="num">{{ e.amount | currency: 'INR' }}</td>
                  <td class="row-actions">
                    @if (confirmDeleteId === e.id) {
                      <button type="button" class="danger" (click)="remove(e)">Confirm delete</button>
                      <button type="button" class="ghost" (click)="confirmDeleteId = null">Cancel</button>
                    } @else {
                      <button type="button" class="ghost" (click)="edit(e)">Edit</button>
                      <button type="button" class="ghost danger-text" (click)="confirmDeleteId = e.id">
                        Delete
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div class="pager">
          <button type="button" class="ghost" (click)="goTo(page - 1)" [disabled]="page === 0">
            Newer
          </button>
          <span>Page {{ page + 1 }} of {{ pageCount() }}</span>
          <button
            type="button"
            class="ghost"
            (click)="goTo(page + 1)"
            [disabled]="page + 1 >= pageCount()"
          >
            Older
          </button>
        </div>
      }
    </section>
  `,
})
export class ExpensesPage implements OnInit {
  private readonly masterData = inject(MasterDataApi);
  private readonly api = inject(ExpenseApi);
  private readonly pageSize = 20;

  /** Active entries only: inactive ones never reach these dropdowns. */
  protected categories: CategoryNode[] = [];
  protected paymentMethods: PaymentMethodOption[] = [];
  protected expenses: Expense[] = [];
  protected summary: ExpenseSummary | null = null;
  protected page = 0;
  protected total = 0;

  protected categoryId: number | null = null;
  protected subCategoryId: number | null = null;
  protected itemId: number | null = null;
  protected paymentMethodId: number | null = null;
  protected amount: number | null = null;
  protected spentAt = nowLocal();
  protected maxDateTime = nowLocal();
  protected note = '';

  protected editing: Expense | null = null;
  /** When editing an expense whose item has been made inactive. */
  protected keptItem: { id: number; label: string } | null = null;
  protected confirmDeleteId: number | null = null;
  protected error = '';
  protected busy = false;

  protected readonly now = nowLocal;

  ngOnInit(): void {
    void this.load();
  }

  /** Whole rupees stay short on a phone; paise show only when there are some. */
  protected digits(amount: number | undefined): string {
    return Number.isInteger(amount ?? 0) ? '1.0-0' : '1.2-2';
  }

  protected subCategories(): SubCategoryNode[] {
    return this.categories.find((c) => c.id === this.categoryId)?.subCategories ?? [];
  }

  protected items(): ItemNode[] {
    return this.subCategories().find((s) => s.id === this.subCategoryId)?.items ?? [];
  }

  protected pageCount(): number {
    return Math.max(1, Math.ceil(this.total / this.pageSize));
  }

  protected onCategoryChange(id: number | null): void {
    this.categoryId = id;
    this.subCategoryId = null;
    this.itemId = null;
  }

  protected onSubCategoryChange(id: number | null): void {
    this.subCategoryId = id;
    this.itemId = null;
  }

  protected async submit(): Promise<void> {
    this.error = '';
    this.maxDateTime = nowLocal();

    const itemId = this.itemId ?? this.keptItem?.id ?? null;
    if (itemId === null) {
      this.error = 'Select a category, sub category and item.';
      return;
    }
    if (this.paymentMethodId === null) {
      this.error = 'Select how you paid.';
      return;
    }
    if (this.amount === null || !(this.amount > 0)) {
      this.error = 'Enter an amount greater than zero.';
      return;
    }
    if (!this.spentAt) {
      this.error = 'Select the date and time.';
      return;
    }
    if (this.spentAt > this.maxDateTime) {
      this.error = 'Date and time cannot be in the future.';
      return;
    }

    const body = {
      itemId,
      paymentMethodId: this.paymentMethodId,
      amount: Math.round(this.amount * 100) / 100,
      spentAt: this.spentAt,
      note: this.note.trim() || null,
    };

    this.busy = true;
    try {
      if (this.editing) {
        await this.api.update(this.editing.id, body);
      } else {
        await this.api.create(body);
        this.page = 0;
      }
      this.resetForm();
      await this.loadExpenses();
    } catch (e) {
      this.error = messageOf(e);
    } finally {
      this.busy = false;
    }
  }

  protected edit(expense: Expense): void {
    this.editing = expense;
    this.error = '';
    this.confirmDeleteId = null;
    this.amount = expense.amount;
    this.spentAt = expense.spentAt.slice(0, 16);
    this.note = expense.note ?? '';
    this.paymentMethodId = this.paymentMethods.some((p) => p.id === expense.paymentMethodId)
      ? expense.paymentMethodId
      : (this.paymentMethods[0]?.id ?? null);

    const category = this.categories.find((c) => c.id === expense.categoryId);
    const sub = category?.subCategories.find((s) => s.id === expense.subCategoryId);
    const item = sub?.items.find((i) => i.id === expense.itemId);
    if (category && sub && item) {
      this.categoryId = category.id;
      this.subCategoryId = sub.id;
      this.itemId = item.id;
      this.keptItem = null;
    } else {
      // The item, or something above it, is inactive now.
      this.categoryId = null;
      this.subCategoryId = null;
      this.itemId = null;
      this.keptItem = { id: expense.itemId, label: expense.itemName ?? 'an inactive item' };
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected async remove(expense: Expense): Promise<void> {
    this.confirmDeleteId = null;
    this.error = '';
    try {
      await this.api.remove(expense.id);
      if (this.editing?.id === expense.id) {
        this.resetForm();
      }
      if (this.expenses.length === 1 && this.page > 0) {
        this.page--;
      }
      await this.loadExpenses();
    } catch (e) {
      this.error = messageOf(e);
    }
  }

  protected async goTo(page: number): Promise<void> {
    this.page = Math.max(0, page);
    await this.loadExpenses();
  }

  /** Keeps the chosen category and payment method, which usually repeat. */
  protected resetForm(): void {
    this.editing = null;
    this.keptItem = null;
    this.itemId = null;
    this.amount = null;
    this.note = '';
    this.spentAt = nowLocal();
    this.maxDateTime = this.spentAt;
    this.error = '';
  }

  private async load(): Promise<void> {
    try {
      const [categories, paymentMethods] = await Promise.all([
        this.masterData.tree(false),
        this.api.paymentMethods(),
      ]);
      this.categories = categories;
      this.paymentMethods = paymentMethods;
      this.paymentMethodId = paymentMethods[0]?.id ?? null;
      await this.loadExpenses();
    } catch (e) {
      this.error = messageOf(e);
    }
  }

  /** Also refreshes the summary, since every add, edit and delete can change it. */
  private async loadExpenses(): Promise<void> {
    const [result, summary] = await Promise.all([
      this.api.list(this.page, this.pageSize),
      this.api.summary(),
    ]);
    this.expenses = result.content;
    this.total = result.totalElements;
    this.summary = summary;
  }
}
