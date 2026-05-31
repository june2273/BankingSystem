// 입금 거래
class DepositTransaction
        implements Transaction {

    private double amount;
    private static final TransactionDao transactionDao = new TransactionDao();

    public DepositTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Account account) {

        account.deposit(amount);

        // 거래내역 기록 (입금은 대상 계좌 없음)
        transactionDao.insert(account.getAccountNumber(), "DEPOSIT", amount, null);
    }
}
