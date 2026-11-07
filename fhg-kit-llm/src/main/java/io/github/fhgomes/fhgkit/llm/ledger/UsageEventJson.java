package io.github.fhgomes.fhgkit.llm.ledger;

import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import io.github.fhgomes.fhgkit.llm.CostEstimate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Hand-written JSON for the ledger's ingest contract: a writer for a batch of {@link AiUsageEvent}s and a small
 * parser for the answer. It exists so the kit needs no JSON library; it only knows these two shapes.
 *
 * <p>Writer: field order and names follow the contract, money is a plain decimal string (never an exponent),
 * absent values are written as {@code null}, attributes are sorted by key so the output is deterministic.
 */
final class UsageEventJson {

    private static final int MAX_DEPTH = 16;
    private static final int MAX_NUMBER_LENGTH = 20;

    private UsageEventJson() {
    }

    // ---- writer ----------------------------------------------------------------------------------------------

    /** Serializes {@code {"events":[...]}}. */
    static String write(List<AiUsageEvent> events) {
        StringBuilder out = new StringBuilder(256 * Math.max(1, events.size()));
        out.append("{\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeEvent(out, events.get(i));
        }
        return out.append("]}").toString();
    }

    private static void writeEvent(StringBuilder out, AiUsageEvent e) {
        out.append("{\"id\":");
        string(out, e.id().toString());
        out.append(",\"occurredAt\":");
        string(out, e.occurredAt().toString());
        out.append(",\"model\":{\"provider\":");
        string(out, e.model().provider());
        out.append(",\"modelId\":");
        string(out, e.model().modelId());
        out.append("},\"purpose\":");
        string(out, e.purpose());
        out.append(",\"outcome\":");
        string(out, e.outcome().name());
        out.append(",\"tokens\":{\"input\":").append(e.tokens().input())
                .append(",\"output\":").append(e.tokens().output())
                .append(",\"estimated\":").append(e.tokens().estimated()).append('}');
        out.append(",\"latencyMs\":").append(e.latency().toMillis());
        out.append(",\"tenantId\":");
        nullableString(out, e.tenantId());
        out.append(",\"userId\":");
        nullableString(out, e.userId());
        out.append(",\"correlationId\":");
        nullableString(out, e.correlationId());
        out.append(",\"attributes\":{");
        boolean first = true;
        for (Map.Entry<String, String> a : new TreeMap<>(e.attributes()).entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            string(out, a.getKey());
            out.append(':');
            string(out, a.getValue());
        }
        out.append("},\"errorType\":");
        nullableString(out, e.errorType());
        out.append(",\"cost\":");
        CostEstimate c = e.cost();
        if (c == null) {
            out.append("null");
        } else {
            out.append("{\"status\":");
            string(out, c.status().name());
            out.append(",\"currency\":");
            string(out, c.currency());
            out.append(",\"input\":");
            string(out, c.input().toPlainString());
            out.append(",\"output\":");
            string(out, c.output().toPlainString());
            out.append(",\"total\":");
            string(out, c.total().toPlainString());
            out.append(",\"pricedAs\":");
            nullableString(out, c.pricedAs());
            out.append('}');
        }
        out.append('}');
    }

    private static void nullableString(StringBuilder out, @Nullable String value) {
        if (value == null) {
            out.append("null");
        } else {
            string(out, value);
        }
    }

    /**
     * Writes a JSON string. Escapes quote, backslash, every control character, U+2028/U+2029 and unpaired
     * surrogates (which UTF-8 cannot carry); everything else, including paired surrogates, is written as is.
     */
    private static void string(StringBuilder out, String s) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (Character.isHighSurrogate(ch) && i + 1 < s.length()
                            && Character.isLowSurrogate(s.charAt(i + 1))) {
                        out.append(ch).append(s.charAt(++i));
                    } else if (ch < 0x20 || ch == 0x7f || ch == '\u2028' || ch == '\u2029'
                            || Character.isSurrogate(ch)) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }

    // ---- parser ----------------------------------------------------------------------------------------------

    /**
     * Reads the 202 answer: {@code accepted}, {@code duplicates} and {@code rejected[]} of {@code {id, reason}}.
     * Other fields are ignored.
     *
     * @throws IllegalArgumentException when the body is not valid JSON or lacks one of those fields
     */
    static SendResult parseResponse(int httpStatus, String body) {
        Object root = new Reader(body).readDocument();
        if (!(root instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("response is not a JSON object");
        }
        int accepted = intField(map, "accepted");
        int duplicates = intField(map, "duplicates");
        List<SendResult.Rejected> rejected = new ArrayList<>();
        if (!(map.get("rejected") instanceof List<?> list)) {
            throw new IllegalArgumentException("response field 'rejected' is missing or not an array");
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> r) || !(r.get("id") instanceof String id)
                    || !(r.get("reason") instanceof String reason)) {
                throw new IllegalArgumentException("response field 'rejected' has a malformed entry");
            }
            rejected.add(new SendResult.Rejected(UUID.fromString(id), reason));
        }
        return new SendResult(httpStatus, accepted, duplicates, rejected);
    }

    private static int intField(Map<?, ?> map, String name) {
        if (!(map.get(name) instanceof BigDecimal n)) {
            throw new IllegalArgumentException("response field '" + name + "' is missing or not a number");
        }
        try {
            return n.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("response field '" + name + "' is not an int");
        }
    }

    /** Recursive-descent JSON reader producing Map, List, String, BigDecimal, Boolean or null. */
    private static final class Reader {

        private final String s;
        private int pos;

        Reader(String s) {
            this.s = s;
        }

        Object readDocument() {
            Object value = readValue(0);
            skipWhitespace();
            if (pos != s.length()) {
                throw error("trailing content");
            }
            return value;
        }

        private @Nullable Object readValue(int depth) {
            if (depth > MAX_DEPTH) {
                throw error("nesting too deep");
            }
            skipWhitespace();
            if (pos >= s.length()) {
                throw error("unexpected end");
            }
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> readObject(depth);
                case '[' -> readArray(depth);
                case '"' -> readString();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> readNumber();
            };
        }

        private Map<String, @Nullable Object> readObject(int depth) {
            Map<String, @Nullable Object> map = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw error("expected a field name");
                }
                String key = readString();
                skipWhitespace();
                expect(':');
                map.put(key, readValue(depth + 1));
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else {
                    expect('}');
                    return map;
                }
            }
        }

        private List<@Nullable Object> readArray(int depth) {
            List<@Nullable Object> list = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(readValue(depth + 1));
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        private String readString() {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c < 0x20) {
                    throw error("control character in string");
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (pos >= s.length()) {
                    break;
                }
                char esc = s.charAt(pos++);
                switch (esc) {
                    case '"', '\\', '/' -> sb.append(esc);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw error("truncated unicode escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("bad unicode escape");
                        }
                        pos += 4;
                    }
                    default -> throw error("bad escape");
                }
            }
            throw error("unterminated string");
        }

        private BigDecimal readNumber() {
            int start = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
            if (start == pos) {
                throw error("unexpected character");
            }
            if (pos - start > MAX_NUMBER_LENGTH) {
                throw error("number too long");
            }
            try {
                return new BigDecimal(s.substring(start, pos));
            } catch (NumberFormatException ex) {
                throw error("bad number");
            }
        }

        private @Nullable Object literal(String word, @Nullable Object value) {
            if (!s.startsWith(word, pos)) {
                throw error("unexpected character");
            }
            pos += word.length();
            return value;
        }

        private void expect(char c) {
            if (!peek(c)) {
                throw error("expected '" + c + "'");
            }
            pos++;
        }

        private boolean peek(char c) {
            return pos < s.length() && s.charAt(pos) == c;
        }

        private void skipWhitespace() {
            while (pos < s.length() && " \t\r\n".indexOf(s.charAt(pos)) >= 0) {
                pos++;
            }
        }

        private IllegalArgumentException error(String what) {
            return new IllegalArgumentException("invalid JSON at offset " + pos + ": " + what);
        }
    }
}
