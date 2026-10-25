package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiUsageEventTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");
    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ModelRef MODEL = new ModelRef("provider", "model-a");

    @Test
    @DisplayName("should apply builder defaults: random id, clock instant, zero tokens, zero latency, no attributes")
    void shouldApplyBuilderDefaults() {
        var event = AiUsageEvent.builder(FIXED).model(MODEL).purpose("summarize").outcome(AiCallOutcome.OK).build();

        assertThat(event.id()).isNotNull();
        assertThat(event.occurredAt()).isEqualTo(NOW);
        assertThat(event.tokens()).isEqualTo(TokenUsage.zero());
        assertThat(event.latency()).isEqualTo(Duration.ZERO);
        assertThat(event.attributes()).isEmpty();
        assertThat(event.tenantId()).isNull();
        assertThat(event.userId()).isNull();
        assertThat(event.correlationId()).isNull();
        assertThat(event.errorType()).isNull();
        assertThat(event.cost()).isNull();
        assertThat(event.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("should generate a different id for each build")
    void shouldGenerateDistinctIds() {
        var builder = AiUsageEvent.builder(FIXED).model(MODEL).purpose("p").outcome(AiCallOutcome.OK);

        assertThat(builder.build().id()).isNotEqualTo(builder.build().id());
    }

    @Test
    @DisplayName("should carry every explicitly set field")
    void shouldCarryEveryField() {
        var id = UUID.randomUUID();
        var at = Instant.parse("2025-05-05T05:05:05Z");
        var cost = new CostEstimate(CostEstimate.Status.ESTIMATED, "USD",
                new BigDecimal("0.000100"), new BigDecimal("0.000200"), new BigDecimal("0.000300"), "model-a");

        var event = AiUsageEvent.builder(FIXED)
                .id(id)
                .occurredAt(at)
                .model(MODEL)
                .purpose("  extract ")
                .outcome(AiCallOutcome.INVALID_OUTPUT)
                .tokens(TokenUsage.exact(10, 20))
                .latency(Duration.ofMillis(250))
                .tenantId("tenant-1")
                .userId("user-1")
                .correlationId("corr-1")
                .attribute("source", "BYOK_USER")
                .attributes(Map.of("turnIndex", "3"))
                .errorType(new InvalidModelOutputException("bad json"))
                .cost(cost)
                .build();

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.occurredAt()).isEqualTo(at);
        assertThat(event.purpose()).isEqualTo("extract");
        assertThat(event.outcome()).isEqualTo(AiCallOutcome.INVALID_OUTPUT);
        assertThat(event.tokens().total()).isEqualTo(30);
        assertThat(event.latency()).isEqualTo(Duration.ofMillis(250));
        assertThat(event.tenantId()).isEqualTo("tenant-1");
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.correlationId()).isEqualTo("corr-1");
        assertThat(event.attributes()).containsOnly(Map.entry("source", "BYOK_USER"), Map.entry("turnIndex", "3"));
        assertThat(event.errorType()).isEqualTo("InvalidModelOutputException");
        assertThat(event.cost()).isEqualTo(cost);
        assertThat(event.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("should copy attributes defensively and expose them unmodifiable")
    void shouldCopyAttributesDefensively() {
        var source = new HashMap<String, String>();
        source.put("k", "v");
        var event = new AiUsageEvent(UUID.randomUUID(), NOW, MODEL, "p", AiCallOutcome.OK, TokenUsage.zero(),
                Duration.ZERO, null, null, null, source, null, null);

        source.put("later", "x");

        assertThat(event.attributes()).containsOnlyKeys("k");
        assertThatThrownBy(() -> event.attributes().put("y", "z")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("should require model, purpose and outcome in the builder")
    void shouldRequireMandatoryFieldsInBuilder() {
        assertThatNullPointerException()
                .isThrownBy(() -> AiUsageEvent.builder(FIXED).purpose("p").outcome(AiCallOutcome.OK).build())
                .withMessage("model");
        assertThatNullPointerException()
                .isThrownBy(() -> AiUsageEvent.builder(FIXED).model(MODEL).outcome(AiCallOutcome.OK).build())
                .withMessage("purpose");
        assertThatNullPointerException()
                .isThrownBy(() -> AiUsageEvent.builder(FIXED).model(MODEL).purpose("p").build())
                .withMessage("outcome");
    }

    @Test
    @DisplayName("should reject blank purpose, negative latency and null attribute values")
    void shouldRejectInvalidValues() {
        var ok = AiUsageEvent.builder(FIXED).model(MODEL).outcome(AiCallOutcome.OK);

        assertThatIllegalArgumentException().isThrownBy(() -> ok.purpose("   ").build()).withMessageContaining("purpose");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ok.purpose("p").latency(Duration.ofMillis(-1)).build())
                .withMessageContaining("latency");
        assertThatNullPointerException().isThrownBy(() -> ok.attribute("k", null));
        assertThatNullPointerException().isThrownBy(() -> new AiUsageEvent(UUID.randomUUID(), NOW, MODEL, "p",
                AiCallOutcome.OK, TokenUsage.zero(), Duration.ZERO, null, null, null, null, null, null));
    }
}
