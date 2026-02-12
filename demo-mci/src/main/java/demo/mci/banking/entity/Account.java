package demo.mci.banking.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * 계좌 정보 클래스 (데모용)
 */
@Getter
@Builder
public class Account {

    private final String accountNo;
    private final String accountName;
    private final String accountType;  // 01:보통예금, 02:정기예금, 03:적금
    private final String openDate;     // YYYYMMDD
    private final String status;       // 1:정상, 2:정지, 9:해지
    private long balance;
    private final long availableBalance;
    private final int interestRate;    // 이율 (소수점 2자리, 예: 250 = 2.50%)

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public void addBalance(long amount) {
        this.balance += amount;
    }

    public void subtractBalance(long amount) {
        this.balance -= amount;
    }
}
