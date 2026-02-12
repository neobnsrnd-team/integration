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

### Protocol Normalization

Adapter pattern for external providers with different response formats:

```java
// External providers use different success/error codes
// Provider A: "00" = success, "99" = error
// Provider B: "SUCCESS" = success, "FAIL" = error
// Provider C: "0000" = success, "9999" = error

ProtocolNormalizer normalizer = registry.getNormalizer("PROVIDER_A");
NormalizedResponse response = normalizer.normalize(externalResponse);

if (response.isSuccess()) {
    Long balance = response.getField("balance");
} else {
    // Access original error details
    String externalCode = response.getExternalErrorCode();  // "99"
    String message = response.getExternalErrorMessage();
}

// Or use orThrow() pattern
try {
    NormalizedResponse response = normalizer.normalize(externalResponse).orThrow();
} catch (ErrorResponseException e) {
    log.error("Provider: {}, Code: {}, Message: {}",
        e.getProviderId(), e.getExternalErrorCode(), e.getExternalErrorMessage());
}
```

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
