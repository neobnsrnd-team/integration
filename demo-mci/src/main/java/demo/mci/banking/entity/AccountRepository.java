package demo.mci.banking.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 가상 계좌 저장소 (데모용)
 */
public class AccountRepository {

    private static final AccountRepository INSTANCE = new AccountRepository();

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    private AccountRepository() {
        // 테스트 데이터 초기화
        accounts.put("1234567890123456789", Account.builder()
                .accountNo("1234567890123456789")
                .accountName("홍길동 보통예금")
                .accountType("01")
                .openDate("20200315")
                .status("1")
                .balance(1000000L)
                .availableBalance(1000000L)
                .interestRate(150)  // 1.50%
                .build());

        accounts.put("9876543210987654321", Account.builder()
                .accountNo("9876543210987654321")
                .accountName("김철수 보통예금")
                .accountType("01")
                .openDate("20210620")
                .status("1")
                .balance(500000L)
                .availableBalance(500000L)
                .interestRate(150)  // 1.50%
                .build());

        accounts.put("1111222233334444555", Account.builder()
                .accountNo("1111222233334444555")
                .accountName("이영희 정기예금")
                .accountType("02")
                .openDate("20230101")
                .status("1")
                .balance(2500000L)
                .availableBalance(0L)  // 정기예금은 출금 불가
                .interestRate(350)  // 3.50%
                .build());
    }

    public static AccountRepository getInstance() {
        return INSTANCE;
    }

    public Account getAccount(String accountNo) {
        return accounts.get(accountNo.trim());
    }

    public Long getBalance(String accountNo) {
        Account account = accounts.get(accountNo.trim());
        return account != null ? account.getBalance() : null;
    }

    public void setBalance(String accountNo, Long balance) {
        Account account = accounts.get(accountNo.trim());
        if (account != null) {
            account.setBalance(balance);
        }
    }

    public boolean exists(String accountNo) {
        return accounts.containsKey(accountNo.trim());
    }
}
