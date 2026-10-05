import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <span class="brand">Daily Rupi</span>
      @if (auth.user(); as user) {
        @if (!user.passwordChangeRequired) {
          <nav>
            <a routerLink="/expenses" routerLinkActive="current">Expenses</a>
            <a routerLink="/budgets" routerLinkActive="current">Budgets</a>
            <a routerLink="/master-data" routerLinkActive="current">Master data</a>
          </nav>
        }
        <span class="spacer"></span>
        <span class="who">{{ user.username }}</span>
        <a routerLink="/change-password" routerLinkActive="current">Change password</a>
        <button type="button" class="ghost" (click)="logout()">Log out</button>
      }
    </header>
    <main>
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }
}
