// 송금 거래
class TransferTransaction
        implements Transaction {

    private double amount;
    private Account targetAccount;
    private static final TransactionDao transactionDao = new TransactionDao();

    public TransferTransaction(
            double amount,
            Account targetAccount) {

        this.amount = amount;
        this.targetAccount = targetAccount;
    }

    @Override
    public void execute(Account account) {

        // 출금이 성공해야 송금 진행 (잔액 부족 시 withdraw가 false 반환 + 메시지 출력)
        if (account.withdraw(amount)) {

            targetAccount.deposit(amount);

            // 거래내역 기록 (대상 계좌 포함)
            transactionDao.insert(
                    account.getAccountNumber(),
                    "TRANSFER",
                    amount,
                    targetAccount.getAccountNumber());

            System.out.println("송금 완료");
        }
    }
}
