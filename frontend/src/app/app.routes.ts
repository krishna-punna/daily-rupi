import { Routes } from '@angular/router';

import { appGuard, loggedInGuard } from './core/guards';
import { BudgetsPage } from './pages/budgets.page';
import { ChangePasswordPage } from './pages/change-password.page';
import { DashboardPage } from './pages/dashboard.page';
import { ExpensesPage } from './pages/expenses.page';
import { LoginPage } from './pages/login.page';
import { MasterDataPage } from './pages/master-data.page';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'change-password', component: ChangePasswordPage, canActivate: [loggedInGuard] },
  { path: 'dashboard', component: DashboardPage, canActivate: [appGuard] },
  { path: 'expenses', component: ExpensesPage, canActivate: [appGuard] },
  { path: 'budgets', component: BudgetsPage, canActivate: [appGuard] },
  { path: 'master-data', component: MasterDataPage, canActivate: [appGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'expenses' },
  { path: '**', redirectTo: 'expenses' },
];
