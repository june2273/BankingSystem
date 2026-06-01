import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// H2 임베디드 DB 연결 관리 + 스키마 초기화
class Database {

    // 파일 모드 + AUTO_SERVER : 앱 실행 중에도 H2 Console / IntelliJ에서 동시 접속 가능
    private static final String URL = "jdbc:h2:./data/bank;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    // 프로그램 전체에서 공유하는 단일 연결
    private static Connection connection;

    static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 연결에 실패했습니다: " + e.getMessage(), e);
        }
        return connection;
    }

    // 앱 시작 시 1회 호출 — 테이블이 없으면 생성
    static void init() {
        String createCustomers = """
                CREATE TABLE IF NOT EXISTS customers (
                    customer_id VARCHAR(50)  PRIMARY KEY,
                    password    VARCHAR(100) NOT NULL,
                    name        VARCHAR(50)  NOT NULL,
                    ssn         VARCHAR(20)  NOT NULL
                )
                """;

        String createAccounts = """
                CREATE TABLE IF NOT EXISTS accounts (
                    account_number   VARCHAR(20)   PRIMARY KEY,
                    customer_id      VARCHAR(50)   NOT NULL,
                    balance          DECIMAL(15,2) NOT NULL DEFAULT 0,
                    account_password VARCHAR(20)   NOT NULL,
                    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
                )
                """;

        String createTransactions = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                    account_number VARCHAR(20)   NOT NULL,
                    type           VARCHAR(10)   NOT NULL,
                    amount         DECIMAL(15,2) NOT NULL,
                    target_account VARCHAR(20),
                    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createCustomers);
            stmt.execute(createAccounts);
            stmt.execute(createTransactions);
            System.out.println(">> [DB] 데이터베이스 초기화 완료 (./data/bank.mv.db)");
        } catch (SQLException e) {
            throw new RuntimeException(">> [DB 오류] 스키마 초기화 실패: " + e.getMessage(), e);
        }
    }

    // 종료 시 연결 정리
    static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println(">> [DB 경고] 연결 종료 중 오류: " + e.getMessage());
        }
    }
}
