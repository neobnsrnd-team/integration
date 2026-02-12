package demo.mci;

import demo.mci.banking.http.BankHttpClient;
import demo.mci.banking.http.BankHttpServer;
import demo.mci.banking.https.BankHttpsClient;
import demo.mci.banking.https.BankHttpsServer;
import demo.mci.banking.tcp.BankTcpClient;
import demo.mci.banking.tcp.BankTcpServer;
import demo.mci.card.http.CardHttpClient;
import demo.mci.card.http.CardHttpServer;
import demo.mci.card.https.CardHttpsClient;
import demo.mci.card.https.CardHttpsServer;
import demo.mci.card.tcp.CardTcpClient;
import demo.mci.card.tcp.CardTcpServer;
import demo.mci.common.DemoConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * 데모 애플리케이션 메인 클래스
 * Banking 및 Card 도메인 지원
 */
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            args = new String[]{"demo"};
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            // ========== Banking TCP ==========
            case "server":
            case "bank-server":
                runBankServer(args);
                break;
            case "client":
            case "bank-client":
                runBankClient(args);
                break;
            case "demo":
            case "bank-demo":
                runBankDemo();
                break;

            // ========== Banking HTTP ==========
            case "http-server":
            case "bank-http-server":
                runBankHttpServer(args);
                break;
            case "http-client":
            case "bank-http-client":
                runBankHttpClient(args);
                break;
            case "http-demo":
            case "bank-http-demo":
                runBankHttpDemo();
                break;

            // ========== Banking HTTPS ==========
            case "https-server":
            case "bank-https-server":
                runBankHttpsServer(args);
                break;
            case "https-client":
            case "bank-https-client":
                runBankHttpsClient(args);
                break;
            case "https-demo":
            case "bank-https-demo":
                runBankHttpsDemo();
                break;

            // ========== Card TCP ==========
            case "card-server":
                runCardServer(args);
                break;
            case "card-client":
                runCardClient(args);
                break;
            case "card-demo":
                runCardDemo();
                break;

            // ========== Card HTTP ==========
            case "card-http-server":
                runCardHttpServer(args);
                break;
            case "card-http-client":
                runCardHttpClient(args);
                break;
            case "card-http-demo":
                runCardHttpDemo();
                break;

            // ========== Card HTTPS ==========
            case "card-https-server":
                runCardHttpsServer(args);
                break;
            case "card-https-client":
                runCardHttpsClient(args);
                break;
            case "card-https-demo":
                runCardHttpsDemo();
                break;

            default:
                printUsage();
        }
    }

    // ==================== Banking TCP ====================

    private static void runBankServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_TCP_PORT;

        BankTcpServer server = new BankTcpServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Bank Server is running. Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runBankClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_TCP_PORT;

        BankTcpClient client = new BankTcpClient(host, port);

        try {
            client.connect();

            log.info("=== Balance Inquiry Test ===");
            client.balanceInquiry("1234567890123456789");

            log.info("=== Transfer Test ===");
            client.transfer("1234567890123456789", "9876543210987654321", 100000);

            log.info("=== Echo Test ===");
            client.echo("Hello, MCI Framework!");

            log.info("=== Heartbeat Test ===");
            client.heartbeat();

            log.info("=== All tests completed ===");

        } catch (Exception e) {
            log.error("Client error", e);
        } finally {
            client.disconnect();
        }
    }

    private static void runBankDemo() {
        int port = DemoConstants.DEFAULT_TCP_PORT;

        BankTcpServer server = new BankTcpServer(port);
        server.start();

        sleep(1000);

        BankTcpClient client = new BankTcpClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("       Banking MCI Framework Demo");
            log.info("========================================");

            log.info("\n[1] Balance Inquiry");
            client.balanceInquiry("1234567890123456789");

            log.info("\n[2] Transfer 50,000 won");
            client.transfer("1234567890123456789", "9876543210987654321", 50000);

            log.info("\n[3] Balance after transfer");
            client.balanceInquiry("1234567890123456789");
            client.balanceInquiry("9876543210987654321");

            log.info("\n[4] Echo Test");
            client.echo("Hello, MCI Framework!");

            log.info("\n[5] Heartbeat");
            client.heartbeat();

            log.info("\n========================================");
            log.info("    Banking Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ==================== Banking HTTP ====================

    private static void runBankHttpServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_HTTP_PORT;

        BankHttpServer server = new BankHttpServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Bank HTTP Server is running. Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runBankHttpClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_HTTP_PORT;

        BankHttpClient client = new BankHttpClient(host, port);

        try {
            client.connect();

            log.info("=== HTTP Balance Inquiry Test ===");
            client.balanceInquiry("1234567890123456789");

            log.info("=== HTTP Transfer Test ===");
            client.transfer("1234567890123456789", "9876543210987654321", 100000);

            log.info("=== HTTP Echo Test ===");
            client.echo("Hello, HTTP MCI Framework!");

            log.info("=== HTTP Heartbeat Test ===");
            client.heartbeat();

            log.info("=== All HTTP tests completed ===");

        } catch (Exception e) {
            log.error("HTTP Client error", e);
        } finally {
            client.disconnect();
        }
    }

    private static void runBankHttpDemo() {
        int port = DemoConstants.DEFAULT_HTTP_PORT;

        BankHttpServer server = new BankHttpServer(port);
        server.start();

        sleep(1000);

        BankHttpClient client = new BankHttpClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("     Banking HTTP MCI Framework Demo");
            log.info("========================================");

            log.info("\n[1] HTTP Balance Inquiry");
            client.balanceInquiry("1234567890123456789");

            log.info("\n[2] HTTP Transfer 50,000 won");
            client.transfer("1234567890123456789", "9876543210987654321", 50000);

            log.info("\n[3] Balance after transfer");
            client.balanceInquiry("1234567890123456789");
            client.balanceInquiry("9876543210987654321");

            log.info("\n[4] HTTP Echo Test");
            client.echo("Hello, HTTP MCI Framework!");

            log.info("\n[5] HTTP Heartbeat");
            client.heartbeat();

            log.info("\n========================================");
            log.info("  Banking HTTP Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("HTTP Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ==================== Banking HTTPS ====================

    private static void runBankHttpsServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_HTTPS_PORT;
        String keyStorePath = args.length > 2 ? args[2] : null;
        String keyStorePassword = args.length > 3 ? args[3] : null;

        BankHttpsServer server = new BankHttpsServer(port, keyStorePath, keyStorePassword);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Bank HTTPS Server is running. Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runBankHttpsClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_HTTPS_PORT;

        BankHttpsClient client = new BankHttpsClient(host, port);

        try {
            client.connect();

            log.info("=== HTTPS Balance Inquiry Test ===");
            client.balanceInquiry("1234567890123456789");

            log.info("=== HTTPS Transfer Test ===");
            client.transfer("1234567890123456789", "9876543210987654321", 100000);

            log.info("=== HTTPS Echo Test ===");
            client.echo("Hello, HTTPS MCI Framework!");

            log.info("=== HTTPS Heartbeat Test ===");
            client.heartbeat();

            log.info("=== All HTTPS tests completed ===");

        } catch (Exception e) {
            log.error("HTTPS Client error", e);
        } finally {
            client.disconnect();
        }
    }

    private static void runBankHttpsDemo() {
        int port = DemoConstants.DEFAULT_HTTPS_PORT;

        BankHttpsServer server = new BankHttpsServer(port);
        server.start();

        sleep(1000);

        BankHttpsClient client = new BankHttpsClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("    Banking HTTPS MCI Framework Demo");
            log.info("========================================");

            log.info("\n[1] HTTPS Balance Inquiry");
            client.balanceInquiry("1234567890123456789");

            log.info("\n[2] HTTPS Transfer 50,000 won");
            client.transfer("1234567890123456789", "9876543210987654321", 50000);

            log.info("\n[3] Balance after transfer");
            client.balanceInquiry("1234567890123456789");
            client.balanceInquiry("9876543210987654321");

            log.info("\n[4] HTTPS Echo Test");
            client.echo("Hello, HTTPS MCI Framework!");

            log.info("\n[5] HTTPS Heartbeat");
            client.heartbeat();

            log.info("\n========================================");
            log.info(" Banking HTTPS Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("HTTPS Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ==================== Card TCP ====================

    private static void runCardServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_CARD_TCP_PORT;

        CardTcpServer server = new CardTcpServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Card Server is running. Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runCardClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_CARD_TCP_PORT;

        CardTcpClient client = new CardTcpClient(host, port);

        try {
            client.connect();

            log.info("=== Card List Test ===");
            client.cardList("CUST001");

            log.info("=== Card Usage History Test ===");
            client.cardUsageHistory("1234567890123456", "20240101", "20240131");

            log.info("=== All Card tests completed ===");

        } catch (Exception e) {
            log.error("Card Client error", e);
        } finally {
            client.disconnect();
        }
    }

    private static void runCardDemo() {
        int port = DemoConstants.DEFAULT_CARD_TCP_PORT;

        CardTcpServer server = new CardTcpServer(port);
        server.start();

        sleep(1000);

        CardTcpClient client = new CardTcpClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("        Card MCI Framework Demo");
            log.info("========================================");

            log.info("\n[1] Card List Inquiry");
            client.cardList("CUST001");

            log.info("\n[2] Card Usage History Inquiry");
            client.cardUsageHistory("1234567890123456", "20240101", "20240131");

            log.info("\n[3] Another Customer's Card List");
            client.cardList("CUST002");

            log.info("\n========================================");
            log.info("     Card Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("Card Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ==================== Card HTTP ====================

    private static void runCardHttpServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_CARD_HTTP_PORT;

        CardHttpServer server = new CardHttpServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Card HTTP Server is running. Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runCardHttpClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_CARD_HTTP_PORT;

        CardHttpClient client = new CardHttpClient(host, port);

        try {
            client.connect();

            log.info("=== HTTP Card List Test ===");
            client.cardList("CUST001");

            log.info("=== HTTP Card Usage History Test ===");
            client.cardUsageHistory("1234567890123456", "20240101", "20240131");

            log.info("=== All HTTP Card tests completed ===");

        } catch (Exception e) {
            log.error("HTTP Card Client error", e);
        } finally {
            client.disconnect();
        }
    }

    private static void runCardHttpDemo() {
        int port = DemoConstants.DEFAULT_CARD_HTTP_PORT;

        CardHttpServer server = new CardHttpServer(port);
        server.start();

        sleep(1000);

        CardHttpClient client = new CardHttpClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("      Card HTTP MCI Framework Demo");
            log.info("========================================");

            log.info("\n[1] HTTP Card List Inquiry");
            client.cardList("CUST001");

            log.info("\n[2] HTTP Card Usage History Inquiry");
            client.cardUsageHistory("1234567890123456", "20240101", "20240131");

            log.info("\n========================================");
            log.info("   Card HTTP Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("HTTP Card Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ==================== Card HTTPS ====================

    private static void runCardHttpsServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_CARD_HTTPS_PORT;
        String keyStorePath = args.length > 2 ? args[2] : null;
        String keyStorePassword = args.length > 3 ? args[3] : null;

        CardHttpsServer server = new CardHttpsServer(port, keyStorePath, keyStorePassword);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Card HTTPS Server is running. Press Ctrl+C to stop.");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void runCardHttpsClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_CARD_HTTPS_PORT;

        CardHttpsClient client = new CardHttpsClient(host, port);

        try {
            client.connect();

            log.info("=== HTTPS Card List Test ===");
            client.cardList("CUST001");

            log.info("=== HTTPS Card Usage History Test ===");
            client.cardUsageHistory("1234567890123456", "20240101", "20240131");

            log.info("=== All HTTPS Card tests completed ===");

        } catch (Exception e) {
            log.error("HTTPS Card Client error", e);
        } finally {
            client.disconnect();
        }
    }

    private static void runCardHttpsDemo() {
        int port = DemoConstants.DEFAULT_CARD_HTTPS_PORT;

        CardHttpsServer server = new CardHttpsServer(port);
        server.start();

        sleep(1000);

        CardHttpsClient client = new CardHttpsClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("     Card HTTPS MCI Framework Demo");
            log.info("========================================");

            log.info("\n[1] HTTPS Card List Inquiry");
            client.cardList("CUST001");

            log.info("\n[2] HTTPS Card Usage History Inquiry");
            client.cardUsageHistory("1234567890123456", "20240101", "20240131");

            log.info("\n========================================");
            log.info("  Card HTTPS Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("HTTPS Card Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ==================== Utility ====================

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar demo-mci.jar <mode> [options]");
        System.out.println();
        System.out.println("Banking TCP Modes:");
        System.out.println("  bank-server [port]           Start Banking TCP server (default: 9001)");
        System.out.println("  bank-client [host] [port]    Start Banking TCP client (default: localhost:9001)");
        System.out.println("  bank-demo                    Run Banking TCP demo (server + client)");
        System.out.println();
        System.out.println("Banking HTTP Modes:");
        System.out.println("  bank-http-server [port]           Start Banking HTTP server (default: 9003)");
        System.out.println("  bank-http-client [host] [port]    Start Banking HTTP client (default: localhost:9003)");
        System.out.println("  bank-http-demo                    Run Banking HTTP demo (server + client)");
        System.out.println();
        System.out.println("Banking HTTPS Modes:");
        System.out.println("  bank-https-server [port] [keystore] [password]  Start Banking HTTPS server (default: 9443)");
        System.out.println("  bank-https-client [host] [port]                 Start Banking HTTPS client (default: localhost:9443)");
        System.out.println("  bank-https-demo                                 Run Banking HTTPS demo (server + client)");
        System.out.println();
        System.out.println("Card TCP Modes:");
        System.out.println("  card-server [port]           Start Card TCP server (default: 9011)");
        System.out.println("  card-client [host] [port]    Start Card TCP client (default: localhost:9011)");
        System.out.println("  card-demo                    Run Card TCP demo (server + client)");
        System.out.println();
        System.out.println("Card HTTP Modes:");
        System.out.println("  card-http-server [port]           Start Card HTTP server (default: 9013)");
        System.out.println("  card-http-client [host] [port]    Start Card HTTP client (default: localhost:9013)");
        System.out.println("  card-http-demo                    Run Card HTTP demo (server + client)");
        System.out.println();
        System.out.println("Card HTTPS Modes:");
        System.out.println("  card-https-server [port] [keystore] [password]  Start Card HTTPS server (default: 9444)");
        System.out.println("  card-https-client [host] [port]                 Start Card HTTPS client (default: localhost:9444)");
        System.out.println("  card-https-demo                                 Run Card HTTPS demo (server + client)");
        System.out.println();
        System.out.println("Legacy Modes (backward compatible):");
        System.out.println("  server, client, demo              -> bank-server, bank-client, bank-demo");
        System.out.println("  http-server, http-client, http-demo -> bank-http-*");
        System.out.println("  https-server, https-client, https-demo -> bank-https-*");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar demo-mci.jar bank-demo");
        System.out.println("  java -jar demo-mci.jar bank-server 9001");
        System.out.println("  java -jar demo-mci.jar card-demo");
        System.out.println("  java -jar demo-mci.jar card-http-demo");
        System.out.println();
        System.out.println("Card HTTP API Examples (curl):");
        System.out.println("  curl http://localhost:9013/health");
        System.out.println("  curl -X POST http://localhost:9013/api/cards \\");
        System.out.println("       -H \"Content-Type: application/json\" \\");
        System.out.println("       -d '{\"messageCode\":\"CRD1\",\"fields\":{\"customerId\":\"CUST001\"}}'");
        System.out.println();
        System.out.println("  curl -X POST http://localhost:9013/api/card-history \\");
        System.out.println("       -H \"Content-Type: application/json\" \\");
        System.out.println("       -d '{\"messageCode\":\"CUH1\",\"fields\":{\"cardNo\":\"1234567890123456\",\"fromDate\":\"20240101\",\"toDate\":\"20240131\"}}'");
    }
}
