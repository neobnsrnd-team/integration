package demo.mci.biz;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 가상 계좌 저장소 (데모용)
 */
public class AccountRepository {

    private static final AccountRepository INSTANCE = new AccountRepository();

    private final Map<String, Long> accountBalances = new ConcurrentHashMap<>();

    private AccountRepository() {
        // 테스트 데이터 초기화
        accountBalances.put("1234567890123456789", 1000000L);
        accountBalances.put("9876543210987654321", 500000L);
        accountBalances.put("1111222233334444555", 2500000L);
    }

    public static AccountRepository getInstance() {
        return INSTANCE;
    }

    public Long getBalance(String accountNo) {
        return accountBalances.get(accountNo.trim());
    }

    public void setBalance(String accountNo, Long balance) {
        accountBalances.put(accountNo.trim(), balance);
    }

    public boolean exists(String accountNo) {
        return accountBalances.containsKey(accountNo.trim());
    }
}
