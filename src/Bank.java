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
    public Account openAccount(Customer customer, double initialBalance, String accountPassword) {
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

    public TransactionResult deposit(Account account, String pin, double amount) {
        if (!verifyAccountPassword(account, pin)) {
            return TransactionResult.WRONG_PIN;
        }
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        account.performTransaction(new DepositTransaction(amount));
        repo.updateBalance(account);
        repo.insertHistory(new TransactionRecord(account.getAccountNumber(), "DEPOSIT", amount, null));
        return TransactionResult.SUCCESS;
    }

    public TransactionResult withdraw(Account account, String pin, double amount) {
        if (!verifyAccountPassword(account, pin)) {
            return TransactionResult.WRONG_PIN;
        }
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        if (!account.performTransaction(new WithdrawTransaction(amount))) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }
        repo.updateBalance(account);
        repo.insertHistory(new TransactionRecord(account.getAccountNumber(), "WITHDRAW", amount, null));
        return TransactionResult.SUCCESS;
    }

    public TransactionResult transfer(Account source, String pin, String targetNo, double amount) {
        if (!verifyAccountPassword(source, pin)) {
            return TransactionResult.WRONG_PIN;
        }
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }
        Account target = repo.findAccount(targetNo);
        if (target == null) {
            return TransactionResult.ACCOUNT_NOT_FOUND;
        }
        if (!source.performTransaction(new TransferTransaction(amount, target))) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }
        repo.updateBalance(source);
        repo.updateBalance(target);
        repo.insertHistory(new TransactionRecord(source.getAccountNumber(), "TRANSFER", amount, targetNo));
        return TransactionResult.SUCCESS;
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
