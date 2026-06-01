# 은행 전산 시스템 (Banking System)

Java로 구현한 콘솔 기반 은행 전산 시스템입니다. 고객 관리, 계좌 개설, 입금·출금·송금 거래를 지원하며,
모든 데이터는 **H2 임베디드 데이터베이스**에 영구 저장되어 프로그램을 재실행해도 유지됩니다.

UML 클래스/시퀀스 다이어그램 설계를 기반으로 하며, 거래는 **Strategy(전략) 패턴**으로,
데이터 접근은 **DAO 패턴**으로 분리했습니다.

---

## 주요 기능

| 메뉴 | 기능 | 설명 |
|------|------|------|
| 1 | 로그인 / 회원가입 | 사용자 인증(`사용자인증`) 및 신규 고객 등록 |
| 2 | 신규 계좌 개설 | 로그인한 고객의 계좌 생성 (계좌번호 `100-1000`부터 자동 부여) |
| 3 | 계좌 비밀번호 검증 | 계좌 단위 PIN 인증(`계좌인증`) |
| 4 | 입금 거래 | `DepositTransaction` 수행 |
| 5 | 출금 거래 | `WithdrawalTransaction` 수행 (잔액 부족 시 차단) |
| 6 | 송금 거래 | `TransferTransaction` — 대상 계좌번호 입력, 엔터 시 기본값 `200-5678` |
| 7 | 계좌 정보 조회 | 본인 계좌 및 송금 대상 계좌 명세 출력 |
| 8 | 종료 | DB 연결 정리 후 프로그램 종료 |

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
고객 (Customer) ──> 은행 (Bank) ──> 계좌 (Account)
                                        │
                                        └─uses─> <<interface>> 거래 (Transaction)
                                                     ├── 입금거래 (DepositTransaction)
                                                     ├── 출금거래 (WithdrawalTransaction)
                                                     └── 송금거래 (TransferTransaction)

데이터 접근 계층 (DAO) ──> Database (H2 연결/스키마)
  ├── CustomerDao      (customers 테이블)
  ├── AccountDao       (accounts 테이블)
  └── TransactionDao   (transactions 테이블)
```

### 핵심 설계

- **Strategy 패턴**: `Account.입금`/`출금`은 `private`(패키지-프라이빗)이며, 외부 코드는 오직
  `거래수행(거래)`을 통해서만 거래를 실행합니다. `Transaction` 인터페이스의 `execute(계좌)`가 진입점입니다.
- **DAO 패턴**: `Bank`는 메모리 컬렉션을 보유하지 않고 DAO에 위임합니다. 잔액 변경은 실제 변경
  지점인 `Account.deposit`/`withdraw`에서 `AccountDao.updateBalance`로 즉시 DB에 동기화됩니다.
- **거래 기록**: 각 `Transaction.execute`는 성공 시 `TransactionDao`로 거래내역 1건을 남깁니다.
  (출금·송금은 잔액 부족 등으로 실패하면 기록하지 않습니다.)
- **인증 구분**: `사용자인증`(은행 단위 로그인)과 `계좌인증`(계좌 단위 PIN 검증)을 분리합니다.

### 소스 구성

| 파일 | 역할 |
|------|------|
| `Main.java` | 콘솔 메뉴 루프 (진입점) |
| `Bank.java` | 고객/계좌 관리 — DAO에 위임 |
| `Customer.java` | 고객 도메인 객체 |
| `Account.java` | 계좌 도메인 객체 (잔액 변경 시 DB 동기화) |
| `Transaction.java` | 거래 인터페이스 |
| `DepositTransaction` / `WithdrawTransaction` / `TransferTransaction` | 거래 전략 구현 |
| `Database.java` | H2 연결 관리 + 스키마 초기화 |
| `CustomerDao` / `AccountDao` / `TransactionDao` | 테이블별 데이터 접근 |

---

## 사용 예시 (영속화 검증)

1. 메뉴 `1`로 회원가입 → 이어서 자동 로그인
2. 메뉴 `2`로 계좌 개설 (예: 초기 예치금 10,000원)
3. 메뉴 `4` 입금 → `5` 출금 → `6` 송금 수행
4. 메뉴 `8`로 종료
5. **프로그램 재실행** 후 같은 아이디로 로그인 → 메뉴 `7` 조회 시 종료 전 잔액이 그대로 유지됨을 확인

송금(메뉴 6)은 대상 계좌번호를 직접 입력하면 해당 계좌로 이체되고, 아무것도 입력하지 않고
엔터를 누르면 기본 가상 계좌(`200-5678`)로 자동 송금됩니다.
