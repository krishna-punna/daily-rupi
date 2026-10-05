# Daily Rupi

A personal daily expenditure tracker: Angular frontend, Spring Boot backend, MySQL database.

**Status:** login, master data management and expense entry work end to end in code:
Spring Boot API plus Angular screens. Budgets, standard (recurring) expenditures and
reports come next.

> This code was written without access to Maven Central or npm, so it has
> **not been compiled or run yet**. Run `mvn test` and `npm install && npm start`
> (see below) and report any failure before building on it.

## What is here

| Path | Contents |
| --- | --- |
| `backend/` | Spring Boot application (Java 21, Maven) |
| `backend/src/main/resources/db/migration/` | Flyway migrations: `V1` login, `V2` master data with seed rows, `V3` payment methods and expenses |
| `frontend/` | Angular application (login, change password, expenses, master data) |
| `db/setup.sql` | One-time script to create the MySQL schema and a least-privilege user |

## Run it

1. Install Java 21, Maven and MySQL 8.
2. Edit `db/setup.sql`, replace `CHANGE_ME` with a strong password, and run it as MySQL root:
   `mysql -u root -p < db/setup.sql`
3. Start the backend with the database credentials in environment variables
   (they are deliberately not stored in the repository):

   ```
   cd backend
   DB_USERNAME=dailyrupi_app DB_PASSWORD='your password' mvn spring-boot:run
   ```

   On Windows PowerShell: `$env:DB_USERNAME="dailyrupi_app"; $env:DB_PASSWORD="your password"; mvn spring-boot:run`

Flyway creates the tables on first start. The API listens on `http://127.0.0.1:8080`
and is not reachable from other devices.

## Run the screens

With the backend running, in a second terminal (Node.js 20.19 or newer):

```
cd frontend
npm install
npm start
```

Open `http://localhost:4200`. The dev server forwards `/api` calls to the backend,
so the browser only ever talks to one origin.

| Screen | What you can do |
| --- | --- |
| Expenses | Pick category, sub category and item from dropdowns, enter amount, date and time, and how you paid. Edit or delete recent expenses. |
| Master data | Add, rename, delete and switch any category, sub category or item between active and inactive. Search the tree. |

Rules worth knowing:

- Date and time default to now. Earlier dates are allowed, future ones are refused
  by both the screen and the API.
- Inactive entries, and everything beneath an inactive parent, are left out of the
  expense dropdowns. The API also refuses them for new expenses.
- An item with expenses recorded against it cannot be deleted, only made inactive.
  A category or sub category cannot be deleted while it still has children.

## Run the tests

```
cd backend
mvn test
```

Tests use an in-memory H2 database, so MySQL is not needed for them.

## Logging in

On first start the app creates one account:

- username: `admin`
- password: `admin`

That password is temporary. Until you change it, the account has no roles and every
endpoint except the account ones answers `403 PASSWORD_CHANGE_REQUIRED`. The new
password needs at least 12 characters and must not contain the username. After the
change you are logged out and log in again with the new password.

## Security at a glance

- Username and password are checked against the `users` table; passwords are stored as BCrypt hashes.
- Server-side session in an HttpOnly, Secure, SameSite=Strict cookie; nothing is kept in browser storage.
- CSRF token required on every write (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header: Angular's defaults).
- Deny by default; `/api/admin/**` needs the ADMIN role, the rest of `/api/**` needs USER.
- Login locks for 15 minutes after 5 wrong passwords, with one error message for every cause.
- Security headers (CSP, frame denial, no-referrer), no stack traces in responses, audit log of logins.

## API so far

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | public | Issues the CSRF cookie; call once before login |
| POST | `/api/auth/login` | public | Form fields `username`, `password` |
| POST | `/api/auth/logout` | logged in | Ends the session |
| GET | `/api/auth/me` | logged in | Username, roles, whether a password change is due |
| PUT | `/api/auth/password` | logged in | JSON `currentPassword`, `newPassword` |
| GET | `/api/master-data` | USER | Active category tree; `?includeInactive=true` for everything |
| POST | `/api/master-data/categories` | USER | Add a category: JSON `name` |
| POST | `/api/master-data/categories/{id}/sub-categories` | USER | Add a sub category |
| POST | `/api/master-data/sub-categories/{id}/items` | USER | Add an item |
| PUT | `/api/master-data/{level}/{id}` | USER | Rename; level is `categories`, `sub-categories` or `items` |
| PATCH | `/api/master-data/{level}/{id}/status` | USER | JSON `active`: true or false |
| DELETE | `/api/master-data/{level}/{id}` | USER | Delete if nothing depends on it |
| GET | `/api/payment-methods` | USER | Active payment methods |
| GET | `/api/expenses?page=&size=` | USER | Expenses, newest first |
| POST | `/api/expenses` | USER | JSON `itemId`, `paymentMethodId`, `amount`, `spentAt`, `note` |
| PUT | `/api/expenses/{id}` | USER | Edit an expense |
| DELETE | `/api/expenses/{id}` | USER | Delete an expense |
| GET | `/api/admin/audit-log` | ADMIN | Recent security events |
