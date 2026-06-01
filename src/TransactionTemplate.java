import java.util.List;

// 거래 공통 골격 — 진짜 Template Method 패턴(GoF).
//   · run()을 final로 고정해 ①PIN검증 ②금액검증 ③도메인연산 ④원자영속화의 "순서"를
//     어떤 하위 클래스도 바꾸거나 건너뛸 수 없게 한다(템플릿 메서드).
//   · 거래 종류마다 달라지는 부분만 추상 원시연산(primitive operation)으로 하위에 위임한다.
//
// SOLID 유지 메모:
//   · SRP — ③ apply()는 순수 도메인 연산(repo를 모름). 영속화 "구조"(④)만 이 골격이 소유.
//   · DIP — 구체 H2가 아니라 BankRepository(추상)에만 의존. runInTransaction으로 원자성 위임.
//   · OCP — 새 거래는 하위 클래스 추가만으로 확장(이 골격은 수정 불필요).
abstract class TransactionTemplate {

    protected final BankRepository repo;

    TransactionTemplate(BankRepository repo) {
        this.repo = repo;
    }

    // 템플릿 메서드 — 골격 고정(final). 하위는 원시연산만 채운다.
    public final TransactionResult run(Account source, String pin, long amount) {
        if (!source.authenticateAccount(source.getAccountNumber(), pin)) {  // ① 공통 — PIN 게이트
            return TransactionResult.WRONG_PIN;
        }
        if (amount <= 0) {                                                  // ② 공통 — 금액 검증
            return TransactionResult.INVALID_AMOUNT;
        }
        if (!apply(source, amount)) {                                       // ③ 가변 — 순수 도메인 연산
            return TransactionResult.INSUFFICIENT_FUNDS;
        }
        repo.runInTransaction(() -> {                                       // ④ 가변 대상 — 원자적 영속화
            for (Account a : affectedAccounts(source)) {
                repo.updateBalance(a);
            }
            repo.insertHistory(record(source, amount));
        });
        return TransactionResult.SUCCESS;
    }

    // ===== 원시 연산(primitive operations) — 하위 클래스가 구현 =====

    // 잔액 변경(순수 도메인). 성공 여부 반환(출금/송금은 잔액 부족 시 false).
    protected abstract boolean apply(Account source, long amount);

    // ④에서 DB에 반영할 계좌들(입·출금은 source 1개, 송금은 source+target 2개).
    protected abstract List<Account> affectedAccounts(Account source);

    // ④에서 남길 거래 기록 1건.
    protected abstract TransactionRecord record(Account source, long amount);
}
