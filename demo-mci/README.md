# MCI Framework Demo

MCI (Message Communication Interface) Framework demo application demonstrating Banking and Card company integrations with support for TCP, HTTP, and HTTPS protocols.

## Features

- **Banking Module**: Balance inquiry, transfer, transaction history, account inquiry, echo, heartbeat
- **Card Module**: Card list inquiry, card usage history inquiry
- **Multi-Protocol Support**: TCP (fixed-length binary), HTTP (JSON/REST), HTTPS (encrypted JSON/REST)
- **YAML-based Message Layouts**: Flexible message definition with field-level configuration
- **Biz Pattern**: Clean separation of business logic from protocol handling

## Project Structure

```
demo-mci/src/main/java/demo/mci/
├── banking/                    # Banking domain
│   ├── biz/                    # Business logic (BAL1, TRF1, TXH1, ACT1, ECH1, HBT1)
│   ├── entity/                 # Account, AccountRepository, TransactionRepository
│   ├── tcp/                    # BankTcpServer, BankTcpClient
│   ├── http/                   # BankHttpServer, BankHttpClient
│   └── https/                  # BankHttpsServer, BankHttpsClient
│
├── card/                       # Card domain
│   ├── biz/                    # Business logic (CRD1, CUH1)
│   ├── entity/                 # Card, CardRepository, CardUsageHistory
│   ├── tcp/                    # CardTcpServer, CardTcpClient
│   ├── http/                   # CardHttpServer, CardHttpClient
│   └── https/                  # CardHttpsServer, CardHttpsClient
│
├── common/                     # Shared components
│   ├── DemoConstants.java      # Ports, timeouts, response codes
│   ├── DemoMessageCodes.java   # Message codes (BAL1, CRD1, etc.)
│   └── DemoLayoutRegistry.java # YAML layout registration
│
└── DemoApplication.java        # Main entry point with all modes
```

## Message Codes

### Banking Messages

| Code | Type | Description |
|------|------|-------------|
| BAL1 | REQ | Balance inquiry request |
| BAL2 | RES | Balance inquiry response |
| TRF1 | REQ | Transfer request |
| TRF2 | RES | Transfer response |
| TXH1 | REQ | Transaction history request |
| TXH2 | RES | Transaction history response |
| ACT1 | REQ | Account inquiry request |
| ACT2 | RES | Account inquiry response |
| ECH1 | REQ | Echo request |
| ECH2 | RES | Echo response |
| HBT1 | REQ | Heartbeat request |
| HBT2 | RES | Heartbeat response |

### Card Messages

| Code | Type | Description |
|------|------|-------------|
| CRD1 | REQ | Card list inquiry request |
| CRD2 | RES | Card list inquiry response |
| CUH1 | REQ | Card usage history request |
| CUH2 | RES | Card usage history response |

## Port Configuration

| Domain | Protocol | Default Port |
|--------|----------|--------------|
| Banking | TCP | 9001 |
| Banking | HTTP | 9003 |
| Banking | HTTPS | 9443 |
| Card | TCP | 9011 |
| Card | HTTP | 9013 |
| Card | HTTPS | 9444 |

## Build

```bash
mvn clean package -pl demo-mci -am
```

## Usage

### Banking Demo

```bash
# TCP demo (server + client)
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-demo

# HTTP demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-http-demo

# HTTPS demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-https-demo

# Run server only
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-server 9001

# Run client only
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar bank-client localhost 9001
```

### Card Demo

```bash
# TCP demo (server + client)
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-demo

# HTTP demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-http-demo

# HTTPS demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-https-demo

# Run server only
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-server 9011

# Run client only
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar card-client localhost 9011
```

### Legacy Commands (Backward Compatible)

```bash
# These map to bank-* commands
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar demo         # -> bank-demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar server       # -> bank-server
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar client       # -> bank-client
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar http-demo    # -> bank-http-demo
java -jar demo-mci/target/demo-mci-1.0.0-SNAPSHOT.jar https-demo   # -> bank-https-demo
```

## REST API Examples

### Card HTTP Server (port 9013)

**Health Check:**
```bash
curl http://localhost:9013/health
```

**Card List Inquiry:**
```bash
curl -X POST http://localhost:9013/api/cards \
  -H "Content-Type: application/json" \
  -d '{
    "messageCode": "CRD1",
    "fields": {
      "customerId": "CUST001"
    }
  }'
```

**Card Usage History:**
```bash
curl -X POST http://localhost:9013/api/card-history \
  -H "Content-Type: application/json" \
  -d '{
    "messageCode": "CUH1",
    "fields": {
      "cardNo": "1234567890123456",
      "fromDate": "20240101",
      "toDate": "20240131"
    }
  }'
```

### Banking HTTP Server (port 9003)

**Balance Inquiry:**
```bash
curl -X POST http://localhost:9003/api/balance \
  -H "Content-Type: application/json" \
  -d '{
    "messageCode": "BAL1",
    "fields": {
      "accountNo": "1234567890123456789"
    }
  }'
```

**Transfer:**
```bash
curl -X POST http://localhost:9003/api/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "messageCode": "TRF1",
    "fields": {
      "fromAccount": "1234567890123456789",
      "toAccount": "9876543210987654321",
      "amount": 50000,
      "memo": "Transfer"
    }
  }'
```

## Test Data

### Banking Accounts

| Account No | Owner | Initial Balance |
|------------|-------|-----------------|
| 1234567890123456789 | 홍길동 | 1,000,000 |
| 9876543210987654321 | 김철수 | 500,000 |
| 1111222233334444555 | 이영희 | 2,500,000 |

### Card Data

| Customer | Card No | Card Name | Type | Limit | Used |
|----------|---------|-----------|------|-------|------|
| CUST001 | 1234567890123456 | 신한카드 Deep Dream | 신용 | 10,000,000 | 1,500,000 |
| CUST001 | 2345678901234567 | 삼성카드 taptap O | 신용 | 5,000,000 | 800,000 |
| CUST001 | 3456789012345678 | 현대카드 M | 체크 | 0 | 0 |
| CUST002 | 4567890123456789 | KB국민카드 탄탄대로 | 신용 | 20,000,000 | 5,000,000 |
| CUST003 | 5678901234567890 | 롯데카드 LOCA | 신용 | 3,000,000 | 1,200,000 |

## Running Tests

### All Tests

```bash
# Run all tests (125 tests)
mvn test -pl demo-mci

# Run all tests with verbose output
mvn test -pl demo-mci -Dsurefire.useFile=false
```

### Integration Tests

```bash
# All integration tests (TCP, HTTP, HTTPS for both Banking and Card)
mvn test -pl demo-mci -Dtest="*IntegrationTest"

# Card integration tests only
mvn test -pl demo-mci -Dtest="Card*IntegrationTest"

# Banking TCP integration tests
mvn test -pl demo-mci -Dtest="ClientServerIntegrationTest"

# Banking HTTP integration tests
mvn test -pl demo-mci -Dtest="HttpIntegrationTest"

# Banking HTTPS integration tests
mvn test -pl demo-mci -Dtest="HttpsIntegrationTest"

# Card TCP integration tests
mvn test -pl demo-mci -Dtest="CardIntegrationTest"

# Card HTTP integration tests
mvn test -pl demo-mci -Dtest="CardHttpIntegrationTest"

# Card HTTPS integration tests
mvn test -pl demo-mci -Dtest="CardHttpsIntegrationTest"
```

### Unit Tests

```bash
# All Biz unit tests
mvn test -pl demo-mci -Dtest="*BizTest"

# Specific Biz tests
mvn test -pl demo-mci -Dtest="BalanceInquiryBizTest"
mvn test -pl demo-mci -Dtest="TransferBizTest"
mvn test -pl demo-mci -Dtest="TransactionHistoryBizTest"
mvn test -pl demo-mci -Dtest="AccountInquiryBizTest"
mvn test -pl demo-mci -Dtest="EchoBizTest"
mvn test -pl demo-mci -Dtest="HeartbeatBizTest"

# YAML layout loading tests
mvn test -pl demo-mci -Dtest="YamlLayoutLoadingTest"
```

### Test Summary

| Test Class | Description | Count |
|------------|-------------|-------|
| `CardIntegrationTest` | Card TCP client-server | 10 |
| `CardHttpIntegrationTest` | Card HTTP client-server | 8 |
| `CardHttpsIntegrationTest` | Card HTTPS client-server | 6 |
| `ClientServerIntegrationTest` | Banking TCP client-server | 16 |
| `HttpIntegrationTest` | Banking HTTP client-server | 18 |
| `HttpsIntegrationTest` | Banking HTTPS client-server | 20 |
| `BalanceInquiryBizTest` | Balance inquiry logic | 4 |
| `TransferBizTest` | Transfer logic | 6 |
| `TransactionHistoryBizTest` | Transaction history logic | 7 |
| `AccountInquiryBizTest` | Account inquiry logic | 6 |
| `EchoBizTest` | Echo logic | 6 |
| `HeartbeatBizTest` | Heartbeat logic | 5 |
| `YamlLayoutLoadingTest` | YAML layout parsing | 13 |
| **Total** | | **125** |

## Architecture

The demo follows a layered architecture:

1. **Protocol Layer** (TCP/HTTP/HTTPS): Handles network communication
2. **Codec Layer**: Encodes/decodes messages using YAML-defined layouts
3. **Biz Layer**: Processes business logic for each message type
4. **Entity Layer**: Domain objects and repositories

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│   Server    │────▶│  Biz Layer  │
│ (TCP/HTTP)  │     │ (TCP/HTTP)  │     │ (BAL1, CRD1)│
└─────────────┘     └─────────────┘     └─────────────┘
                           │                    │
                           ▼                    ▼
                    ┌─────────────┐     ┌─────────────┐
                    │LayoutManager│     │ Repository  │
                    │ (YAML Codec)│     │  (Entity)   │
                    └─────────────┘     └─────────────┘
```

## License

Copyright 2024-2025. All rights reserved.
