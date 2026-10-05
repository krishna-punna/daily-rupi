import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Any logged-in account, including one that still has to change its password. */
export const loggedInGuard: CanActivateFn = () => {
  const user = inject(AuthService).user();
  return user ? true : inject(Router).parseUrl('/login');
};

/**
 * The application screens: logged in, temporary password already replaced.
 * This only decides what to show; the backend enforces the same rule on every request.
 */
export const appGuard: CanActivateFn = () => {
  const user = inject(AuthService).user();
  const router = inject(Router);
  if (!user) {
    return router.parseUrl('/login');
  }
  return user.passwordChangeRequired ? router.parseUrl('/change-password') : true;
};
