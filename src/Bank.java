import java.util.ArrayList;

// 은행 클래스
class Bank {

    private ArrayList<Customer> customers;

    public Bank() {
        customers = new ArrayList<>();
    }

    // UML : 사용자인증
    public boolean authenticateUser(String customerId, String password) {
        for (Customer customer : customers) {
            if (customer.getCustomerId().equals(customerId)
                    && customer.getPassword().equals(password)) {
                System.out.println(">> [인증 결과] 로그인 성공! [" + customer.getName() + "]님 환영합니다.");
                return true;
            }
        }
        System.out.println(">> [인증 결과] 로그인 실패 : 아이디 또는 비밀번호가 틀렸습니다.");
        return false;
    }

    // UML : 고객등록
    public void registerCustomer(Customer customer) {
        if (!customers.contains(customer)) {
            customers.add(customer);
        }
        System.out.println("   (System) -> 은행 전산망에 고객 정보가 등록되었습니다.");
    }

    // UML : 고객조회
    public void findCustomer(Customer customer) {
        System.out.println("   (System) -> 정상 등록된 고객 정보 조회를 완료했습니다.");
    }

    // UML : 계좌생성
    public Account createAccount(Customer customer, double initialBalance, String accountPassword) {
        registerCustomer(customer);
        findCustomer(customer);

        String accountNumber = "100-" + (1000 + customer.getAccounts().size());
        Account account = new Account(accountNumber, initialBalance, accountPassword);
        customer.addAccount(account);

        System.out.println(">> [전산 결과] 신규 계좌가 생성되었습니다. [계좌번호: " + accountNumber + "]");
        return account;
    }

    // UML : 계좌조회
    public void viewAccount(Account account) {
        account.printInfo();
    }
}
