import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface CurrentUser {
  username: string;
  roles: string[];
  passwordChangeRequired: boolean;
}

/**
 * Talks to the backend's login endpoints. The session lives in an HttpOnly
 * cookie the browser manages; nothing is stored in localStorage.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** The logged-in account, or null. */
  readonly user = signal<CurrentUser | null>(null);

  /** Called once at startup: picks up an existing session after a page reload. */
  async restore(): Promise<void> {
    try {
      await this.primeCsrf();
      this.user.set(await firstValueFrom(this.http.get<CurrentUser>('/api/auth/me')));
    } catch {
      this.user.set(null);
    }
  }

  async login(username: string, password: string): Promise<CurrentUser> {
    await this.primeCsrf();
    // Spring Security's login filter reads form fields, not JSON.
    const body = new HttpParams().set('username', username).set('password', password);
    const user = await firstValueFrom(this.http.post<CurrentUser>('/api/auth/login', body));
    this.user.set(user);
    return user;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post('/api/auth/logout', null));
    } finally {
      this.user.set(null);
    }
  }

  /** The server ends the session afterwards, so the user logs in again. */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await firstValueFrom(this.http.put('/api/auth/password', { currentPassword, newPassword }));
    this.user.set(null);
  }

  /** Makes the server issue the XSRF-TOKEN cookie that writes need. */
  private async primeCsrf(): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
  }
}
