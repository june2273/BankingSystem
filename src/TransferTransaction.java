// 송금 거래
class TransferTransaction
        implements Transaction {

    private double amount;
    private Account targetAccount;

    public TransferTransaction(
            double amount,
            Account targetAccount) {

        this.amount = amount;
        this.targetAccount = targetAccount;
    }

    @Override
    public void execute(Account account) {

        if (account.getBalance() >= amount) {

            account.withdraw(amount);

            targetAccount.deposit(amount);

            System.out.println("송금 완료");
        }
        else {

            System.out.println(
                    ">> [오류] 송금 실패 : 잔액 부족 (현재 잔액 : " + account.getBalance() + "원)");
        }
    }
}