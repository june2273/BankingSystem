import java.util.List;

// 은행 — 서비스/오케스트레이터. 업무 흐름을 조율하고 영속화는 BankRepository에 위임한다.
//   · 콘솔 출력을 가지지 않는다(출력은 표현 계층 Main). 결과는 값으로 반환.
//   · 모든 거래는 PIN 검증 게이트를 먼저 통과해야 한다(보안).
class Bank {

    private final BankRepository repo;

    public Bank(BankRepository repo) {
        this.repo = repo;
    }

    // UML : 사용자인증 — 성공 시 Customer, 실패 시 null
    public Customer authenticate(String customerId, String password) {
        return repo.findByCredentials(customerId, password);
    }

    // UML : 고객등록 — 새로 등록되면 true, 이미 있으면 false
    public boolean register(Customer customer) {
        if (repo.existsCustomer(customer.getCustomerId())) {
            return false;
        }
        repo.insertCustomer(customer);
        return true;
    }

    // 회원가입 중복 검사 — 동일 주민등록번호로 이미 가입된 회원이 있는지
    public boolean isSsnRegistered(String ssn) {
        return repo.existsBySsn(ssn);
    }

    // 회원가입 중복 검사 — 이미 사용 중인 아이디인지
    public boolean isIdTaken(String customerId) {
        return repo.existsCustomer(customerId);
    }

    // UML : 계좌생성 — 계좌번호는 DB 기준 보유 계좌 수로 생성
    public Account openAccount(Customer customer, long initialBalance, String accountPassword) {
        String accountNumber = "100-" + (1000 + repo.countMainAccounts());
        Account account = new Account(accountNumber, initialBalance, accountPassword);
        repo.insertAccount(account, customer.getCustomerId());
        return account;
    }

    // 고객이 이미 계좌를 보유했는지 (중복 개설 차단용) — DB 기준
    public boolean hasAccount(Customer customer) {
        return repo.existsAccountByCustomer(customer.getCustomerId());
    }

    // 재실행 후 로그인 시 고객의 대표 계좌를 DB에서 복원 (없으면 null)
    public Account loadAccount(Customer customer) {
        return repo.findFirstAccount(customer.getCustomerId());
    }

    // 계좌번호로 계좌 조회 (송금 대상 표시용) — 없으면 null
    public Account findAccount(String accountNumber) {
        return repo.findAccount(accountNumber);
    }

    // UML : 계좌인증 — case 3 보조 메뉴 + 거래 PIN 게이트 공용
    public boolean verifyAccountPassword(Account account, String pin) {
        return account.authenticateAccount(account.getAccountNumber(), pin);
    }

    // 계좌의 최근 거래내역 limit건 조회 (계좌 정보 최종 조회용)
    public List<TransactionRecord> recentHistory(String accountNumber, int limit) {
        return repo.findRecentHistory(accountNumber, limit);
    }

    // ===== 거래 (PIN 검증 게이트 내장 → 전략 실행 → 성공 시에만 영속화) =====

    // 공통 거래 골격(합성으로 구현한 템플릿 메소드). 변하지 않는 뼈대(①PIN검증 ②금액검증 ③전략실행
    // ④영속화)는 여기 고정하고, 달라지는 단계(전략·영속 대상·기록)만 인자로 위임받는다.
    //   · 세 거래의 검증 로직 중복을 제거하고, ④ 영속화를 runInTransaction으로 감싸 원자성을 보장한다.
    private TransactionResult settle(Account source, String pin, long amount,
                                     Transaction strategy,
                                     List<Account> affected,
                                     TransactionRecord record) {
        if (!verifyAccountPassword(source, pin)) {       // ① 공통 — PIN 검증 게이트
            return TransactionResult.WRONG_PIN;
        }
        if (amount <= 0) {                               // ② 공통 — 금액 검증
            return TransactionResult.INVALID_AMOUNT;
        }
        if (!source.performTransaction(strategy)) {      // ③ 가변 — 도메인 전략 실행
            return TransactionResult.INSUFFICIENT_FUNDS;
        }
        repo.runInTransaction(() -> {                    // ④ 성공 시에만 — 원자적 영속화
            for (Account a : affected) {
                repo.updateBalance(a);
            }
            repo.insertHistory(record);
        });
        return TransactionResult.SUCCESS;
    }

    public TransactionResult deposit(Account account, String pin, long amount) {
        return settle(account, pin, amount,
                new DepositTransaction(amount),
                List.of(account),
                new TransactionRecord(account.getAccountNumber(), "DEPOSIT", amount, null));
    }

    public TransactionResult withdraw(Account account, String pin, long amount) {
        return settle(account, pin, amount,
                new WithdrawTransaction(amount),
                List.of(account),
                new TransactionRecord(account.getAccountNumber(), "WITHDRAW", amount, null));
    }

    // 대상을 계좌번호로 받는 송금 — 대상 조회 후 객체 버전에 위임
    public TransactionResult transfer(Account source, String pin, String targetNo, long amount) {
        if (targetNo.equals(source.getAccountNumber())) {   // P2 불변식 — 자기송금 금지(서비스에서 강제)
            return TransactionResult.SELF_TRANSFER;
        }
        Account target = repo.findAccount(targetNo);
        if (target == null) {
            return TransactionResult.ACCOUNT_NOT_FOUND;
        }
        return transfer(source, pin, target, amount);
    }

    // 메서드 오버로딩 — 호출부가 이미 대상 Account를 들고 있을 때 조회를 생략
    public TransactionResult transfer(Account source, String pin, Account target, long amount) {
        if (target.getAccountNumber().equals(source.getAccountNumber())) {  // P2 불변식 — 자기송금 금지
            return TransactionResult.SELF_TRANSFER;
        }
        return settle(source, pin, amount,
                new TransferTransaction(amount, target),
                List.of(source, target),                    // 출금·입금 계좌를 한 트랜잭션에서 함께 갱신
                new TransactionRecord(source.getAccountNumber(), "TRANSFER", amount,
                        target.getAccountNumber()));
    }

    // 시연용 가상 송금 대상 계좌(200-5678)를 DB에 1회 시드. system 고객을 소유자로 사용.
    public Account ensureTargetAccount() {
        String targetNumber = "200-5678";
        if (!repo.existsCustomer("system")) {
            repo.insertCustomer(new Customer("system", "system", "시스템", "000000-0000000"));
        }
        if (!repo.existsAccount(targetNumber)) {
            Account target = new Account(targetNumber, 50000, "5678");
            repo.insertAccount(target, "system");
            return target;
        }
        return repo.findAccount(targetNumber);
    }
}
