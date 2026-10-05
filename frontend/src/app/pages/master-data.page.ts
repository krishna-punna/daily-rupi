import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { CategoryNode, Level, MasterDataApi } from '../core/api';
import { messageOf } from '../core/api-error.interceptor';
import { AddBox } from './add-box';
import { EntryRow } from './entry-row';

@Component({
  selector: 'app-master-data-page',
  imports: [FormsModule, EntryRow, AddBox],
  template: `
    <section class="card">
      <h1>Master data</h1>
      <p class="hint">
        Categories contain sub categories, which contain items. Inactive entries stay here but
        disappear from the dropdowns when you add an expense, along with everything beneath them.
      </p>

      <div class="toolbar">
        <input
          class="grow"
          type="search"
          [(ngModel)]="filter"
          placeholder="Search categories, sub categories and items"
          aria-label="Search master data"
        />
        <label class="check">
          <input type="checkbox" [(ngModel)]="showInactive" /> Show inactive
        </label>
      </div>

      @if (error) {
        <p class="error" role="alert">{{ error }}</p>
      }

      <app-add-box placeholder="New category name" (add)="run(api.addCategory($event))" />

      @if (loading) {
        <p class="hint">Loading…</p>
      }

      <ul class="tree">
        @for (category of visibleCategories(); track category.id) {
          <li>
            <app-entry-row
              [name]="category.name"
              [active]="category.active"
              [isDefault]="category.isDefault"
              [expandable]="true"
              [expanded]="isOpen('c', category.id)"
              [childCount]="category.subCategories.length"
              (toggleExpand)="toggle('c', category.id)"
              (rename)="run(api.rename('categories', category.id, $event))"
              (setActive)="run(api.setActive('categories', category.id, $event))"
              (remove)="run(api.remove('categories', category.id))"
            />
            @if (isOpen('c', category.id)) {
              <ul>
                @for (sub of visibleSubCategories(category); track sub.id) {
                  <li>
                    <app-entry-row
                      [name]="sub.name"
                      [active]="sub.active"
                      [isDefault]="sub.isDefault"
                      [expandable]="true"
                      [expanded]="isOpen('s', sub.id)"
                      [childCount]="sub.items.length"
                      (toggleExpand)="toggle('s', sub.id)"
                      (rename)="run(api.rename('sub-categories', sub.id, $event))"
                      (setActive)="run(api.setActive('sub-categories', sub.id, $event))"
                      (remove)="run(api.remove('sub-categories', sub.id))"
                    />
                    @if (isOpen('s', sub.id)) {
                      <ul>
                        @for (item of visibleItems(sub); track item.id) {
                          <li>
                            <app-entry-row
                              [name]="item.name"
                              [active]="item.active"
                              [isDefault]="item.isDefault"
                              (rename)="run(api.rename('items', item.id, $event))"
                              (setActive)="run(api.setActive('items', item.id, $event))"
                              (remove)="run(api.remove('items', item.id))"
                            />
                          </li>
                        }
                        <li>
                          <app-add-box
                            [placeholder]="'New item under ' + sub.name"
                            (add)="run(api.addItem(sub.id, $event))"
                          />
                        </li>
                      </ul>
                    }
                  </li>
                }
                <li>
                  <app-add-box
                    [placeholder]="'New sub category under ' + category.name"
                    (add)="run(api.addSubCategory(category.id, $event))"
                  />
                </li>
              </ul>
            }
          </li>
        } @empty {
          @if (!loading) {
            <li class="hint">Nothing matches.</li>
          }
        }
      </ul>
    </section>
  `,
})
export class MasterDataPage implements OnInit {
  protected readonly api = inject(MasterDataApi);

  protected categories: CategoryNode[] = [];
  protected filter = '';
  protected showInactive = true;
  protected loading = true;
  protected error = '';

  /** Open rows, remembered across reloads: "c12" is category 12, "s40" is sub category 40. */
  private readonly open = new Set<string>();

  ngOnInit(): void {
    void this.reload();
  }

  protected isOpen(kind: 'c' | 's', id: number): boolean {
    // While searching, everything that matches is shown expanded.
    return this.term() !== '' || this.open.has(kind + id);
  }

  protected toggle(kind: 'c' | 's', id: number): void {
    const key = kind + id;
    if (this.open.has(key)) {
      this.open.delete(key);
    } else {
      this.open.add(key);
    }
  }

  /** Runs one change, then reloads the tree so the screen shows what the server has. */
  protected async run(action: Promise<unknown>): Promise<void> {
    this.error = '';
    try {
      await action;
    } catch (e) {
      this.error = messageOf(e);
    }
    await this.reload();
  }

  protected visibleCategories(): CategoryNode[] {
    return this.categories.filter(
      (c) =>
        this.statusOk(c.active) &&
        (this.matches(c.name) || c.subCategories.some((s) => this.subMatches(s))),
    );
  }

  protected visibleSubCategories(category: CategoryNode): CategoryNode['subCategories'] {
    const showAll = this.matches(category.name);
    return category.subCategories.filter(
      (s) => this.statusOk(s.active) && (showAll || this.subMatches(s)),
    );
  }

  protected visibleItems(sub: CategoryNode['subCategories'][number]) {
    const term = this.term();
    const showAll = term === '' || sub.name.toLowerCase().includes(term);
    const anyItemMatches = sub.items.some((i) => i.name.toLowerCase().includes(term));
    return sub.items.filter(
      (i) =>
        this.statusOk(i.active) &&
        (showAll || !anyItemMatches || i.name.toLowerCase().includes(term)),
    );
  }

  private subMatches(sub: CategoryNode['subCategories'][number]): boolean {
    return this.matches(sub.name) || sub.items.some((i) => this.matches(i.name));
  }

  private matches(name: string): boolean {
    const term = this.term();
    return term === '' || name.toLowerCase().includes(term);
  }

  private statusOk(active: boolean): boolean {
    return this.showInactive || active;
  }

  private term(): string {
    return this.filter.trim().toLowerCase();
  }

  private async reload(): Promise<void> {
    try {
      this.categories = await this.api.tree(true);
    } catch (e) {
      this.error = messageOf(e);
    } finally {
      this.loading = false;
    }
  }
}
