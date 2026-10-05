import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  BudgetApi,
  BudgetLine,
  DashboardApi,
  DayTotal,
  ExpenseApi,
  ExpenseSummary,
  MonthBudget,
  MonthDashboard,
} from '../core/api';
import { messageOf } from '../core/api-error.interceptor';
import { shiftMonth, thisMonth } from '../core/months';
import { SummaryTiles } from './summary-tiles';

/** Short rupee amounts for chart labels: ₹950, ₹14.3k, ₹1.2L, ₹3.4Cr. */
function compactInr(amount: number): string {
  const short = (n: number, unit: string) => `₹${Number(n.toFixed(1))}${unit}`;
  if (amount >= 1e7) {
    return short(amount / 1e7, 'Cr');
  }
  if (amount >= 1e5) {
    return short(amount / 1e5, 'L');
  }
  if (amount >= 1e3) {
    return short(amount / 1e3, 'k');
  }
  return `₹${Math.round(amount)}`;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, SummaryTiles],
  styles: `
    app-summary-tiles {
      margin-bottom: 16px;
    }

    .panels {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 16px;
    }

    .panels .card {
      margin: 0;
      min-width: 0;
    }

    .panels .wide {
      grid-column: 1 / -1;
    }

    h2 small {
      display: inline;
      margin-left: 6px;
      font-size: 0.8rem;
      font-weight: 400;
    }

    .up {
      color: var(--danger);
    }

    .down {
      color: var(--accent);
    }

    /* Vertical bar charts: daily spending and the last six months. */

    .chart {
      position: relative;
      display: flex;
      align-items: flex-end;
      gap: 2px;
      height: 140px;
      border-bottom: 1px solid var(--line);
    }

    .chart .col {
      flex: 1;
      min-width: 0;
      height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      padding: 0;
      min-height: 0;
      border: none;
      border-radius: 0;
      background: none;
      cursor: pointer;
    }

    .chart .col.later {
      cursor: default;
    }

    h2 a {
      color: var(--accent);
    }

    .chart .col:hover .fill,
    .chart .col.picked .fill {
      background: var(--ink);
    }

    .fill {
      display: block;
      min-height: 2px;
      background: var(--accent);
      border-radius: 4px 4px 0 0;
    }

    .fill.empty {
      background: var(--line);
    }

    .avg {
      position: absolute;
      left: 0;
      right: 0;
      border-top: 1px dashed var(--muted);
      pointer-events: none;
    }

    .avg span {
      position: absolute;
      right: 0;
      bottom: 2px;
      padding: 0 4px;
      font-size: 0.75rem;
      color: var(--muted);
      background: var(--surface);
    }

    .axis {
      display: flex;
      justify-content: space-between;
      margin-top: 4px;
      font-size: 0.75rem;
      color: var(--muted);
    }

    .caption {
      margin: 8px 0 0;
      font-variant-numeric: tabular-nums;
    }

    .months .col {
      cursor: default;
    }

    .months .col .fill {
      background: var(--off);
    }

    .months .col.picked .fill {
      background: var(--accent);
    }

    .months .value {
      font-size: 0.75rem;
      color: var(--muted);
      text-align: center;
      white-space: nowrap;
      font-variant-numeric: tabular-nums;
    }

    .months .col {
      gap: 2px;
    }

    .months .fill {
      margin: 0 auto;
      width: min(100%, 40px);
    }

    .month-axis {
      display: flex;
      gap: 2px;
      margin-top: 4px;
      font-size: 0.75rem;
      color: var(--muted);
    }

    .month-axis span {
      flex: 1;
      min-width: 0;
      text-align: center;
    }

    /* Ranked lists with a bar under each row. */

    .rows {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .rows li + li {
      margin-top: 12px;
    }

    .line {
      display: flex;
      align-items: baseline;
      gap: 8px;
    }

    .line .grow {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .line .num {
      font-weight: 600;
    }

    .rows small {
      display: inline;
    }

    .rows .bar {
      margin-top: 4px;
    }

    .meta {
      display: flex;
      justify-content: space-between;
      gap: 8px;
      margin-top: 2px;
    }

    .meta small {
      display: block;
    }

    @media (max-width: 720px) {
      .panels {
        grid-template-columns: 1fr;
      }

      .chart {
        height: 120px;
      }
    }
  `,
  template: `
    <app-summary-tiles [summary]="summary" />

    <section class="card">
      <div class="toolbar">
        <h1 class="grow">{{ month + '-01' | date: 'MMMM yyyy' }}</h1>
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
            <small>Spent</small>
            <strong>{{ data.total | currency: 'INR' }}</strong>
          </div>
          <div>
            <small>Expenses</small>
            <strong>{{ data.count }}</strong>
          </div>
          <div>
            <small>Per day</small>
            <strong>{{ data.dailyAverage | currency: 'INR' }}</strong>
          </div>
          @if (budget && budget.totalBudget > 0) {
            <div>
              <small>Budget used</small>
              <strong [class.over]="budgetUsed() > 100">
                {{ budgetUsed() | number: '1.0-0' }}% of {{ budget.totalBudget | currency: 'INR' : 'symbol' : '1.0-0' }}
              </strong>
            </div>
          }
        </div>
        @if (comparison(); as c) {
          <p class="hint" role="status">
            <strong [class.up]="c.change > 0" [class.down]="c.change < 0">
              {{ c.change > 0 ? '▲' : c.change < 0 ? '▼' : '' }}
              {{ c.change === 0 ? 'Same' : (abs(c.change) | currency: 'INR') + (c.change > 0 ? ' more' : ' less') }}
              @if (c.percent !== null) {
                ({{ abs(c.percent) | number: '1.0-0' }}%)
              }
            </strong>
            than {{ c.label }}
          </p>
        }
        @if (data.count === 0) {
          <p class="hint">No expenses in this month{{ data.days.length === 0 ? ' yet' : '' }}.</p>
        }
      }
      @if (error) {
        <p class="error" role="alert">{{ error }}</p>
      }
    </section>

    @if (data) {
      <div class="panels">
        @if (data.days.length > 0) {
          <section class="card wide">
            <h2>Daily spending</h2>
            <div class="chart" role="group" aria-label="Spending per day">
              @if (data.dailyAverage > 0) {
                <div class="avg" [style.bottom.%]="(data.dailyAverage / dayMax()) * 100">
                  <span>avg {{ compact(data.dailyAverage) }}</span>
                </div>
              }
              @for (day of data.days; track day.date) {
                <button
                  type="button"
                  class="col"
                  [class.picked]="day.date === pickedDay?.date"
                  (click)="pickedDay = day"
                  [attr.aria-label]="(day.date | date: 'EEE d MMM') + ': ' + (day.total | currency: 'INR')"
                  [title]="(day.date | date: 'EEE d MMM') + ': ' + (day.total | currency: 'INR')"
                >
                  <span
                    class="fill"
                    [class.empty]="day.total === 0"
                    [style.height.%]="(day.total / dayMax()) * 100"
                  ></span>
                </button>
              }
              @for (slot of daysToCome(); track slot) {
                <span class="col later" aria-hidden="true"></span>
              }
            </div>
            <div class="axis">
              <span>{{ data.days[0].date | date: 'd MMM' }}</span>
              <span>{{ monthEnd() | date: 'd MMM' }}</span>
            </div>
            <p class="caption">
              @if (pickedDay) {
                <strong>{{ pickedDay.date | date: 'EEE d MMM' }}:</strong>
                {{ pickedDay.total | currency: 'INR' }}
              } @else if (biggestDay(); as top) {
                Biggest day: <strong>{{ top.date | date: 'EEE d MMM' }}</strong>,
                {{ top.total | currency: 'INR' }}.
                <span class="hint">Tap a bar to see any day.</span>
              }
            </p>
          </section>
        }

        <section class="card wide">
          <h2>Last 6 months</h2>
          <div class="chart months" role="group" aria-label="Spending per month">
            @for (m of data.recentMonths; track m.month) {
              <div
                class="col"
                [class.picked]="m.month === data.month"
                [attr.aria-label]="(m.month + '-01' | date: 'MMMM yyyy') + ': ' + (m.total | currency: 'INR')"
                [title]="(m.month + '-01' | date: 'MMMM yyyy') + ': ' + (m.total | currency: 'INR')"
              >
                <span class="value">{{ compact(m.total) }}</span>
                <span class="fill" [class.empty]="m.total === 0" [style.height.%]="(m.total / monthMax()) * 80"></span>
              </div>
            }
          </div>
          <div class="month-axis">
            @for (m of data.recentMonths; track m.month) {
              <span>{{ m.month + '-01' | date: 'MMM' }}</span>
            }
          </div>
        </section>

        @if (spentLines().length > 0) {
          <section class="card">
            <h2>By category</h2>
            <ul class="rows">
              @for (line of spentLines(); track line.categoryId) {
                <li>
                  <div class="line">
                    <span class="grow">{{ line.categoryName }}</span>
                    <span class="num">{{ line.spent | currency: 'INR' }}</span>
                  </div>
                  <div class="bar"><span [style.width.%]="share(line.spent)"></span></div>
                  <small>{{ share(line.spent) | number: '1.0-0' }}% of the month</small>
                </li>
              }
            </ul>
          </section>
        }

        @if (budgetLines().length > 0) {
          <section class="card">
            <h2>Budget vs spent <small><a routerLink="/budgets">Edit budgets</a></small></h2>
            <ul class="rows">
              @for (line of budgetLines(); track line.categoryId) {
                <li>
                  <div class="line">
                    <span class="grow">{{ line.categoryName }}</span>
                    <span class="num" [class.over]="line.spent > line.budget!">
                      {{ line.spent | currency: 'INR' : 'symbol' : '1.0-0' }}
                      <small>of {{ line.budget | currency: 'INR' : 'symbol' : '1.0-0' }}</small>
                    </span>
                  </div>
                  <div class="bar" [class.over]="line.spent > line.budget!">
                    <span [style.width.%]="usedPercent(line)"></span>
                  </div>
                  <div class="meta">
                    <small>{{ (line.spent / line.budget!) * 100 | number: '1.0-0' }}% used</small>
                    <small [class.over]="line.remaining! < 0">
                      {{ abs(line.remaining!) | currency: 'INR' }} {{ line.remaining! < 0 ? 'over' : 'left' }}
                    </small>
                  </div>
                </li>
              }
            </ul>
          </section>
        }

        @if (data.topItems.length > 0) {
          <section class="card">
            <h2>Top items</h2>
            <ol class="rows">
              @for (item of data.topItems; track item.itemId) {
                <li>
                  <div class="line">
                    <span class="grow">{{ item.itemName }}</span>
                    <span class="num">{{ item.total | currency: 'INR' }}</span>
                  </div>
                  <div class="meta">
                    <small>{{ item.categoryName }}</small>
                    <small>{{ item.count }} {{ item.count === 1 ? 'time' : 'times' }}</small>
                  </div>
                </li>
              }
            </ol>
          </section>
        }

        @if (data.paymentMethods.length > 0) {
          <section class="card">
            <h2>Payment methods</h2>
            <ul class="rows">
              @for (method of data.paymentMethods; track method.name) {
                <li>
                  <div class="line">
                    <span class="grow">{{ method.name }}</span>
                    <span class="num">{{ method.total | currency: 'INR' }}</span>
                  </div>
                  <div class="bar"><span [style.width.%]="share(method.total)"></span></div>
                  <div class="meta">
                    <small>{{ share(method.total) | number: '1.0-0' }}% of the month</small>
                    <small>{{ method.count }} {{ method.count === 1 ? 'payment' : 'payments' }}</small>
                  </div>
                </li>
              }
            </ul>
          </section>
        }
      </div>
    }
  `,
})
export class DashboardPage implements OnInit {
  private readonly dashboardApi = inject(DashboardApi);
  private readonly budgetApi = inject(BudgetApi);
  private readonly expenseApi = inject(ExpenseApi);

  protected month = thisMonth();
  protected data: MonthDashboard | null = null;
  protected budget: MonthBudget | null = null;
  protected summary: ExpenseSummary | null = null;
  protected pickedDay: DayTotal | null = null;
  protected error = '';

  protected readonly abs = Math.abs;

  ngOnInit(): void {
    void this.load();
  }

  protected compact(amount: number): string {
    return compactInr(amount);
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

  private daysInMonth(): number {
    const [year, m] = (this.data?.month ?? this.month).split('-').map(Number);
    return new Date(year, m, 0).getDate();
  }

  /** Empty slots for the rest of a month in progress, so the chart always spans the month. */
  protected daysToCome(): number[] {
    const shown = this.data?.days.length ?? 0;
    return Array.from({ length: shown > 0 ? this.daysInMonth() - shown : 0 }, (_, i) => i);
  }

  protected monthEnd(): string {
    return `${this.data?.month ?? this.month}-${this.daysInMonth()}`;
  }

  /** Largest day, so the tallest bar fills the chart; never zero. */
  protected dayMax(): number {
    return Math.max(1, ...(this.data?.days.map((d) => d.total) ?? []));
  }

  protected monthMax(): number {
    return Math.max(1, ...(this.data?.recentMonths.map((m) => m.total) ?? []));
  }

  protected biggestDay(): DayTotal | null {
    const days = this.data?.days ?? [];
    const top = days.reduce<DayTotal | null>((best, d) => (!best || d.total > best.total ? d : best), null);
    return top && top.total > 0 ? top : null;
  }

  /** Percent of this month's total. */
  protected share(amount: number): number {
    return this.data && this.data.total > 0 ? (amount / this.data.total) * 100 : 0;
  }

  protected spentLines(): BudgetLine[] {
    return (this.budget?.lines ?? []).filter((l) => l.spent > 0).sort((a, b) => b.spent - a.spent);
  }

  protected budgetLines(): BudgetLine[] {
    return (this.budget?.lines ?? []).filter((l) => l.budget !== null);
  }

  protected usedPercent(line: BudgetLine): number {
    return line.budget ? Math.min(100, (line.spent / line.budget) * 100) : 0;
  }

  /** Spending in budgeted categories as a percent of the month's total budget. */
  protected budgetUsed(): number {
    const b = this.budget;
    return b && b.totalBudget > 0 ? ((b.totalSpent - b.unbudgetedSpent) / b.totalBudget) * 100 : 0;
  }

  /**
   * This month against the same days of last month while the month is in progress, and
   * against the whole of last month once it is over. Null when there is nothing to compare.
   */
  protected comparison(): { change: number; percent: number | null; label: string } | null {
    const d = this.data;
    if (!d || d.days.length === 0) {
      return null;
    }
    const inProgress = d.days.length < this.daysInMonth();
    const before = inProgress ? d.previousSamePeriod : d.previousMonthTotal;
    if (before === 0 && d.total === 0) {
      return null;
    }
    const label = inProgress
      ? `the first ${d.days.length} ${d.days.length === 1 ? 'day' : 'days'} of last month`
      : 'last month';
    return {
      change: d.total - before,
      percent: before > 0 ? ((d.total - before) / before) * 100 : null,
      label,
    };
  }

  private async load(): Promise<void> {
    this.error = '';
    const requested = this.month;
    try {
      const [data, budget, summary] = await Promise.all([
        this.dashboardApi.month(requested),
        this.budgetApi.month(requested),
        this.expenseApi.summary(),
      ]);
      if (requested !== this.month) {
        return; // The user moved to another month while this was loading.
      }
      this.data = data;
      this.budget = budget;
      this.summary = summary;
      this.pickedDay = null;
    } catch (e) {
      this.error = messageOf(e);
    }
  }
}
