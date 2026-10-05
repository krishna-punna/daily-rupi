import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** One category, sub category or item line with its rename, status and delete actions. */
@Component({
  selector: 'app-entry-row',
  imports: [FormsModule],
  template: `
    <div class="entry" [class.inactive]="!active()">
      @if (editing) {
        <input
          class="grow"
          [(ngModel)]="draft"
          maxlength="80"
          aria-label="New name"
          (keyup.enter)="save()"
          (keyup.escape)="editing = false"
        />
        <button type="button" (click)="save()" [disabled]="!draft.trim()">Save</button>
        <button type="button" class="ghost" (click)="editing = false">Cancel</button>
      } @else {
        @if (expandable()) {
          <button
            type="button"
            class="link grow"
            [attr.aria-expanded]="expanded()"
            (click)="toggleExpand.emit()"
          >
            <span class="caret">{{ expanded() ? '▾' : '▸' }}</span> {{ name() }}
            <span class="count">{{ childCount() }}</span>
          </button>
        } @else {
          <span class="grow">{{ name() }}</span>
        }
        @if (!isDefault()) {
          <span class="tag">Custom</span>
        }
        <span class="tag" [class.off]="!active()">{{ active() ? 'Active' : 'Inactive' }}</span>
        <button type="button" class="ghost" (click)="startEdit()">Rename</button>
        <button type="button" class="ghost" (click)="setActive.emit(!active())">
          {{ active() ? 'Make inactive' : 'Make active' }}
        </button>
        @if (confirming) {
          <button type="button" class="danger" (click)="confirming = false; remove.emit()">
            Confirm delete
          </button>
          <button type="button" class="ghost" (click)="confirming = false">Cancel</button>
        } @else {
          <button type="button" class="ghost danger-text" (click)="confirming = true">Delete</button>
        }
      }
    </div>
  `,
})
export class EntryRow {
  readonly name = input.required<string>();
  readonly active = input.required<boolean>();
  readonly isDefault = input.required<boolean>();
  readonly expandable = input(false);
  readonly expanded = input(false);
  readonly childCount = input(0);

  readonly toggleExpand = output<void>();
  readonly rename = output<string>();
  readonly setActive = output<boolean>();
  readonly remove = output<void>();

  protected editing = false;
  protected confirming = false;
  protected draft = '';

  protected startEdit(): void {
    this.draft = this.name();
    this.editing = true;
  }

  protected save(): void {
    const name = this.draft.trim();
    if (!name) {
      return;
    }
    this.editing = false;
    if (name !== this.name()) {
      this.rename.emit(name);
    }
  }
}
