import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** A text box with an Add button, used at each level of the master data tree. */
@Component({
  selector: 'app-add-box',
  imports: [FormsModule],
  template: `
    <form class="add-box" (ngSubmit)="submit()">
      <input
        name="name"
        [(ngModel)]="value"
        maxlength="80"
        [placeholder]="placeholder()"
        [attr.aria-label]="placeholder()"
      />
      <button type="submit" [disabled]="!value.trim()">Add</button>
    </form>
  `,
})
export class AddBox {
  readonly placeholder = input.required<string>();
  readonly add = output<string>();

  protected value = '';

  protected submit(): void {
    const name = this.value.trim();
    if (name) {
      this.add.emit(name);
      this.value = '';
    }
  }
}
