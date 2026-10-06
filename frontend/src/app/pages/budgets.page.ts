import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BudgetApi, BudgetLine, MonthBudget } from '../core/api';
import { messageOf } from '../core/api-error.interceptor';
import { shiftMonth, thisMonth } from '../core/months';

@Component({
  selector: 'app-budgets-page',
  imports: [FormsModule, CurrencyPipe, DatePipe, DecimalPipe],
  template: `
    <section class="card">
      <div class="toolbar">
        <h1 class="grow">Budgets for {{ month + '-01' | date: 'MMMM yyyy' }}</h1>
        <button type="button" class="ghost" (click)="go(-1)" aria-label="Previous month">‹</button>
        <input
          class="month-input"
          type="month"
          name="month"
          [ngModel]="month"
          (ngModelChange)="pick($event)"
          aria-label="Month"
        />
        <button type="button" class="ghost" (click)="go(1)" aria-label="Next month">›</button>
      </div>

      @if (data) {
        <div class="stats">
          <div>
            <small>Budget</small>
            <strong>{{ data.totalBudget | currency: 'INR' }}</strong>
          </div>
          <div>
            <small>Spent</small>
            <strong>{{ data.totalSpent | currency: 'INR' }}</strong>
          </div>
          @if (hasBudgets()) {
            <div>
              <small>Left in budgeted categories</small>
              <strong [class.over]="left() < 0">{{ left() | currency: 'INR' }}</strong>
            </div>
          }
          @if (data.unbudgetedSpent > 0) {
            <div>
              <small>Spent without a budget</small>
              <strong>{{ data.unbudgetedSpent | currency: 'INR' }}</strong>
            </div>
          }
        </div>

        @if (!hasBudgets()) {
          <p class="hint">
            No budgets set for this month yet. Enter an amount next to a category, or start from
            last month's budgets.
          </p>
        }
        <div class="actions">
          <button type="button" class="ghost" (click)="copyPrevious()" [disabled]="busy">
            Copy last month's budgets
          </button>
          @if (notice) {
            <span class="hint" role="status">{{ notice }}</span>
          }
        </div>
      }
      @if (error) {
        <p class="error" role="alert">{{ error }}</p>
      }
    </section>

    @if (data) {
      <section class="card">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Category</th>
                <th class="budget-col">Budget (₹)</th>
                <th class="num">Spent</th>
                <th class="num">Left</th>
                <th class="bar-col">Used</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (line of data.lines; track line.categoryId) {
                <tr>
                  <td>
                    {{ line.categoryName }}
                    @if (!line.categoryActive) {
                      <span class="tag off">inactive</span>
                    }
                  </td>
                  <td class="budget-col">
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      inputmode="decimal"
                      placeholder="Not set"
                      [name]="'budget-' + line.categoryId"
                      [(ngModel)]="drafts[line.categoryId]"
                      [disabled]="!line.categoryActive && line.budget === null"
                      (keydown.enter)="save(line)"
                      [attr.aria-label]="'Budget for ' + line.categoryName"
                    />
                  </td>
                  <td class="num">{{ line.spent | currency: 'INR' }}</td>
                  <td class="num" [class.over]="line.remaining !== null && line.remaining < 0">
                    @if (line.remaining !== null) {
                      {{ line.remaining | currency: 'INR' }}
                    }
                  </td>
                  <td class="bar-col">
                    @if (line.budget !== null) {
                      <div class="bar" [class.over]="line.spent > line.budget">
                        <span [style.width.%]="usedPercent(line)"></span>
                      </div>
                      <small>{{ (line.spent / line.budget) * 100 | number: '1.0-0' }}%</small>
                    }
                  </td>
                  <td class="row-actions">
                    @if (changed(line)) {
                      <button type="button" (click)="save(line)" [disabled]="busy">Save</button>
                      <button type="button" class="ghost" (click)="revert(line)">Undo</button>
                    } @else if (line.budget !== null) {
                      <button
                        type="button"
                        class="ghost danger-text"
                        (click)="remove(line)"
                        [disabled]="busy"
                      >
                        Remove
                      </button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    }
  `,
})
export class BudgetsPage implements OnInit {
  private readonly api = inject(BudgetApi);

  protected month = thisMonth();
  protected data: MonthBudget | null = null;
  /** The amount typed in each row, by category id; null means empty. */
  protected drafts: Record<number, number | null> = {};
  protected error = '';
  protected notice = '';
  protected busy = false;

  ngOnInit(): void {
    void this.load();
  }

  protected hasBudgets(): boolean {
    return this.data?.lines.some((l) => l.budget !== null) ?? false;
  }

  /** Budget minus what was spent in categories that have a budget. */
  protected left(): number {
    const d = this.data;
    return d ? d.totalBudget - (d.totalSpent - d.unbudgetedSpent) : 0;
  }

  protected changed(line: BudgetLine): boolean {
    const draft = this.drafts[line.categoryId];
    return (draft ?? null) !== line.budget;
  }

  protected usedPercent(line: BudgetLine): number {
    return line.budget ? Math.min(100, (line.spent / line.budget) * 100) : 0;
  }

  protected go(by: number): void {
    void this.pick(shiftMonth(this.month, by));
  }

  protected async pick(month: string): Promise<void> {
    // The month input reports an empty string while it is being cleared.
    if (!/^\d{4}-\d{2}$/.test(month)) {
      return;
    }
    this.month = month;
    await this.load();
  }

  protected revert(line: BudgetLine): void {
    this.drafts[line.categoryId] = line.budget;
    this.error = '';
  }

  protected async save(line: BudgetLine): Promise<void> {
    const draft = this.drafts[line.categoryId];
    if (!this.changed(line)) {
      return;
    }
    if (draft === null || draft === undefined) {
      // Clearing the box means removing the budget.
      await this.remove(line);
      return;
    }
    if (!(draft > 0)) {
      this.error = `Enter a budget greater than zero for ${line.categoryName}.`;
      return;
    }
    await this.run(
      () => this.api.set(this.month, line.categoryId, Math.round(draft * 100) / 100),
      line.categoryId,
    );
  }

  protected async remove(line: BudgetLine): Promise<void> {
    await this.run(() => this.api.remove(this.month, line.categoryId), line.categoryId);
  }

  protected async copyPrevious(): Promise<void> {
    await this.run(async () => {
      const result = await this.api.copyFromPreviousMonth(this.month);
      this.notice =
        result.copied === 0
          ? 'Nothing to copy: last month has no budgets this month is missing.'
          : `Copied ${result.copied} budget${result.copied === 1 ? '' : 's'} from last month.`;
      return result.month;
    });
  }

  private async load(): Promise<void> {
    this.notice = '';
    await this.run(() => this.api.month(this.month));
  }

  /**
   * Runs one call that returns the month, then shows it. Unsaved edits on other rows are
   * kept; the row the call saved (savedCategoryId) takes the server's value.
   */
  private async run(call: () => Promise<MonthBudget>, savedCategoryId?: number): Promise<void> {
    this.error = '';
    this.busy = true;
    const requested = this.month;
    try {
      const data = await call();
      if (data.month !== requested || requested !== this.month) {
        return; // The user moved to another month while this was loading.
      }
      const keep = this.data?.month === data.month ? this.unsavedDrafts() : {};
      if (savedCategoryId !== undefined) {
        delete keep[savedCategoryId];
      }
      this.data = data;
      this.drafts = {};
      for (const line of data.lines) {
        this.drafts[line.categoryId] = line.categoryId in keep ? keep[line.categoryId] : line.budget;
      }
    } catch (e) {
      this.error = messageOf(e);
    } finally {
      this.busy = false;
    }
  }

  private unsavedDrafts(): Record<number, number | null> {
    const unsaved: Record<number, number | null> = {};
    for (const line of this.data?.lines ?? []) {
      if (this.changed(line)) {
        unsaved[line.categoryId] = this.drafts[line.categoryId] ?? null;
      }
    }
    return unsaved;
  }
}
