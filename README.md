# 은행 전산 시스템 (Banking System)

Java로 구현한 콘솔 기반 은행 전산 시스템입니다. 고객 관리, 계좌 개설, 입금·출금·송금 거래를 지원하며,
모든 데이터는 **H2 임베디드 데이터베이스**에 영구 저장되어 프로그램을 재실행해도 유지됩니다.

UML 클래스/시퀀스 다이어그램 설계를 기반으로, **SOLID 원칙**에 따라 표현 → 서비스 → 도메인 → 영속의
네 계층으로 분리했습니다. 거래는 **Strategy(전략) 패턴**으로 구현하고, 모든 JDBC는 단일
**`BankRepository`** 에 격리했습니다. 또한 **모든 거래는 계좌 비밀번호(PIN) 검증을 통과해야** 수행됩니다.

---

## 주요 기능

| 메뉴 | 기능 | 설명 |
|------|------|------|
| 1 | 로그인 / 회원가입 | 사용자 인증 및 신규 고객 등록 (주민번호·아이디 중복 차단) |
| 2 | 신규 계좌 개설 | 로그인한 고객의 계좌 생성 (계좌번호 `100-1000`부터 자동 부여, 1인 1계좌) |
| 3 | 계좌 비밀번호 검증 | 계좌 단위 PIN 사전 확인(`계좌인증`) |
| 4 | 입금 거래 | `DepositTransaction` — **계좌 비밀번호 입력 후** 수행 |
| 5 | 출금 거래 | `WithdrawTransaction` — **계좌 비밀번호 입력 후** 수행 (잔액 부족 시 차단) |
| 6 | 송금 거래 | `TransferTransaction` — **계좌 비밀번호 입력 후** 수행. 대상 계좌번호 입력, 엔터 시 기본값 `200-5678` |
| 7 | 계좌 정보 조회 | 본인 계좌 명세 + 최근 거래내역(최대 3건) 출력 |
| 8 | 종료 | DB 연결 정리 후 프로그램 종료 |

> **거래 결과**는 `TransactionResult` enum으로 구분됩니다:
> `SUCCESS`(성공) / `WRONG_PIN`(비밀번호 불일치) / `INSUFFICIENT_FUNDS`(잔액 부족) /
> `INVALID_AMOUNT`(금액 오류) / `ACCOUNT_NOT_FOUND`(대상 계좌 없음). 실패한 거래는 DB에 기록되지 않습니다.

---

## 요구 사항

- **JDK 25** (`.idea/misc.xml`에 `openjdk-25`로 구성)
- **H2 데이터베이스 JDBC 드라이버** — `lib/h2-2.2.224.jar`
- Maven/Gradle을 쓰지 않는 순수 IntelliJ IDEA 프로젝트이므로, 컴파일·실행 시 H2 jar를
  반드시 클래스패스에 포함해야 합니다.
- `--enable-preview` 필요 (`Main.java`가 프리뷰 기능을 사용)

---

## 빌드 & 실행

```bash
# 1) 컴파일 (H2 jar를 클래스패스에 포함)
javac --enable-preview --release 25 -cp "lib/*" -d out src/*.java

# 2) 실행 (out 디렉터리 + H2 jar를 클래스패스에 포함)
java --enable-preview -cp "out:lib/*" Main
```

> **IntelliJ에서 실행 시**: `File → Project Structure → Libraries → +`로 `lib/h2-2.2.224.jar`를
> 모듈 라이브러리로 등록하세요. (이미 `BankingSystem.iml`에 등록되어 있습니다.)

H2 드라이버가 없다면 아래 명령으로 내려받습니다:

```bash
curl -fsSL -o lib/h2-2.2.224.jar \
  https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar
```

---

## 데이터베이스 (H2 임베디드)

- 모든 상태는 임베디드 H2 파일 DB(`./data/bank.mv.db`, gitignore 처리됨)에 저장됩니다.
- 연결 URL: `jdbc:h2:./data/bank;AUTO_SERVER=TRUE`
  - `AUTO_SERVER=TRUE` 덕분에 앱이 실행 중이어도 H2 Console / IntelliJ에서 동시 접속·조회가 가능합니다.
- 스키마(`customers`, `accounts`, `transactions`)는 시작 시 `Database.init()`가 자동 생성합니다.

### 스키마

```sql
customers     (customer_id PK, password, name, ssn)
accounts      (account_number PK, customer_id FK→customers, balance, account_password)
transactions  (id PK AUTO_INCREMENT, account_number, type, amount, target_account, created_at)
              -- type: DEPOSIT | WITHDRAW | TRANSFER  (입·출금은 target_account NULL)
```

### DB 내용 확인 (IntelliJ Ultimate 없이도 가능)

IntelliJ의 Database 툴 창은 Ultimate 전용이지만, H2 jar에 내장된 **H2 Console**은 에디션과
무관하게(무료 Community 포함) 동작합니다.

```bash
# 브라우저 콘솔: http://localhost:8082
# 접속 정보 — JDBC URL: 위 연결 URL, 사용자: sa, 비밀번호: (공란)
java -cp "lib/*" org.h2.tools.Console

# 또는 셸에서 일회성 쿼리
java -cp "lib/*" org.h2.tools.Shell \
  -url "jdbc:h2:./data/bank;AUTO_SERVER=TRUE" -user sa -password "" \
  -sql "SELECT * FROM transactions"
```

---

## 아키텍처

```
[표현]   Main                       ── 메뉴/입력/출력, PIN 입력, TransactionResult 분기
   │
[서비스] Bank                       ── 업무 조율 + PIN 검증 게이트 + 영속화 위임 (출력 없음)
   │      deposit/withdraw/transfer(account, pin, amount) → TransactionResult
   ├──> [도메인] 고객 (Customer) · 계좌 (Account, 순수)
   │             거래 (Transaction) ─┬── 입금거래 (DepositTransaction)
   │                                 ├── 출금거래 (WithdrawTransaction)
   │                                 └── 송금거래 (TransferTransaction)
   │             거래이력 (TransactionRecord) · TransactionResult (enum)
   └──> [영속]   BankRepository ──> Database (H2 연결/스키마)
                 (customers · accounts · transactions 3테이블의 모든 JDBC를 한 클래스에 격리)
```

### 핵심 설계

- **계층 분리 (SOLID)**: 표현(`Main`) → 서비스(`Bank`) → 도메인 → 영속(`BankRepository`)으로 책임을 분리했습니다.
  도메인(`Account`, 거래 전략)은 DB·출력을 가지지 않는 **순수 객체**이고, 출력은 오직 `Main`이 담당합니다.
- **PIN 검증 게이트**: 모든 거래(`Bank.deposit/withdraw/transfer`)는 먼저 `계좌인증(authenticateAccount)`으로
  계좌 비밀번호를 검증합니다. 불일치 시 `WRONG_PIN`을 반환하고 거래를 시작하지 않습니다. 검증을
  서비스 계층에서 강제하므로 UI를 우회할 수 없습니다. (메뉴 3은 별도의 '사전 확인' 용도로 유지)
- **Strategy 패턴**: `Account.입금`/`출금`은 패키지-프라이빗이며, 외부 코드는 오직 `거래수행(performTransaction)`을
  통해서만 거래를 실행합니다. `Transaction.execute(계좌)`는 성공 여부(`boolean`)만 반환하는 순수 연산입니다.
- **영속화는 서비스의 책임**: 전략이 성공한 뒤에만 `Bank`가 `BankRepository.updateBalance`로 잔액을,
  `insertHistory`로 거래내역 1건을 기록합니다. 실패한 거래(PIN 오류·잔액 부족·금액 오류·계좌 없음)는
  **DB에 흔적을 남기지 않습니다.**
- **거래이력·라벨 일원화**: `TransactionRecord`가 거래 한 건을 표현하고 `type → 한글 라벨` 변환을
  담당합니다(표시 로직이 영속 계층에 흩어지지 않음).
- **인증 구분**: `사용자인증`(은행 단위 로그인)과 `계좌인증`(계좌 단위 PIN 검증)을 분리합니다.

> 클래스 수는 **11개 + `Main`** 입니다(설계 제약: ~10개). 기존 DAO 3개(`CustomerDao`/`AccountDao`/`TransactionDao`)를
> `BankRepository` 1개로 통합해 제약을 충족했습니다. 완전한 DIP(Repository 인터페이스+구현체 분리)는
> 제약을 위해 보류했고, JDBC를 한 클래스에 격리해 추후 인터페이스 추출만으로 확장 가능합니다.

### 소스 구성

| 파일 | 계층 | 역할 |
|------|------|------|
| `Main.java` | 표현 | 콘솔 메뉴 루프 + PIN 입력 + 결과 출력 (진입점) |
| `Bank.java` | 서비스 | 업무 조율 + PIN 검증 게이트 + 영속화 위임 |
| `Customer.java` | 도메인 | 고객 도메인 객체 (순수) |
| `Account.java` | 도메인 | 계좌 도메인 객체 (순수 — DB/출력 없음) |
| `Transaction.java` | 도메인 | 거래 인터페이스 (`boolean execute`) |
| `DepositTransaction` / `WithdrawTransaction` / `TransferTransaction` | 도메인 | 거래 전략 구현 |
| `TransactionRecord.java` | 도메인 | 거래이력 값 객체 + 한글 라벨 |
| `TransactionResult.java` | 도메인 | 거래 결과 enum |
| `BankRepository.java` | 영속 | 고객·계좌·거래 3테이블의 모든 JDBC 격리 |
| `Database.java` | 영속 | H2 연결 관리 + 스키마 초기화 |

---

## 사용 예시 (영속화 검증)

1. 메뉴 `1`로 회원가입 → 이어서 자동 로그인
2. 메뉴 `2`로 계좌 개설 (예: 초기 예치금 10,000원, 계좌 비밀번호 설정)
3. 메뉴 `4` 입금 → `5` 출금 → `6` 송금 수행 — **각 거래마다 계좌 비밀번호 입력**
4. 메뉴 `8`로 종료
5. **프로그램 재실행** 후 같은 아이디로 로그인 → 메뉴 `7` 조회 시 종료 전 잔액과 최근 거래내역이 그대로 유지됨을 확인

각 거래는 계좌 비밀번호를 입력해야 하며, 비밀번호가 틀리면 거래가 취소됩니다(잔액·거래내역 변동 없음).
송금(메뉴 6)은 대상 계좌번호를 직접 입력하면 해당 계좌로 이체되고, 아무것도 입력하지 않고
엔터를 누르면 기본 가상 계좌(`200-5678`)로 자동 송금됩니다.
