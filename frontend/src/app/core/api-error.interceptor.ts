import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/** Sends the user to the right screen when the server says the session is gone. */
export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const isAuthCall = req.url.startsWith('/api/auth/');
      if (error.status === 401 && !isAuthCall) {
        auth.user.set(null);
        void router.navigateByUrl('/login');
      } else if (error.status === 403 && error.error?.code === 'PASSWORD_CHANGE_REQUIRED') {
        void router.navigateByUrl('/change-password');
      }
      return throwError(() => error);
    }),
  );
};

/** The message the backend sent, or a safe fallback. */
export function messageOf(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Cannot reach the server. Is the backend running?';
    }
    const message = error.error?.message;
    if (typeof message === 'string' && message.length > 0) {
      return message;
    }
  }
  return 'Something went wrong. Please try again.';
}
