package io.github.fhgomes.fhgkit.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiUsageTrackerTest {

    private static final Instant START = Instant.parse("2026-01-02T03:04:05Z");
    private static final ModelRef REQUESTED = new ModelRef("provider", "model-a");
    private static final ModelRef SERVED = new ModelRef("provider", "model-a-snapshot");

    /** A clock that advances by a fixed step on every read, so latency is deterministic. */
    private static final class SteppingClock extends Clock {
        private Instant now;
        private final Duration step;

        SteppingClock(Instant start, Duration step) {
            this.now = start;
            this.step = step;
        }

        @Override
        public Instant instant() {
            Instant current = now;
            now = now.plus(step);
            return current;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final List<AiUsageEvent> recorded = new ArrayList<>();
    private final AiUsageRecorder recorder = recorded::add;
    private final CostEstimator estimator = new DefaultCostEstimator(new InMemoryPriceTable(
            List.of(ModelPrice.of("model-a-snapshot", new BigDecimal("2.00"), new BigDecimal("8.00"))),
            "USD", "unit-test", LocalDate.of(2026, 1, 1)));
    private final AiCallContext ctx = AiCallContext.builder("summarize")
            .tenantId("tenant-1").userId("user-1").correlationId("corr-1")
            .attribute("source", "BYOK_USER")
            .build();

    private AiUsageTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new AiUsageTracker(recorder, estimator, new SteppingClock(START, Duration.ofMillis(100)));
    }

    @Test
    @DisplayName("should return the value and record OK with served model, tokens, latency, context and cost")
    void shouldRecordSuccess() throws Exception {
        String value = tracker.track(ctx, REQUESTED, () -> AiCallResult.of("answer", SERVED, TokenUsage.exact(1_000, 500)));

        assertThat(value).isEqualTo("answer");
        assertThat(recorded).hasSize(1);
        var event = recorded.get(0);
        assertThat(event.outcome()).isEqualTo(AiCallOutcome.OK);
        assertThat(event.model()).isEqualTo(SERVED);
        assertThat(event.tokens()).isEqualTo(TokenUsage.exact(1_000, 500));
        assertThat(event.occurredAt()).isEqualTo(START);
        assertThat(event.latency()).isEqualTo(Duration.ofMillis(100));
        assertThat(event.purpose()).isEqualTo("summarize");
        assertThat(event.tenantId()).isEqualTo("tenant-1");
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.correlationId()).isEqualTo("corr-1");
        assertThat(event.attributes()).isEqualTo(Map.of("source", "BYOK_USER"));
        assertThat(event.errorType()).isNull();
        assertThat(event.cost()).isNotNull();
        assertThat(event.cost().status()).isEqualTo(CostEstimate.Status.ESTIMATED);
        assertThat(event.cost().total()).isEqualTo(new BigDecimal("0.006000"));
        assertThat(event.cost().pricedAs()).isEqualTo("model-a-snapshot");
    }

    @Test
    @DisplayName("should leave cost null when no estimator is configured")
    void shouldLeaveCostNullWithoutEstimator() throws Exception {
        var noEstimator = new AiUsageTracker(recorder, null, Clock.fixed(START, ZoneOffset.UTC));

        noEstimator.track(ctx, REQUESTED, () -> AiCallResult.of(1, SERVED, TokenUsage.exact(1, 1)));

        assertThat(recorded).singleElement().satisfies(e -> {
            assertThat(e.cost()).isNull();
            assertThat(e.latency()).isEqualTo(Duration.ZERO);
        });
    }

    @Test
    @DisplayName("should record FAILED_TIMEOUT for a TimeoutException and rethrow it")
    void shouldRecordTimeout() {
        var timeout = new TimeoutException("deadline");

        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> { throw timeout; })).isSameAs(timeout);

        assertFailure(AiCallOutcome.FAILED_TIMEOUT, "TimeoutException");
    }

    @Test
    @DisplayName("should record FAILED_TIMEOUT when a timeout is buried in the cause chain")
    void shouldRecordTimeoutFromCauseChain() {
        var wrapped = new IllegalStateException("call failed", new RuntimeException(new TimeoutException("deadline")));

        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> { throw wrapped; })).isSameAs(wrapped);

        assertFailure(AiCallOutcome.FAILED_TIMEOUT, "IllegalStateException");
    }

    @Test
    @DisplayName("should record FAILED_TIMEOUT for an HttpTimeoutException")
    void shouldRecordHttpTimeout() {
        var wrapped = new UncheckedIOException(new HttpTimeoutException("request timed out"));

        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> { throw wrapped; })).isSameAs(wrapped);

        assertFailure(AiCallOutcome.FAILED_TIMEOUT, "UncheckedIOException");
    }

    @Test
    @DisplayName("should record INVALID_OUTPUT for InvalidModelOutputException and rethrow it")
    void shouldRecordInvalidOutput() {
        var invalid = new InvalidModelOutputException("not json");

        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> { throw invalid; })).isSameAs(invalid);

        assertFailure(AiCallOutcome.INVALID_OUTPUT, "InvalidModelOutputException");
    }

    @Test
    @DisplayName("should record REFUSED for ModelRefusedException and rethrow it")
    void shouldRecordRefused() {
        var refused = new ModelRefusedException("safety");

        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> { throw refused; })).isSameAs(refused);

        assertFailure(AiCallOutcome.REFUSED, "ModelRefusedException");
    }

    @Test
    @DisplayName("should record FAILED_UPSTREAM for any other exception, checked or not, and rethrow it")
    void shouldRecordUpstream() {
        var io = new IOException("connection reset");

        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> { throw io; })).isSameAs(io);

        assertFailure(AiCallOutcome.FAILED_UPSTREAM, "IOException");
    }

    @Test
    @DisplayName("should use ModelRef.unknown as requested model when the caller passes it")
    void shouldAcceptUnknownModel() {
        assertThatThrownBy(() -> tracker.track(ctx, ModelRef.unknown("provider"), () -> { throw new IOException(); }))
                .isInstanceOf(IOException.class);

        assertThat(recorded).singleElement().satisfies(e -> assertThat(e.model().isUnknown()).isTrue());
    }

    @Test
    @DisplayName("should never let a failing recorder mask the result or the exception")
    void shouldNotPropagateRecorderFailures() throws Exception {
        var broken = new AiUsageTracker(e -> { throw new IllegalStateException("ledger down"); }, estimator,
                Clock.fixed(START, ZoneOffset.UTC));
        var upstream = new IOException("boom");

        String value = broken.track(ctx, REQUESTED, () -> AiCallResult.of("ok", SERVED, TokenUsage.zero()));
        assertThatThrownBy(() -> broken.track(ctx, REQUESTED, () -> { throw upstream; })).isSameAs(upstream);

        assertThat(value).isEqualTo("ok");
    }

    @Test
    @DisplayName("should record the event without cost when the estimator fails")
    void shouldRecordWithoutCostWhenEstimatorFails() throws Exception {
        CostEstimator failing = (m, t) -> { throw new IllegalStateException("pricing down"); };
        var withFailingEstimator = new AiUsageTracker(recorder, failing, Clock.fixed(START, ZoneOffset.UTC));

        withFailingEstimator.track(ctx, REQUESTED, () -> AiCallResult.of("ok", SERVED, TokenUsage.exact(1, 1)));

        assertThat(recorded).singleElement().satisfies(e -> {
            assertThat(e.outcome()).isEqualTo(AiCallOutcome.OK);
            assertThat(e.cost()).isNull();
        });
    }

    @Test
    @DisplayName("should treat a null result as a failure of the call")
    void shouldTreatNullResultAsFailure() {
        assertThatThrownBy(() -> tracker.track(ctx, REQUESTED, () -> null)).isInstanceOf(NullPointerException.class);

        assertFailure(AiCallOutcome.FAILED_UPSTREAM, "NullPointerException");
    }

    @Test
    @DisplayName("classify should follow the documented order")
    void classifyShouldFollowOrder() {
        assertThat(AiUsageTracker.classify(new TimeoutException())).isEqualTo(AiCallOutcome.FAILED_TIMEOUT);
        assertThat(AiUsageTracker.classify(new InvalidModelOutputException("x"))).isEqualTo(AiCallOutcome.INVALID_OUTPUT);
        assertThat(AiUsageTracker.classify(new ModelRefusedException("x"))).isEqualTo(AiCallOutcome.REFUSED);
        assertThat(AiUsageTracker.classify(new RuntimeException("x"))).isEqualTo(AiCallOutcome.FAILED_UPSTREAM);
    }

    private void assertFailure(AiCallOutcome outcome, String errorType) {
        assertThat(recorded).hasSize(1);
        var event = recorded.get(0);
        assertThat(event.outcome()).isEqualTo(outcome);
        assertThat(event.model()).isEqualTo(REQUESTED);
        assertThat(event.tokens()).isEqualTo(TokenUsage.zero());
        assertThat(event.errorType()).isEqualTo(errorType);
        assertThat(event.cost()).isNull();
        assertThat(event.occurredAt()).isEqualTo(START);
        assertThat(event.latency()).isEqualTo(Duration.ofMillis(100));
        assertThat(event.purpose()).isEqualTo("summarize");
        assertThat(event.tenantId()).isEqualTo("tenant-1");
    }
}
