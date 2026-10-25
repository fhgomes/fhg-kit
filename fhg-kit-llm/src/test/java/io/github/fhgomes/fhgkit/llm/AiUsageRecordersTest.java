package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiUsageRecordersTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC);

    private final AiUsageEvent event = AiUsageEvent.builder(FIXED)
            .model(new ModelRef("provider", "model-a"))
            .purpose("summarize")
            .outcome(AiCallOutcome.OK)
            .tokens(TokenUsage.exact(10, 5))
            .latency(Duration.ofMillis(42))
            .tenantId("t")
            .attribute("promptVersion", "v1")
            .cost(new CostEstimate(CostEstimate.Status.ESTIMATED, "USD",
                    new BigDecimal("0.000030"), new BigDecimal("0.000050"), new BigDecimal("0.000080"), "model-a"))
            .build();

    @Test
    @DisplayName("noop should accept events and do nothing")
    void noopShouldDoNothing() {
        assertThatCode(() -> AiUsageRecorders.noop().record(event)).doesNotThrowAnyException();
        assertThatNullPointerException().isThrownBy(() -> AiUsageRecorders.noop().record(null));
    }

    @Test
    @DisplayName("logging should not throw, with or without cost")
    void loggingShouldNotThrow() {
        var withoutCost = AiUsageEvent.builder(FIXED).model(ModelRef.unknown("p")).purpose("p")
                .outcome(AiCallOutcome.FAILED_UPSTREAM).errorType("IOException").build();

        assertThatCode(() -> AiUsageRecorders.logging().record(event)).doesNotThrowAnyException();
        assertThatCode(() -> AiUsageRecorders.logging().record(withoutCost)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("composite should call every delegate in order")
    void compositeShouldCallAll() {
        var calls = new ArrayList<String>();
        var composite = AiUsageRecorders.composite(e -> calls.add("first"), e -> calls.add("second"), e -> calls.add("third"));

        composite.record(event);

        assertThat(calls).containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("composite should still call the remaining delegates when one throws, then rethrow the first error")
    void compositeShouldContinueAfterFailureThenRethrow() {
        var calls = new ArrayList<String>();
        var composite = AiUsageRecorders.composite(
                e -> { throw new IllegalStateException("first broke"); },
                e -> calls.add("second"),
                e -> { throw new IllegalStateException("third broke"); });

        assertThatIllegalStateException().isThrownBy(() -> composite.record(event))
                .withMessage("first broke")
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));
        assertThat(calls).containsExactly("second");
    }

    @Test
    @DisplayName("composite should reject null delegates")
    void compositeShouldRejectNullDelegate() {
        assertThatNullPointerException().isThrownBy(() -> AiUsageRecorders.composite(AiUsageRecorders.noop(), null));
    }

    @Test
    @DisplayName("safe should swallow runtime exceptions from the delegate and keep working")
    void safeShouldSwallowAndContinue() {
        List<AiUsageEvent> received = new ArrayList<>();
        var flaky = new AiUsageRecorder() {
            int calls;

            @Override
            public void record(AiUsageEvent e) {
                if (++calls == 1) {
                    throw new IllegalStateException("ledger down");
                }
                received.add(e);
            }
        };
        var safe = AiUsageRecorders.safe(flaky);

        assertThatCode(() -> safe.record(event)).doesNotThrowAnyException();
        safe.record(event);

        assertThat(received).containsExactly(event);
    }

    @Test
    @DisplayName("safe should not wrap twice")
    void safeShouldNotWrapTwice() {
        var once = AiUsageRecorders.safe(e -> { });

        assertThat(AiUsageRecorders.safe(once)).isSameAs(once);
        assertThat(AiUsageRecorders.safe(AiUsageRecorders.noop())).isSameAs(AiUsageRecorders.noop());
    }
}
