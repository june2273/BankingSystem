// 출금 거래 (UML 명세)
class WithdrawalTransaction implements Transaction {

    private double amount;

    public WithdrawalTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Account account) {
        account.withdraw(amount);
    }
}
