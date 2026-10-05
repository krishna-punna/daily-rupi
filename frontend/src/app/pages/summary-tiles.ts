import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { ExpenseSummary } from '../core/api';

/** Today, this week and this month totals, shown on the Expenses and Dashboard screens. */
@Component({
  selector: 'app-summary-tiles',
  imports: [CurrencyPipe, DatePipe],
  host: { role: 'region', 'aria-label': 'Spending summary' },
  styles: `
    :host {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 8px;
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
      :host {
        gap: 6px;
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
    <div class="stat">
      <span>Today</span>
      <strong>{{ summary()?.today ?? 0 | currency: 'INR' : 'symbol' : digits(summary()?.today) }}</strong>
      <small>{{ summary()?.date | date: 'EEE d MMM' }}</small>
    </div>
    <div class="stat">
      <span>This week</span>
      <strong>{{ summary()?.week ?? 0 | currency: 'INR' : 'symbol' : digits(summary()?.week) }}</strong>
      <small>Since {{ summary()?.weekStart | date: 'EEE d MMM' }}</small>
    </div>
    <div class="stat">
      <span>This month</span>
      <strong>{{ summary()?.month ?? 0 | currency: 'INR' : 'symbol' : digits(summary()?.month) }}</strong>
      <small>{{ summary()?.monthStart | date: 'MMMM yyyy' }}</small>
    </div>
  `,
})
export class SummaryTiles {
  readonly summary = input<ExpenseSummary | null>(null);

  protected digits(amount: number | undefined): string {
    return Number.isInteger(amount ?? 0) ? '1.0-0' : '1.2-2';
  }
}
