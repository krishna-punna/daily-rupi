import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { apiErrorInterceptor } from './core/api-error.interceptor';
import { AuthService } from './core/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // HttpClient's built-in XSRF support reads the XSRF-TOKEN cookie and sends
    // it back as the X-XSRF-TOKEN header on every write, which is what the
    // Spring Boot backend expects.
    provideHttpClient(withInterceptors([apiErrorInterceptor])),
    { provide: LOCALE_ID, useValue: 'en-IN' },
    // Ask the server who is logged in before the first screen is shown.
    provideAppInitializer(() => inject(AuthService).restore()),
  ],
};
