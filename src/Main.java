import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Bank bank = new Bank();

        Customer loginCustomer = null;
        Account myAccount = null;

        // 시연용 가상 송금 대상 계좌 (계좌번호: 200-5678, 초기잔액: 50000원)
        Account targetAccount = new Account("200-5678", 50000, "5678");

        boolean isRunning = true;

        while (isRunning) {
            System.out.println("==================================================");
            System.out.println("  1. 가입 및 사용자 로그인 인증");
            System.out.println("  2. 신규 계좌 개설");
            System.out.println("  3. 금융 계좌 비밀번호 검증");
            System.out.println("  4. 입금 거래 수행");
            System.out.println("  5. 출금 거래 수행");
            System.out.println("  6. 타인에게 송금 수행");
            System.out.println("  7. 전산 계좌 정보 최종 조회");
            System.out.println("  8. 프로그램 시연 종료");
            System.out.println("==================================================");
            System.out.print(">> 원하시는 항목 번호를 선택하세요: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    // 1번을 누르면 내부에서 또 다른 서브 메뉴판 출력
                    System.out.println("\n==================================================");
                    System.out.println("  1. 로그인");
                    System.out.println("  2. 회원가입");
                    System.out.println("==================================================");
                    System.out.print(">> 번호를 선택하세요: ");
                    int subChoice = scanner.nextInt();
                    scanner.nextLine(); // 버퍼 비우기

                    // 🌟 퀴즈 및 가독성을 위해 서브 메뉴도 switch-case문으로 전환했습니다.
                    switch (subChoice) {
                        case 1: // 1. 로그인 선택 시
                            System.out.println("\n[1-1] 사용자 로그인 인증");
                            System.out.print(" -> 인증할 아이디 입력: ");
                            String inputId = scanner.nextLine();
                            System.out.print(" -> 인증할 비밀번호 입력: ");
                            String inputPwd = scanner.nextLine();

                            // 은행 내부 검증 시스템 작동 (authenticateUser)
                            boolean success = bank.authenticateUser(inputId, inputPwd);

                            if (success) {
                                // 회원가입할 때 참조해둔 유저 인스턴스가 있다면 로그인 세션으로 연결
                                if (loginCustomer != null && loginCustomer.getCustomerId().equals(inputId)) {
                                    System.out.println(">> 유저 세션 연결 완료. 계좌 개설 및 거래가 가능합니다.");
                                }
                            } else {
                                System.out.println(">> [안내] 메인 메뉴로 돌아갑니다.");
                            }
                            break;

                        case 2: // 2. 회원가입 선택 시
                            System.out.println("\n[1-2] 신규 회원 가입 절차");
                            System.out.print(" -> 신규 등록할 성함 입력: ");
                            String name = scanner.nextLine();
                            System.out.print(" -> 주민등록번호 입력: ");
                            String ssn = scanner.nextLine();
                            System.out.print(" -> 생성할 사용자 ID 입력: ");
                            String id = scanner.nextLine();
                            System.out.print(" -> 생성할 사용자 비밀번호 입력: ");
                            String pwd = scanner.nextLine();

                            // 가입 절차: 고객 객체를 생성하고 은행 리스트에 등록 (UML: registerCustomer)
                            loginCustomer = new Customer(id, pwd, name, ssn);
                            bank.registerCustomer(loginCustomer);
                            System.out.println(">> 회원가입이 완료되었습니다. 이제 1번 로그인을 진행해 주세요.");
                            break;

                        default: // 1, 2 외의 잘못된 번호 입력 시
                            System.out.println("잘못된 선택입니다. 서브 메뉴를 종료하고 메인으로 돌아갑니다.");
                            break;
                    }
                    break; // 메인 switch문의 case 1을 탈출


                case 2:
                    System.out.println("\n[2] 신규 계좌 개설 세션");
                    if (loginCustomer == null) {
                        System.out.println("오류: 1번 항목을 통해 가입 및 로그인을 먼저 완료해주세요.");
                        break;
                    }
                    System.out.print(" -> 최초로 예치할 금액 입력(원): ");
                    double initialBalance = scanner.nextDouble();
                    scanner.nextLine();
                    System.out.print(" -> 설정할 계좌 비밀번호 4자리 입력: ");
                    String accountPassword = scanner.nextLine();

                    // 시퀀스 다이어그램 흐름: 고객 -> 은행 -> 계좌 생성 -> 고객에게 반환
                    myAccount = loginCustomer.openAccount(bank, initialBalance, accountPassword);
                    break;

                case 3:
                    System.out.println("\n[3] 금융 계좌 비밀번호 검증 세션");
                    if (myAccount == null) {
                        System.out.println("오류: 2번 항목을 수행하여 계좌를 먼저 생성해야 합니다.");
                        break;
                    }
                    System.out.print(" -> 본인의 계좌번호 확인 입력: ");
                    String checkNum = scanner.nextLine();
                    System.out.print(" -> 계좌 비밀번호 입력: ");
                    String checkPwd = scanner.nextLine();

                    if (myAccount.authenticateAccount(checkNum, checkPwd)) {
                        System.out.println(">> [인증 결과] 계좌 비밀번호 일치 확인.");
                    } else {
                        System.out.println(">> [인증 결과] 계좌 비밀번호 불일치! 접근이 거부되었습니다.");
                    }
                    break;

                case 4:
                    System.out.println("\n[4] 입금 거래 수행");
                    if (myAccount == null) {
                        System.out.println("오류: 계좌가 없습니다.");
                        break;
                    }
                    System.out.print(" -> 입금할 금액 입력(원): ");
                    double depAmount = scanner.nextDouble();

                    myAccount.performTransaction(new DepositTransaction(depAmount));
                    System.out.println("   현재 잔액 : " + myAccount.getBalance() + "원");
                    break;

                case 5:
                    System.out.println("\n[5] 출금 거래 수행");
                    if (myAccount == null) {
                        System.out.println("오류: 계좌가 없습니다.");
                        break;
                    }
                    System.out.print(" -> 출금할 금액 입력(원): ");
                    double withAmount = scanner.nextDouble();

                    myAccount.performTransaction(new WithdrawalTransaction(withAmount));
                    System.out.println("   현재 잔액 : " + myAccount.getBalance() + "원");
                    break;

                case 6:
                    System.out.println("\n[6] 상대방에게 송금 수행");
                    if (myAccount == null) {
                        System.out.println("오류: 계좌가 없습니다.");
                        break;
                    }
                    System.out.println(" -> [안내] 가상의 송금 대상 정보가 자동 세팅됩니다. (계좌번호: 200-5678)");
                    System.out.print(" -> 송금(이체)할 금액 입력(원): ");
                    double transAmount = scanner.nextDouble();

                    myAccount.performTransaction(new TransferTransaction(transAmount, targetAccount));
                    System.out.println("   현재 잔액 : " + myAccount.getBalance() + "원");
                    break;

                case 7:
                    System.out.println("\n[7] 전산 계좌 정보 최종 조회");
                    if (myAccount == null) {
                        System.out.println("오류: 조회할 계좌 정보가 전산에 등록되어 있지 않습니다.");
                        break;
                    }
                    System.out.println("[" + loginCustomer.getName() + " 고객님의 현재 전산 계좌 명세]");
                    bank.viewAccount(myAccount);

                    System.out.println("\n[송금 상대방 계좌 명세]");
                    targetAccount.printInfo();
                    break;

                case 8:
                    System.out.println("\n>> 은행 전산 프로그램 시연을 종료합니다. 발표를 마치겠습니다.");
                    isRunning = false;
                    break;

                default:
                    System.out.println("잘못된 번호입니다. 1번부터 8번 사이의 정수를 입력하세요.");
                    break;
            }
        }
        scanner.close();
    }
}
