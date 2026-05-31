# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Java banking system implementing customer management, account handling, and transactions. The design follows the UML class diagram described below. Currently in early development — `src/Main.java` is a placeholder.

## Build & Run

This is a plain IntelliJ IDEA project (no Maven/Gradle). Build and run via the command line:

```bash
# Compile all sources
javac -d out src/*.java

# Run
java -cp out Main
```

Java version: **JDK 25** (configured in `.idea/misc.xml` as `openjdk-25`). Use `--enable-preview` if instance main methods (JEP 445) are used, as the current `Main.java` uses the preview `void main()` syntax.

```bash
javac --enable-preview --release 25 -d out src/*.java
java --enable-preview -cp out Main
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
