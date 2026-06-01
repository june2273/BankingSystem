// 송금 거래 — 순수 도메인 연산: 출금 성공 시 대상 계좌로 입금. (DB 반영/출력은 Bank가 담당)
//   · Transaction(실행) + Describable(설명)을 동시에 구현 — 다중 인터페이스 구현(자바식 다중 상속).
class TransferTransaction implements Transaction, Describable {

    private long amount;
    private Account targetAccount;

    public TransferTransaction(long amount, Account targetAccount) {
        this.amount = amount;
        this.targetAccount = targetAccount;
    }

    @Override
    public boolean execute(Account account) {
        // 출금이 성공해야 송금 진행 (잔액 부족 시 false)
        if (account.withdraw(amount)) {
            targetAccount.deposit(amount);
            return true;
        }
        return false;
    }

    @Override
    public String describe() {
        return "송금 " + amount + "원 → " + targetAccount.getAccountNumber();
    }
}
