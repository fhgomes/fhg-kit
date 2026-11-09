package io.github.fhgomes.fhgkit.llm.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fhgomes.fhgkit.llm.AiCallOutcome;
import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import io.github.fhgomes.fhgkit.llm.CostEstimate;
import io.github.fhgomes.fhgkit.llm.ModelRef;
import io.github.fhgomes.fhgkit.llm.TokenUsage;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsageEventJsonTest {

    private static final UUID ID = UUID.fromString("8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11");
    private static final Instant AT = Instant.parse("2026-11-01T03:15:58.198Z");

    private static AiUsageEvent.Builder base() {
        return AiUsageEvent.builder()
                .id(ID)
                .occurredAt(AT)
                .model(new ModelRef("openai-compatible", "model-a"))
                .purpose("REVALIDATE")
                .outcome(AiCallOutcome.OK);
    }

    @Test
    @DisplayName("should write a full event exactly as the contract shows it")
    void shouldWriteGoldenFullEvent() {
        var event = base()
                .tokens(TokenUsage.exact(38762, 106))
                .latency(Duration.ofMillis(12388))
                .attribute("operation", "deadline")
                .attribute("promptVersion", "revalidate.v1")
                .attribute("entityType", "event")
                .cost(new CostEstimate(CostEstimate.Status.ESTIMATED, "USD",
                        new BigDecimal("0.116286"), new BigDecimal("0.001590"), new BigDecimal("0.117876"), "model-a"))
                .build();

        assertThat(UsageEventJson.write(List.of(event))).isEqualTo("{\"events\":[{"
                + "\"id\":\"8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11\","
                + "\"occurredAt\":\"2026-11-01T03:15:58.198Z\","
                + "\"model\":{\"provider\":\"openai-compatible\",\"modelId\":\"model-a\"},"
                + "\"purpose\":\"REVALIDATE\",\"outcome\":\"OK\","
                + "\"tokens\":{\"input\":38762,\"output\":106,\"estimated\":false},"
                + "\"latencyMs\":12388,"
                + "\"tenantId\":null,\"userId\":null,\"correlationId\":null,"
                + "\"attributes\":{\"entityType\":\"event\",\"operation\":\"deadline\",\"promptVersion\":\"revalidate.v1\"},"
                + "\"errorType\":null,"
                + "\"cost\":{\"status\":\"ESTIMATED\",\"currency\":\"USD\",\"input\":\"0.116286\",\"output\":\"0.001590\","
                + "\"total\":\"0.117876\",\"pricedAs\":\"model-a\"}"
                + "}]}");
    }

    @Test
    @DisplayName("should write nulls for absent optional parts and an empty attributes object")
    void shouldWriteNullsForAbsentParts() {
        var event = base().outcome(AiCallOutcome.FAILED_TIMEOUT).errorType("HttpTimeoutException")
                .tenantId("t-1").userId("u-1").correlationId("c-1")
                .tokens(TokenUsage.estimated(0, 0)).build();

        assertThat(UsageEventJson.write(List.of(event))).isEqualTo("{\"events\":[{"
                + "\"id\":\"8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11\","
                + "\"occurredAt\":\"2026-11-01T03:15:58.198Z\","
                + "\"model\":{\"provider\":\"openai-compatible\",\"modelId\":\"model-a\"},"
                + "\"purpose\":\"REVALIDATE\",\"outcome\":\"FAILED_TIMEOUT\","
                + "\"tokens\":{\"input\":0,\"output\":0,\"estimated\":true},"
                + "\"latencyMs\":0,"
                + "\"tenantId\":\"t-1\",\"userId\":\"u-1\",\"correlationId\":\"c-1\","
                + "\"attributes\":{},"
                + "\"errorType\":\"HttpTimeoutException\","
                + "\"cost\":null"
                + "}]}");
    }

    @Test
    @DisplayName("should write money as plain decimal strings, never with an exponent")
    void shouldWriteMoneyWithoutExponent() {
        var cost = new CostEstimate(CostEstimate.Status.UNKNOWN_MODEL, "USD",
                new BigDecimal("0E-8"), new BigDecimal("1E+3"), new BigDecimal("1000.000000"), null);

        var json = UsageEventJson.write(List.of(base().cost(cost).build()));

        assertThat(json).contains("\"input\":\"0.00000000\"", "\"output\":\"1000\"", "\"total\":\"1000.000000\"",
                "\"pricedAs\":null", "\"status\":\"UNKNOWN_MODEL\"");
    }

    @Test
    @DisplayName("should write several events in order, comma separated, and an empty array for none")
    void shouldWriteBatches() {
        var second = base().id(UUID.fromString("00000000-0000-0000-0000-000000000002")).build();

        var json = UsageEventJson.write(List.of(base().build(), second));

        assertThat(json).startsWith("{\"events\":[{\"id\":\"8f0c1d7e").contains("},{\"id\":\"00000000-0000-0000-0000-000000000002\"");
        assertThat(UsageEventJson.write(List.of())).isEqualTo("{\"events\":[]}");
    }

    @Test
    @DisplayName("should escape quotes, backslashes and every control character")
    void shouldEscapeControlCharacters() {
        var event = base().correlationId("a\"b\\c\n\r\t\b\f\u0001\u001f\u007f").build();

        assertThat(UsageEventJson.write(List.of(event)))
                .contains("\"correlationId\":\"a\\\"b\\\\c\\n\\r\\t\\b\\f\\u0001\\u001f\\u007f\"");
    }

    @Test
    @DisplayName("should keep accents, CJK and paired surrogates raw, and escape unpaired surrogates and line separators")
    void shouldHandleUnicode() {
        var event = base().correlationId("a\u00E7\u00E3o \u65E5\u672C \uD83D\uDE00").userId("x\uD800y\uDC00z").tenantId("l\u2028s\u2029").build();

        var json = UsageEventJson.write(List.of(event));

        assertThat(json).contains("\"correlationId\":\"a\u00E7\u00E3o \u65E5\u672C \uD83D\uDE00\"");
        assertThat(json).contains("\"userId\":\"x\\ud800y\\udc00z\"");
        assertThat(json).contains("\"tenantId\":\"l\\u2028s\\u2029\"");
    }

    @Test
    @DisplayName("should escape attribute keys and values the same way")
    void shouldEscapeAttributes() {
        var event = base().attributes(Map.of("k\"ey", "v\nal")).build();

        assertThat(UsageEventJson.write(List.of(event))).contains("\"attributes\":{\"k\\\"ey\":\"v\\nal\"}");
    }

    @Test
    @DisplayName("should write the same bytes for the same event regardless of attribute insertion order")
    void shouldBeDeterministic() {
        var a = base().attribute("b", "2").attribute("a", "1").build();
        var b = base().attribute("a", "1").attribute("b", "2").build();

        assertThat(UsageEventJson.write(List.of(a))).isEqualTo(UsageEventJson.write(List.of(b)));
    }

    @Test
    @DisplayName("should parse the contract's 202 body")
    void shouldParseAcceptedBody() {
        var body = "{ \"accepted\": 2, \"duplicates\": 1, \"rejected\": [ "
                + "{ \"id\": \"8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11\", \"reason\": \"OCCURRED_AT_IN_FUTURE\" } ] }";

        var result = UsageEventJson.parseResponse(202, body);

        assertThat(result.httpStatus()).isEqualTo(202);
        assertThat(result.accepted()).isEqualTo(2);
        assertThat(result.duplicates()).isEqualTo(1);
        assertThat(result.rejected()).containsExactly(new SendResult.Rejected(ID, "OCCURRED_AT_IN_FUTURE"));
    }

    @Test
    @DisplayName("should ignore unknown fields of any shape, and read escapes inside strings")
    void shouldIgnoreUnknownFields() {
        var body = "{\"extra\":{\"a\":[1,2.5e3,true,false,null,\"x\\u00e7\\n\"]},\"accepted\":0,\"duplicates\":0,\"rejected\":[],\"note\":\"ok\"}";

        var result = UsageEventJson.parseResponse(202, body);

        assertThat(result).isEqualTo(new SendResult(202, 0, 0, List.of()));
    }

    @Test
    @DisplayName("should reject bodies that are not the expected object")
    void shouldRejectUnexpectedBodies() {
        for (String body : List.of("", "not json", "[]", "{\"accepted\":1}", "{\"accepted\":1,\"duplicates\":0}",
                "{\"accepted\":\"1\",\"duplicates\":0,\"rejected\":[]}",
                "{\"accepted\":1.5,\"duplicates\":0,\"rejected\":[]}",
                "{\"accepted\":1,\"duplicates\":0,\"rejected\":[{\"id\":\"not-a-uuid\",\"reason\":\"X\"}]}",
                "{\"accepted\":1,\"duplicates\":0,\"rejected\":[{\"reason\":\"X\"}]}",
                "{\"accepted\":1,\"duplicates\":0,\"rejected\":[]} trailing",
                "{\"accepted\":1,\"duplicates\":0,\"rejected\":[],}",
                "{\"accepted\":1e999999999,\"duplicates\":0,\"rejected\":[]}",
                "{\"accepted\":-1,\"duplicates\":0,\"rejected\":[]}",
                "{\"accepted\":1,\"duplicates\":0,\"rejected\":[\"unterminated]}")) {
            assertThatIllegalArgumentException().as(body).isThrownBy(() -> UsageEventJson.parseResponse(202, body));
        }
    }

    @Test
    @DisplayName("should refuse absurd nesting instead of overflowing the stack")
    void shouldRefuseDeepNesting() {
        var body = "{\"x\":" + "[".repeat(10_000) + "]".repeat(10_000) + "}";

        assertThatIllegalArgumentException().isThrownBy(() -> UsageEventJson.parseResponse(202, body))
                .withMessageContaining("nesting too deep");
    }

    @Test
    @DisplayName("should write tokens.cacheRead and cacheWrite only when above zero")
    void shouldWriteCachePartsOnlyWhenPositive() {
        String none = UsageEventJson.write(List.of(base().tokens(TokenUsage.exact(10, 2)).build()));
        String both = UsageEventJson.write(List.of(base().tokens(TokenUsage.exact(36000, 100, 35000, 900)).build()));
        String readOnly = UsageEventJson.write(List.of(base().tokens(TokenUsage.exact(36000, 100, 35000, 0)).build()));
        String writeOnly = UsageEventJson.write(List.of(base().tokens(TokenUsage.exact(36000, 100, 0, 900)).build()));

        assertThat(none).contains("\"tokens\":{\"input\":10,\"output\":2,\"estimated\":false},")
                .doesNotContain("cacheRead").doesNotContain("cacheWrite");
        assertThat(both).contains(
                "\"tokens\":{\"input\":36000,\"output\":100,\"estimated\":false,\"cacheRead\":35000,\"cacheWrite\":900},");
        assertThat(readOnly).contains("\"estimated\":false,\"cacheRead\":35000}").doesNotContain("cacheWrite");
        assertThat(writeOnly).contains("\"estimated\":false,\"cacheWrite\":900}").doesNotContain("cacheRead");
    }
}
