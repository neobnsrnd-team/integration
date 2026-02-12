package demo.mci;

import demo.mci.common.DemoConstants;
import demo.mci.http.HttpDemoClient;
import demo.mci.http.HttpDemoServer;
import demo.mci.tcp.TcpDemoClient;
import demo.mci.tcp.TcpDemoServer;
import lombok.extern.slf4j.Slf4j;

/**
 * 데모 애플리케이션 메인 클래스
 */
@Slf4j
public class DemoApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            args=new String[]{"demo"};
            //return;
        }

        String mode = args[0].toLowerCase();

        if (mode==null) mode="server";

        switch (mode) {
            case "server":
                runServer(args);
                break;
            case "client":
                runClient(args);
                break;
            case "demo":
                runDemo();
                break;
            case "http-server":
                runHttpServer(args);
                break;
            case "http-client":
                runHttpClient(args);
                break;
            case "http-demo":
                runHttpDemo();
                break;
            default:
                printUsage();
        }
    }

    /**
     * 서버 실행
     */
    private static void runServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_TCP_PORT;

        TcpDemoServer server = new TcpDemoServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("Server is running. Press Ctrl+C to stop.");

        // 서버 대기
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 클라이언트 실행
     */
    private static void runClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_TCP_PORT;

        TcpDemoClient client = new TcpDemoClient(host, port);

        try {
            client.connect();

            // 기본 테스트 시나리오
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

    /**
     * 통합 데모 실행 (서버 + 클라이언트)
     */
    private static void runDemo() {
        int port = DemoConstants.DEFAULT_TCP_PORT;

        // 서버 시작
        TcpDemoServer server = new TcpDemoServer(port);
        server.start();

        // 잠시 대기
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 클라이언트 테스트
        TcpDemoClient client = new TcpDemoClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("         MCI Framework Demo");
            log.info("========================================");

            // 1. 잔액조회
            log.info("\n[1] Balance Inquiry");
            client.balanceInquiry("1234567890123456789");

            // 2. 이체
            log.info("\n[2] Transfer 50,000 won");
            client.transfer("1234567890123456789", "9876543210987654321", 50000);

            // 3. 이체 후 잔액 확인
            log.info("\n[3] Balance after transfer");
            client.balanceInquiry("1234567890123456789");
            client.balanceInquiry("9876543210987654321");

            // 4. 에코 테스트
            log.info("\n[4] Echo Test");
            client.echo("Hello, MCI Framework!");

            // 5. 하트비트
            log.info("\n[5] Heartbeat");
            client.heartbeat();

            log.info("\n========================================");
            log.info("         Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    // ========== HTTP 모드 ==========

    /**
     * HTTP 서버 실행
     */
    private static void runHttpServer(String[] args) {
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DemoConstants.DEFAULT_HTTP_PORT;

        HttpDemoServer server = new HttpDemoServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        log.info("HTTP Server is running. Press Ctrl+C to stop.");

        // 서버 대기
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * HTTP 클라이언트 실행
     */
    private static void runHttpClient(String[] args) {
        String host = args.length > 1 ? args[1] : DemoConstants.DEFAULT_HOST;
        int port = args.length > 2 ? Integer.parseInt(args[2]) : DemoConstants.DEFAULT_HTTP_PORT;

        HttpDemoClient client = new HttpDemoClient(host, port);

        try {
            client.connect();

            // 기본 테스트 시나리오
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

    /**
     * HTTP 통합 데모 실행 (서버 + 클라이언트)
     */
    private static void runHttpDemo() {
        int port = DemoConstants.DEFAULT_HTTP_PORT;

        // 서버 시작
        HttpDemoServer server = new HttpDemoServer(port);
        server.start();

        // 잠시 대기
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 클라이언트 테스트
        HttpDemoClient client = new HttpDemoClient(DemoConstants.DEFAULT_HOST, port);

        try {
            client.connect();

            log.info("========================================");
            log.info("       HTTP MCI Framework Demo");
            log.info("========================================");

            // 1. 잔액조회
            log.info("\n[1] HTTP Balance Inquiry");
            client.balanceInquiry("1234567890123456789");

            // 2. 이체
            log.info("\n[2] HTTP Transfer 50,000 won");
            client.transfer("1234567890123456789", "9876543210987654321", 50000);

            // 3. 이체 후 잔액 확인
            log.info("\n[3] Balance after transfer");
            client.balanceInquiry("1234567890123456789");
            client.balanceInquiry("9876543210987654321");

            // 4. 에코 테스트
            log.info("\n[4] HTTP Echo Test");
            client.echo("Hello, HTTP MCI Framework!");

            // 5. 하트비트
            log.info("\n[5] HTTP Heartbeat");
            client.heartbeat();

            log.info("\n========================================");
            log.info("      HTTP Demo completed successfully");
            log.info("========================================");

        } catch (Exception e) {
            log.error("HTTP Demo error", e);
        } finally {
            client.disconnect();
            server.stop();
        }
    }

    /**
     * 사용법 출력
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar demo-mci.jar <mode> [options]");
        System.out.println();
        System.out.println("TCP Modes:");
        System.out.println("  server [port]           Start TCP server (default port: 9001)");
        System.out.println("  client [host] [port]    Start TCP client (default: localhost:9001)");
        System.out.println("  demo                    Run integrated TCP demo (server + client)");
        System.out.println();
        System.out.println("HTTP Modes:");
        System.out.println("  http-server [port]           Start HTTP server (default port: 9003)");
        System.out.println("  http-client [host] [port]    Start HTTP client (default: localhost:9003)");
        System.out.println("  http-demo                    Run integrated HTTP demo (server + client)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar demo-mci.jar server");
        System.out.println("  java -jar demo-mci.jar server 9001");
        System.out.println("  java -jar demo-mci.jar client localhost 9001");
        System.out.println("  java -jar demo-mci.jar demo");
        System.out.println();
        System.out.println("  java -jar demo-mci.jar http-server");
        System.out.println("  java -jar demo-mci.jar http-server 9003");
        System.out.println("  java -jar demo-mci.jar http-client localhost 9003");
        System.out.println("  java -jar demo-mci.jar http-demo");
        System.out.println();
        System.out.println("HTTP API Examples (curl):");
        System.out.println("  curl http://localhost:9003/health");
        System.out.println("  curl -X POST http://localhost:9003/api/balance \\");
        System.out.println("       -H \"Content-Type: application/json\" \\");
        System.out.println("       -d '{\"messageCode\":\"BAL1\",\"fields\":{\"accountNo\":\"1234567890123456789\"}}'");
    }
}
