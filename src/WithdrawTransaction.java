// 출금 거래 (UML 명세)
class WithdrawalTransaction implements Transaction {

    private double amount;
    private static final TransactionDao transactionDao = new TransactionDao();

    public WithdrawalTransaction(double amount) {
        this.amount = amount;
    }

    @Override
    public void execute(Account account) {
        // 출금 성공 시에만 거래내역 기록 (잔액 부족이면 withdraw가 false 반환)
        if (account.withdraw(amount)) {
            transactionDao.insert(account.getAccountNumber(), "WITHDRAW", amount, null);
        }
    }
}
