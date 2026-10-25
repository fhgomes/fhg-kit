package io.github.fhgomes.fhgkit.llm;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One model call, after the fact. This is the row of an AI usage ledger.
 *
 * <p>The event is intentionally content-free: it never carries the prompt, the completion or an error message
 * body. Put a hash or a version of the prompt in {@link #attributes()} if you need to correlate; put the
 * exception's class name (not its message) in {@link #errorType()}.
 *
 * <p>Build one with {@link #builder()}; the canonical constructor validates every part.
 *
 * @param id            unique id of the event (random by default)
 * @param occurredAt    when the call happened
 * @param model         the model that served (or was asked to serve) the call
 * @param purpose       what the application wanted from the model, in its own vocabulary (never blank; map an
 *                      enum or a feature name here). Budgets and reports group by purpose.
 * @param outcome       how the call ended
 * @param tokens        tokens consumed; zero for failures unless known
 * @param latency       wall-clock time of the call, never negative
 * @param tenantId      the tenant (organization, workspace) the call is billed to, when multi-tenant
 * @param userId        the end user who triggered the call, when known; an opaque id, not an e-mail
 * @param correlationId request, trace or run id to join with the application's own logs
 * @param attributes    free-form dimensions, e.g. {@code source=BYOK_USER}, {@code turnIndex=3},
 *                      {@code promptHash=...}, {@code promptVersion=v2}, {@code operation=extract}. Never null,
 *                      copied on construction, unmodifiable
 * @param errorType     exception class simple name or an error category for failures; never a message body
 * @param cost          cost snapshot at record time, or {@code null} when the consumer prices events on read
 */
public record AiUsageEvent(
        UUID id,
        Instant occurredAt,
        ModelRef model,
        String purpose,
        AiCallOutcome outcome,
        TokenUsage tokens,
        Duration latency,
        @Nullable String tenantId,
        @Nullable String userId,
        @Nullable String correlationId,
        Map<String, String> attributes,
        @Nullable String errorType,
        @Nullable CostEstimate cost) {

    /**
     * Validates the event and copies {@code attributes} into an unmodifiable map.
     *
     * @throws NullPointerException     when a required part (or an attribute key or value) is null
     * @throws IllegalArgumentException when {@code purpose} is blank or {@code latency} is negative
     */
    public AiUsageEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(model, "model");
        purpose = Checks.requireNonBlank(purpose, "purpose");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(tokens, "tokens");
        Objects.requireNonNull(latency, "latency");
        if (latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative, got " + latency);
        }
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    /** Returns {@code true} when {@link #outcome()} is {@link AiCallOutcome#OK}. */
    public boolean isSuccess() {
        return outcome == AiCallOutcome.OK;
    }

    /** A builder using the system UTC clock for the default {@link #occurredAt()}. */
    public static Builder builder() {
        return new Builder(Clock.systemUTC());
    }

    /** A builder whose default {@link #occurredAt()} is read from the given clock (inject a fixed one in tests). */
    public static Builder builder(Clock clock) {
        return new Builder(clock);
    }

    /**
     * Fluent builder. Defaults: random {@code id}, {@code occurredAt} = clock instant at {@link #build()},
     * {@code tokens} = {@link TokenUsage#zero()}, {@code latency} = {@link Duration#ZERO}, empty attributes.
     * {@code model}, {@code purpose} and {@code outcome} have no default and must be set.
     */
    public static final class Builder {

        private final Clock clock;
        private @Nullable UUID id;
        private @Nullable Instant occurredAt;
        private @Nullable ModelRef model;
        private @Nullable String purpose;
        private @Nullable AiCallOutcome outcome;
        private TokenUsage tokens = TokenUsage.zero();
        private Duration latency = Duration.ZERO;
        private @Nullable String tenantId;
        private @Nullable String userId;
        private @Nullable String correlationId;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private @Nullable String errorType;
        private @Nullable CostEstimate cost;

        private Builder(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        /** Sets the event id; random when not set. */
        public Builder id(UUID id) {
            this.id = Objects.requireNonNull(id, "id");
            return this;
        }

        /** Sets when the call happened; the clock's instant at {@link #build()} when not set. */
        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
            return this;
        }

        /** Required. */
        public Builder model(ModelRef model) {
            this.model = Objects.requireNonNull(model, "model");
            return this;
        }

        /** Required. */
        public Builder purpose(String purpose) {
            this.purpose = purpose;
            return this;
        }

        /** Required. */
        public Builder outcome(AiCallOutcome outcome) {
            this.outcome = Objects.requireNonNull(outcome, "outcome");
            return this;
        }

        /** Defaults to {@link TokenUsage#zero()}. */
        public Builder tokens(TokenUsage tokens) {
            this.tokens = Objects.requireNonNull(tokens, "tokens");
            return this;
        }

        /** Defaults to {@link Duration#ZERO}. */
        public Builder latency(Duration latency) {
            this.latency = Objects.requireNonNull(latency, "latency");
            return this;
        }

        /** Optional. */
        public Builder tenantId(@Nullable String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /** Optional. */
        public Builder userId(@Nullable String userId) {
            this.userId = userId;
            return this;
        }

        /** Optional. */
        public Builder correlationId(@Nullable String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /** Adds one attribute, replacing a previous value for the same key. */
        public Builder attribute(String key, String value) {
            attributes.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** Adds every entry of the map, replacing previous values for the same keys. */
        public Builder attributes(Map<String, String> attributes) {
            Objects.requireNonNull(attributes, "attributes").forEach(this::attribute);
            return this;
        }

        /** Optional; the exception's simple class name or a category, never a message. */
        public Builder errorType(@Nullable String errorType) {
            this.errorType = errorType;
            return this;
        }

        /** Shortcut for {@code errorType(error.getClass().getSimpleName())}. */
        public Builder errorType(Throwable error) {
            this.errorType = Objects.requireNonNull(error, "error").getClass().getSimpleName();
            return this;
        }

        /** Optional cost snapshot. */
        public Builder cost(@Nullable CostEstimate cost) {
            this.cost = cost;
            return this;
        }

        /**
         * Creates the event, applying defaults for what was not set.
         *
         * @throws NullPointerException     when model, purpose or outcome was not set
         * @throws IllegalArgumentException when a value is invalid (see the canonical constructor)
         */
        public AiUsageEvent build() {
            return new AiUsageEvent(
                    id != null ? id : UUID.randomUUID(),
                    occurredAt != null ? occurredAt : clock.instant(),
                    Objects.requireNonNull(model, "model"),
                    Objects.requireNonNull(purpose, "purpose"),
                    Objects.requireNonNull(outcome, "outcome"),
                    tokens,
                    latency,
                    tenantId,
                    userId,
                    correlationId,
                    attributes,
                    errorType,
                    cost);
        }
    }
}
