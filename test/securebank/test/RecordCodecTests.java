package securebank.test;

import securebank.repository.RecordCodec;

import java.util.Arrays;
import java.util.List;

import static securebank.test.Assert.*;
import static securebank.test.TestRunner.suite;
import static securebank.test.TestRunner.test;

final class RecordCodecTests {

    private RecordCodecTests() {
    }

    static void run() {
        suite("RecordCodec (injection defence for the data files)");

        test("plain and empty values round-trip", () -> {
            List<String> fields = Arrays.asList("alice", "", "100.00");
            assertEquals(fields, RecordCodec.decode(RecordCodec.encode(fields)), "round trip");
        });

        test("a pipe inside a value cannot create an extra field", () -> {
            List<String> fields = Arrays.asList("evil|ADMIN|0.00", "next");
            assertEquals(fields, RecordCodec.decode(RecordCodec.encode(fields)), "still two fields");
        });

        test("a line break inside a value cannot create an extra record", () -> {
            List<String> fields = Arrays.asList("x\nfake|record", "y\r\nz");
            String line = RecordCodec.encode(fields);
            assertFalse(line.contains("\n") || line.contains("\r"), "one line only");
            assertEquals(fields, RecordCodec.decode(line), "value preserved");
        });

        test("hostile text and backslashes are just data", () -> {
            List<String> fields = Arrays.asList("'; DROP TABLE users; --", "<script>x</script>", "C:\\temp\\new", "\\p");
            assertEquals(fields, RecordCodec.decode(RecordCodec.encode(fields)), "round trip");
        });

        test("corrupt escapes are rejected", () -> {
            assertThrows(IllegalArgumentException.class, () -> RecordCodec.decode("abc\\"), "dangling");
            assertThrows(IllegalArgumentException.class, () -> RecordCodec.decode("a\\qb"), "unknown");
        });
    }
}
