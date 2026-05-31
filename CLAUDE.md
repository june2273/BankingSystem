# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java banking system implementing customer management, account handling, and transactions. The design follows the UML class diagram described below. Currently in early development — `src/Main.java` is a placeholder.

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
- Data access goes through DAO classes (`CustomerDao`, `AccountDao`, `TransactionDao`); `Bank` delegates to them. `Account.deposit`/`withdraw` sync the new balance to the DB via `AccountDao.updateBalance`. Each `Transaction.execute` logs a row via `TransactionDao` (withdraw/transfer only on success).
- Inspect the DB without IntelliJ Ultimate (works in free Community too) via the bundled H2 Console:
  ```bash
  # Web console at http://localhost:8082 (URL above, user "sa", empty password)
  java -cp "lib/*" org.h2.tools.Console
  # Or a one-off query from the shell:
  java -cp "lib/*" org.h2.tools.Shell -url "jdbc:h2:./data/bank;AUTO_SERVER=TRUE" -user sa -password "" -sql "SELECT * FROM transactions"
  ```

## Architecture

The system models a bank with customers, accounts, and transactions. The core classes to implement (per UML design):

### Class Relationships

```
고객 (Customer) ──1──> 은행 (Bank) ──0..*──> 계좌 (Account)
                                               │
                                               └──uses──> <<interface>> 거래 (Transaction)
                                                              ├── 입금거래 (DepositTransaction)
                                                              ├── 출금거래 (WithdrawTransaction)
                                                              └── 송금거래 (TransferTransaction)
```

### Classes

**고객 (Customer)**
- Fields: `고객ID: String`, `비밀번호: String`, `이름: String`, `주민등록번호: String`
- Method: `계좌개설(은행)` — opens an account through the bank

**은행 (Bank)**
- Methods: `사용자인증(고객ID, 비밀번호)`, `고객등록(고객)`, `고객조회(고객)`, `계좌생성(고객)`, `계좌조회(계좌)`

**계좌 (Account)**
- Fields: `계좌번호: String`, `잔액: double`, `계좌비밀번호: String`
- Methods: `거래수행(거래)` (public), `계좌인증(계좌번호, 계좌비밀번호)` (public), `입금(금액)` (private), `출금(금액)` (private)

**거래 (Transaction)** — interface
- Method: `execute(계좌)`

**입금거래 (DepositTransaction)**, **출금거래 (WithdrawTransaction)**
- Field: `금액: double`
- Implements `execute(계좌)`

**송금거래 (TransferTransaction)**
- Fields: `금액: double`, `대상계좌: 계좌`
- Implements `execute(계좌)`

### Design Notes

- `입금` and `출금` on `계좌` are **private** — external code triggers them only through `거래수행(거래)` (Strategy pattern via the `거래` interface).
- `계좌인증` is the account-level PIN check, separate from `사용자인증` which is the bank-level customer login.
- `Customer` owns the relationship to `Bank` (a customer opens an account at a bank), but `Bank` aggregates many `계좌` instances.
