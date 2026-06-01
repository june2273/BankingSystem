import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// 계좌(accounts) 테이블 데이터 접근
class AccountDao {

    void insert(Account account, String customerId) {
        String sql = "INSERT INTO accounts (account_number, customer_id, balance, account_password) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, customerId);
            ps.setDouble(3, account.getBalance());
            ps.setString(4, account.getAccountPassword());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 등록 실패: " + e.getMessage(), e);
        }
    }

    // 잔액 변경(입금/출금/송금) 후 DB 동기화
    void updateBalance(Account account) {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, account.getBalance());
            ps.setString(2, account.getAccountNumber());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 잔액 갱신 실패: " + e.getMessage(), e);
        }
    }

    boolean exists(String accountNumber) {
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
    boolean existsByCustomer(String customerId) {
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
    int countMainAccounts() {
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

    // DB에 저장된 계좌를 Account 객체로 복원 (재실행 후 로그인 시 사용)
    Account findByNumber(String accountNumber) {
        String sql = "SELECT account_number, balance, account_password "
                + "FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getString("account_number"),
                            rs.getDouble("balance"),
                            rs.getString("account_password"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 조회 실패: " + e.getMessage(), e);
        }
    }

    // 고객의 첫 번째(대표) 계좌 — 재실행 후 로그인 시 myAccount 복원용
    Account findFirstByCustomer(String customerId) {
        String sql = "SELECT account_number, balance, account_password "
                + "FROM accounts WHERE customer_id = ? ORDER BY account_number LIMIT 1";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getString("account_number"),
                            rs.getDouble("balance"),
                            rs.getString("account_password"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 계좌 조회 실패: " + e.getMessage(), e);
        }
    }
}
