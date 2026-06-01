// 출금 거래 — 순수 도메인 연산. 잔액 부족 시 false (DB 기록·출력은 Bank가 담당)
//   · Transaction(실행) + Describable(설명)을 동시에 구현 — 다중 인터페이스 구현(자바식 다중 상속).
class WithdrawTransaction implements Transaction, Describable {

    private long amount;

    public WithdrawTransaction(long amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(Account account) {
        return account.withdraw(amount); // 잔액 충분 시 true
    }

    @Override
    public String describe() {
        return "출금 " + amount + "원";
    }
}
