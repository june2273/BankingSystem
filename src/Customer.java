// 고객 클래스 — 순수 도메인(식별 정보 보유). 계좌 목록은 DB가 진실의 원천이므로 메모리에 두지 않음.
class Customer {

    private String customerId;
    private String password;
    private String name;
    private String ssn;

    public Customer(String customerId,
                    String password,
                    String name,
                    String ssn) {

        this.customerId = customerId;
        this.password = password;
        this.name = name;
        this.ssn = ssn;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getSsn() {
        return ssn;
    }

    // UML : 계좌개설(은행) — 은행에 위임하여 생성된 계좌를 반환
    public Account openAccount(Bank bank, long initialBalance, String accountPassword) {
        return bank.openAccount(this, initialBalance, accountPassword);
    }
}
