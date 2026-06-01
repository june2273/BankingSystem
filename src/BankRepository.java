import java.util.List;

// 영속 계층 추상화(DIP). Bank(서비스)는 이 인터페이스에만 의존하고 구체 DB 기술(H2)은 알지 못한다.
// 구현체(H2BankRepository)를 갈아끼우면 테스트용 InMemory 저장소로도 교체 가능.
interface BankRepository {

    // ===== customers =====
    void insertCustomer(Customer customer);
    boolean existsCustomer(String customerId);
    boolean existsBySsn(String ssn);
    Customer findByCredentials(String customerId, String password);

    // ===== accounts =====
    void insertAccount(Account account, String customerId);
    void updateBalance(Account account);
    boolean existsAccount(String accountNumber);
    boolean existsAccountByCustomer(String customerId);
    int countMainAccounts();
    Account findAccount(String accountNumber);
    Account findFirstAccount(String customerId);

    // ===== transactions =====
    void insertHistory(TransactionRecord record);
    List<TransactionRecord> findRecentHistory(String accountNumber, int limit);

    // 여러 영속 작업을 하나의 원자적 트랜잭션으로 실행 (송금 원자성의 토대 — JDBC 세부는 구현체에 격리).
    void runInTransaction(Runnable work);
}
