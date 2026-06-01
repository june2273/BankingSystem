import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// 거래내역(transactions) 테이블 데이터 접근
class TransactionDao {

    // 거래 1건 기록. 입·출금은 targetAccount=null, 송금은 대상 계좌번호.
    void insert(String accountNumber, String type, double amount, String targetAccount) {
        String sql = "INSERT INTO transactions (account_number, type, amount, target_account) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            if (targetAccount == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, targetAccount);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 거래내역 기록 실패: " + e.getMessage(), e);
        }
    }

    // 특정 계좌의 최근 거래내역을 limit건만큼 최신순으로 조회 (계좌 정보 조회용)
    List<String> findRecentByAccount(String accountNumber, int limit) {
        String sql = "SELECT type, amount, target_account, "
                + "FORMATDATETIME(created_at, 'yyyy-MM-dd HH:mm:ss') AS created_at "
                + "FROM transactions WHERE account_number = ? "
                + "ORDER BY id DESC LIMIT ?";
        List<String> history = new ArrayList<>();
        try (PreparedStatement ps = Database.getConnection().prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    double amount = rs.getDouble("amount");
                    String target = rs.getString("target_account");
                    String when = rs.getString("created_at");

                    String label = switch (type) {
                        case "DEPOSIT"  -> "입금";
                        case "WITHDRAW" -> "출금";
                        case "TRANSFER" -> "송금";
                        default          -> type;
                    };

                    String line = "[" + when + "] " + label + " " + amount + "원";
                    if (target != null) {
                        line += " → " + target;
                    }
                    history.add(line);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 거래내역 조회 실패: " + e.getMessage(), e);
        }
        return history;
    }
}
