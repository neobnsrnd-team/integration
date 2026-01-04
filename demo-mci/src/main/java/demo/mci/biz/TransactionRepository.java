package demo.mci.biz;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 가상 거래내역 저장소 (데모용)
 */
public class TransactionRepository {

    private static final TransactionRepository INSTANCE = new TransactionRepository();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Map<String, List<Transaction>> transactionHistory = new ConcurrentHashMap<>();

    private TransactionRepository() {
        initTestData();
    }

    public static TransactionRepository getInstance() {
        return INSTANCE;
    }

    /**
     * 거래내역 조회
     */
    public List<Transaction> getTransactions(String accountNo, String fromDate, String toDate) {
        List<Transaction> transactions = transactionHistory.get(accountNo.trim());
        if (transactions == null) {
            return Collections.emptyList();
        }

        LocalDate from = LocalDate.parse(fromDate, DATE_FORMAT);
        LocalDate to = LocalDate.parse(toDate, DATE_FORMAT);

        return transactions.stream()
                .filter(tx -> {
                    LocalDate txDate = LocalDate.parse(tx.getTxDate(), DATE_FORMAT);
                    return !txDate.isBefore(from) && !txDate.isAfter(to);
                })
                .sorted(Comparator.comparing(Transaction::getTxDate).reversed()
                        .thenComparing(Comparator.comparing(Transaction::getTxTime).reversed()))
                .collect(Collectors.toList());
    }

    /**
     * 거래 추가
     */
    public void addTransaction(String accountNo, Transaction transaction) {
        transactionHistory.computeIfAbsent(accountNo.trim(), k -> new ArrayList<>())
                .add(transaction);
    }

    /**
     * 테스트 데이터 초기화
     */
    private void initTestData() {
        String account1 = "1234567890123456789";
        String account2 = "9876543210987654321";

        // 계좌1 거래내역
        addTransaction(account1, Transaction.builder()
                .txDate("20240115").txTime("143022").txType("1")
                .amount(500000L).balance(1500000L).memo("SLRY").build());
        addTransaction(account1, Transaction.builder()
                .txDate("20240114").txTime("091530").txType("2")
                .amount(50000L).balance(1000000L).memo("ATM").build());
        addTransaction(account1, Transaction.builder()
                .txDate("20240113").txTime("180000").txType("2")
                .amount(30000L).balance(1050000L).memo("CARD").build());
        addTransaction(account1, Transaction.builder()
                .txDate("20240112").txTime("100000").txType("1")
                .amount(100000L).balance(1080000L).memo("TRFR").build());
        addTransaction(account1, Transaction.builder()
                .txDate("20240110").txTime("120000").txType("2")
                .amount(200000L).balance(980000L).memo("BILL").build());

        // 계좌2 거래내역
        addTransaction(account2, Transaction.builder()
                .txDate("20240115").txTime("110000").txType("1")
                .amount(1000000L).balance(1500000L).memo("DPST").build());
        addTransaction(account2, Transaction.builder()
                .txDate("20240114").txTime("150000").txType("2")
                .amount(100000L).balance(500000L).memo("WDRL").build());
        addTransaction(account2, Transaction.builder()
                .txDate("20240113").txTime("093000").txType("2")
                .amount(50000L).balance(600000L).memo("TRFR").build());
    }

    /**
     * 거래 정보
     */
    @Getter
    @Builder
    public static class Transaction {
        private final String txDate;     // 거래일자 (yyyyMMdd)
        private final String txTime;     // 거래시간 (HHmmss)
        private final String txType;     // 거래구분 (1:입금, 2:출금)
        private final Long amount;       // 거래금액
        private final Long balance;      // 거래후잔액
        private final String memo;       // 적요코드

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("txDate", txDate);
            map.put("txTime", txTime);
            map.put("txType", txType);
            map.put("amount", amount);
            map.put("balance", balance);
            map.put("memo", memo);
            return map;
        }
    }
}
