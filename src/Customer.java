// 고객 클래스

import java.util.ArrayList;
import java.util.List;

class Customer {

    private String customerId;
    private String password;
    private String name;
    private String ssn;

    // 고객은 여러 계좌를 가질 수 있음
    private ArrayList<Account> accounts;

    public Customer(String customerId,
                    String password,
                    String name,
                    String ssn) {

        this.customerId = customerId;
        this.password = password;
        this.name = name;
        this.ssn = ssn;

        accounts = new ArrayList<>();
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

    // 계좌 보유
    public void addAccount(Account account) {
        accounts.add(account);
    }

    public ArrayList<Account> getAccounts() {
        return accounts;
    }

    // UML : 계좌개설(은행) — 시퀀스: 은행이 계좌 정보를 반환
    public Account openAccount(Bank bank, double initialBalance, String accountPassword) {
        return bank.createAccount(this, initialBalance, accountPassword);
    }
}
