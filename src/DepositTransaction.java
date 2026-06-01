import java.util.List;

// 입금 거래 — TransactionTemplate의 원시연산만 구현(골격은 상위가 final로 고정).
class DepositTransaction extends TransactionTemplate {

    DepositTransaction(BankRepository repo) {
        super(repo);
    }

    @Override
    protected boolean apply(Account source, long amount) {
        source.deposit(amount);
        return true; // 입금은 항상 성공
    }

    @Override
    protected List<Account> affectedAccounts(Account source) {
        return List.of(source);
    }

    @Override
    protected TransactionRecord record(Account source, long amount) {
        return new TransactionRecord(source.getAccountNumber(), "DEPOSIT", amount, null);
    }
}
