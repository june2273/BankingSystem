# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 기능·구조·OOP/SOLID/디자인 패턴 개념 설명은 [README.md](README.md) 참고.
> 이 문서는 **빌드·실행·DB 점검·디버깅** 등 운영/문제 해결 중심입니다.

## Project Overview

A Java banking system (customers, accounts, transactions) split into clean layers
(presentation → service → domain → persistence) following SOLID. State persists to an embedded
H2 database; every transaction is gated behind an account-PIN check; multi-account transactions
(transfer) are committed atomically via a single transaction boundary.

**12 classes + `Main`.** Money is modeled as `long` (won, integer) and stored as `BIGINT`.
Transactions use a real **Template Method** (`TransactionTemplate.run()` is `final`); deposit/withdraw/transfer
are subclasses implementing only the primitive ops.

## Build & Run

Plain IntelliJ IDEA project (no Maven/Gradle). Depends on the **H2 JDBC driver**
(`lib/h2-2.2.224.jar`) — the jar must be on the classpath for both compile and run.

```bash
# Compile all sources (H2 jar on classpath)
javac --enable-preview --release 25 -cp "lib/*" -d out src/*.java

# Run (out dir + H2 jar on classpath)
java --enable-preview -cp "out:lib/*" Main

# Lint-clean rebuild (use when debugging — surfaces all warnings)
rm -rf out && mkdir -p out && javac --enable-preview --release 25 -Xlint:all -cp "lib/*" -d out src/*.java
```

- **JDK 25** (`.idea/misc.xml` → `openjdk-25`). `--enable-preview` is required (`Main.java` uses preview features) and the `--release 25` must match the runtime.
- In IntelliJ, add `lib/h2-2.2.224.jar` as a module library (Project Structure → Libraries). Already registered in `BankingSystem.iml`.
- Download the driver if missing:
  ```bash
  curl -fsSL -o lib/h2-2.2.224.jar \
    https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
  ```

## Debugging & Troubleshooting

### ⚠️ Schema-change gotcha — reset the DB after editing `Database.init()`
`CREATE TABLE IF NOT EXISTS` does **not** alter columns on an existing table. If you change a column
type (e.g. the money columns were migrated `DECIMAL(15,2)` → `BIGINT`), the old `./data/bank.mv.db`
keeps the **old** schema and you'll get type/precision mismatches at runtime. Fix:

```bash
rm -rf data    # data/ is gitignored and fully regenerated on next run
```
On next launch you should see `>> [DB] 데이터베이스 초기화 완료 (./data/bank.mv.db)` (BIGINT schema recreated).

### Common errors seen in this project (and the fix)
| Symptom | Cause | Fix |
|---------|-------|-----|
| `incompatible types: possible lossy conversion from double to long` | A money param/field still `double` after the `long` migration | Make the whole money path `long` (params, fields, `setLong/getLong`, `nextLong`) |
| `unreported exception SQLException; must be caught or declared` in `runInTransaction` | `setAutoCommit/commit` throw **checked** `SQLException`, but only `RuntimeException` was caught | Catch `Exception` (covers checked `SQLException` from set/commit **and** `RuntimeException` from `work`), rollback, rethrow as `RuntimeException` — see `H2BankRepository.runInTransaction` |
| `Feature is not supported in --release ...` / preview errors | Missing or mismatched `--enable-preview` between compile and run | Pass `--enable-preview` to **both** `javac` and `java`, with matching `--release 25` |
| `ClassNotFoundException: org.h2.Driver` / `No suitable driver` | H2 jar not on classpath | Include `lib/*` on the classpath for both compile and run |
| `>> [DB 오류] 거래 처리 실패 — 롤백되었습니다: ...` | A persist step inside `runInTransaction` failed; the transaction was rolled back | Inspect the wrapped cause; in-memory `Account` balances may diverge from DB until reload (see note below) |

### Atomicity & the in-memory divergence note
`TransactionTemplate.run()` wraps `updateBalance` + `insertHistory` in `repo.runInTransaction(...)` so a transfer
commits both accounts' balances and the history row together, or rolls all back. **Honest limitation:**
on rollback the exception propagates out of the menu loop (terminating it), but the already-mutated
in-memory `Account` objects are *not* reverted — DB and memory diverge until the app reloads accounts
from the DB. Acceptable for this console demo; a fuller fix would `try/catch` the transaction call in
`Main` and reload the account.

### Verifying the "failed transactions leave no DB trace" invariant
After exercising failure paths (wrong PIN, insufficient funds, amount ≤ 0, non-numeric input,
nonexistent target, self-transfer), the `transactions` table must have **zero** new rows and balances
must be unchanged. Confirm at the DB level with the Shell command below (`SELECT COUNT(*) FROM transactions`).

## Database (H2, embedded)

- State persists to `./data/bank.mv.db` (gitignored). Connection URL `jdbc:h2:./data/bank;AUTO_SERVER=TRUE`
  — `AUTO_SERVER=TRUE` lets the H2 Console / IntelliJ connect concurrently while the app runs.
- A **single static shared `Connection`** lives in `Database.java`; the transaction boundary
  (`runInTransaction`) toggles its auto-commit. Schema is auto-created by `Database.init()` on startup.
- Tables: `customers` (PK customer_id), `accounts` (PK account_number, FK customer_id, `balance BIGINT`),
  `transactions` (id, account_number, type DEPOSIT|WITHDRAW|TRANSFER, `amount BIGINT`, target_account, created_at).
- Inspect the DB without IntelliJ Ultimate (works in free Community too) via the bundled H2 tools:
  ```bash
  # Web console at http://localhost:8082 (URL above, user "sa", empty password)
  java -cp "lib/*" org.h2.tools.Console

  # One-off query from the shell
  java -cp "lib/*" org.h2.tools.Shell -url "jdbc:h2:./data/bank;AUTO_SERVER=TRUE" \
    -user sa -password "" -sql "SELECT * FROM accounts; SELECT * FROM transactions"
  ```

## Architecture (debugging map — where to look)

```
[표현]   Main                          ── 메뉴/입력/출력, PIN 입력, TransactionResult → 한글 메시지
   │
[서비스] Bank                          ── 거래 템플릿 디스패치(deposit/withdraw/transfer → run()), 출력 없음
   │                                      deposit/withdraw/transfer(account, pin, amount) → TransactionResult
   ├──> [도메인] Customer · Account(순수, long) · TransactionTemplate(추상) ← Deposit/Withdraw/Transfer
   │             · TransactionRecord · TransactionResult
   └──> [영속]   BankRepository(interface) ──> H2BankRepository ──> Database (H2 단일 공유 연결/스키마)
```

**디버깅 시 진입점 가이드**
- 거래 흐름·검증 순서 버그 → `TransactionTemplate.run()`(공통 골격) + `Bank`의 세 거래 메서드(디스패치).
- 잔액/금액 오류 → `Account`(long 연산) + `H2BankRepository`의 `setLong/getLong`.
- 원자성/롤백 → `H2BankRepository.runInTransaction` (단일 공유 커넥션의 commit/rollback).
- 출력 형식 → `Main` (유일한 출력 계층) + `Account.toString()`/`TransactionRecord.toString()`.
- DB 스키마/연결 → `Database.java`.
- 영속 계약 교체(InMemory 등) → `BankRepository` 인터페이스 구현체만 바꾸고 `Main`의 배선 한 줄 수정.

### Key design invariants (don't break these when editing)
- **PIN gate in the skeleton**: `TransactionTemplate.run()` calls `authenticateAccount` first; a wrong PIN returns `WRONG_PIN` and the transaction never runs. The `final run()` makes the gate un-skippable by any subclass. (`Main` case 3 is a standalone "preview" PIN check.)
- **Pure domain**: `Account` holds no DAO and prints nothing. `deposit`/`withdraw` are package-private, triggered only through the transaction templates (`TransactionTemplate.apply`). The varying domain op (`apply`) does **not** know `BankRepository`.
- **Persistence is the skeleton's job**: only after `apply()` succeeds does `run()` call `updateBalance` (for each `affectedAccounts`) + `insertHistory`, wrapped in `runInTransaction`. Failed transactions leave **no DB trace**.
- **Output is the presentation's job**: domain/service return values (`TransactionResult`, `boolean`, `Customer`); `Main` is the only layer that prints.
- **Money is `long`** end-to-end (model + `BIGINT` columns). Don't reintroduce `double`/`nextDouble`/`setDouble`.
- **Self-transfer guard lives in the service** (`Bank.transfer` → `SELF_TRANSFER`), with the `Main` UI guard as a secondary convenience.

## Reference docs (out of sync — verify before trusting)
`무제 폴더/EVALUATION.md`, `무제 폴더/추천.md`, `무제 폴더/CODE_GUIDE.md` predate the `long`/interface/
Template-Method/`toString()` refactor and describe the **older** 11-class `double` Strategy design. Treat as
historical rationale (P1–P6 improvement comments), not current truth.
