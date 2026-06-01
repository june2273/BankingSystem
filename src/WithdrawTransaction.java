import java.util.List;

// 출금 거래 — 잔액 부족 시 apply()가 false → 상위 run()이 INSUFFICIENT_FUNDS 반환.
class WithdrawTransaction extends TransactionTemplate {

    WithdrawTransaction(BankRepository repo) {
        super(repo);
    }

    @Override
    protected boolean apply(Account source, long amount) {
        return source.withdraw(amount); // 잔액 충분 시 true
    }

    @Override
    protected List<Account> affectedAccounts(Account source) {
        return List.of(source);
    }

    @Override
    protected TransactionRecord record(Account source, long amount) {
        return new TransactionRecord(source.getAccountNumber(), "WITHDRAW", amount, null);
    }
}
