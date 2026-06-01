# 코드 설명서 — BankingSystem 안내

> 이 문서는 "각 클래스가 무슨 역할이고, 그 안의 메서드가 무슨 일을 하는지"를
> 위에서 아래로 한 번 읽으며 이해하도록 쓴 설명서입니다.
> 개념·UML·SOLID 이론은 [README.md](README.md), 빌드·디버깅은 [CLAUDE.md](CLAUDE.md)를 참고하세요.

---

## 0. 30초 요약 — 이 프로그램은 무엇인가

콘솔(터미널)에서 동작하는 **은행 시뮬레이터**입니다. 사용자는 메뉴 번호를 눌러
**회원가입 → 로그인 → 계좌 개설 → 입금/출금/송금 → 조회**를 합니다.
모든 데이터(고객·계좌·거래내역)는 **H2라는 작은 파일 DB**(`./data/bank.mv.db`)에 저장되어,
프로그램을 껐다 켜도 잔액이 그대로 남습니다.

핵심 설계 한 줄: **"각 클래스는 딱 한 가지 일만 한다"** — 화면 출력은 `Main`만, 업무 조율은 `Bank`만,
돈 계산은 `Account`만, DB 접근은 `H2BankRepository`만. 그래서 한 곳을 고쳐도 다른 곳이 안 깨집니다.

---

## 1. 은행 창구에 비유하기 (가장 빠른 직관)

코드의 4개 계층을 실제 은행에 빗대면 이렇습니다. 이 그림만 머리에 넣으면 나머지는 술술 읽힙니다.

| 코드 | 은행 비유 | 하는 일 | 안 하는 일 |
|------|----------|---------|-----------|
| **Main** | 번호표 키오스크 / 안내 데스크 | 사용자와 **직접 대면**. 메뉴 보여주고, 입력 받고, 결과를 화면에 출력 | 돈 계산·DB 저장 (직접 안 함) |
| **Bank** | 창구 직원 | 업무를 **접수하고 조율**. "입금이요? 그럼 입금 절차 담당에게 넘길게요" | 화면 출력, 직접 금고 열기 |
| **TransactionTemplate**<br>(+ Deposit/Withdraw/Transfer) | 거래 처리 **표준 매뉴얼** | "①신분확인 ②금액확인 ③잔액처리 ④장부기록" 절차를 **정해진 순서대로** 강제 | — |
| **Account / Customer** | 통장 / 고객 신상카드 | 잔액·계좌번호·이름 같은 **데이터**를 들고 있음. 입금/출금 시 잔액 숫자만 바꿈 | DB 저장, 화면 출력 |
| **BankRepository**<br>**H2BankRepository / Database** | 금고 + 금고 담당자 | 실제 **저장/조회**. SQL로 DB에 읽고 씀 | 업무 판단 (시키는 대로만) |
| **TransactionResult / TransactionRecord** | 처리 결과 도장 / 거래 전표 | "성공/잔액부족" 같은 **결과 값**, "입금 5만원" 같은 **기록** | — |

**황금률: 호출은 항상 위에서 아래로만 흐릅니다.**
`Main → Bank → 거래 매뉴얼 → 금고`. 아래 계층은 위 계층을 절대 호출하지 않습니다(=의존성 단방향).

```
[표현]   Main                     ← 사용자와 대면, 유일하게 화면에 출력
   │  (메서드 호출)
[서비스] Bank                     ← 업무 조율, 출력 안 함
   │
[도메인] TransactionTemplate ─┬─ DepositTransaction
   │     Account · Customer   ├─ WithdrawTransaction
   │     TransactionRecord    └─ TransferTransaction
   │     TransactionResult
[영속]   BankRepository(규약) → H2BankRepository(실제 SQL) → Database(연결·스키마)
```

---

## 2. 클래스 한 줄 지도 (전체 13개: 12 + Main)

먼저 전체를 훑어보세요. 각 클래스의 자세한 설명은 §3부터입니다.

| 계층 | 클래스 | 한 줄 역할 |
|------|--------|-----------|
| 표현 | **Main** | 메뉴 루프·입력·출력. 프로그램의 시작점(`main`) |
| 서비스 | **Bank** | 업무 조율자. 가입/인증/개설/거래를 적절한 담당에게 분배 |
| 도메인 | **Account** | 계좌. 잔액(`long`)을 들고 입금/출금으로 숫자만 변경 |
| 도메인 | **Customer** | 고객. 아이디·비밀번호·이름·주민번호 보관 |
| 도메인 | **TransactionTemplate** | 거래 공통 골격(추상). `run()`이 4단계 순서를 고정 |
| 도메인 | **DepositTransaction** | 입금 거래 — 매뉴얼의 "잔액처리" 칸만 채움 |
| 도메인 | **WithdrawTransaction** | 출금 거래 — 잔액 부족 시 실패 처리 |
| 도메인 | **TransferTransaction** | 송금 거래 — 두 계좌(출금+입금)를 함께 처리 |
| 도메인 | **TransactionResult** | 거래 결과 enum(성공/잔액부족/PIN틀림/…) |
| 도메인 | **TransactionRecord** | 거래 전표 값 객체. 한 줄 문자열로 출력 |
| 영속 | **BankRepository** | 저장소 **규약**(인터페이스). "이런 기능이 있어야 한다"만 선언 |
| 영속 | **H2BankRepository** | 위 규약의 **실제 구현**. 모든 SQL이 여기 모여 있음 |
| 영속 | **Database** | H2 연결 + 테이블 생성. 프로그램 전체가 공유하는 단 1개의 연결 |

---

## 3. 도메인 계층 — 데이터를 들고 있는 클래스들

가장 단순하고 의존성이 적어서 여기서부터 읽는 게 제일 쉽습니다.

### 3-1. `Customer` — 고객 신상카드 [src/Customer.java](src/Customer.java)

고객 한 명의 식별 정보만 들고 있는 **순수 데이터 클래스**입니다. DB 접근도, 출력도 하지 않습니다.

| 필드 | 의미 |
|------|------|
| `customerId` | 로그인 아이디 (PK) |
| `password` | 로그인 비밀번호 |
| `name` | 이름 |
| `ssn` | 주민등록번호 (1인 1가입 검사에 사용) |

| 메서드 | 하는 일 |
|--------|---------|
| `getCustomerId()` / `getPassword()` / `getName()` / `getSsn()` | 필드 값을 그대로 돌려주는 게터 |
| `openAccount(bank, 초기금액, 계좌비번)` | **계좌 개설을 은행에 위임.** 고객이 직접 계좌를 만드는 게 아니라 `bank.openAccount(this, ...)`를 호출해 결과 `Account`를 받아 돌려줌. "고객이 창구에 가서 계좌를 신청한다"를 코드로 표현 |

> 💡 **왜 계좌 목록을 안 들고 있나?** 계좌의 진실은 DB에 있습니다(진실의 원천 = single source of truth).
> 메모리에 목록을 또 두면 DB와 어긋날 수 있어, 필요할 때마다 DB에서 조회합니다.

### 3-2. `Account` — 통장 [src/Account.java](src/Account.java)

계좌 하나. **돈 계산의 주인공**이지만, 자기 잔액 숫자만 바꿀 뿐 DB 저장이나 출력은 모릅니다(순수 도메인).

| 필드 | 의미 |
|------|------|
| `accountNumber` | 계좌번호 (예: `100-1000`) |
| `balance` | 잔액. **`long`(원 단위 정수)** — 소수점 오차가 없고 DB의 `BIGINT`와 타입이 일치 |
| `accountPassword` | 거래용 4자리 PIN |

| 메서드 | 하는 일 |
|--------|---------|
| `Account(번호, 비번)` | 생성자 **오버로딩** — 잔액 0원으로 만드는 짧은 형태. 내부에서 아래 생성자를 호출 |
| `Account(번호, 초기잔액, 비번)` | 본체 생성자 |
| `authenticateAccount(번호, 비번)` | **PIN 검증.** 입력한 계좌번호+비밀번호가 이 계좌와 일치하면 `true`. 모든 거래 전에 이 게이트를 통과해야 함 |
| `deposit(금액)` | 입금 — `balance += amount`. 반환값 없음(항상 성공). **package-private**(같은 폴더만 호출) |
| `withdraw(금액)` | 출금 — 잔액이 충분하면 빼고 `true`, 부족하면 그대로 두고 `false`. **package-private** |
| `getBalance()` / `getAccountNumber()` / `getAccountPassword()` | 게터 |
| `toString()` | `Object.toString()` **재정의**. `"계좌번호 100-1000 / 잔액 50000원"` 형태로 출력. `System.out.println(account)`만 써도 이 형식이 나옴(다형성) |

> 💡 **`deposit`/`withdraw`가 왜 `public`이 아닌가?** (앞에 `public`이 없는 package-private)
> 외부에서 아무나 잔액을 바꾸면 위험합니다. **거래 매뉴얼(TransactionTemplate)만** 잔액을 만질 수 있게
> 일부러 접근을 좁혔습니다. PIN 검증·장부기록을 건너뛴 "맨손 입금"을 막는 잠금장치입니다.

### 3-3. `TransactionResult` — 처리 결과 도장 [src/TransactionResult.java](src/TransactionResult.java)

거래 한 건의 결과를 **하나의 값**으로 표현하는 enum입니다. 왜 `boolean`이 아니라 enum이냐면,
"실패"에도 종류(잔액부족 vs PIN틀림 vs …)가 있어서 화면에 다른 메시지를 보여줘야 하기 때문입니다.

| 상수 | 언제 |
|------|------|
| `SUCCESS` | 거래 성공 |
| `WRONG_PIN` | 계좌 비밀번호 불일치 |
| `INSUFFICIENT_FUNDS` | 잔액 부족 |
| `INVALID_AMOUNT` | 금액이 0 이하 |
| `ACCOUNT_NOT_FOUND` | 송금 대상 계좌 없음 |
| `SELF_TRANSFER` | 본인 계좌로 송금 시도 |

> 도메인/서비스는 이 **값만 반환**하고, "무슨 문장으로 보여줄지"는 `Main`이 결정합니다(§5의 `reportResult`).

### 3-4. `TransactionRecord` — 거래 전표 [src/TransactionRecord.java](src/TransactionRecord.java)

거래 한 건을 기록하는 **값 객체**. DB의 `transactions` 테이블 한 행에 대응합니다.

| 필드 | 의미 |
|------|------|
| `accountNo` | 거래한 계좌 |
| `type` | `"DEPOSIT"` / `"WITHDRAW"` / `"TRANSFER"` |
| `amount` | 금액(`long`) |
| `targetAccountNo` | 송금 대상 (입·출금은 `null`) |
| `createdAt` | 거래 시각. 기록할 땐 `null`(DB가 자동으로 현재시각 채움), 조회할 땐 DB에서 읽어옴 |

| 메서드 | 하는 일 |
|--------|---------|
| `TransactionRecord(계좌, 타입, 금액, 대상)` | 기록용 생성자(시각 없음) — 내부에서 아래 5인자 생성자 호출 |
| `TransactionRecord(..., createdAt)` | 조회용 생성자(시각 포함) |
| 각종 게터 | 필드 반환 |
| `label()` | `"DEPOSIT"` → `"입금"` 처럼 영문 타입을 한글 라벨로 변환. **이 변환을 여기 한 곳에만** 둬서 표시 로직이 흩어지지 않게 함 |
| `toString()` | 재정의. `"[2026-06-01 12:00:00] 송금 50000원 → 200-5678"` 형태 한 줄 |

### 3-5. 거래 매뉴얼 4종 — 이 프로젝트의 핵심 (Template Method 패턴)

여기가 가장 중요하고 처음엔 가장 헷갈리는 부분입니다. **천천히 읽으세요.**

#### `TransactionTemplate` (추상 부모) [src/TransactionTemplate.java](src/TransactionTemplate.java)

모든 거래(입금/출금/송금)가 공통으로 따르는 **4단계 절차서**입니다.
핵심은 `run()` 메서드에 `final`이 붙어 있다는 점 — **자식이 이 순서를 바꾸거나 건너뛸 수 없습니다.**

```java
public final TransactionResult run(Account source, String pin, long amount) {
    if (!source.authenticateAccount(source.getAccountNumber(), pin))  // ① PIN 검증 (공통)
        return TransactionResult.WRONG_PIN;
    if (amount <= 0)                                                   // ② 금액 검증 (공통)
        return TransactionResult.INVALID_AMOUNT;
    if (!apply(source, amount))                                        // ③ 잔액 처리 (거래마다 다름)
        return TransactionResult.INSUFFICIENT_FUNDS;
    repo.runInTransaction(() -> {                                      // ④ 원자적 DB 저장 (공통 구조)
        for (Account a : affectedAccounts(source)) repo.updateBalance(a);
        repo.insertHistory(record(source, amount));
    });
    return TransactionResult.SUCCESS;
}
```

- **①②④는 모든 거래가 똑같으므로 부모가 1번만 작성** → 입금/출금/송금에 복붙할 필요가 없음(중복 제거).
- **③과 "어떤 계좌·전표를 저장하나"는 거래마다 다름** → 아래 3개의 **추상 메서드(빈칸)** 로 자식에게 위임:

| 추상 메서드(빈칸) | 자식이 채워야 할 내용 |
|------|------|
| `apply(source, amount)` | 실제 잔액 변경. 성공하면 `true`(출금/송금은 잔액 부족 시 `false`) |
| `affectedAccounts(source)` | ④에서 DB에 반영할 계좌 목록(입·출금 1개, 송금 2개) |
| `record(source, amount)` | ④에서 남길 거래 전표 1건 |

> 💡 **비유:** `run()`은 "거래 처리 양식"이고, 빈칸 3개만 거래 종류별로 다르게 채우는 셈입니다.
> 신입(새 거래 종류)이 와도 양식의 순서(보안·검증)는 절대 못 건드리고 빈칸만 채웁니다 → 실수 방지.

#### `DepositTransaction` (입금) [src/DepositTransaction.java](src/DepositTransaction.java)

빈칸을 가장 단순하게 채운 자식입니다.

| 빈칸 | 입금의 구현 |
|------|-----------|
| `apply` | `source.deposit(amount); return true;` — 입금은 **항상 성공** |
| `affectedAccounts` | `List.of(source)` — 내 계좌 하나만 |
| `record` | `"DEPOSIT"` 전표, 대상 `null` |

#### `WithdrawTransaction` (출금) [src/WithdrawTransaction.java](src/WithdrawTransaction.java)

입금과 거의 같지만 `apply`가 실패할 수 있습니다.

| 빈칸 | 출금의 구현 |
|------|-----------|
| `apply` | `return source.withdraw(amount);` — 잔액 부족이면 `false` → 부모 `run()`이 `INSUFFICIENT_FUNDS` 반환 |
| `affectedAccounts` | `List.of(source)` |
| `record` | `"WITHDRAW"` 전표, 대상 `null` |

#### `TransferTransaction` (송금) [src/TransferTransaction.java](src/TransferTransaction.java)

유일하게 **계좌 2개**를 다루므로 `target`(받는 계좌)을 **생성자로 주입**받습니다.
(`run()`의 시그니처는 고정이라 매개변수로 못 받기 때문에 생성자로 받음)

| 빈칸 | 송금의 구현 |
|------|-----------|
| `apply` | `source`에서 출금 성공하면 → `target`에 입금하고 `true`. 출금 실패면 `false`(입금 안 함) |
| `affectedAccounts` | `List.of(source, target)` — **두 계좌 모두** ④에서 한 트랜잭션으로 갱신 |
| `record` | `"TRANSFER"` 전표, 대상 = `target` 계좌번호 |

> 💡 **왜 송금이 중요한가:** 출금 계좌·입금 계좌·거래전표 **3가지가 전부 같이 저장되거나, 하나라도 실패하면
> 전부 취소**되어야 합니다(안 그러면 돈이 증발). 이 "전부 아니면 전무"를 ④의 `runInTransaction`이 보장합니다.

---

## 4. 서비스 & 영속 계층 — 조율자와 금고

### 4-1. `Bank` — 창구 직원(업무 조율자) [src/Bank.java](src/Bank.java)

`Main`(화면)과 저장소 사이에서 **업무를 조율**합니다. 화면 출력은 하지 않고, 결과를 **값으로 반환**만 합니다.
`BankRepository`(인터페이스)에만 의존하므로 DB 기술이 H2든 다른 것이든 `Bank`는 바뀌지 않습니다.

| 메서드 | 하는 일 |
|--------|---------|
| `Bank(repo)` | 생성자. 저장소를 **주입**받아 보관(의존성 주입) |
| `authenticate(아이디, 비번)` | 로그인 — 일치하는 `Customer` 반환, 없으면 `null` |
| `register(customer)` | 회원 등록 — 아이디 중복이면 `false`, 아니면 저장 후 `true` |
| `isSsnRegistered(ssn)` | 같은 주민번호가 이미 있나? (1인 1가입 검사) |
| `isIdTaken(아이디)` | 이미 쓰는 아이디인가? |
| `openAccount(customer, 초기금액, 비번)` | 계좌 개설 — 계좌번호를 `"100-" + (1000 + 기존계좌수)`로 만들어 저장 후 `Account` 반환 |
| `hasAccount(customer)` | 이 고객이 이미 계좌가 있나? (1인 1계좌 중복 차단) |
| `loadAccount(customer)` | 재실행 후 로그인 시 DB에서 대표 계좌 복원 |
| `findAccount(번호)` | 계좌번호로 계좌 조회(송금 대상 확인용) |
| `recentHistory(번호, 건수)` | 최근 거래내역 N건 조회 |
| **`deposit(account, pin, 금액)`** | **입금 거래를 `DepositTransaction`에 위임:** `new DepositTransaction(repo).run(account, pin, 금액)` |
| **`withdraw(...)`** | 출금을 `WithdrawTransaction`에 위임 |
| **`transfer(source, pin, 대상번호, 금액)`** | 송금(번호 버전): 자기송금이면 `SELF_TRANSFER`, 대상 없으면 `ACCOUNT_NOT_FOUND`, 아니면 객체 버전에 위임 |
| **`transfer(source, pin, 대상Account, 금액)`** | 송금(객체 버전, **오버로딩**): 호출부가 이미 대상 객체를 들고 있을 때 조회 생략. `TransferTransaction`에 위임 |
| `ensureTargetAccount()` | 시연용 가상 송금 대상 계좌(`200-5678`)를 DB에 1회 시드 |

> 💡 거래 3개 메서드를 보면 **`Bank`는 판단·조율만 하고 실제 절차는 거래 매뉴얼(Template)에 넘깁니다.**
> "입금이요? 입금 담당에게 넘길게요" 딱 그 역할입니다.

### 4-2. `BankRepository` — 금고 사용 규약(인터페이스) [src/BankRepository.java](src/BankRepository.java)

"저장소라면 이런 기능들이 있어야 한다"를 **선언만** 한 인터페이스입니다. 구현(SQL)은 없습니다.
`Bank`가 이 규약에만 의존하기 때문에, 나중에 H2 대신 "메모리 저장소"로 바꿔도 `Bank` 코드는 안 바뀝니다(DIP).

선언된 기능들(고객/계좌/거래 + 트랜잭션 경계): `insertCustomer`, `existsCustomer`, `existsBySsn`,
`findByCredentials`, `insertAccount`, `updateBalance`, `existsAccount`, `existsAccountByCustomer`,
`countMainAccounts`, `findAccount`, `findFirstAccount`, `insertHistory`, `findRecentHistory`,
`runInTransaction`.

### 4-3. `H2BankRepository` — 실제 금고 담당자 [src/H2BankRepository.java](src/H2BankRepository.java)

위 규약의 **실제 구현**. 프로젝트의 모든 **SQL이 이 한 파일에** 모여 있습니다.
각 메서드는 "SQL 준비 → 값 바인딩 → 실행 → 결과를 객체로 변환" 패턴을 따릅니다.

대표 메서드 몇 개만 짚으면:

| 메서드 | SQL이 하는 일 |
|--------|--------------|
| `insertCustomer` | `INSERT INTO customers ...` — 고객 1명 저장 |
| `findByCredentials` | `SELECT ... WHERE id=? AND password=?` — 맞으면 `Customer` 생성해 반환, 없으면 `null` |
| `insertAccount` | `INSERT INTO accounts ...` — `setLong`으로 잔액 저장 |
| `updateBalance` | `UPDATE accounts SET balance=?` — 거래 후 잔액 동기화 |
| `countMainAccounts` | `SELECT COUNT(*) ... LIKE '100-%'` — 계좌번호 생성용 카운트 |
| `findAccount` / `findFirstAccount` | 계좌를 `Account` 객체로 복원(`getLong("balance")`) |
| `insertHistory` | 거래 전표 1건 저장. 대상이 `null`이면 `setNull` |
| `findRecentHistory` | 최근 N건을 최신순(`ORDER BY id DESC`)으로 조회, 시각 포맷팅 |
| **`runInTransaction(work)`** | **이 프로젝트 원자성의 심장** ↓ |

```java
public void runInTransaction(Runnable work) {
    Connection c = Database.getConnection();
    try {
        c.setAutoCommit(false);   // "지금부터 묶음 처리" — 자동 저장 끔
        work.run();               // updateBalance ×N + insertHistory 실행
        c.commit();               // 전부 성공해야 한꺼번에 확정
    } catch (Exception e) {
        c.rollback();             // 하나라도 실패하면 전부 취소(돈 증발 방지)
        throw new RuntimeException("... 롤백되었습니다 ...", e);
    } finally {
        c.setAutoCommit(true);    // 원래대로 복구
    }
}
```

> 💡 평소 DB는 SQL 한 줄마다 즉시 저장(auto-commit)합니다. 송금처럼 여러 줄이 **전부 또는 전무**여야 할 땐
> auto-commit을 끄고 `work`를 다 돌린 뒤 한 번에 `commit`합니다. 중간에 터지면 `rollback`으로 통째 취소.

### 4-4. `Database` — 금고 그 자체(연결·스키마) [src/Database.java](src/Database.java)

H2 DB 연결을 관리하고 테이블을 만듭니다.

| 메서드 | 하는 일 |
|--------|---------|
| `getConnection()` | **프로그램 전체가 공유하는 단 1개의 연결**을 돌려줌(없거나 닫혔으면 새로 연결). 이 "단일 공유 연결" 덕분에 `runInTransaction`의 트랜잭션 경계가 성립 |
| `init()` | 앱 시작 시 1회. `customers`/`accounts`/`transactions` 3개 테이블을 `IF NOT EXISTS`로 생성 |
| `close()` | 종료 시 연결 정리 |

테이블 구조 요약:

| 테이블 | 주요 컬럼 |
|--------|-----------|
| `customers` | `customer_id`(PK), `password`, `name`, `ssn` |
| `accounts` | `account_number`(PK), `customer_id`(FK), `balance BIGINT`, `account_password` |
| `transactions` | `id`(자동증가 PK), `account_number`, `type`, `amount BIGINT`, `target_account`, `created_at` |

> ⚠️ **주의:** `init()`은 `CREATE TABLE IF NOT EXISTS`라 **기존 테이블의 컬럼 타입은 바꾸지 않습니다.**
> 스키마를 수정했다면 `rm -rf data`로 DB를 지우고 재실행해야 새 스키마가 반영됩니다(자세히는 [CLAUDE.md](CLAUDE.md)).

---

## 5. 표현 계층 — `Main` [src/Main.java](src/Main.java)

프로그램의 **시작점**이자 **유일하게 화면에 출력**하는 곳입니다. 사용자 입력을 받아 `Bank`에 일을 시키고,
돌아온 결과(`TransactionResult`, `Account`, `Customer`)를 한글 메시지로 바꿔 보여줍니다.

### main()의 뼈대

```
1. Database.init()                          ← DB 테이블 준비
2. Bank bank = new Bank(new H2BankRepository())  ← 서비스에 저장소 주입 (DIP 배선)
3. bank.ensureTargetAccount()               ← 시연용 송금 대상 계좌 시드
4. while (isRunning) { 메뉴 출력 → 번호 입력 → switch 분기 }   ← 메인 루프
```

세션 상태는 지역 변수 3개로 관리합니다: `loginCustomer`(로그인한 고객), `isLoggedIn`(로그인 여부),
`myAccount`(내 계좌). 로그인 실패 시 이 셋을 모두 초기화해 "로그아웃 후에도 거래되는 허점"을 막습니다.

### 메뉴(switch case)별 역할

| 메뉴 | 코드 | 하는 일 | 호출하는 Bank 메서드 |
|------|------|---------|---------------------|
| 1 | case 1 | 로그인 / 회원가입(가입 후 자동 로그인). 아이디·주민번호 중복 검사 | `authenticate`, `isSsnRegistered`, `isIdTaken`, `register`, `loadAccount` |
| 2 | case 2 | 계좌 개설(로그인 필수, 1인 1계좌) | `hasAccount`, `loginCustomer.openAccount` |
| 3 | case 3 | 계좌 비밀번호 검증(DB 없이 메모리 객체로) | `myAccount.authenticateAccount` |
| 4 | case 4 | 입금 | `bank.deposit` |
| 5 | case 5 | 출금 | `bank.withdraw` |
| 6 | case 6 | 송금(기본 대상 `200-5678`, 자기송금 1차 차단) | `bank.transfer` |
| 7 | case 7 | 계좌·최근 거래내역 조회 | `bank.recentHistory` + `toString()` 출력 |
| 8 | case 8 | 종료 — `Database.close()` 후 루프 탈출 | — |

각 거래 메뉴(4·5·6)는 **금액 입력 시 `nextLong()`** 로 받고 `InputMismatchException`을 잡아
"숫자를 입력해주세요"로 방어합니다.

### reportResult() — 결과 값을 메시지로 번역

거래 메서드들이 돌려준 `TransactionResult`를 사용자 문장으로 바꾸는 헬퍼입니다.
**"출력은 표현 계층의 책임"** 원칙이 가장 잘 드러나는 곳입니다.

```java
case SUCCESS            -> 성공 메시지 + "현재 잔액 : ...원"
case WRONG_PIN          -> ">> [오류] 계좌 비밀번호가 일치하지 않습니다."
case INSUFFICIENT_FUNDS -> ">> [오류] 잔액이 부족합니다."
case INVALID_AMOUNT     -> ">> [오류] 금액은 0원보다 커야 합니다."
case ACCOUNT_NOT_FOUND  -> ">> [오류] 존재하지 않는 계좌번호입니다."
case SELF_TRANSFER      -> ">> [오류] 본인 계좌로는 송금할 수 없습니다."
```

---

## 6. 흐름 따라가기 — 코드는 실제로 어떻게 협력하나

표만 봐선 안 잡히는 "객체들의 협력"을 두 시나리오로 추적해봅시다.

### 시나리오 A: **입금 5만원** (가장 단순)

```
사용자가 메뉴 4 선택, 금액 50000, PIN 입력
  └─ Main.case4: bank.deposit(myAccount, "1234", 50000) 호출
       └─ Bank.deposit: new DepositTransaction(repo).run(myAccount, "1234", 50000)
            └─ TransactionTemplate.run():   ← 고정된 4단계
                 ① authenticateAccount → PIN 맞음 ✅
                 ② 50000 > 0 ✅
                 ③ apply() → DepositTransaction.apply → myAccount.deposit(50000)  (메모리 잔액 +5만)
                 ④ runInTransaction:
                      - updateBalance(myAccount)   ← DB UPDATE
                      - insertHistory("DEPOSIT" 전표)  ← DB INSERT
                      → 둘 다 성공 → commit
                 return SUCCESS
       ← Main.reportResult(SUCCESS): "입금 완료 / 현재 잔액 : ...원" 출력
```

### 시나리오 B: **송금 2만원** (계좌 2개 + 원자성)

```
사용자가 메뉴 6 선택, 대상 200-5678, 금액 20000, PIN 입력
  └─ Main.case6: 자기송금 1차 체크(통과) → bank.transfer(myAccount, PIN, "200-5678", 20000)
       └─ Bank.transfer(번호버전): 자기송금? 아님 → target = repo.findAccount("200-5678")
            → target 있음 → transfer(myAccount, PIN, target, 20000) (객체버전에 위임)
                 └─ new TransferTransaction(repo, target).run(myAccount, PIN, 20000)
                      └─ TransactionTemplate.run():
                           ① PIN ✅  ② 금액 ✅
                           ③ apply: myAccount.withdraw(20000) 성공 → target.deposit(20000)
                           ④ runInTransaction:
                                - updateBalance(myAccount)   ← 출금 계좌
                                - updateBalance(target)      ← 입금 계좌
                                - insertHistory("TRANSFER")  ← 전표
                                → 셋 다 성공해야 commit / 하나라도 실패하면 전부 rollback
                           return SUCCESS
       ← reportResult(SUCCESS) 출력
```

> 이 두 흐름을 비교하면 **Template Method의 가치**가 보입니다. ①②④의 골격은 완전히 동일하고,
> 입금/송금의 차이는 ③`apply`와 ④에 넘기는 "계좌 개수·전표"뿐입니다. 그래서 새 거래가 생겨도
> 골격은 그대로 두고 자식 클래스 하나만 추가하면 됩니다(OCP — 확장엔 열림, 수정엔 닫힘).

---

## 7. 처음 읽을 때 헷갈리기 쉬운 포인트 (FAQ)

**Q. 왜 `Account.deposit`이 직접 DB에 저장 안 하나요?**
A. `Account`는 "잔액 숫자"만 책임집니다(순수 도메인). DB 저장은 거래 매뉴얼의 ④단계가 `repo`로 합니다.
역할을 섞지 않아야(SRP) 테스트·변경이 쉽습니다.

**Q. `run()`에 왜 `final`이 붙었나요?**
A. 자식이 거래 절차(보안 게이트·검증 순서)를 **바꾸지 못하게 잠그기** 위해서입니다. 자식은 빈칸 3개만 채웁니다.

**Q. `transfer`가 왜 두 개(번호 버전 / 객체 버전)인가요?**
A. **메서드 오버로딩.** 대상이 "번호 문자열"일 때와 "이미 가진 Account 객체"일 때를 모두 편하게 호출하려고요.
번호 버전은 조회 후 객체 버전에 위임해 중복을 없앴습니다.

**Q. 자기송금 검사가 왜 `Main`과 `Bank` 양쪽에 있나요?**
A. `Main`은 빠른 사용자 안내(UX)용 1차 방어, `Bank`는 서비스를 직접 호출해도 뚫리지 않게 하는 **진짜 강제**입니다.
(현재 `Bank`에 번호/객체 버전 두 곳 + `Main` 한 곳으로 가드가 다소 중복되어 있는 건 알려진 정리 대상입니다.)

**Q. 실패한 거래는 DB에 흔적이 남나요?**
A. 안 남습니다. ③에서 실패(PIN 틀림·잔액 부족·금액 0)하면 ④에 도달하지 못해 **DB에 아무것도 안 씁니다.**
④ 도중 실패도 `rollback`으로 전부 취소됩니다.

**Q. 어디부터 코드를 읽으면 되나요?**
A. ① [Account.java](src/Account.java)(가장 단순) → ② [TransactionTemplate.java](src/TransactionTemplate.java)(핵심)
→ ③ [Bank.java](src/Bank.java)(조율) → ④ [Main.java](src/Main.java)(전체 흐름) 순서를 추천합니다.

---

## 8. 더 보기

- **개념·UML·SOLID·디자인패턴 이론** → [README.md](README.md) (클래스/시퀀스 다이어그램 포함)
- **빌드·실행·DB 점검·디버깅** → [CLAUDE.md](CLAUDE.md)
