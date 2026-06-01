# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java banking system implementing customer management, account handling, and transactions. The design follows the UML class diagram described below, refactored into clean layers (presentation → service → domain → persistence) following SOLID principles. State is persisted to an embedded H2 database, and every transaction is gated behind an account-PIN check.

## Build & Run

This is a plain IntelliJ IDEA project (no Maven/Gradle). It depends on the **H2 database** JDBC driver (`lib/h2-2.2.224.jar`), so the jar must be on the classpath for both compile and run.

```bash
# Compile all sources (H2 jar on classpath)
javac --enable-preview --release 25 -cp "lib/*" -d out src/*.java

# Run (out dir + H2 jar on classpath)
java --enable-preview -cp "out:lib/*" Main
```

Java version: **JDK 25** (configured in `.idea/misc.xml` as `openjdk-25`). `--enable-preview` is required because `Main.java` uses preview features. In IntelliJ, add `lib/h2-2.2.224.jar` as a module library (Project Structure → Libraries).

## Database (H2, embedded)

- All state is persisted to an embedded H2 file DB at `./data/bank.mv.db` (gitignored). The connection URL is `jdbc:h2:./data/bank;AUTO_SERVER=TRUE` — `AUTO_SERVER=TRUE` lets the H2 Console / IntelliJ connect concurrently while the app runs.
- Schema (`customers`, `accounts`, `transactions`) is auto-created on startup by `Database.init()`. Tables: `customers` (PK customer_id), `accounts` (PK account_number, FK customer_id, balance), `transactions` (id, account_number, type DEPOSIT|WITHDRAW|TRANSFER, amount, target_account, created_at).
- All JDBC is isolated in a single persistence class, `BankRepository` (it consolidates what used to be `CustomerDao`/`AccountDao`/`TransactionDao` into one class). `Bank` (the service layer) holds a `BankRepository` and delegates persistence to it. The domain objects (`Account`, the `Transaction` strategies) are **pure** — they never touch the DB. After a transaction succeeds, `Bank` persists the new balance(s) via `BankRepository.updateBalance` and logs a `transactions` row via `BankRepository.insertHistory` (withdraw/transfer only on success, so failed transactions leave no DB trace).
- Inspect the DB without IntelliJ Ultimate (works in free Community too) via the bundled H2 Console:
  ```bash
  # Web console at http://localhost:8082 (URL above, user "sa", empty password)
  java -cp "lib/*" org.h2.tools.Console
  # Or a one-off query from the shell:
  java -cp "lib/*" org.h2.tools.Shell -url "jdbc:h2:./data/bank;AUTO_SERVER=TRUE" -user sa -password "" -sql "SELECT * FROM transactions"
  ```

## Architecture

The system models a bank with customers, accounts, and transactions, split into four layers. There are **11 classes + `Main`** (the professor caps the class count at ~10; the DAO trio was merged into one `BankRepository` to stay within budget).

### Layered structure

```
[표현]   Main                          ── 메뉴/입력/출력, PIN 입력, TransactionResult 분기
   │
[서비스] Bank                          ── 업무 조율 + PIN 검증 게이트 + 영속화 위임 (출력 없음)
   │                                      거래: deposit/withdraw/transfer(account, pin, amount) → TransactionResult
   ├──> [도메인] Customer · Account(순수) · Transaction 전략 · TransactionRecord · TransactionResult
   └──> [영속]   BankRepository ──> Database (H2 연결/스키마)
```

### Class Relationships

```
고객 (Customer) ──1──> 은행 (Bank) ──0..*──> 계좌 (Account)
                                               │
                                               └──uses──> <<interface>> 거래 (Transaction)
                                                              ├── 입금거래 (DepositTransaction)
                                                              ├── 출금거래 (WithdrawTransaction)
                                                              └── 송금거래 (TransferTransaction)

Bank ──> BankRepository ──> Database          (모든 JDBC는 BankRepository에 격리)
Bank ──returns──> TransactionResult (enum)    (SUCCESS/WRONG_PIN/INSUFFICIENT_FUNDS/INVALID_AMOUNT/ACCOUNT_NOT_FOUND)
거래이력 (TransactionRecord)                   (거래 1건 + type→한글 라벨)
```

### Classes

**고객 (Customer)**
- Fields: `고객ID: String`, `비밀번호: String`, `이름: String`, `주민등록번호: String`
- Method: `계좌개설(은행)` — opens an account through the bank

**은행 (Bank)** — service layer; constructor-injected with a `BankRepository`, no console output
- Account/customer mgmt: `authenticate(id, pwd)`, `register(customer)`, `openAccount(customer, initial, pin)`, `hasAccount`, `loadAccount`, `verifyAccountPassword(account, pin)`, duplicate checks (`isSsnRegistered`, `isIdTaken`)
- Transactions (each verifies PIN first, returns `TransactionResult`): `deposit(account, pin, amount)`, `withdraw(account, pin, amount)`, `transfer(source, pin, targetNo, amount)`
- History: `recentHistory(accountNo, limit)` → `List<TransactionRecord>`

**계좌 (Account)** — pure domain (no DB, no I/O)
- Fields: `계좌번호: String`, `잔액: double`, `계좌비밀번호: String`
- Methods: `performTransaction(거래)` (public, returns boolean), `authenticateAccount(계좌번호, 계좌비밀번호)` (public), `deposit(금액)`/`withdraw(금액)` (package-private — `deposit` mutates only, `withdraw` returns boolean)

**거래 (Transaction)** — interface
- Method: `boolean execute(계좌)` — pure domain operation; returns success (no DB/output)

**입금거래 (DepositTransaction)**, **출금거래 (WithdrawTransaction)**
- Field: `금액: double`; `execute` returns boolean (deposit always true, withdraw false on insufficient funds)

**송금거래 (TransferTransaction)**
- Fields: `금액: double`, `대상계좌: 계좌`; `execute` withdraws from source then deposits to target

**거래이력 (TransactionRecord)** — value object for one transaction
- Fields: `accountNo`, `type`, `amount`, `targetAccountNo`, `createdAt`
- `label()` maps `type → 한글` and `format()` renders one history line (label logic lives here, not in the repository)

**TransactionResult** — enum: `SUCCESS`, `WRONG_PIN`, `INSUFFICIENT_FUNDS`, `INVALID_AMOUNT`, `ACCOUNT_NOT_FOUND`

**BankRepository** — persistence layer; all JDBC for the `customers`/`accounts`/`transactions` tables lives here

### Design Notes

- **PIN gate on every transaction**: `Bank.deposit/withdraw/transfer` call `Account.authenticateAccount` first; a wrong PIN returns `WRONG_PIN` and the transaction never runs. The check is enforced in the service (not just the UI), so it can't be bypassed. `Main` case 3 stays as a standalone "preview" PIN check.
- **Pure domain**: `Account` and the `Transaction` strategies hold no DAO and print nothing. `입금`/`출금` are package-private and triggered only through `performTransaction(거래)` (Strategy pattern).
- **Persistence is the service's job**: only after a strategy succeeds does `Bank` call `BankRepository.updateBalance` + `insertHistory`. Failed transactions (wrong PIN, insufficient funds, invalid amount, account not found) leave **no DB trace**.
- **Output is the presentation's job**: domain/service return values (`TransactionResult`, `boolean`, `Customer`); `Main` is the only layer that prints.
- `계좌인증` is the account-level PIN check, separate from `사용자인증` which is the bank-level customer login.
- **DIP note**: full Dependency-Inversion (a `BankRepository` interface + H2 implementation) was deferred to respect the ~10-class cap; isolating all JDBC in one `BankRepository` keeps most of the benefit and makes a future interface extraction trivial.
