package securebank.util;

import securebank.exception.InputClosedException;

import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Scanner;

/**
 * All console input and output. Output is encoded (control characters removed) so stored data cannot
 * inject terminal escape codes. Input lines are capped at 256 characters. Passwords are hidden
 * when a real terminal is available.
 */
public final class ConsoleUtil {

    public static final int MAX_INPUT_LENGTH = 256;

    private final Scanner scanner;
    private final PrintStream out;
    private final Console console; // null when there is no real terminal (e.g. IntelliJ Run window)

    public ConsoleUtil(Scanner scanner, PrintStream out, Console console) {
        this.scanner = scanner;
        this.out = out;
        this.console = console;
    }

    public static ConsoleUtil forSystem() {
        return new ConsoleUtil(new Scanner(System.in), System.out, realTerminalOrNull());
    }

    //JDK 22+ returns a Console even when redirected isTerminal() tells the two apart
    private static Console realTerminalOrNull() {
        Console c = System.console();
        if (c == null) {
            return null;
        }
        try {
            Method isTerminal = Console.class.getMethod("isTerminal");
            return Boolean.TRUE.equals(isTerminal.invoke(c)) ? c : null;
        } catch (NoSuchMethodException olderJdk) {
            return c;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    public boolean isPasswordHidden() {
        return console != null;
    }

    public void printLine(String message) {
        out.println(encode(message));
    }

    // Replaces control characters and invisible direction overrides with '?'.
    static String encode(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder safe = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            boolean unsafe = Character.isISOControl(cp) || !Character.isDefined(cp)
                    || (cp >= 0x202A && cp <= 0x202E) || (cp >= 0x2066 && cp <= 0x2069)
                    || cp == 0x200E || cp == 0x200F || cp == 0x2028 || cp == 0x2029;
            safe.appendCodePoint(unsafe ? '?' : cp);
        });
        return safe.toString();
    }

    //@throws InputClosedException if the input has ended
    public String readLine(String prompt) {
        String line;
        if (console != null) {
            line = console.readLine("%s", prompt);
        } else {
            out.print(prompt);
            out.flush();
            line = scanner.hasNextLine() ? scanner.nextLine() : null;
        }
        if (line == null) {
            throw new InputClosedException();
        }
        return line.length() > MAX_INPUT_LENGTH ? line.substring(0, MAX_INPUT_LENGTH) : line;
    }

    /** Returns a char[] so the caller can wipe it after use. */
    public char[] readPassword(String prompt) {
        if (console != null) {
            char[] password = console.readPassword("%s", prompt);
            if (password == null) {
                throw new InputClosedException();
            }
            return password;
        }
        return readLine(prompt).toCharArray();
    }
}
