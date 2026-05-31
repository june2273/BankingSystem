// 계좌 클래스
class Account {

    private String accountNumber;
    private double balance;
    private String accountPassword;

    // 잔액 변경을 DB에 동기화하기 위한 DAO (모든 계좌가 공유)
    private static final AccountDao accountDao = new AccountDao();

    public Account(String accountNumber,
                   double initialBalance,
                   String accountPassword) {

        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.accountPassword = accountPassword;
    }

    // UML : 계좌인증
    public boolean authenticateAccount(
            String accountNumber,
            String accountPassword) {

        return this.accountNumber.equals(accountNumber)
                && this.accountPassword.equals(accountPassword);
    }

    // UML : 거래수행
    public void performTransaction(
            Transaction transaction) {

        transaction.execute(this);
    }

    // UML : 입금
    void deposit(double amount) {

        balance += amount;

        accountDao.updateBalance(this); // DB 잔액 동기화

        System.out.println(
                amount + "원 입금 완료");
    }

    // UML : 출금 — 성공 시 true, 잔액 부족 시 false
    boolean withdraw(double amount) {

        if (balance >= amount) {

            balance -= amount;

            accountDao.updateBalance(this); // DB 잔액 동기화

            System.out.println(
                    amount + "원 출금 완료");
            return true;
        }
        else {
            System.out.println(">> [오류] 잔액 부족 (현재 잔액 : " + balance + "원)");
            return false;
        }
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

    public void printInfo() {

        System.out.println(
                "계좌번호 : " + accountNumber);

        System.out.println(
                "잔액 : " + balance);
    }
}