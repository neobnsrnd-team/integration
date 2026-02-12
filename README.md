# Springware MCI Framework

**Message Communication Interface (MCI) Framework** - A production-ready Java framework for building secure, scalable financial communication systems with multi-protocol support.

## Overview

Springware MCI provides a comprehensive foundation for enterprise messaging systems, supporting fixed-length binary protocols (TCP), REST APIs (HTTP/HTTPS), and protocol normalization for external provider integration.

### Key Features

- **Multi-Protocol Support**: TCP, UDP, HTTP, HTTPS with unified programming model
- **YAML-based Message Layouts**: Flexible fixed/variable-length message definitions
- **Protocol Normalization**: Adapter pattern for external provider integration with different error formats
- **Connection Management**: Connection pooling, circuit breaker, health checks
- **SSL/TLS Support**: Full HTTPS support with certificate management
- **Async Messaging**: CompletableFuture-based asynchronous communication
- **Sensitive Data Masking**: Field-level masking for PCI-DSS compliance
- **Comprehensive Testing**: 177 unit and integration tests

## Module Structure

```
springware-mci-parent
├── springware-common          # Shared utilities and base exceptions
├── springware-mci-core        # Core framework (server, client, protocol)
└── demo-mci                   # Demo applications (Banking, Card domains)
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 17 |
| Network I/O | Netty 4.1.104 |
| Serialization | Jackson 2.16.1 (JSON/YAML/XML) |
| Logging | SLF4J 2.0.9 + Logback 1.4.14 |
| Build | Maven 3.x |
| Testing | JUnit 5, Mockito 5.8.0, AssertJ 3.24.2 |

---

## Core Framework (springware-mci-core)

### Message & Protocol

| Component | Description |
|-----------|-------------|
| `Message` | Central message object with type-safe field access |
| `MessageType` | REQUEST, RESPONSE, ACK, NACK, HEARTBEAT |
| `TransportType` | TCP, UDP, HTTP |
| `MessageCodec` | Encode/decode interface with layout integration |
| `ProtocolConfig` | Protocol configuration (length field, charset, framing) |

### Message Layout System

YAML-based message definition with automatic encoding/decoding:

```yaml
id: BAL1
description: Balance Inquiry Request
fields:
  - { name: msgCode, length: 4, type: S }
  - { name: orgCode, length: 3, type: S }
  - { name: txDate, length: 8, type: S, expression: "${DATE:yyyyMMdd}" }
  - { name: accountNo, length: 19, type: S, masked: true }
```

**Field Types:**
| Type | Code | Description |
|------|------|-------------|
| STRING | S | Left-aligned, space-padded |
| NUMBER | N | Right-aligned, zero-padded |
| AMOUNT | A | Right-aligned with decimal places |
| DATE | D | YYYYMMDD format |
| TIME | T | HHmmss format |
| BINARY | B | Raw binary data |
| VARCHAR | V | Variable-length string |

### Server Components

| Component | Description |
|-----------|-------------|
| `MciServer` | Server interface with handler registration |
| `TcpServer` | Netty-based TCP server with NIO |
| `HttpServer` | HTTP/HTTPS server with REST endpoints |
| `Biz` | Business logic interface |
| `BizRegistry` | Message code to handler mapping |
| `MessageContext` | Request context (client info, timestamps) |

### Client Components

| Component | Description |
|-----------|-------------|
| `MciClient` | Client interface with sync/async send |
| `TcpClient` | Netty-based TCP client |
| `HttpClient` | Java 11+ HttpClient wrapper |
| `ConnectionPool` | TCP connection pooling |
| `CircuitBreaker` | Fault tolerance pattern |
| `HealthChecker` | Periodic health monitoring |

### Exception Hierarchy

```
SpringwareException
└── MciException
    ├── ConnectionException    # Connection failures
    ├── TimeoutException       # Request timeouts
    ├── ProtocolException      # Encoding/decoding errors
    ├── LayoutException        # YAML parsing errors
    ├── ErrorResponseException # External provider errors (with original error code)
    └── ExternalProviderException
```

### Protocol Normalization (프로토콜 정규화)

#### 개요

외부 서비스 제공자(External Provider)들은 각자 고유한 프로토콜 규격을 사용합니다:
- **성공/실패 코드**: "00", "SUCCESS", "0000" 등 제공자마다 다른 형식
- **에러 필드명**: "error_message", "error_reason", "errMsg" 등 제공자마다 다른 필드명
- **에러 코드 체계**: 수십~수백 개의 다양한 에러 코드

이러한 차이로 인해 내부 시스템에서 모든 외부 에러 코드를 매핑하는 것은 **현실적으로 불가능**합니다.

#### 해결 방안: ErrorResponseException

`ErrorResponseException`은 외부 시스템의 원본 에러 정보를 그대로 래핑하여 제공합니다:

```java
@Getter
public class ErrorResponseException extends MciException {
    private final String providerId;           // 제공자 ID
    private final String externalErrorCode;    // 외부 에러 코드 (원본)
    private final String externalErrorMessage; // 외부 에러 메시지 (원본)
    private final Message originalResponse;    // 원본 응답 전체
}
```

#### 외부 제공자별 프로토콜 차이 예시

| 제공자 | 성공 코드 | 에러 코드 예시 | 코드 필드 | 메시지 필드 |
|--------|----------|---------------|-----------|-------------|
| Provider A | "00" | "01", "02", "99" | resultCode | error_message |
| Provider B | "SUCCESS" | "FAIL", "ACCT_ERR", "TIMEOUT" | status | error_reason |
| Provider C | "0000" | "1001", "1002", "9999" | rspCode | errMsg |

#### 에러 코드 매핑 전략

**매핑 가능한 주요 코드만 내부 코드로 변환:**

| 제공자 | 외부 코드 | 내부 코드 | 설명 |
|--------|----------|----------|------|
| Provider A | "00" | "0000" | 성공 |
| Provider A | "01" | "1001" | 무효 계좌 |
| Provider A | "02" | "1002" | 잔액 부족 |
| Provider B | "SUCCESS" | "0000" | 성공 |
| Provider B | "ACCT_ERR" | "1001" | 무효 계좌 |
| Provider C | 동일 | 동일 | 내부 형식과 동일 |

**매핑되지 않은 코드는 원본 그대로 보존:**
- 외부 시스템의 에러 코드가 수백 개일 수 있음
- 모든 코드를 매핑하는 것은 유지보수 부담
- `ErrorResponseException`을 통해 원본 코드에 직접 접근 가능

#### 사용 패턴

**패턴 1: 성공/실패 확인 후 처리**
```java
ProtocolNormalizer normalizer = registry.getNormalizer("PROVIDER_A");
NormalizedResponse response = normalizer.normalize(externalResponse);

if (response.isSuccess()) {
    // 성공 처리
    Long balance = response.getField("balance");
    processBalance(balance);
} else {
    // 실패 처리 - 원본 에러 정보 접근
    String externalCode = response.getExternalErrorCode();    // "99" (원본)
    String externalMsg = response.getExternalErrorMessage();  // "System error" (원본)
    String internalCode = response.getErrorCode();            // "9999" (매핑된 코드, 없으면 원본)

    log.error("외부 시스템 오류 - 제공자: {}, 외부코드: {}, 메시지: {}",
        response.getProviderId(), externalCode, externalMsg);
}
```

**패턴 2: orThrow()로 예외 발생**
```java
try {
    NormalizedResponse response = normalizer.normalize(externalResponse).orThrow();
    // 성공시에만 이 코드 실행
    Long balance = response.getField("balance");

} catch (ErrorResponseException e) {
    // 외부 에러 정보에 직접 접근
    String providerId = e.getProviderId();              // "PROVIDER_A"
    String externalCode = e.getExternalErrorCode();     // "99" (원본 코드)
    String externalMsg = e.getExternalErrorMessage();   // "System error"

    // 원본 응답의 다른 필드도 접근 가능
    String detail = e.getOriginalString("errorDetail");

    // 특정 에러 코드 확인
    if (e.hasExternalErrorCode("TIMEOUT")) {
        // 타임아웃 재시도 로직
    }
}
```

**패턴 3: 일관된 내부 코드로 처리**
```java
// 모든 제공자에서 동일한 내부 코드로 처리 가능
NormalizedResponse responseA = normalizerA.normalize(providerAResponse);
NormalizedResponse responseB = normalizerB.normalize(providerBResponse);
NormalizedResponse responseC = normalizerC.normalize(providerCResponse);

// 제공자와 관계없이 동일한 내부 코드로 비교
if ("1001".equals(responseA.getErrorCode())) {
    // 무효 계좌 처리 - Provider A의 "01"이 "1001"로 매핑됨
}
if ("1001".equals(responseB.getErrorCode())) {
    // 무효 계좌 처리 - Provider B의 "ACCT_ERR"이 "1001"로 매핑됨
}
```

#### 클래스 구조

```
springware-mci-core/
└── common/
    ├── response/
    │   ├── ResponseStatus.java        # SUCCESS, FAILURE, UNKNOWN
    │   └── NormalizedResponse.java    # 정규화된 응답 래퍼
    ├── exception/
    │   └── ErrorResponseException.java # 외부 에러 래핑 예외
    └── protocol/normalize/
        ├── ProtocolNormalizer.java         # 정규화기 인터페이스
        ├── AbstractProtocolNormalizer.java # 추상 기본 클래스
        ├── ProtocolNormalizerRegistry.java # 정규화기 레지스트리
        └── DefaultProtocolNormalizer.java  # 기본 정규화기

demo-mci/
└── external/
    ├── ExternalProviderConstants.java  # 제공자별 상수 정의
    ├── ExternalProviderRegistry.java   # 데모 정규화기 등록
    ├── normalizer/
    │   ├── ProviderANormalizer.java    # "00"/"99" 형식
    │   ├── ProviderBNormalizer.java    # "SUCCESS"/"FAIL" 형식
    │   └── ProviderCNormalizer.java    # "0000"/"9999" 형식
    └── simulator/
        ├── ProviderASimulator.java     # Provider A 응답 시뮬레이터
        ├── ProviderBSimulator.java     # Provider B 응답 시뮬레이터
        └── ProviderCSimulator.java     # Provider C 응답 시뮬레이터
```

#### 새로운 제공자 추가 방법

1. **상수 정의** (`ExternalProviderConstants.java`)
```java
public static final String PROVIDER_X = "PROVIDER_X";
public static final String PROVIDER_X_CODE_FIELD = "result_code";
public static final String PROVIDER_X_ERROR_FIELD = "err_msg";
public static final String PROVIDER_X_SUCCESS = "OK";
public static final Map<String, String> PROVIDER_X_ERROR_MAPPING = Map.of(
    "OK", "0000",
    "INVALID_ACCT", "1001"
    // 주요 에러만 매핑, 나머지는 원본 유지
);
```

2. **정규화기 구현** (`ProviderXNormalizer.java`)
```java
public class ProviderXNormalizer extends AbstractProtocolNormalizer {
    public ProviderXNormalizer() {
        super(
            ExternalProviderConstants.PROVIDER_X,
            ExternalProviderConstants.PROVIDER_X_CODE_FIELD,
            ExternalProviderConstants.PROVIDER_X_ERROR_FIELD,
            Set.of(ExternalProviderConstants.PROVIDER_X_SUCCESS),
            ExternalProviderConstants.PROVIDER_X_ERROR_MAPPING
        );
    }
}
```

3. **레지스트리 등록** (`ExternalProviderRegistry.java`)
```java
normalizerRegistry.register(new ProviderXNormalizer());
```

#### 핵심 설계 원칙

1. **원본 보존**: 외부 에러 코드/메시지를 항상 원본 그대로 접근 가능
2. **선택적 매핑**: 주요 에러만 내부 코드로 매핑, 나머지는 원본 유지
3. **일관된 인터페이스**: 모든 제공자에 대해 동일한 `NormalizedResponse` 사용
4. **예외 래핑**: `ErrorResponseException`으로 외부 에러 정보 전달
5. **확장성**: 새로운 제공자 추가 시 정규화기만 구현

---

## Demo Application (demo-mci)

### Banking Domain

| Message | Code | Description |
|---------|------|-------------|
| Balance Inquiry | BAL1/BAL2 | Query account balance |
| Transfer | TRF1/TRF2 | Transfer money between accounts |
| Transaction History | TXH1/TXH2 | Query transaction history |
| Account Inquiry | ACT1/ACT2 | Query account information |
| Echo | ECH1/ECH2 | Echo service for testing |
| Heartbeat | HBT1/HBT2 | Health check service |

### Card Domain

| Message | Code | Description |
|---------|------|-------------|
| Card List | CRD1/CRD2 | Query customer's cards |
| Usage History | CUH1/CUH2 | Query card usage history |

### External Provider Integration

| Provider | Success Code | Error Codes | Code Field | Message Field |
|----------|--------------|-------------|------------|---------------|
| Provider A | "00" | "01", "02", "99" | resultCode | error_message |
| Provider B | "SUCCESS" | "FAIL", "ERROR", "ACCT_ERR" | status | error_reason |
| Provider C | "0000" | "1001", "1002", "9999" | rspCode | errMsg |

### Port Configuration

| Domain | TCP | HTTP | HTTPS |
|--------|-----|------|-------|
| Banking | 9001 | 9003 | 9443 |
| Card | 9011 | 9013 | 9444 |

---

## Quick Start

### Build

```bash
mvn clean package
```

### Run Banking Demo

```bash
# TCP demo (server + client)
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-demo

# HTTP demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-http-demo

# HTTPS demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-https-demo
```

### Run Card Demo

```bash
# TCP demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-demo

# HTTP demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-http-demo
```

### REST API Examples

**Balance Inquiry:**
```bash
curl -X POST http://localhost:9003/api/balance \
  -H "Content-Type: application/json" \
  -d '{"messageCode":"BAL1","fields":{"accountNo":"1234567890123456789"}}'
```

**Card List:**
```bash
curl -X POST http://localhost:9013/api/cards \
  -H "Content-Type: application/json" \
  -d '{"messageCode":"CRD1","fields":{"customerId":"CUST001"}}'
```

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        Application Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   Banking   │  │    Card     │  │   External Provider     │  │
│  │    Biz      │  │    Biz      │  │      Normalizer         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│                        Framework Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │   Server    │  │   Client    │  │     Layout Manager      │  │
│  │ TCP/HTTP/S  │  │ TCP/HTTP/S  │  │   (YAML MessageCodec)   │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Conn Pool  │  │Circuit Break│  │    Message Logger       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│                        Transport Layer                            │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    Netty NIO (TCP/HTTP/HTTPS)                │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## Testing

### Run All Tests

```bash
# All tests (177 tests)
mvn test

# Demo module only
mvn test -pl demo-mci
```

### Test Categories

```bash
# Integration tests
mvn test -pl demo-mci -Dtest="*IntegrationTest"

# Unit tests
mvn test -pl demo-mci -Dtest="*BizTest"

# Protocol normalizer tests
mvn test -pl demo-mci -Dtest="*NormalizerTest,*ResponseTest,*ExceptionTest"
```

### Test Summary

| Category | Tests |
|----------|-------|
| Banking TCP Integration | 16 |
| Banking HTTP Integration | 18 |
| Banking HTTPS Integration | 20 |
| Card TCP Integration | 10 |
| Card HTTP Integration | 8 |
| Card HTTPS Integration | 6 |
| Business Logic (Biz) | 34 |
| Layout Loading | 13 |
| Protocol Normalizer | 52 |
| **Total** | **177** |

---

## Project Structure

```
springware-mci/
├── springware-common/                    # Shared utilities
│   └── src/main/java/springware/common/
│       └── exception/
│           └── SpringwareException.java
│
├── springware-mci-core/                  # Core framework
│   └── src/main/java/springware/mci/
│       ├── common/
│       │   ├── core/                     # Message, MessageType, TransportType
│       │   ├── protocol/                 # MessageCodec, ProtocolConfig
│       │   │   └── normalize/            # ProtocolNormalizer, Registry
│       │   ├── layout/                   # MessageLayout, FieldDefinition, YAML
│       │   ├── logging/                  # MessageLogger, Masking
│       │   ├── response/                 # NormalizedResponse, ResponseStatus
│       │   ├── exception/                # Exception hierarchy
│       │   └── http/                     # HTTP utilities
│       ├── server/
│       │   ├── core/                     # MciServer, MessageContext
│       │   ├── biz/                      # Biz, BizRegistry
│       │   ├── tcp/                      # TcpServer
│       │   ├── http/                     # HttpServer
│       │   └── config/                   # ServerConfig
│       └── client/
│           ├── core/                     # MciClient, ConnectionState
│           ├── tcp/                      # TcpClient
│           ├── http/                     # HttpClient
│           ├── pool/                     # ConnectionPool
│           ├── circuitbreaker/           # CircuitBreaker
│           ├── healthcheck/              # HealthChecker
│           └── config/                   # ClientConfig
│
└── demo-mci/                             # Demo application
    ├── src/main/java/demo/mci/
    │   ├── banking/                      # Banking domain
    │   │   ├── biz/                      # Business logic
    │   │   ├── entity/                   # Account, Transaction
    │   │   ├── tcp/                      # TCP server/client
    │   │   ├── http/                     # HTTP server/client
    │   │   └── https/                    # HTTPS server/client
    │   ├── card/                         # Card domain
    │   │   ├── biz/                      # Business logic
    │   │   ├── entity/                   # Card, UsageHistory
    │   │   ├── tcp/                      # TCP server/client
    │   │   ├── http/                     # HTTP server/client
    │   │   └── https/                    # HTTPS server/client
    │   ├── external/                     # External providers
    │   │   ├── normalizer/               # Protocol normalizers
    │   │   └── simulator/                # Provider simulators
    │   └── common/                       # Constants, codes
    ├── src/main/resources/
    │   └── layouts/                      # YAML message layouts
    └── src/test/java/demo/mci/           # Tests
```

---

## Response Codes

| Code | Description |
|------|-------------|
| 0000 | Success |
| 1001 | Invalid account |
| 1002 | Insufficient balance |
| 2001 | Invalid card |
| 2002 | Card suspended |
| 2003 | Card expired |
| 9999 | System error |

---

## Documentation

- [Demo Application README](demo-mci/README.md) - Detailed demo usage guide
- [Architecture Diagrams](docs/diagrams/) - PlantUML diagrams

---

## License

Copyright 2024-2025. All rights reserved.
