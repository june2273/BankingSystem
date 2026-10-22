# Banking System

A console banking system in Java for an object-oriented design course: customers, one account per customer, and PIN-checked deposits, withdrawals and transfers, persisted in an embedded H2 database. The point of the project is the design — four layers, SOLID, Template Method for transactions and a Repository for persistence — rather than features.

## Problem

Deposit, withdraw and transfer share the same steps (check the PIN, check the amount, change balances, save) but differ in which accounts they touch. Written separately, the checks get copied three times and can drift apart, and a transfer that saves the withdrawal but fails on the deposit loses money. The course also capped the design at a small number of classes, so every abstraction had to earn its place.

## Approach

- **Four layers, dependencies pointing down only:** presentation (`Main`, the only class that prints) → service (`Bank`) → domain (`Customer`, `Account`, transactions) → persistence (`BankRepository` interface, `H2BankRepository`, `Database`). 12 classes plus `Main`.
- **Template Method for transactions.** `TransactionTemplate.run()` is `final` and fixes the order: ① PIN check → ② amount check → ③ domain operation → ④ save. `DepositTransaction`, `WithdrawTransaction` and `TransferTransaction` only implement `apply`, `affectedAccounts` and `record`, so no subclass can skip the PIN check.
- **One database transaction per money movement.** `runInTransaction` turns off auto-commit on the shared H2 connection; a transfer updates both balances and inserts the history row, then commits, or rolls back if any step throws.
- **Result Object instead of exceptions.** Each transaction returns one `TransactionResult` (`SUCCESS`, `WRONG_PIN`, `INSUFFICIENT_FUNDS`, `INVALID_AMOUNT`, `ACCOUNT_NOT_FOUND`, `SELF_TRANSFER`); `Main` maps it to a message. Failed transactions change neither balances nor history.
- **Money as `long`** (whole won), matching the `BIGINT` column, to avoid floating-point error.
- The service depends only on the `BankRepository` interface (DIP), so another storage implementation could be wired in from `Main`; only the H2 one exists.

## Results

- Menu features: sign-up / login (duplicate ID and resident-number checks), open an account (numbers assigned from `100-1000`), PIN check, deposit, withdraw, transfer, account view with the 3 most recent transactions.
- Persistence checked by hand: after quitting and restarting, logging in shows the same balance and recent history. There are no automated tests.
- The README.ko.md contains the full UML class diagram and nine sequence diagrams (overview plus ① – ⑧).

## Tech stack

- Java (JDK 25, `--enable-preview`), plain `javac` / IntelliJ, no build tool
- H2 2.2.224 embedded database over JDBC (`lib/h2-2.2.224.jar`)
- Mermaid for the UML diagrams

## Run

```bash
javac --enable-preview --release 25 -cp "lib/*" -d out src/*.java
java --enable-preview -cp "out:lib/*" Main
```

The database is created under `data/` on first run. Build, database inspection and debugging notes are in [CLAUDE.md](CLAUDE.md).

## Lessons learned

- **Putting the PIN check in the `final` template** made it impossible to forget in a new transaction type, which was the main argument for Template Method over Strategy here.
- **Atomicity has to be designed in.** The transfer only became safe once balance updates and the history row went through one `runInTransaction` call.
- **Limits.** Login passwords, account PINs and resident registration numbers are stored in plain text, so this is a course exercise, not something to run with real data. There are no automated tests, and the single shared connection assumes one user at a time.

## References

- E. Gamma, R. Helm, R. Johnson, J. Vlissides, *Design Patterns: Elements of Reusable Object-Oriented Software*, Addison-Wesley, 1994. (Template Method)
- M. Fowler, *Patterns of Enterprise Application Architecture*, Addison-Wesley, 2002. (Repository)
- H2 Database Engine, https://www.h2database.com (MPL 2.0 / EPL 1.0)

---
한국어 문서: [README.ko.md](README.ko.md)
