// 입금 거래 — 순수 도메인 연산 (DB 기록·출력은 Bank가 담당)
class DepositTransaction implements Transaction {

    private double amount;

    public DepositTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(Account account) {
        account.deposit(amount);
        return true; // 입금은 항상 성공
    }
}
