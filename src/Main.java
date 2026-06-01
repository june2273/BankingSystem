import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // H2 데이터베이스 초기화 (테이블 생성)
        Database.init();

        // 서비스(은행) ← 영속(BankRepository) 주입
        Bank bank = new Bank(new BankRepository());

        Customer loginCustomer = null;
        boolean isLoggedIn = false;
        Account myAccount = null;

        // 시연용 가상 송금 대상 계좌 (계좌번호: 200-5678) — DB에 시드/복원
        bank.ensureTargetAccount();

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

            int choice;
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println(">> [오류] 숫자를 입력해주세요.");
                scanner.nextLine(); // 잘못된 입력 버퍼 비우기
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.println("\n==================================================");
                    System.out.println("  1. 로그인");
                    System.out.println("  2. 회원가입");
                    System.out.println("==================================================");
                    System.out.print(">> 번호를 선택하세요: ");

                    int subChoice;
                    try {
                        subChoice = scanner.nextInt();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">> [오류] 숫자를 입력해주세요.");
                        scanner.nextLine();
                        break;
                    }

                    switch (subChoice) {
                        case 1: // 1. 로그인 선택 시
                            System.out.println("\n[1-1] 사용자 로그인 인증");
                            System.out.print(" -> 인증할 아이디 입력: ");
                            String inputId = scanner.nextLine();
                            System.out.print(" -> 인증할 비밀번호 입력: ");
                            String inputPwd = scanner.nextLine();

                            Customer authed = bank.authenticate(inputId, inputPwd);

                            if (authed != null) {
                                System.out.println(">> [인증 결과] 로그인 성공! [" + authed.getName() + "]님 환영합니다.");
                                loginCustomer = authed;
                                isLoggedIn = true;
                                // 재실행 시 DB에 보관된 계좌를 복원
                                myAccount = bank.loadAccount(authed);
                                if (myAccount != null) {
                                    System.out.println(">> 기존 계좌를 불러왔습니다. [계좌번호: "
                                            + myAccount.getAccountNumber() + ", 잔액: "
                                            + myAccount.getBalance() + "원]");
                                }
                                System.out.println(">> 유저 세션 연결 완료. 계좌 개설 및 거래가 가능합니다.");
                            } else {
                                isLoggedIn = false;
                                System.out.println(">> [인증 결과] 로그인 실패 : 아이디 또는 비밀번호가 틀렸습니다.");
                                System.out.println(">> [안내] 메인 메뉴로 돌아갑니다.");
                            }
                            break;

                        case 2: // 2. 회원가입 선택 시
                            System.out.println("\n[1-2] 신규 회원 가입 절차");
                            System.out.print(" -> 신규 등록할 성함 입력: ");
                            String name = scanner.nextLine();
                            System.out.print(" -> 주민등록번호 입력: ");
                            String ssn = scanner.nextLine();

                            // 동일 주민등록번호가 이미 가입돼 있으면 가입 차단 (1인 1가입)
                            if (bank.isSsnRegistered(ssn)) {
                                System.out.println(">> [오류] 이미 가입된 회원입니다. (동일 주민등록번호 존재)");
                                break;
                            }

                            // 중복되지 않는 아이디를 받을 때까지 반복 입력
                            String id;
                            while (true) {
                                System.out.print(" -> 생성할 사용자 ID 입력: ");
                                id = scanner.nextLine().trim();
                                if (id.isEmpty()) {
                                    System.out.println(">> [오류] 아이디는 비워둘 수 없습니다. 다시 입력해주세요.");
                                    continue;
                                }
                                if (bank.isIdTaken(id)) {
                                    System.out.println(">> [오류] 중복된 ID입니다. 다시 입력해주세요.");
                                    continue;
                                }
                                break;
                            }

                            System.out.print(" -> 생성할 사용자 비밀번호 입력: ");
                            String pwd = scanner.nextLine();

                            bank.register(new Customer(id, pwd, name, ssn));
                            System.out.println(">> 회원가입이 완료되었습니다.");

                            System.out.println("\n==================================================");
                            System.out.println("   가입 승인에 따라 실시간 로그인 인증을 연이어 진행합니다.");
                            System.out.println("==================================================");
                            System.out.print(" -> 로그인할 아이디 입력: ");
                            String autoId = scanner.nextLine();
                            System.out.print(" -> 로그인할 비밀번호 입력: ");
                            String autoPwd = scanner.nextLine();

                            Customer autoAuthed = bank.authenticate(autoId, autoPwd);

                            if (autoAuthed != null) {
                                System.out.println(">> [인증 결과] 로그인 성공! [" + autoAuthed.getName() + "]님 환영합니다.");
                                loginCustomer = autoAuthed;
                                isLoggedIn = true;
                                System.out.println(">> 유저 세션 연결 완료. 계좌 개설 및 거래가 가능합니다.");
                            } else {
                                loginCustomer = null;
                                isLoggedIn = false;
                                System.out.println("주의: 가입 완료 후 로그인 시도에 실패했습니다. 메인 메뉴에서 재인증이 필요합니다.");
                            }
                            break;

                        default:
                            System.out.println("잘못된 선택입니다. 서브 메뉴를 종료하고 메인으로 돌아갑니다.");
                            break;
                    }
                    break;

                case 2:
                    System.out.println("\n[2] 신규 계좌 개설 세션");
                    // 회원가입만 정상 수행하고 로그인을 제쳤을 때 발생하는 전산 우회를 원천 차단
                    if (!isLoggedIn) {
                        System.out.println("오류: 1번 항목을 통해 가입 및 로그인을 먼저 완료해주세요.");
                        break;
                    }
                    // 이미 계좌를 보유한 고객은 중복 개설 차단 (DB 기준 — 재실행 후에도 유효)
                    if (bank.hasAccount(loginCustomer)) {
                        System.out.println("이미 계좌가 존재합니다. (한 고객당 1개 계좌만 개설할 수 있습니다.)");
                        if (myAccount == null) {
                            myAccount = bank.loadAccount(loginCustomer);
                        }
                        break;
                    }
                    System.out.print(" -> 최초로 예치할 금액 입력(원): ");
                    double initialBalance;
                    try {
                        initialBalance = scanner.nextDouble();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">> [오류] 숫자를 입력해주세요.");
                        scanner.nextLine();
                        break;
                    }
                    if (initialBalance < 0) {
                        System.out.println(">> [오류] 초기 예치금은 0원 이상이어야 합니다.");
                        break;
                    }
                    System.out.print(" -> 설정할 계좌 비밀번호 4자리 입력: ");
                    String accountPassword = scanner.nextLine();

                    myAccount = loginCustomer.openAccount(bank, initialBalance, accountPassword);
                    System.out.println(">> [전산 결과] 신규 계좌가 생성되었습니다. [계좌번호: "
                            + myAccount.getAccountNumber() + "]");
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

                    // 계좌번호 + 비밀번호를 함께 검증 (도메인 계좌인증)
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
                    double depAmount;
                    try {
                        depAmount = scanner.nextDouble();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">> [오류] 숫자를 입력해주세요.");
                        scanner.nextLine();
                        break;
                    }
                    System.out.print(" -> 계좌 비밀번호 입력: ");
                    String depPin = scanner.nextLine();

                    reportResult(bank.deposit(myAccount, depPin, depAmount), "입금 완료", myAccount);
                    break;

                case 5:
                    System.out.println("\n[5] 출금 거래 수행");
                    if (myAccount == null) {
                        System.out.println("오류: 계좌가 없습니다.");
                        break;
                    }
                    System.out.print(" -> 출금할 금액 입력(원): ");
                    double withAmount;
                    try {
                        withAmount = scanner.nextDouble();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">> [오류] 숫자를 입력해주세요.");
                        scanner.nextLine();
                        break;
                    }
                    System.out.print(" -> 계좌 비밀번호 입력: ");
                    String withPin = scanner.nextLine();

                    reportResult(bank.withdraw(myAccount, withPin, withAmount), "출금 완료", myAccount);
                    break;

                case 6:
                    System.out.println("\n[6] 상대방에게 송금 수행");
                    if (myAccount == null) {
                        System.out.println("오류: 계좌가 없습니다.");
                        break;
                    }

                    System.out.print(" -> 송금 대상 계좌번호 입력 (엔터 시 기본값 200-5678): ");
                    String targetInput = scanner.nextLine().trim();
                    String targetNo = targetInput.isEmpty() ? "200-5678" : targetInput;

                    if (targetNo.equals(myAccount.getAccountNumber())) {
                        System.out.println(">> [오류] 본인 계좌로는 송금할 수 없습니다.");
                        break;
                    }

                    System.out.print(" -> 송금(이체)할 금액 입력(원): ");
                    double transAmount;
                    try {
                        transAmount = scanner.nextDouble();
                        scanner.nextLine();
                    } catch (InputMismatchException e) {
                        System.out.println(">> [오류] 숫자를 입력해주세요.");
                        scanner.nextLine();
                        break;
                    }
                    System.out.print(" -> 계좌 비밀번호 입력: ");
                    String transPin = scanner.nextLine();

                    reportResult(bank.transfer(myAccount, transPin, targetNo, transAmount), "송금 완료", myAccount);
                    break;

                case 7:
                    System.out.println("\n[7] 전산 계좌 정보 최종 조회");
                    if (myAccount == null) {
                        System.out.println("오류: 조회할 계좌 정보가 전산에 등록되어 있지 않습니다.");
                        break;
                    }
                    System.out.println("[" + loginCustomer.getName() + " 고객님의 현재 전산 계좌 명세]");
                    System.out.println("계좌번호 : " + myAccount.getAccountNumber());
                    System.out.println("잔액 : " + myAccount.getBalance());

                    System.out.println("\n[최근 거래내역 (최대 3건)]");
                    List<TransactionRecord> history = bank.recentHistory(myAccount.getAccountNumber(), 3);
                    if (history.isEmpty()) {
                        System.out.println("   거래내역이 없습니다.");
                    } else {
                        int no = 1;
                        for (TransactionRecord record : history) {
                            System.out.println("   " + no + ". " + record.format());
                            no++;
                        }
                    }
                    break;

                case 8:
                    System.out.println("\n>> 은행 전산 프로그램 시연을 종료합니다. 발표를 마치겠습니다.");
                    Database.close();
                    isRunning = false;
                    break;

                default:
                    System.out.println("잘못된 번호입니다. 1번부터 8번 사이의 정수를 입력하세요.");
                    break;
            }
        }
        scanner.close();
    }

    // 거래 결과(TransactionResult)를 사용자 메시지로 변환 — 표현 계층 책임
    private static void reportResult(TransactionResult result, String successMessage, Account account) {
        switch (result) {
            case SUCCESS -> {
                System.out.println(successMessage);
                System.out.println("   현재 잔액 : " + account.getBalance() + "원");
            }
            case WRONG_PIN ->
                    System.out.println(">> [오류] 계좌 비밀번호가 일치하지 않습니다. 거래가 취소되었습니다.");
            case INSUFFICIENT_FUNDS ->
                    System.out.println(">> [오류] 잔액이 부족합니다.");
            case INVALID_AMOUNT ->
                    System.out.println(">> [오류] 금액은 0원보다 커야 합니다.");
            case ACCOUNT_NOT_FOUND ->
                    System.out.println(">> [오류] 존재하지 않는 계좌번호입니다.");
        }
    }
}
