// 은행 클래스 — 데이터 접근은 DAO에 위임 (H2 DB 영속화)
class Bank {

    private final CustomerDao customerDao = new CustomerDao();
    private final AccountDao accountDao = new AccountDao();

    // UML : 사용자인증 — 성공 시 Customer 반환, 실패 시 null
    public Customer authenticateUser(String customerId, String password) {
        Customer customer = customerDao.findByCredentials(customerId, password);
        if (customer != null) {
            System.out.println(">> [인증 결과] 로그인 성공! [" + customer.getName() + "]님 환영합니다.");
        } else {
            System.out.println(">> [인증 결과] 로그인 실패 : 아이디 또는 비밀번호가 틀렸습니다.");
        }
        return customer;
    }

    // UML : 고객등록 — 이미 존재하면 건너뜀
    public void registerCustomer(Customer customer) {
        if (customerDao.exists(customer.getCustomerId())) {
            System.out.println("   (System) -> 이미 등록된 아이디입니다. 등록을 건너뜁니다.");
            return;
        }
        customerDao.insert(customer);
        System.out.println("   (System) -> 은행 전산망(DB)에 고객 정보가 등록되었습니다.");
    }

    // UML : 고객조회
    public void findCustomer(Customer customer) {
        if (customerDao.exists(customer.getCustomerId())) {
            System.out.println("   (System) -> 정상 등록된 고객 정보 조회를 완료했습니다.");
        } else {
            System.out.println("   (System) -> [경고] 등록되지 않은 고객입니다.");
        }
    }

    // UML : 계좌생성 — 계좌번호는 DB 기준 보유 계좌 수로 생성
    public Account createAccount(Customer customer, double initialBalance, String accountPassword) {
        registerCustomer(customer);
        findCustomer(customer);

        String accountNumber = "100-" + (1000 + accountDao.countMainAccounts());
        Account account = new Account(accountNumber, initialBalance, accountPassword);

        accountDao.insert(account, customer.getCustomerId());
        customer.addAccount(account);

        System.out.println(">> [전산 결과] 신규 계좌가 생성되었습니다. [계좌번호: " + accountNumber + "]");
        return account;
    }

    // UML : 계좌조회
    public void viewAccount(Account account) {
        account.printInfo();
    }

    // 재실행 후 로그인 시 고객의 대표 계좌를 DB에서 복원 (없으면 null)
    public Account loadAccount(Customer customer) {
        return accountDao.findFirstByCustomer(customer.getCustomerId());
    }

    // 계좌번호로 계좌 조회 (송금 대상 확인용) — 없으면 null
    public Account findAccount(String accountNumber) {
        return accountDao.findByNumber(accountNumber);
    }

    // 시연용 송금 대상 계좌(200-5678)를 DB에 1회 시드. system 고객을 소유자로 사용.
    public Account ensureTargetAccount() {
        String targetNumber = "200-5678";

        if (!customerDao.exists("system")) {
            customerDao.insert(new Customer("system", "system", "시스템", "000000-0000000"));
        }
        if (!accountDao.exists(targetNumber)) {
            Account target = new Account(targetNumber, 50000, "5678");
            accountDao.insert(target, "system");
            return target;
        }
        return accountDao.findByNumber(targetNumber);
    }
}
