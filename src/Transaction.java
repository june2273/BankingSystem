// 거래 전략 계약 — 순수 도메인 연산. 성공 여부만 반환(영속화·출력은 서비스/표현 계층 담당).
public interface Transaction {
    boolean execute(Account account);
}
