package securebank.test;

// Runs all test suites and prints a summary. Exit code 0 means every test passed
public final class TestRunner {

    @FunctionalInterface
    public interface Body {
        void run() throws Exception;
    }

    private static int passed;
    private static int failed;

    private TestRunner() {
    }

    static void suite(String name) {
        System.out.println();
        System.out.println("== " + name);
    }

    static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  FAIL  " + name + "  ->  " + t);
        }
    }

    public static void main(String[] args) {
        AccountTests.run();
        InputValidatorTests.run();
        PasswordHasherTests.run();
        RecordCodecTests.run();
        AuthenticationServiceTests.run();
        BankingServiceTests.run();
        PersistenceTests.run();
        SecurityOutputTests.run();
        System.out.println();
        System.out.println("Tests passed: " + passed + ", failed: " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
