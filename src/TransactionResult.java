// 거래 결과 — 거래 한 건의 처리 결과를 한 값으로 표현 (출력은 표현 계층 Main이 담당)
public enum TransactionResult {
    SUCCESS,             // 거래 성공
    WRONG_PIN,           // 계좌 비밀번호 불일치 (PIN 검증 게이트 실패)
    INSUFFICIENT_FUNDS,  // 잔액 부족 (출금/송금)
    INVALID_AMOUNT,      // 금액이 0 이하 등 유효하지 않음
    ACCOUNT_NOT_FOUND    // 송금 대상 계좌가 존재하지 않음
}
