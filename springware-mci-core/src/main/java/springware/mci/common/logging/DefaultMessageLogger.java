package springware.mci.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import springware.mci.common.core.Message;
import springware.mci.common.layout.FieldDefinition;
import springware.mci.common.layout.MessageLayout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기본 메시지 로거 구현
 *
 * 서버측 2단계 로깅:
 * 1단계: 헤더 정보만 로깅 (메시지 수신 즉시)
 * 2단계: 상세 정보 로깅 (마스킹 적용, 비동기 처리 가능)
 */
@Slf4j
public class DefaultMessageLogger implements MessageLogger {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Logger messageLog;
    private LogLevel logLevel = LogLevel.DETAIL_MASKED;
    private Charset charset = StandardCharsets.UTF_8;
    private final Map<String, MaskingRule> maskingRules = new ConcurrentHashMap<>();

    public DefaultMessageLogger() {
        this.messageLog = LoggerFactory.getLogger("MESSAGE_LOG");
    }

    public DefaultMessageLogger(String loggerName) {
        this.messageLog = LoggerFactory.getLogger(loggerName);
    }

    @Override
    public void logSend(Message message, MessageLayout layout, byte[] rawData) {
        if (logLevel == LogLevel.NONE) {
            return;
        }

        logHeader("SEND", message, rawData);

        if (logLevel.isEnabled(LogLevel.DETAIL_MASKED)) {
            logDetail("SEND", message, layout);
        }
    }

    @Override
    public void logReceive(Message message, MessageLayout layout, byte[] rawData) {
        if (logLevel == LogLevel.NONE) {
            return;
        }

        logHeader("RECV", message, rawData);

        if (logLevel.isEnabled(LogLevel.DETAIL_MASKED)) {
            logDetail("RECV", message, layout);
        }
    }

    @Override
    public void logHeader(String direction, Message message, byte[] rawData) {
        if (logLevel == LogLevel.NONE) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        int dataLength = rawData != null ? rawData.length : 0;

        messageLog.info("[{}] {} | MsgId={} | Code={} | Type={} | Len={}",
                timestamp,
                direction,
                truncate(message.getMessageId(), 8),
                message.getMessageCode(),
                message.getMessageType(),
                dataLength);
    }

    @Override
    public void logDetail(String direction, Message message, MessageLayout layout) {
        if (!logLevel.isEnabled(LogLevel.DETAIL_MASKED)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[DETAIL] ").append(direction).append(" | ");
        sb.append("MsgId=").append(truncate(message.getMessageId(), 8)).append("\n");

        if (layout != null) {
            for (FieldDefinition field : layout.getFields()) {
                String fieldName = field.getName();
                Object value = message.getField(fieldName);
                String displayValue = formatFieldValue(field, value);

                sb.append("  ").append(fieldName).append("=").append(displayValue);

                if (field.getDescription() != null) {
                    sb.append(" (").append(field.getDescription()).append(")");
                }
                sb.append("\n");
            }
        } else {
            // 레이아웃 없이 필드만 로깅
            for (Map.Entry<String, Object> entry : message.getFields().entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                String displayValue = applyMasking(fieldName, value != null ? value.toString() : "");
                sb.append("  ").append(fieldName).append("=").append(displayValue).append("\n");
            }
        }

        messageLog.info(sb.toString());
    }

    @Override
    public void setLogLevel(LogLevel level) {
        this.logLevel = level;
    }

    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public void addMaskingRule(MaskingRule rule) {
        maskingRules.put(rule.getFieldName(), rule);
    }

    /**
     * 문자셋 설정
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /**
     * 필드 값 포맷팅 (마스킹 적용)
     */
    private String formatFieldValue(FieldDefinition field, Object value) {
        if (value == null) {
            return "[null]";
        }

        String strValue = value.toString();

        // 필드에 마스킹 설정이 있거나, 마스킹 규칙이 등록된 경우
        if (field.isMasked() || maskingRules.containsKey(field.getName())) {
            if (logLevel == LogLevel.FULL) {
                // FULL 레벨에서는 마스킹 안함 (개발용)
                return strValue;
            }
            return applyMasking(field.getName(), strValue);
        }

        return strValue;
    }

    /**
     * 마스킹 적용
     */
    private String applyMasking(String fieldName, String value) {
        MaskingRule rule = maskingRules.get(fieldName);
        if (rule != null) {
            return rule.apply(value);
        }

        // 기본 전체 마스킹
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 문자열 자르기
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }

    /**
     * 기본 마스킹 규칙 등록 (금융 시스템용)
     */
    public void registerDefaultMaskingRules() {
        // 주민번호
        addMaskingRule(MaskingRule.ssn("ssn"));
        addMaskingRule(MaskingRule.ssn("ssnNo"));
        addMaskingRule(MaskingRule.ssn("jumin"));

        // 카드번호
        addMaskingRule(MaskingRule.cardNumber("cardNo"));
        addMaskingRule(MaskingRule.cardNumber("cardNumber"));

        // 계좌번호
        addMaskingRule(MaskingRule.accountNumber("acctNo"));
        addMaskingRule(MaskingRule.accountNumber("accountNo"));
        addMaskingRule(MaskingRule.accountNumber("accountNumber"));

        // 전화번호
        addMaskingRule(MaskingRule.phoneNumber("phoneNo"));
        addMaskingRule(MaskingRule.phoneNumber("telNo"));
        addMaskingRule(MaskingRule.phoneNumber("mobile"));

        // 비밀번호
        addMaskingRule(MaskingRule.full("password"));
        addMaskingRule(MaskingRule.full("pwd"));
        addMaskingRule(MaskingRule.full("pin"));
    }
}
