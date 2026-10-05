import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface ItemNode {
  id: number;
  name: string;
  isDefault: boolean;
  active: boolean;
}

export interface SubCategoryNode extends ItemNode {
  items: ItemNode[];
}

export interface CategoryNode extends ItemNode {
  subCategories: SubCategoryNode[];
}

/** URL segment for each level of master data. */
export type Level = 'categories' | 'sub-categories' | 'items';

export interface PaymentMethodOption {
  id: number;
  name: string;
}

export interface ExpenseRequest {
  itemId: number;
  paymentMethodId: number;
  amount: number;
  /** Local date and time, yyyy-MM-ddTHH:mm */
  spentAt: string;
  note: string | null;
}

export interface Expense {
  id: number;
  amount: number;
  spentAt: string;
  note: string | null;
  categoryId: number | null;
  categoryName: string | null;
  subCategoryId: number | null;
  subCategoryName: string | null;
  itemId: number;
  itemName: string | null;
  paymentMethodId: number;
  paymentMethodName: string | null;
}

export interface ExpensePage {
  content: Expense[];
  page: number;
  size: number;
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class MasterDataApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/master-data';

  /** Active entries only unless includeInactive is set (the management screen). */
  tree(includeInactive: boolean): Promise<CategoryNode[]> {
    return firstValueFrom(
      this.http.get<CategoryNode[]>(this.base, { params: { includeInactive } }),
    );
  }

  addCategory(name: string): Promise<unknown> {
    return firstValueFrom(this.http.post(`${this.base}/categories`, { name }));
  }

  addSubCategory(categoryId: number, name: string): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`${this.base}/categories/${categoryId}/sub-categories`, { name }),
    );
  }

  addItem(subCategoryId: number, name: string): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`${this.base}/sub-categories/${subCategoryId}/items`, { name }),
    );
  }

  rename(level: Level, id: number, name: string): Promise<unknown> {
    return firstValueFrom(this.http.put(`${this.base}/${level}/${id}`, { name }));
  }

  setActive(level: Level, id: number, active: boolean): Promise<unknown> {
    return firstValueFrom(this.http.patch(`${this.base}/${level}/${id}/status`, { active }));
  }

  remove(level: Level, id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(`${this.base}/${level}/${id}`));
  }
}

@Injectable({ providedIn: 'root' })
export class ExpenseApi {
  private readonly http = inject(HttpClient);

  paymentMethods(): Promise<PaymentMethodOption[]> {
    return firstValueFrom(this.http.get<PaymentMethodOption[]>('/api/payment-methods'));
  }

  list(page: number, size: number): Promise<ExpensePage> {
    return firstValueFrom(this.http.get<ExpensePage>('/api/expenses', { params: { page, size } }));
  }

  create(body: ExpenseRequest): Promise<Expense> {
    return firstValueFrom(this.http.post<Expense>('/api/expenses', body));
  }

  update(id: number, body: ExpenseRequest): Promise<Expense> {
    return firstValueFrom(this.http.put<Expense>(`/api/expenses/${id}`, body));
  }

  remove(id: number): Promise<unknown> {
    return firstValueFrom(this.http.delete(`/api/expenses/${id}`));
  }
}
