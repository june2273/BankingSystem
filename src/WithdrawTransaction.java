// 출금 거래 — 순수 도메인 연산. 잔액 부족 시 false (DB 기록·출력은 Bank가 담당)
class WithdrawTransaction implements Transaction {

    private double amount;

    public WithdrawTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(Account account) {
        return account.withdraw(amount); // 잔액 충분 시 true
    }
}
