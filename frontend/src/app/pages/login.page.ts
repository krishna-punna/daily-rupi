import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { messageOf } from '../core/api-error.interceptor';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [FormsModule],
  template: `
    <section class="card narrow">
      <h1>Log in</h1>
      <form (ngSubmit)="submit()">
        <label>
          Username
          <input name="username" [(ngModel)]="username" autocomplete="username" required autofocus />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            [(ngModel)]="password"
            autocomplete="current-password"
            required
          />
        </label>
        @if (error) {
          <p class="error" role="alert">{{ error }}</p>
        }
        <button type="submit" [disabled]="busy || !username || !password">Log in</button>
      </form>
    </section>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected username = '';
  protected password = '';
  protected error = '';
  protected busy = false;

  protected async submit(): Promise<void> {
    this.busy = true;
    this.error = '';
    try {
      const user = await this.auth.login(this.username.trim(), this.password);
      this.password = '';
      await this.router.navigateByUrl(user.passwordChangeRequired ? '/change-password' : '/expenses');
    } catch (e) {
      this.error = messageOf(e);
    } finally {
      this.busy = false;
    }
  }
}
