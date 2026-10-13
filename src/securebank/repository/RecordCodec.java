package securebank.repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns field values into one text line and back values are never concatenated into a record, so a
 * '|' or line break inside a value can never create extra fields or records
 * Escapes: {@code \\} backslash, {@code \p} pipe, {@code \n} line feed, {@code \r} carriage return
 */
public final class RecordCodec {

    private RecordCodec() {
    }

    public static String encode(List<String> fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                line.append('|');
            }
            for (char c : fields.get(i).toCharArray()) {
                switch (c) {
                    case '\\': line.append("\\\\"); break;
                    case '|':  line.append("\\p"); break;
                    case '\n': line.append("\\n"); break;
                    case '\r': line.append("\\r"); break;
                    default:   line.append(c);
                }
            }
        }
        return line.toString();
    }

    // @throws IllegalArgumentException if the line has an invalid escape sequence
    public static List<String> decode(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '|') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c == '\\') {
                if (++i >= line.length()) {
                    throw new IllegalArgumentException("Dangling escape.");
                }
                switch (line.charAt(i)) {
                    case '\\': current.append('\\'); break;
                    case 'p':  current.append('|'); break;
                    case 'n':  current.append('\n'); break;
                    case 'r':  current.append('\r'); break;
                    default:   throw new IllegalArgumentException("Unknown escape.");
                }
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
