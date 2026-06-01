import java.util.List;

// 송금 거래 — 대상 계좌는 거래마다 달라지므로 생성자로 주입(고정 run() 시그니처로는 못 받음).
//   · apply(): 출금 성공 시에만 대상에 입금 → 두 계좌가 함께 바뀐다.
//   · affectedAccounts(): source+target 둘 다 한 트랜잭션에서 갱신(④에서 원자적으로).
class TransferTransaction extends TransactionTemplate {

    private final Account target;

    TransferTransaction(BankRepository repo, Account target) {
        super(repo);
        this.target = target;
    }

    @Override
    protected boolean apply(Account source, long amount) {
        if (source.withdraw(amount)) { // 출금이 성공해야 송금 진행
            target.deposit(amount);
            return true;
        }
        return false;
    }

    @Override
    protected List<Account> affectedAccounts(Account source) {
        return List.of(source, target);
    }

    @Override
    protected TransactionRecord record(Account source, long amount) {
        return new TransactionRecord(source.getAccountNumber(), "TRANSFER", amount,
                target.getAccountNumber());
    }
}
