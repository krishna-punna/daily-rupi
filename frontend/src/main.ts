import { registerLocaleData } from '@angular/common';
import localeEnIn from '@angular/common/locales/en-IN';
import { bootstrapApplication } from '@angular/platform-browser';

import { App } from './app/app';
import { appConfig } from './app/app.config';

// Indian number grouping (1,00,000) and the rupee sign for the currency pipe.
registerLocaleData(localeEnIn);

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
