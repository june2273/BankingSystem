// 계좌 클래스 — 순수 도메인. DB 영속화/콘솔 출력은 가지지 않는다(SRP).
//   · 잔액 변경은 상태만 바꾸고, DB 반영은 서비스(Bank)가 BankRepository로 수행.
//   · 입금/출금은 거래 전략을 통해서만 호출(거래수행) — 패키지 전용 가시성.
//   · 금액은 원 단위 정수(long)로 모델링 — 부동소수점 오차 없이 DB(BIGINT)와 타입 일치.
class Account {

    private String accountNumber;
    private long balance;
    private String accountPassword;

    // 생성자 오버로딩 — 잔액 0으로 개설하는 짧은 형태
    public Account(String accountNumber, String accountPassword) {
        this(accountNumber, 0L, accountPassword);
    }

    public Account(String accountNumber,
                   long initialBalance,
                   String accountPassword) {

        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.accountPassword = accountPassword;
    }

    // UML : 계좌인증 — 거래 전 PIN 검증 게이트에서 재사용
    public boolean authenticateAccount(
            String accountNumber,
            String accountPassword) {

        return this.accountNumber.equals(accountNumber)
                && this.accountPassword.equals(accountPassword);
    }

    // UML : 거래수행 — 전략 실행 결과(성공 여부)를 그대로 반환
    public boolean performTransaction(Transaction transaction) {
        return transaction.execute(this);
    }

    // UML : 입금 — 잔액만 증가(순수). DB 반영은 호출 측(Bank)이 수행
    void deposit(long amount) {
        balance += amount;
    }

    // UML : 출금 — 성공 시 true, 잔액 부족 시 false (출력/DB 없음, 순수)
    boolean withdraw(long amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public long getBalance() {
        return balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    // Object.toString() 재정의 — 계좌를 다형적으로 출력(디버깅·로그·조회 일관)
    @Override
    public String toString() {
        return "계좌번호 " + accountNumber + " / 잔액 " + balance + "원";
    }
}
