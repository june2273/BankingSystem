// 입금 거래
class DepositTransaction
        implements Transaction {

    private double amount;

    public DepositTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Account account) {

        account.deposit(amount);
    }
}