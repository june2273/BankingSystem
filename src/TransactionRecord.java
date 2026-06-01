// 거래이력 — 거래 한 건을 표현하는 값 객체.
// type→한글 라벨 변환을 여기 한곳에 두어, 표시 로직이 DAO에 흩어지지 않게 함(OCP).
class TransactionRecord {

    private final String accountNo;       // 거래를 수행한 계좌
    private final String type;            // DEPOSIT | WITHDRAW | TRANSFER
    private final double amount;
    private final String targetAccountNo; // 송금 대상 (입·출금은 null)
    private final String createdAt;       // 조회 시 DB에서 채움 (기록 시에는 null)

    // 기록용 — created_at은 DB가 CURRENT_TIMESTAMP로 채움
    TransactionRecord(String accountNo, String type, double amount, String targetAccountNo) {
        this(accountNo, type, amount, targetAccountNo, null);
    }

    // 조회용 — DB에서 읽은 created_at 포함
    TransactionRecord(String accountNo, String type, double amount,
                      String targetAccountNo, String createdAt) {
        this.accountNo = accountNo;
        this.type = type;
        this.amount = amount;
        this.targetAccountNo = targetAccountNo;
        this.createdAt = createdAt;
    }

    String getAccountNo()       { return accountNo; }
    String getType()            { return type; }
    double getAmount()          { return amount; }
    String getTargetAccountNo() { return targetAccountNo; }
    String getCreatedAt()       { return createdAt; }

    // 거래 종류 → 한글 라벨 (표시 로직 일원화)
    String label() {
        return switch (type) {
            case "DEPOSIT"  -> "입금";
            case "WITHDRAW" -> "출금";
            case "TRANSFER" -> "송금";
            default          -> type;
        };
    }

    // 거래내역 한 줄 표현 — "[시각] 라벨 금액원 → 대상"
    String format() {
        String line = "[" + createdAt + "] " + label() + " " + amount + "원";
        if (targetAccountNo != null) {
            line += " → " + targetAccountNo;
        }
        return line;
    }
}
