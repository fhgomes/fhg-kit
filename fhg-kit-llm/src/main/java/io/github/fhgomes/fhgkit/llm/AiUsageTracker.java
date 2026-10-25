package io.github.fhgomes.fhgkit.llm;

import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a model call, measures it, classifies its outcome and records an {@link AiUsageEvent}, so applications
 * stop re-implementing the same decorator.
 *
 * <pre>{@code
 * var tracker = new AiUsageTracker(recorder, estimator, Clock.systemUTC());
 * String answer = tracker.track(
 *         AiCallContext.builder("summarize").tenantId(tenant).build(),
 *         new ModelRef("openai", "gpt-4o-mini"),
 *         () -> {
 *             var response = client.chat(prompt);
 *             return AiCallResult.of(response.text(), response.model(), response.tokens());
 *         });
 * }</pre>
 *
 * <p>Outcome classification of an exception thrown by the call, in this order:
 * <ol>
 *   <li>{@link AiCallOutcome#FAILED_TIMEOUT} when the exception or any cause in its chain is a
 *       {@link TimeoutException} or an {@link HttpTimeoutException};</li>
 *   <li>{@link AiCallOutcome#INVALID_OUTPUT} for an {@link InvalidModelOutputException};</li>
 *   <li>{@link AiCallOutcome#REFUSED} for a {@link ModelRefusedException};</li>
 *   <li>{@link AiCallOutcome#FAILED_UPSTREAM} for anything else.</li>
 * </ol>
 * Failure events carry the requested model, zero tokens and the exception's simple class name as
 * {@link AiUsageEvent#errorType()}; the exception is always rethrown unchanged after the event is recorded.
 *
 * <p>Latency and {@link AiUsageEvent#occurredAt()} (the start of the call) are read from the injected
 * {@link Clock}. Recording goes through {@link AiUsageRecorders#safe(AiUsageRecorder)} and the cost estimator is
 * guarded the same way: a failing recorder or estimator is logged at WARN and never changes what the caller
 * gets back.
 *
 * <p>Thread-safe.
 */
public final class AiUsageTracker {

    private static final Logger LOG = LoggerFactory.getLogger(AiUsageTracker.class);

    private final AiUsageRecorder recorder;
    private final @Nullable CostEstimator estimator;
    private final Clock clock;

    /**
     * @param recorder  where events go; wrapped with {@link AiUsageRecorders#safe(AiUsageRecorder)}
     * @param estimator prices successful calls; {@code null} leaves {@link AiUsageEvent#cost()} null so the
     *                  consumer can price on read
     * @param clock     source of {@link AiUsageEvent#occurredAt()} and latency
     */
    public AiUsageTracker(AiUsageRecorder recorder, @Nullable CostEstimator estimator, Clock clock) {
        this.recorder = AiUsageRecorders.safe(Objects.requireNonNull(recorder, "recorder"));
        this.estimator = estimator;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Tracker with a cost estimator and the system UTC clock. */
    public AiUsageTracker(AiUsageRecorder recorder, CostEstimator estimator) {
        this(recorder, Objects.requireNonNull(estimator, "estimator"), Clock.systemUTC());
    }

    /** Tracker without cost snapshots, using the system UTC clock. */
    public AiUsageTracker(AiUsageRecorder recorder) {
        this(recorder, null, Clock.systemUTC());
    }

    /**
     * Runs the call, records one event whatever happens, and returns the call's value.
     *
     * @param ctx            purpose, tenant, user, correlation and attributes of the call
     * @param requestedModel the model being asked; used for failure events (pass {@link ModelRef#unknown(String)}
     *                       when it is not decided yet)
     * @param call           performs the model call and returns value, served model and tokens; must not return
     *                       null
     * @param <T>            the application's result type
     * @return {@link AiCallResult#value()} of what the call returned
     * @throws Exception whatever the call threw, unchanged, after the failure event was recorded
     */
    public <T> T track(AiCallContext ctx, ModelRef requestedModel, Callable<AiCallResult<T>> call) throws Exception {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(requestedModel, "requestedModel");
        Objects.requireNonNull(call, "call");
        Instant startedAt = clock.instant();
        AiCallResult<T> result;
        try {
            result = Objects.requireNonNull(call.call(), "the call returned null instead of an AiCallResult");
        } catch (Exception e) {
            recordFailure(ctx, requestedModel, startedAt, e);
            throw e;
        }
        recordSuccess(ctx, startedAt, result);
        return result.value();
    }

    private void recordSuccess(AiCallContext ctx, Instant startedAt, AiCallResult<?> result) {
        try {
            var event = eventFor(ctx, startedAt)
                    .model(result.model())
                    .outcome(AiCallOutcome.OK)
                    .tokens(result.tokens())
                    .cost(estimateQuietly(result.model(), result.tokens()))
                    .build();
            recorder.record(event);
        } catch (RuntimeException e) {
            LOG.warn("Could not record AI usage for purpose {} ({}: {})",
                    ctx.purpose(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void recordFailure(AiCallContext ctx, ModelRef requestedModel, Instant startedAt, Exception error) {
        try {
            var event = eventFor(ctx, startedAt)
                    .model(requestedModel)
                    .outcome(classify(error))
                    .tokens(TokenUsage.zero())
                    .errorType(error)
                    .build();
            recorder.record(event);
        } catch (RuntimeException e) {
            LOG.warn("Could not record failed AI usage for purpose {} ({}: {})",
                    ctx.purpose(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private AiUsageEvent.Builder eventFor(AiCallContext ctx, Instant startedAt) {
        return AiUsageEvent.builder(clock)
                .occurredAt(startedAt)
                .latency(Duration.between(startedAt, clock.instant()))
                .purpose(ctx.purpose())
                .tenantId(ctx.tenantId())
                .userId(ctx.userId())
                .correlationId(ctx.correlationId())
                .attributes(ctx.attributes());
    }

    private @Nullable CostEstimate estimateQuietly(ModelRef model, TokenUsage tokens) {
        if (estimator == null) {
            return null;
        }
        try {
            return estimator.estimate(model, tokens);
        } catch (RuntimeException e) {
            LOG.warn("Cost estimator {} failed for {} ({}: {}); recording the event without cost",
                    estimator, model.key(), e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Classifies an exception thrown by a model call (see the class documentation for the rules).
     *
     * @param error the exception
     * @return the outcome to record; never {@link AiCallOutcome#OK}
     */
    public static AiCallOutcome classify(Throwable error) {
        Objects.requireNonNull(error, "error");
        if (isTimeout(error)) {
            return AiCallOutcome.FAILED_TIMEOUT;
        }
        if (error instanceof InvalidModelOutputException) {
            return AiCallOutcome.INVALID_OUTPUT;
        }
        if (error instanceof ModelRefusedException) {
            return AiCallOutcome.REFUSED;
        }
        return AiCallOutcome.FAILED_UPSTREAM;
    }

    private static boolean isTimeout(Throwable error) {
        Set<Throwable> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = error; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof TimeoutException || t instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** The clock this tracker reads time from. */
    public Clock clock() {
        return clock;
    }
}
