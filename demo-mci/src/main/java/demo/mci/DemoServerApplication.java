package demo.mci;

import demo.mci.common.DemoConstants;
import demo.mci.tcp.TcpDemoClient;
import demo.mci.tcp.TcpDemoServer;
import lombok.extern.slf4j.Slf4j;

/**
 * 데모 애플리케이션 메인 클래스
 */
@Slf4j
public class DemoServerApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"server", "9001"};
        }

        String mode = args[0].toLowerCase();



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

    /**
     * 사용법 출력
     */
    private static void printUsage() {
        System.out.println("Usage: java -jar demo-mci.jar <mode> [options]");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  server [port]           Start TCP server (default port: 9001)");
        System.out.println("  client [host] [port]    Start TCP client (default: localhost:9001)");
        System.out.println("  demo                    Run integrated demo (server + client)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar demo-mci.jar server");
        System.out.println("  java -jar demo-mci.jar server 9001");
        System.out.println("  java -jar demo-mci.jar client localhost 9001");
        System.out.println("  java -jar demo-mci.jar demo");
    }
}
