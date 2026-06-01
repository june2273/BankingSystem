import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// 고객(customers) 테이블 데이터 접근
class CustomerDao {

    // 고객 등록 (PK 충돌 = 중복 가입은 호출 측에서 exists()로 사전 차단)
    void insert(Customer customer) {
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

    boolean exists(String customerId) {
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
    boolean existsBySsn(String ssn) {
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
    Customer findByCredentials(String customerId, String password) {
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
}
