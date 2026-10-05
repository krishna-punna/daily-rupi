import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { messageOf } from '../core/api-error.interceptor';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-change-password-page',
  imports: [FormsModule],
  template: `
    <section class="card narrow">
      <h1>Change password</h1>
      @if (auth.user()?.passwordChangeRequired) {
        <p class="hint">
          You are using the temporary password. Choose your own to unlock the app.
        </p>
      }
      <form (ngSubmit)="submit()">
        <label>
          Current password
          <input
            name="current"
            type="password"
            [(ngModel)]="current"
            autocomplete="current-password"
            required
          />
        </label>
        <label>
          New password
          <input
            name="next"
            type="password"
            [(ngModel)]="next"
            autocomplete="new-password"
            minlength="12"
            maxlength="72"
            required
          />
          <small>At least 12 characters, and not containing your username.</small>
        </label>
        <label>
          New password again
          <input
            name="confirm"
            type="password"
            [(ngModel)]="confirm"
            autocomplete="new-password"
            required
          />
        </label>
        @if (error) {
          <p class="error" role="alert">{{ error }}</p>
        }
        <button type="submit" [disabled]="busy">Change password</button>
      </form>
    </section>
  `,
})
export class ChangePasswordPage {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected current = '';
  protected next = '';
  protected confirm = '';
  protected error = '';
  protected busy = false;

  protected async submit(): Promise<void> {
    this.error = '';
    if (this.next.length < 12) {
      this.error = 'The new password needs at least 12 characters.';
      return;
    }
    if (this.next !== this.confirm) {
      this.error = 'The two new passwords do not match.';
      return;
    }
    this.busy = true;
    try {
      await this.auth.changePassword(this.current, this.next);
      await this.router.navigateByUrl('/login');
    } catch (e) {
      this.error = messageOf(e);
    } finally {
      this.busy = false;
    }
  }
}
