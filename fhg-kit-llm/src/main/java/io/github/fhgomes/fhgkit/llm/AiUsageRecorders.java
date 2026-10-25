package io.github.fhgomes.fhgkit.llm;

import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ready-made {@link AiUsageRecorder} implementations and combinators.
 *
 * <p>All of them are stateless and thread-safe.
 */
public final class AiUsageRecorders {

    /**
     * Name of the logger used by {@link #logging()}: {@code io.github.fhgomes.fhgkit.llm.AiUsageRecorder}.
     * Route it to its own appender to get a plain-text usage ledger.
     */
    public static final String LOGGER_NAME = "io.github.fhgomes.fhgkit.llm.AiUsageRecorder";

    private static final Logger USAGE_LOG = LoggerFactory.getLogger(LOGGER_NAME);
    private static final Logger LOG = LoggerFactory.getLogger(AiUsageRecorders.class);

    private AiUsageRecorders() {
    }

    /** Discards every event. */
    public static AiUsageRecorder noop() {
        return Noop.INSTANCE;
    }

    /**
     * Logs one INFO line per event to {@value #LOGGER_NAME}, with every field of the event and no prompt
     * content. Useful as the default before an application plugs in a persistent ledger.
     */
    public static AiUsageRecorder logging() {
        return Logging.INSTANCE;
    }

    /**
     * Calls every delegate in order, for every event. A delegate that throws does not prevent the following ones
     * from being called; the first exception is rethrown after all delegates ran. Wrap the result with
     * {@link #safe(AiUsageRecorder)} when it must never throw.
     *
     * @param delegates the recorders to fan out to; none may be null
     */
    public static AiUsageRecorder composite(AiUsageRecorder... delegates) {
        Objects.requireNonNull(delegates, "delegates");
        var copy = List.of(delegates); // rejects null elements
        if (copy.size() == 1) {
            return copy.get(0);
        }
        return new Composite(copy);
    }

    /**
     * Wraps a recorder so that any {@link RuntimeException} it throws is logged at WARN and swallowed. Accounting
     * must never break the model call that it accounts for.
     *
     * @param delegate the recorder to protect
     */
    public static AiUsageRecorder safe(AiUsageRecorder delegate) {
        Objects.requireNonNull(delegate, "delegate");
        if (delegate instanceof Safe || delegate instanceof Noop) {
            return delegate;
        }
        return new Safe(delegate);
    }

    private enum Noop implements AiUsageRecorder {
        INSTANCE;

        @Override
        public void record(AiUsageEvent event) {
            Objects.requireNonNull(event, "event");
        }

        @Override
        public String toString() {
            return "AiUsageRecorders.noop()";
        }
    }

    private enum Logging implements AiUsageRecorder {
        INSTANCE;

        @Override
        public void record(AiUsageEvent event) {
            Objects.requireNonNull(event, "event");
            if (!USAGE_LOG.isInfoEnabled()) {
                return;
            }
            var cost = event.cost();
            USAGE_LOG.info(
                    "ai-usage id={} at={} purpose={} model={} outcome={} tokensIn={} tokensOut={} estimated={} "
                            + "latencyMs={} tenant={} user={} correlation={} errorType={} cost={} costStatus={} "
                            + "currency={} pricedAs={} attributes={}",
                    event.id(),
                    event.occurredAt(),
                    event.purpose(),
                    event.model().key(),
                    event.outcome(),
                    event.tokens().input(),
                    event.tokens().output(),
                    event.tokens().estimated(),
                    event.latency().toMillis(),
                    event.tenantId(),
                    event.userId(),
                    event.correlationId(),
                    event.errorType(),
                    cost != null ? cost.total() : null,
                    cost != null ? cost.status() : null,
                    cost != null ? cost.currency() : null,
                    cost != null ? cost.pricedAs() : null,
                    event.attributes());
        }

        @Override
        public String toString() {
            return "AiUsageRecorders.logging()";
        }
    }

    private static final class Composite implements AiUsageRecorder {

        private final List<AiUsageRecorder> delegates;

        private Composite(List<AiUsageRecorder> delegates) {
            this.delegates = delegates;
        }

        @Override
        public void record(AiUsageEvent event) {
            Objects.requireNonNull(event, "event");
            RuntimeException first = null;
            for (AiUsageRecorder delegate : delegates) {
                try {
                    delegate.record(event);
                } catch (RuntimeException e) {
                    if (first == null) {
                        first = e;
                    } else {
                        first.addSuppressed(e);
                    }
                }
            }
            if (first != null) {
                throw first;
            }
        }

        @Override
        public String toString() {
            return "AiUsageRecorders.composite(" + delegates + ")";
        }
    }

    private static final class Safe implements AiUsageRecorder {

        private final AiUsageRecorder delegate;

        private Safe(AiUsageRecorder delegate) {
            this.delegate = delegate;
        }

        @Override
        public void record(AiUsageEvent event) {
            try {
                delegate.record(event);
            } catch (RuntimeException e) {
                LOG.warn("AI usage recorder {} failed for event {} ({}: {}); the event is lost",
                        delegate, event.id(), e.getClass().getSimpleName(), e.getMessage());
            }
        }

        @Override
        public String toString() {
            return "AiUsageRecorders.safe(" + delegate + ")";
        }
    }
}
