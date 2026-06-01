// 계좌 클래스 — 순수 도메인. DB 영속화/콘솔 출력은 가지지 않는다(SRP).
//   · 잔액 변경은 상태만 바꾸고, DB 반영은 서비스(Bank)가 BankRepository로 수행.
//   · 입금/출금은 거래 전략을 통해서만 호출(거래수행) — 패키지 전용 가시성.
class Account {

    private String accountNumber;
    private double balance;
    private String accountPassword;

    public Account(String accountNumber,
                   double initialBalance,
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
    void deposit(double amount) {
        balance += amount;
    }

    // UML : 출금 — 성공 시 true, 잔액 부족 시 false (출력/DB 없음, 순수)
    boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public double getBalance() {
        return balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountPassword() {
        return accountPassword;
    }
}
