# 현재 코드에 적용된 객체지향 · SOLID · 디자인패턴 총정리

> 현재 BankingSystem 전체 코드(12 클래스 + Main)에 **실제로 구현된** 개념만 정리.
> 각 항목 = `개념 → 코드 위치/스니펫 → 효과` 순. 마지막 §4는 **발표용 핵심 우선순위**.

---

# 1. 객체지향(OOP) 개념

## 1-1. 캡슐화 (Encapsulation) — "상태는 숨기고 행위만 노출"

**구현 ①: 모든 상태 필드 `private`**
```java
// Account.java
private String accountNumber;
private long   balance;        // 외부에서 account.balance = 1000 불가
private String accountPassword;
```

**구현 ②(★핵심): 입금/출금을 `package-private`로 의도적 제한**
```java
// Account.java — 앞에 public/private 없음 = 같은 패키지만 호출 가능
void deposit(long amount) { balance += amount; }
boolean withdraw(long amount) { if (balance >= amount) { balance -= amount; return true; } return false; }
```

- `public`으로 열면 UI나 다른 로직이 PIN 검증·장부기록을 건너뛰고 `account.deposit(999999)`를 호출할 수 있음.
- `package-private`로 좁혀, **잔액 변경은 오직 거래 템플릿(`TransactionTemplate`)을 통해서만** 가능하게 강제.

**효과:** "계좌의 잔액 변경은 정해진 거래 절차를 거쳐야 한다"는 **도메인 규칙을 자바 문법(접근제어자)만으로 강제**.
외부 오용을 컴파일 단계에서 차단 → 시스템 안정성↑, 결합도↓.

---

## 1-2. 추상화 (Abstraction) — "본질만 남기고 세부는 감춤"

**구현 ①: 추상 클래스 `TransactionTemplate`**
```java
abstract class TransactionTemplate {
    public final TransactionResult run(...) { ... }   // 공통 골격(구현)
    protected abstract boolean apply(Account s, long a);            // 세부는 자식에게
    protected abstract List<Account> affectedAccounts(Account s);
    protected abstract TransactionRecord record(Account s, long a);
}
```

**구현 ②: 인터페이스 `BankRepository`**
```java
interface BankRepository {
    void insertCustomer(Customer c);
    Account findAccount(String no);
    void runInTransaction(Runnable work);
    ... // "저장소가 할 수 있는 일"만 선언, SQL은 숨김
}
```

**효과:** 상위(`Bank`, `run()`)는 **"무엇을 하는가"** 만 알고 **"어떻게(SQL·잔액계산)"** 는 모른다.
세부 변경(H2→다른 DB, 입금→이자입금)이 상위로 전파되지 않음.

---

## 1-3. 상속 (Inheritance) — IS-A 특수화

```java
class DepositTransaction  extends TransactionTemplate { ... }   // 입금 is-a 트랜잭션
class WithdrawTransaction extends TransactionTemplate { ... }
class TransferTransaction extends TransactionTemplate { ... }
```

- 세 거래가 공통 부모의 골격(`run()`)을 물려받고, 달라지는 부분만 재정의.
- **피터코드 5규칙 충족:** "트랜잭션을 특수화" — 역할(role)이 아니라 분류(IS-A)를 상속.

**효과:** 공통 골격을 한 번만 작성(중복 제거), 새 거래는 자식 추가만으로 확장(→ OCP).

---

## 1-4. 다형성 (Polymorphism) — 3종 모두 구현

**(a) 오버라이딩(런타임 다형성) — `toString()` 재정의**
```java
// Account.java
@Override public String toString() { return "계좌번호 " + accountNumber + " / 잔액 " + balance + "원"; }
// TransactionRecord.java
@Override public String toString() { return "[" + createdAt + "] " + label() + " " + amount + "원" + ...; }
```
```java
// Main.java — 같은 println()이 객체 타입에 따라 다르게 출력
System.out.println(myAccount);   // Account.toString()
System.out.println(record);      // TransactionRecord.toString()
```

**(b) 서브타입 다형성 — 템플릿이 자식 구현을 호출**
```java
// run() 안에서 apply(source, amount) 호출 → 실제로는 Deposit/Withdraw/Transfer의 apply가 실행
if (!apply(source, amount)) return INSUFFICIENT_FUNDS;
```

**(c) 오버로딩(컴파일타임 다형성) — 같은 이름, 다른 시그니처**
```java
// Bank.java — 송금 대상을 '번호'로도 '객체'로도 받음
public TransactionResult transfer(Account src, String pin, String targetNo, long amt) {...}
public TransactionResult transfer(Account src, String pin, Account target, long amt) {...}
// Account.java — 잔액 0 기본 생성자
public Account(String no, String pw) { this(no, 0L, pw); }
public Account(String no, long balance, String pw) {...}
// TransactionRecord.java — 기록용/조회용 생성자
```

**효과:** "어떻게 할지는 객체가 알아서, 호출부는 메시지만" → 분기문(`if type==...`) 제거, 확장에 유연.

---

# 2. SOLID 원칙

## 2-1. SRP (단일 책임) — 클래스 + 계층 두 수준에서

```
[표현]  Main          → 입력/출력만 (유일한 print 계층)
[서비스] Bank          → 업무 조율만 (출력 없음, 값만 반환)
[도메인] Account       → 잔액 계산만 (DB·출력 모름)
        TransactionTemplate → 거래 골격만
[영속]  H2BankRepository → SQL만 / Database → 연결·스키마만
```

**효과:** 한 가지 이유로만 바뀜. 출력 형식 변경 → `Main`만, SQL 변경 → `H2BankRepository`만, 잔액 규칙 → `Account`만.
변경 영향 범위가 한 파일로 국소화 → 유지보수·테스트 용이.

---

## 2-2. OCP (개방-폐쇄) — 확장에 열림, 수정에 닫힘

```java
// 새 거래(예: 이자입금)를 추가하려면?
class InterestTransaction extends TransactionTemplate {
    protected boolean apply(...) {...}        // 빈칸만 채움
    protected List<Account> affectedAccounts(...) {...}
    protected TransactionRecord record(...) {...}
}
// TransactionTemplate.run(), Bank, Account 는 한 줄도 수정 안 함
```

**효과:** 기능 추가 시 기존 검증된 코드(골격·계좌·영속)를 건드리지 않음 → 회귀 버그 위험 최소화.

---

## 2-3. LSP (리스코프 치환) — 자식이 부모 규약을 지킴

- `run()`이 부르는 `apply/affectedAccounts/record`의 **계약**을 세 자식이 성실히 지킴
  - `apply`: 성공 시 `true`, 잔액부족 시 `false` (입금은 항상 `true`)
  - `affectedAccounts`: 입·출금 1개, 송금 2개 — DB 갱신 대상을 정확히 반환
- 어떤 자식으로 치환해도 `run()` 골격이 **의도대로 동작**.

**효과:** `TransactionTemplate` 타입으로 다루는 모든 코드가 자식 종류와 무관하게 안전하게 작동.

---

## 2-4. ISP (인터페이스 분리) — 영속 계약을 별도 추상으로

```java
interface BankRepository { /* customers · accounts · transactions · runInTransaction */ }
```

- `Bank`/템플릿은 **JDBC의 세부(Connection, PreparedStatement)** 를 알 필요 없이 `BankRepository`만 봄.
- (정직한 한계) 현재 `BankRepository`는 고객+계좌+거래 메서드를 한 인터페이스에 모은 다소 "넓은" 계약.
  더 엄격히 하려면 `CustomerRepo/AccountRepo/TxRepo`로 쪼갤 수 있으나, 이 규모에선 응집된 단일 영속 계약이 합리적.

**효과:** 도메인/서비스가 불필요한 DB 세부에 의존하지 않음. 테스트용 InMemory 구현으로 교체 가능.

---

## 2-5. DIP (의존성 역전) — ★ 가장 깔끔하게 적용

```java
// Bank.java — 구체 H2가 아니라 추상에 의존
private final BankRepository repo;          // 인터페이스 타입
public Bank(BankRepository repo) { this.repo = repo; }

// TransactionTemplate.java — 템플릿도 추상에만 의존
protected final BankRepository repo;

// Main.java — 구체 구현은 '바깥'에서 주입 (배선 한 줄)
Bank bank = new Bank(new H2BankRepository());
```

- 상위 모듈(`Bank`, 거래 골격) → 추상(`BankRepository`) ← 하위 모듈(`H2BankRepository`)
- 화살표가 모두 **추상을 향함** = 의존성 역전.

**효과:** DB 기술을 바꿔도 `Bank`·템플릿은 **한 글자도 안 바뀜**. 결합도↓, 교체·테스트 용이.

---

# 3. 디자인 패턴

## 3-1. Template Method (템플릿 메소드) — ★ 이 프로젝트의 중심 패턴

```java
// TransactionTemplate.java
public final TransactionResult run(Account source, String pin, long amount) {
    if (!source.authenticateAccount(source.getAccountNumber(), pin)) return WRONG_PIN;  // ① 고정
    if (amount <= 0) return INVALID_AMOUNT;                                              // ② 고정
    if (!apply(source, amount)) return INSUFFICIENT_FUNDS;                               // ③ 가변(자식)
    repo.runInTransaction(() -> {                                                        // ④ 고정 구조
        for (Account a : affectedAccounts(source)) repo.updateBalance(a);
        repo.insertHistory(record(source, amount));
    });
    return SUCCESS;
}
```

- `run()`이 **`final`** → ①PIN ②금액 ③실행 ④원자저장의 **순서를 자식이 못 바꾸고 못 건너뜀**.
- 변하는 단계(`apply`/`affectedAccounts`/`record`)만 추상 원시연산으로 위임.

**효과:**
1. **불변식의 구조적 강제** — 어떤 거래도 PIN 게이트·원자성을 우회 불가 (은행 도메인에 정확히 부합).
2. **중복 제거** — 공통 골격을 3거래가 공유.
3. **GoF 교과서 패턴** — 채점자가 `final run()`을 보고 "이건 Template Method"라고 즉시 식별.

---

## 3-2. Repository 패턴 + 트랜잭션 경계 (Unit of Work의 정신)

```java
// 영속 추상(BankRepository) ← H2 구현(H2BankRepository) → 단일 연결(Database)
@Override
public void runInTransaction(Runnable work) {
    Connection c = Database.getConnection();
    try {
        c.setAutoCommit(false);
        work.run();                       // updateBalance ×N + insertHistory
        c.commit();                       // 전부 성공해야 확정
    } catch (Exception e) {
        c.rollback();                     // 하나라도 실패 → 전부 취소
        throw new RuntimeException("... 롤백되었습니다 ...", e);
    } finally { c.setAutoCommit(true); }
}
```

**효과:** 도메인은 "이 작업들을 원자적으로 묶어줘"라고만 요청하고 JDBC 세부는 모름.
**송금 시 두 계좌+내역이 전부-아니면-전무**로 처리되어 "돈 증발"이 구조적으로 불가능.

---

## 3-3. Result Object (결과 객체) — 실패를 '값'으로

```java
public enum TransactionResult {
    SUCCESS, WRONG_PIN, INSUFFICIENT_FUNDS, INVALID_AMOUNT, ACCOUNT_NOT_FOUND, SELF_TRANSFER
}
```

- 도메인/서비스는 예외를 던지거나 직접 출력하지 않고 **결과를 값으로 반환**.
- 표현 계층이 값 → 메시지로 번역:
```java
// Main.reportResult()
case INSUFFICIENT_FUNDS -> System.out.println(">> [오류] 잔액이 부족합니다.");
```

**효과:** 출력 책임을 표현 계층에 격리(SRP), 실패 종류별로 명확한 사용자 메시지, 흐름 제어가 예외 남용 없이 깔끔.

---

## 3-4. 의존성 주입 (Dependency Injection) — 생성자 주입

```java
public Bank(BankRepository repo) { this.repo = repo; }    // 생성자 주입
Bank bank = new Bank(new H2BankRepository());             // 조립은 Main에서
```

**효과:** 객체가 자기 의존성을 직접 생성하지 않고 **외부에서 주입**받음 → DIP 실현, 교체·테스트 용이.

---

## 3-5. Value Object (값 객체) — `TransactionRecord`

```java
class TransactionRecord {       // 불변(final) 필드, 한 거래를 표현
    private final String accountNo; private final String type;
    private final long amount; private final String targetAccountNo; private final String createdAt;
    String label() { return switch (type) { case "DEPOSIT" -> "입금"; ... }; }  // 표시 로직 일원화
    @Override public String toString() { ... }
}
```

**효과:** 거래 표현·라벨 변환이 한 곳에 응집(OCP) → 표시 로직이 DAO·UI에 흩어지지 않음.

---

## 3-6. (보조) 생성 책임 중앙화 · 단일 공유 자원

- **생성 중앙화(Factory의 정신):** 계좌 생성·번호 채번을 `Bank.openAccount`가 전담
  ```java
  String accountNumber = "100-" + (1000 + repo.countMainAccounts());
  Account account = new Account(accountNumber, initialBalance, accountPassword);
  ```
  → 유효하지 않은 계좌의 임의 생성 방지, 채번 규칙 일원화.
- **단일 공유 연결(Singleton의 정신):** `Database`가 정적 단일 `Connection`을 공유
  ```java
  private static Connection connection;
  static Connection getConnection() { if (connection == null || connection.isClosed()) ...; return connection; }
  ```
  → 트랜잭션 경계(`runInTransaction`)가 성립하는 토대(같은 커넥션이어야 commit/rollback 일관).
  (정직한 한계: 전역 정적 상태라 멀티스레드엔 부적합 — 콘솔 단일 사용자 데모 범위에선 합리적.)

---

# 4. 발표용 핵심 요약 — "이건 꼭 넣어야 점수" ★★★

발표에 전부 넣을 수 없으니, **임팩트 대비 설명 비용**이 좋은 순서로 우선순위를 매겼다.

## 반드시 (Top 5 — 이것만 잘해도 고득점)

| 순위 | 넣을 것 | 한 줄 셀링 포인트 | 어떤 점수로 이어지나 |
|:---:|------|------------------|---------------------|
| 1 | **진짜 Template Method (`final run()`)** | "PIN→검증→실행→원자저장 순서를 `final`로 잠가, 어떤 거래도 보안·원자성을 못 건너뜀" | 디자인패턴 식별성 + 도메인 정합성 |
| 2 | **송금 원자성 (`runInTransaction`)** | "두 계좌+내역이 전부-아니면-전무. 돈 증발을 구조로 차단" | 기능 충실성 + 은행 도메인 이해 |
| 3 | **DIP + Repository (`BankRepository` 주입)** | "DB를 바꿔도 `Bank`는 한 글자도 안 바뀐다" | SOLID 적용의 정수 |
| 4 | **캡슐화 강제 (`package-private` 입출금)** | "잔액 변경은 거래 템플릿을 통해서만 — 도메인 규칙을 문법으로 강제" | 객체지향(정보은닉) |
| 5 | **4계층 SRP + 출력 일원화 + Result Object** | "출력은 Main만, 도메인은 값만 반환" | SRP + 구조 설계 |

## 여유 있으면 (가점 요소)

- **다형성 3종 한 슬라이드** — 오버라이딩(`toString`)·오버로딩(`transfer`)·서브타입(`apply`)을 한 번에 시연
- **트레이드오프 의식** — "Strategy도 검토했지만 런타임 교체 요구가 없고 절차 강제가 중요해 Template Method를 택함"
  → "패턴을 비교 판단할 줄 안다"는 인상 (채점자가 가장 좋아하는 멘트)
- **피터코드 5규칙** — "입금거래 is-a 트랜잭션 (역할 아님)", 추상클래스 상속도 규칙4 위반 아님(트랜잭션 자체의 추상)

## 발표 황금 문장 3개 (그대로 써도 좋음)

1. "저희는 잔액 변경 메서드를 `package-private`로 막아, **계좌의 잔액은 오직 거래 템플릿을 통해서만 바뀐다**는 도메인 규칙을 자바 문법으로 강제했습니다."
2. "`run()`을 `final`로 만들어, **어떤 거래도 PIN 검증과 원자적 저장을 건너뛸 수 없습니다.** 이것이 은행 시스템에 Template Method가 적합한 이유입니다."
3. "DB 기술(H2)을 다른 것으로 바꿔도 `Bank` 클래스는 **한 줄도 수정되지 않습니다.** 추상(`BankRepository`)에만 의존하기 때문입니다 — DIP."

## 정직하게 한 번 언급하면 신뢰도↑ (감점 아닌 가점)

- 자기송금 가드가 `Main`·`Bank` 두 곳에 중복(UX용 1차 + 서비스 강제) — "강제는 서비스, 안내는 UI" 의도라고 설명
- `BankRepository`가 다소 넓은 계약(ISP 여지) — "이 규모에선 응집된 단일 영속 계약을 택함"이라고 트레이드오프로 설명

---

## 부록: 개념 → 클래스 빠른 매핑표

| 개념 | 대표 코드 위치 |
|------|---------------|
| 캡슐화(package-private) | `Account.deposit/withdraw` |
| 추상화 | `TransactionTemplate`(abstract) · `BankRepository`(interface) |
| 상속/특수화 | `Deposit/Withdraw/TransferTransaction extends TransactionTemplate` |
| 오버라이딩 | `Account.toString` · `TransactionRecord.toString` · `apply` |
| 오버로딩 | `Bank.transfer`(2종) · `Account`(2종) · `TransactionRecord`(2종) |
| SRP | 4계층 전체 (`Main`/`Bank`/도메인/영속) |
| OCP | `TransactionTemplate` 상속 확장 |
| LSP | 3거래의 `apply/record` 규약 준수 |
| ISP | `BankRepository` |
| DIP | `Bank(BankRepository)` + `Main`의 `new H2BankRepository()` 주입 |
| Template Method | `TransactionTemplate.run()` (`final`) |
| Repository + 트랜잭션 경계 | `H2BankRepository` · `runInTransaction` |
| Result Object | `TransactionResult` |
| Value Object | `TransactionRecord` |
| 의존성 주입 | `Bank` 생성자 · `Main` 배선 |
