// 입금 거래 — 순수 도메인 연산 (DB 기록·출력은 Bank가 담당)
//   · Transaction(실행) + Describable(설명)을 동시에 구현 — 다중 인터페이스 구현(자바식 다중 상속).
class DepositTransaction implements Transaction, Describable {

    private long amount;

    public DepositTransaction(long amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(Account account) {
        account.deposit(amount);
        return true; // 입금은 항상 성공
    }

    @Override
    public String describe() {
        return "입금 " + amount + "원";
    }
}
