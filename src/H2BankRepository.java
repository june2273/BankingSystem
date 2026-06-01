import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// BankRepository의 H2 구현체. 모든 JDBC를 한곳에 격리한다(고객·계좌·거래 3테이블 통합).
// 도메인/서비스는 BankRepository(인터페이스)에만 의존하고 이 클래스의 존재를 알지 못함(DIP).
class H2BankRepository implements BankRepository {

    // ===== customers =====

    @Override
    public void insertCustomer(Customer customer) {
        String sql = "INSERT INTO customers (customer_id, password, name, ssn) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerId());
            ps.setString(2, customer.getPassword());
            ps.setString(3, customer.getName());
            ps.setString(4, customer.getSsn());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 고객 등록 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsCustomer(String customerId) {
        String sql = "SELECT 1 FROM customers WHERE customer_id = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 고객 조회 실패: " + e.getMessage(), e);
        }
    }

    // 주민등록번호 중복 검사 — 1인 1가입(동일 주민번호 재가입) 차단용
    @Override
    public boolean existsBySsn(String ssn) {
        String sql = "SELECT 1 FROM customers WHERE ssn = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, ssn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 주민번호 조회 실패: " + e.getMessage(), e);
        }
    }

    // 인증용 — 아이디/비밀번호가 일치하면 Customer 반환, 아니면 null
    @Override
    public Customer findByCredentials(String customerId, String password) {
        String sql = "SELECT customer_id, password, name, ssn FROM customers "
                + "WHERE customer_id = ? AND password = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getString("customer_id"),
                            rs.getString("password"),
                            rs.getString("name"),
                            rs.getString("ssn"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 인증 조회 실패: " + e.getMessage(), e);
        }
    }

    // ===== accounts =====

    @Override
    public void insertAccount(Account account, String customerId) {
        String sql = "INSERT INTO accounts (account_number, customer_id, balance, account_password) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, customerId);
            ps.setLong(3, account.getBalance());
            ps.setString(4, account.getAccountPassword());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 등록 실패: " + e.getMessage(), e);
        }
    }

    // 잔액 변경(입금/출금/송금) 후 DB 동기화
    @Override
    public void updateBalance(Account account) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, account.getBalance());
            ps.setString(2, account.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 잔액 갱신 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsAccount(String accountNumber) {
        String sql = "SELECT 1 FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 조회 실패: " + e.getMessage(), e);
        }
    }

    // 해당 고객이 이미 계좌를 보유했는지 — 중복 개설 차단용 (DB가 진실의 원천)
    @Override
    public boolean existsAccountByCustomer(String customerId) {
        String sql = "SELECT 1 FROM accounts WHERE customer_id = ? LIMIT 1";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 보유 조회 실패: " + e.getMessage(), e);
        }
    }

    // 계좌번호 생성용 — 전체 '100-' 계열 계좌 수 (계좌번호 PK는 고객 무관 전역 유일)
    @Override
    public int countMainAccounts() {
        String sql = "SELECT COUNT(*) FROM accounts WHERE account_number LIKE '100-%'";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 수 조회 실패: " + e.getMessage(), e);
        }
    }

    // DB에 저장된 계좌를 Account 객체로 복원 (송금 대상 확인·재실행 복원용)
    @Override
    public Account findAccount(String accountNumber) {
        String sql = "SELECT account_number, balance, account_password "
                + "FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getString("account_number"),
                            rs.getLong("balance"),
                            rs.getString("account_password"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 조회 실패: " + e.getMessage(), e);
        }
    }

    // 고객의 첫 번째(대표) 계좌 — 재실행 후 로그인 시 myAccount 복원용
    @Override
    public Account findFirstAccount(String customerId) {
        String sql = "SELECT account_number, balance, account_password "
                + "FROM accounts WHERE customer_id = ? ORDER BY account_number LIMIT 1";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getString("account_number"),
                            rs.getLong("balance"),
                            rs.getString("account_password"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 조회 실패: " + e.getMessage(), e);
        }
    }

    // ===== transactions =====

    // 거래 1건 기록. 입·출금은 targetAccountNo=null, 송금은 대상 계좌번호.
    @Override
    public void insertHistory(TransactionRecord record) {
        String sql = "INSERT INTO transactions (account_number, type, amount, target_account) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, record.getAccountNo());
            ps.setString(2, record.getType());
            ps.setLong(3, record.getAmount());
            if (record.getTargetAccountNo() == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, record.getTargetAccountNo());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 거래내역 기록 실패: " + e.getMessage(), e);
        }
    }

    // 특정 계좌의 최근 거래내역을 limit건만큼 최신순으로 조회 (라벨은 TransactionRecord가 담당)
    @Override
    public List<TransactionRecord> findRecentHistory(String accountNumber, int limit) {
        String sql = "SELECT type, amount, target_account, "
                + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at "
                + "FROM transactions WHERE account_number = ? "
                + "ORDER BY id DESC LIMIT ?";
        List<TransactionRecord> history = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new TransactionRecord(
                            accountNumber,
                            rs.getString("type"),
                            rs.getLong("amount"),
                            rs.getString("target_account"),
                            rs.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 거래내역 조회 실패: " + e.getMessage(), e);
        }
        return history;
    }

    // ===== 트랜잭션 경계 =====

    // work 안의 여러 영속 작업을 하나의 원자적 단위로 실행한다.
    //   · 전체가 공유하는 단일 커넥션에 auto-commit을 끄고 work 수행 → 모두 성공해야 commit.
    //   · 도중 실패(RuntimeException) 시 rollback하여 부분 반영(돈 증발)을 막는다.
    @Override
    public void runInTransaction(Runnable work) {
        Connection c = Database.getConnection();
        try {
            c.setAutoCommit(false);
            work.run();          // 도중 실패 시 RuntimeException(영속 메서드가 감싸 던짐)
            c.commit();
        } catch (Exception e) {  // SQLException(set/commit) + RuntimeException(work) 모두 처리
            try {
                c.rollback();
            } catch (SQLException ignore) {
                // rollback 실패는 원래 예외를 가리지 않도록 무시
            }
            throw new RuntimeException(">> [DB 오류] 거래 처리 실패 — 롤백되었습니다: " + e.getMessage(), e);
        } finally {
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignore) {
                // auto-commit 복구 실패는 치명적이지 않으므로 무시
            }
        }
    }
}
